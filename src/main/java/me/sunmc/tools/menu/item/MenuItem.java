package me.sunmc.tools.menu.item;

import me.sunmc.tools.utils.bukkit.SoundUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Enhanced menu item with advanced features:
 * - Click type specific actions (left, right, shift, etc.)
 * - Conditional visibility and click handling
 * - Sound effects on interaction
 * - Update callbacks for dynamic items
 * - State management
 *
 * @version 1.0.0
 */
public class MenuItem {

    private @NonNull ItemStack itemStack;
    private @NonNull Consumer<MenuItemClickEvent> clickAction;
    private final @NonNull Map<ClickType, Consumer<MenuItemClickEvent>> clickTypeActions;
    private @Nullable Predicate<Player> visibilityCondition;
    private @Nullable Predicate<Player> clickCondition;
    private @Nullable Sound clickSound;
    private float soundVolume = 1.0f;
    private float soundPitch = 1.0f;
    private @Nullable Consumer<MenuItem> updateCallback;
    private boolean closeOnClick = false;
    private boolean animated = false;
    private long lastUpdate = 0;

    /**
     * Creates a new MenuItem with the specified ItemStack.
     *
     * @param itemStack The ItemStack to display.
     */
    public MenuItem(@NonNull ItemStack itemStack) {
        this.itemStack = itemStack;
        this.clickAction = event -> {};
        this.clickTypeActions = new EnumMap<>(ClickType.class);
    }

    /**
     * Creates a new MenuItem with the specified ItemStack and click action.
     *
     * @param itemStack   The ItemStack to display.
     * @param clickAction The action to perform when clicked.
     */
    public MenuItem(@NonNull ItemStack itemStack, @NonNull Consumer<MenuItemClickEvent> clickAction) {
        this.itemStack = itemStack;
        this.clickAction = clickAction;
        this.clickTypeActions = new EnumMap<>(ClickType.class);
    }

    /**
     * Sets the click action for this menu item.
     *
     * @param clickAction The action to perform when clicked.
     * @return This MenuItem instance for method chaining.
     */
    public @NonNull MenuItem setClickAction(@NonNull Consumer<MenuItemClickEvent> clickAction) {
        this.clickAction = clickAction;
        return this;
    }

    /**
     * Sets a click action for a specific click type.
     *
     * @param clickType   The type of click.
     * @param clickAction The action to perform.
     * @return This MenuItem instance for method chaining.
     */
    public @NonNull MenuItem setClickAction(@NonNull ClickType clickType, @NonNull Consumer<MenuItemClickEvent> clickAction) {
        this.clickTypeActions.put(clickType, clickAction);
        return this;
    }

    /**
     * Sets the action for left clicks only.
     *
     * @param clickAction The action to perform.
     * @return This MenuItem instance for method chaining.
     */
    public @NonNull MenuItem onLeftClick(@NonNull Consumer<MenuItemClickEvent> clickAction) {
        return this.setClickAction(ClickType.LEFT, clickAction);
    }

    /**
     * Sets the action for right clicks only.
     *
     * @param clickAction The action to perform.
     * @return This MenuItem instance for method chaining.
     */
    public @NonNull MenuItem onRightClick(@NonNull Consumer<MenuItemClickEvent> clickAction) {
        return this.setClickAction(ClickType.RIGHT, clickAction);
    }

    /**
     * Sets the action for shift-left clicks only.
     *
     * @param clickAction The action to perform.
     * @return This MenuItem instance for method chaining.
     */
    public @NonNull MenuItem onShiftLeftClick(@NonNull Consumer<MenuItemClickEvent> clickAction) {
        return this.setClickAction(ClickType.SHIFT_LEFT, clickAction);
    }

    /**
     * Sets the action for shift-right clicks only.
     *
     * @param clickAction The action to perform.
     * @return This MenuItem instance for method chaining.
     */
    public @NonNull MenuItem onShiftRightClick(@NonNull Consumer<MenuItemClickEvent> clickAction) {
        return this.setClickAction(ClickType.SHIFT_RIGHT, clickAction);
    }

    /**
     * Sets a condition that determines if this item is visible to a player.
     *
     * @param condition The visibility condition.
     * @return This MenuItem instance for method chaining.
     */
    public @NonNull MenuItem setVisibilityCondition(@NonNull Predicate<Player> condition) {
        this.visibilityCondition = condition;
        return this;
    }

    /**
     * Sets a condition that determines if a player can click this item.
     *
     * @param condition The click condition.
     * @return This MenuItem instance for method chaining.
     */
    public @NonNull MenuItem setClickCondition(@NonNull Predicate<Player> condition) {
        this.clickCondition = condition;
        return this;
    }

    /**
     * Sets the sound to play when this item is clicked.
     *
     * @param sound  The sound to play.
     * @param volume The volume (0.0 - 1.0).
     * @param pitch  The pitch (0.5 - 2.0).
     * @return This MenuItem instance for method chaining.
     */
    public @NonNull MenuItem setClickSound(@NonNull Sound sound, float volume, float pitch) {
        this.clickSound = sound;
        this.soundVolume = volume;
        this.soundPitch = pitch;
        return this;
    }

    /**
     * Sets the sound to play when this item is clicked.
     *
     * @param sound The sound to play (default volume and pitch).
     * @return This MenuItem instance for method chaining.
     */
    public @NonNull MenuItem setClickSound(@NonNull Sound sound) {
        return this.setClickSound(sound, 1.0f, 1.0f);
    }

