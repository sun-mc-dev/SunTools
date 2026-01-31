package me.sunmc.tools.menu.item;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Event wrapper for menu item clicks.
 * Provides convenient access to click information and player data.
 */
public record MenuItemClickEvent(@NonNull Player player, @NonNull InventoryClickEvent bukkitEvent,
                                 @NonNull MenuItem menuItem) {

    /**
     * Gets the player who clicked the item.
     *
     * @return The player.
     */
    @Override
    public @NonNull Player player() {
        return this.player;
    }

    /**
     * Gets the underlying Bukkit inventory click event.
     *
     * @return The Bukkit event.
     */
    @Override
    public @NonNull InventoryClickEvent bukkitEvent() {
        return this.bukkitEvent;
    }

    /**
     * Gets the menu item that was clicked.
     *
     * @return The menu item.
     */
    @Override
    public @NonNull MenuItem menuItem() {
        return this.menuItem;
    }

    /**
     * Gets the slot that was clicked.
     *
     * @return The slot number.
     */
    public int getSlot() {
        return this.bukkitEvent.getSlot();
    }

    /**
     * Gets the click type.
     *
     * @return The click type.
     */
    public @NonNull ClickType getClickType() {
        return this.bukkitEvent.getClick();
    }

    /**
     * Checks if the click was a left click.
     *
     * @return True if left click, false otherwise.
     */
    public boolean isLeftClick() {
        return this.bukkitEvent.isLeftClick();
    }

    /**
     * Checks if the click was a right click.
     *
     * @return True if right click, false otherwise.
     */
    public boolean isRightClick() {
        return this.bukkitEvent.isRightClick();
    }

    /**
     * Checks if the click was a shift click.
     *
     * @return True if shift click, false otherwise.
     */
    public boolean isShiftClick() {
        return this.bukkitEvent.isShiftClick();
    }

    /**
     * Cancels the event.
     */
    public void setCancelled(boolean cancelled) {
        this.bukkitEvent.setCancelled(cancelled);
    }

    /**
     * Checks if the event is cancelled.
     *
     * @return True if cancelled, false otherwise.
     */
    public boolean isCancelled() {
        return this.bukkitEvent.isCancelled();
    }
}