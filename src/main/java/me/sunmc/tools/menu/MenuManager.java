package me.sunmc.tools.menu;

import me.sunmc.tools.component.Component;
import me.sunmc.tools.registry.AutoRegister;
import me.sunmc.tools.utils.java.SinglePointInitiator;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manager for handling open menus and tracking which players have which menus open.
 * This is a singleton component that integrates with the library's component system.
 */
@AutoRegister(Component.class)
public class MenuManager extends SinglePointInitiator implements Component {

    private static MenuManager instance;

    private final @NonNull Map<UUID, Menu> openMenus;

    public MenuManager() {
        instance = this;
        this.openMenus = new HashMap<>();
    }

    /**
     * Gets the singleton instance of the MenuManager.
     *
     * @return The MenuManager instance.
     */
    public static @NonNull MenuManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("MenuManager has not been initialized yet");
        }
        return instance;
    }

    /**
     * Registers a menu as open for a player.
     *
     * @param player The player.
     * @param menu   The menu.
     */
    public void registerMenu(@NonNull Player player, @NonNull Menu menu) {
        this.openMenus.put(player.getUniqueId(), menu);
    }

    /**
     * Unregisters a player's open menu.
     *
     * @param player The player.
     */
    public void unregisterMenu(@NonNull Player player) {
        this.openMenus.remove(player.getUniqueId());
    }

    /**
     * Gets the menu that a player currently has open.
     *
     * @param player The player.
     * @return The player's open menu, or null if they don't have one open.
     */
    public @Nullable Menu getOpenMenu(@NonNull Player player) {
        return this.openMenus.get(player.getUniqueId());
    }

    /**
     * Checks if a player has a menu open.
     *
     * @param player The player.
     * @return True if the player has a menu open, false otherwise.
     */
    public boolean hasMenuOpen(@NonNull Player player) {
        return this.openMenus.containsKey(player.getUniqueId());
    }

    /**
     * Closes all open menus.
     */
    public void closeAllMenus() {
        this.openMenus.values().forEach(Menu::close);
        this.openMenus.clear();
    }

    /**
     * Gets all currently open menus.
     *
     * @return Map of player UUIDs to their open menus.
     */
    public @NonNull Map<UUID, Menu> getOpenMenus() {
        return new HashMap<>(this.openMenus);
    }

    @Override
    public void onDisable() {
        this.closeAllMenus();
    }
}