    /**
     * Sets an update callback that is called when the item needs to be updated.
     *
     * @param callback The update callback.
     * @return This MenuItem instance for method chaining.
     */
    public @NonNull MenuItem setUpdateCallback(@NonNull Consumer<MenuItem> callback) {
        this.updateCallback = callback;
        return this;
    }

    /**
     * Sets whether clicking this item should close the menu.
     *
     * @param closeOnClick True to close on click, false otherwise.
     * @return This MenuItem instance for method chaining.
     */
    public @NonNull MenuItem setCloseOnClick(boolean closeOnClick) {
        this.closeOnClick = closeOnClick;
        return this;
    }

    /**
     * Marks this item as animated.
     * Animated items will have their update callback called periodically.
     *
     * @param animated True to enable animation, false otherwise.
     * @return This MenuItem instance for method chaining.
     */
    public @NonNull MenuItem setAnimated(boolean animated) {
        this.animated = animated;
        return this;
    }

    /**
     * Updates the ItemStack for this menu item.
     *
     * @param itemStack The new ItemStack.
     * @return This MenuItem instance for method chaining.
     */
    public @NonNull MenuItem setItemStack(@NonNull ItemStack itemStack) {
        this.itemStack = itemStack;
        this.lastUpdate = System.currentTimeMillis();
        return this;
    }

    /**
     * Checks if this item is visible to the specified player.
     *
     * @param player The player to check.
     * @return True if visible, false otherwise.
     */
    public boolean isVisible(@NonNull Player player) {
        return this.visibilityCondition == null || this.visibilityCondition.test(player);
    }

    /**
     * Checks if the specified player can click this item.
     *
     * @param player The player to check.
     * @return True if clickable, false otherwise.
     */
    public boolean isClickable(@NonNull Player player) {
        return this.clickCondition == null || this.clickCondition.test(player);
    }

    /**
     * Updates this item if it has an update callback.
     */
    public void update() {
        if (this.updateCallback != null) {
            this.updateCallback.accept(this);
            this.lastUpdate = System.currentTimeMillis();
        }
    }

    /**
     * Called when this menu item is clicked.
     *
     * @param player The player who clicked.
     * @param event  The inventory click event.
     */
    public void onClick(@NonNull Player player, @NonNull InventoryClickEvent event) {
        // Check if player can click
        if (!this.isClickable(player)) {
            event.setCancelled(true);
            return;
        }

        MenuItemClickEvent clickEvent = new MenuItemClickEvent(player, event, this);

        // Play sound if configured
        if (this.clickSound != null) {
            SoundUtil.playSound(player, this.clickSound, this.soundVolume, this.soundPitch);
        }

        // Execute click type specific action if available
        Consumer<MenuItemClickEvent> typeAction = this.clickTypeActions.get(event.getClick());
        if (typeAction != null) {
            typeAction.accept(clickEvent);
        } else {
            // Execute general click action
            this.clickAction.accept(clickEvent);
        }

        // Close menu if configured
        if (this.closeOnClick) {
            player.closeInventory();
        }
    }

    /**
     * Gets the ItemStack for this menu item.
     *
     * @return The ItemStack.
     */
    public @NonNull ItemStack getItemStack() {
        return this.itemStack;
    }

    /**
     * Gets whether this item is animated.
     *
     * @return True if animated, false otherwise.
     */
    public boolean isAnimated() {
        return this.animated;
    }

    /**
     * Gets the timestamp of the last update.
     *
     * @return The last update timestamp in milliseconds.
     */
    public long getLastUpdate() {
        return this.lastUpdate;
    }

    /**
     * Creates a simple menu item that does nothing when clicked.
     *
     * @param itemStack The ItemStack to display.
     * @return A new MenuItem.
     */
    public static @NonNull MenuItem of(@NonNull ItemStack itemStack) {
        return new MenuItem(itemStack);
    }

    /**
     * Creates a menu item with a click action.
     *
     * @param itemStack   The ItemStack to display.
     * @param clickAction The action to perform when clicked.
     * @return A new MenuItem.
     */
    public static @NonNull MenuItem of(@NonNull ItemStack itemStack, @NonNull Consumer<MenuItemClickEvent> clickAction) {
        return new MenuItem(itemStack, clickAction);
    }

    /**
     * Creates a menu item from a Material.
     *
     * @param material The material to use.
     * @return A new MenuItem.
     */
    public static @NonNull MenuItem of(@NonNull Material material) {
        return new MenuItem(new ItemStack(material));
    }

    /**
     * Creates an empty/placeholder menu item.
     *
     * @param itemStack The ItemStack to use as placeholder.
     * @return A new MenuItem that cancels all clicks.
     */
    public static @NonNull MenuItem placeholder(@NonNull ItemStack itemStack) {
        return new MenuItem(itemStack, event -> event.setCancelled(true));
    }

    /**
     * Creates a back button menu item.
     *
     * @return A new MenuItem configured as a back button.
     */
    public static @NonNull MenuItem backButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        item.editMeta(meta -> {
            meta.displayName(Component.text("← Back"));
        });

        return new MenuItem(item)
                .setClickSound(Sound.UI_BUTTON_CLICK)
                .setCloseOnClick(false);
    }

    /**
     * Creates a close button menu item.
     *
     * @return A new MenuItem configured as a close button.
     */
    public static @NonNull MenuItem closeButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        item.editMeta(meta -> {
            meta.displayName(Component.text("✖ Close", net.kyori.adventure.text.format.NamedTextColor.RED));
        });

        return new MenuItem(item)
                .setClickSound(Sound.UI_BUTTON_CLICK)
                .setCloseOnClick(true);
    }
}