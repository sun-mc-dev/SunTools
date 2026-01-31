package me.sunmc.tools.menu.input;

import me.sunmc.tools.Tools;
import me.sunmc.tools.menu.Menu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Abstract menu that supports chat input from players.
 * When input is requested, the menu closes and listens for the next chat message.
 * After receiving input, the menu can reopen or perform custom actions.
 *
 * <p>Example usage:
 * <pre>{@code
 * public class NameInputMenu extends InputMenu {
 *     public NameInputMenu(Player viewer) {
 *         super(viewer);
 *     }
 *
 *     @Override
 *     public void init() {
 *         // Add item that requests input
 *         setItem(13, MenuItem.of(item, event -> {
 *             requestInput("Enter your name:", input -> {
 *                 player.sendMessage("You entered: " + input);
 *                 open(); // Reopen menu
 *             });
 *         }));
 *     }
 * }
 * }</pre>
 *
 * @version 1.0.0
 */
public abstract class InputMenu extends Menu {

    private static final @NonNull Map<UUID, InputSession> activeSessions = new ConcurrentHashMap<>();
    private static @Nullable InputListener listener;

    /**
     * Constructs a new InputMenu for the specified player.
     *
     * @param viewer The player who will view this menu.
     */
    public InputMenu(@NonNull Player viewer) {
        super(viewer);
        ensureListenerRegistered();
    }

    /**
     * Requests text input from the player.
     * The menu will close and wait for the player's next chat message.
     *
     * @param prompt   The prompt to display to the player.
     * @param callback The callback to handle the input.
     */
    protected void requestInput(@NonNull String prompt, @NonNull Consumer<String> callback) {
        this.requestInput(Component.text(prompt), callback, null);
    }

    /**
     * Requests text input from the player with validation.
     *
     * @param prompt    The prompt to display to the player.
     * @param callback  The callback to handle valid input.
     * @param validator The validator for the input (null to accept all).
     */
    protected void requestInput(@NonNull String prompt, @NonNull Consumer<String> callback,
                                @Nullable Predicate<String> validator) {
        this.requestInput(Component.text(prompt), callback, validator);
    }

    /**
     * Requests text input from the player.
     *
     * @param prompt   The prompt component to display.
     * @param callback The callback to handle the input.
     */
    protected void requestInput(@NonNull Component prompt, @NonNull Consumer<String> callback) {
        this.requestInput(prompt, callback, null);
    }

    /**
     * Requests text input from the player with validation.
     *
     * @param prompt    The prompt component to display.
     * @param callback  The callback to handle valid input.
     * @param validator The validator for the input (null to accept all).
     */
    protected void requestInput(@NonNull Component prompt, @NonNull Consumer<String> callback,
                                @Nullable Predicate<String> validator) {
        Player player = this.getViewer();
        if (player == null) {
            return;
        }

        // Close current menu
        this.close();

        // Display prompt
        player.sendMessage(Component.empty());
        player.sendMessage(prompt);
        player.sendMessage(Component.text("Type 'cancel' to cancel", NamedTextColor.GRAY));
        player.sendMessage(Component.empty());

        // Create and register session
        InputSession session = new InputSession(
                player.getUniqueId(),
                callback,
                validator,
                this
        );
        activeSessions.put(player.getUniqueId(), session);
    }

    /**
     * Cancels any active input session for this menu's viewer.
     */
    protected void cancelInput() {
        Player player = this.getViewer();
        if (player != null) {
            activeSessions.remove(player.getUniqueId());
            player.sendMessage(Component.text("Input cancelled.", NamedTextColor.RED));
        }
    }

    /**
     * Called when input is successfully received and validated.
     * Override to add custom behavior after input.
     *
     * @param input The received input.
     */
    protected void onInputReceived(@NonNull String input) {
    }

    /**
     * Called when input is cancelled by the player.
     * Override to add custom behavior after cancellation.
     */
    protected void onInputCancelled() {
    }

    /**
     * Called when input fails validation.
     * Override to add custom behavior for invalid input.
     *
     * @param input The invalid input.
     */
    protected void onInputInvalid(@NonNull String input) {
        Player player = this.getViewer();
        if (player != null) {
            player.sendMessage(Component.text("Invalid input. Please try again.", NamedTextColor.RED));
        }
    }

    /**
     * Ensures the input listener is registered.
     */
    private static void ensureListenerRegistered() {
        if (listener == null) {
            listener = new InputListener();
            Tools.getInstance().getServer().getPluginManager()
                    .registerEvents(listener, Tools.getInstance());
        }
    }

    /**
     * Unregisters the input listener.
     * This should be called when the plugin is disabled.
     */
    public static void unregisterListener() {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        activeSessions.clear();
    }

    /**
         * Represents an active input session.
         */
        private record InputSession(@NonNull UUID playerId, @NonNull Consumer<String> callback,
                                    @Nullable Predicate<String> validator, @NonNull InputMenu menu) {

        public boolean validateAndProcess(@NonNull String input) {
                if (validator != null && !validator.test(input)) {
                    menu.onInputInvalid(input);
                    return false;
                }

                callback.accept(input);
                menu.onInputReceived(input);
                return true;
            }
        }

    /**
     * Listener for handling chat input.
     */
    private static class InputListener implements Listener {

        @EventHandler(priority = EventPriority.LOWEST)
        public void onChat(@NonNull AsyncPlayerChatEvent event) {
            Player player = event.getPlayer();
            InputSession session = activeSessions.get(player.getUniqueId());

            if (session == null) {
                return;
            }

            event.setCancelled(true);
            String message = event.getMessage();

            // Handle cancellation
            if (message.equalsIgnoreCase("cancel")) {
                activeSessions.remove(player.getUniqueId());
                Tools.getInstance().getSchedulerAdapter().executeSync(() -> {
                    player.sendMessage(Component.text("Input cancelled.", NamedTextColor.RED));
                    session.menu().onInputCancelled();
                });
                return;
            }

            // Validate and process input
            Tools.getInstance().getSchedulerAdapter().executeSync(() -> {
                if (session.validateAndProcess(message)) {
                    activeSessions.remove(player.getUniqueId());
                }
            });
        }

        @EventHandler
        public void onQuit(@NonNull PlayerQuitEvent event) {
            activeSessions.remove(event.getPlayer().getUniqueId());
        }
    }
}