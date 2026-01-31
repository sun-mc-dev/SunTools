package me.sunmc.tools.menu;

import me.sunmc.tools.Tools;
import me.sunmc.tools.menu.item.MenuItem;
import me.sunmc.tools.menu.pattern.MenuPattern;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Enhanced base class for creating custom GUI menus in Minecraft.
 * Provides advanced features including:
 * - Auto-updating menus with configurable intervals
 * - Menu history/back navigation
 * - Async initialization support
 * - Menu actions and callbacks
 * - Sound effects on interactions
 * - Close guards to prevent accidental closes
 *
 * @version 1.0.0
 */
public abstract class Menu implements InventoryHolder {

    private final @NonNull Map<Integer, MenuItem> items;
    private final @NonNull UUID viewerId;
    private final @NonNull List<Consumer<Player>> openActions;
    private final @NonNull List<Consumer<Player>> closeActions;
    private @Nullable Inventory inventory;
    private boolean allowItemMovement = false;
    private boolean autoRefresh = false;
    private int refreshInterval = 20; // ticks
    private @Nullable Integer taskId;
    private @Nullable Menu previousMenu;
    private boolean preventClose = false;
    private boolean playSounds = true;
    private long lastInteraction = 0;
    private static final long INTERACTION_COOLDOWN = 100; // milliseconds

    /**
     * Constructs a new Menu for the specified player.
     *
     * @param viewer The player who will view this menu.
     */
    public Menu(@NonNull Player viewer) {
        this.viewerId = viewer.getUniqueId();
        this.items = new ConcurrentHashMap<>();
        this.openActions = new ArrayList<>();
        this.closeActions = new ArrayList<>();
    }

    /**
     * Gets the title of the menu.
     * This method must be implemented by subclasses to provide the menu title.
     *
     * @return The menu title as a Component.
     */
    public abstract @NonNull Component getTitle();

    /**
     * Gets the number of rows in the menu.
     * Must be between 1 and 6 for chest inventories.
     *
     * @return The number of rows.
     */
    public abstract int getRows();

    /**
     * Gets the inventory type for this menu.
     * By default, returns CHEST. Override to use other inventory types.
     *
     * @return The inventory type.
     */
    public @NonNull InventoryType getInventoryType() {
        return InventoryType.CHEST;
    }

    /**
     * Initializes the menu content.
     * This method is called when the menu is opened and should be used
     * to set up all menu items and content.
     */
    public abstract void init();

    /**
     * Called when the menu is opened.
     * Override to add custom behavior when the menu opens.
     *
     * @param player The player opening the menu.
     */
    public void onOpen(@NonNull Player player) {
        this.openActions.forEach(action -> action.accept(player));
    }

    /**
     * Called when the menu is closed.
     * Override to add custom behavior when the menu closes.
     *
     * @param player The player closing the menu.
     * @param event  The inventory close event.
     */
    public void onClose(@NonNull Player player, @NonNull InventoryCloseEvent event) {
        this.closeActions.forEach(action -> action.accept(player));
    }

    /**
     * Called when an item in the menu is clicked.
     * Override to add custom behavior for unhandled clicks.
     *
     * @param player The player who clicked.
     * @param slot   The slot that was clicked.
     * @param event  The inventory click event.
     */
    public void onClick(@NonNull Player player, int slot, @NonNull InventoryClickEvent event) {
    }

    /**
     * Opens the menu for the viewer.
     * If async initialization is enabled, the menu will be prepared in the background.
     */
    public void open() {
        Player player = this.getViewer();
        if (player == null) {
            return;
        }

        this.inventory = this.createInventory();
        this.init();
        this.render();

        player.openInventory(this.inventory);
        this.onOpen(player);

        MenuManager.getInstance().registerMenu(player, this);

        if (this.autoRefresh) {
            this.startAutoRefresh();
        }
    }

    /**
     * Opens the menu asynchronously.
     * Performs initialization in the background before opening.
     */
    public void openAsync() {
        Player player = this.getViewer();
        if (player == null) {
            return;
        }

        Tools.getInstance().getSchedulerAdapter().executeAsync(() -> {
            this.inventory = this.createInventory();
            this.init();

            Tools.getInstance().getSchedulerAdapter().executeSync(() -> {
                this.render();
                player.openInventory(this.inventory);
                this.onOpen(player);
                MenuManager.getInstance().registerMenu(player, this);

                if (this.autoRefresh) {
                    this.startAutoRefresh();
                }
            });
        });
    }

    /**
     * Closes the menu for the viewer.
     */
    public void close() {
        this.stopAutoRefresh();
        Player player = this.getViewer();
        if (player != null) {
            player.closeInventory();
        }
    }

    /**
     * Refreshes the menu content without closing it.
     */
    public void refresh() {
        if (this.inventory != null) {
            this.inventory.clear();
            this.init();
            this.render();
        }
    }

    /**
     * Sets a menu item at the specified slot.
     *
     * @param slot     The slot to set the item in.
     * @param menuItem The menu item to set.
     */
    public void setItem(int slot, @NonNull MenuItem menuItem) {
        this.items.put(slot, menuItem);
    }

    /**
     * Sets a menu item at the specified row and column.
     *
     * @param row      The row (0-indexed).
     * @param column   The column (0-indexed).
     * @param menuItem The menu item to set.
     */
    public void setItem(int row, int column, @NonNull MenuItem menuItem) {
        this.setItem(row * 9 + column, menuItem);
    }

    /**
     * Fills the entire menu with the specified menu item.
     *
     * @param menuItem The menu item to fill with.
     */
    public void fill(@NonNull MenuItem menuItem) {
        int size = this.getRows() * 9;
        for (int i = 0; i < size; i++) {
            this.setItem(i, menuItem);
        }
    }

    /**
     * Fills the border of the menu with the specified menu item.
     *
     * @param menuItem The menu item to use for the border.
     */
    public void fillBorder(@NonNull MenuItem menuItem) {
        int rows = this.getRows();
        int lastRow = rows - 1;

        for (int col = 0; col < 9; col++) {
            this.setItem(0, col, menuItem);
            this.setItem(lastRow, col, menuItem);
        }

        for (int row = 1; row < lastRow; row++) {
            this.setItem(row, 0, menuItem);
            this.setItem(row, 8, menuItem);
        }
    }

    /**
     * Fills a rectangular area with the specified menu item.
     *
     * @param startRow The starting row (inclusive).
     * @param startCol The starting column (inclusive).
     * @param endRow   The ending row (inclusive).
     * @param endCol   The ending column (inclusive).
     * @param menuItem The menu item to fill with.
     */
    public void fillRectangle(int startRow, int startCol, int endRow, int endCol, @NonNull MenuItem menuItem) {
        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                this.setItem(row, col, menuItem);
            }
        }
    }

    /**
     * Applies a menu pattern to this menu.
     *
     * @param pattern The pattern to apply.
     * @see MenuPattern
     */
    public void applyPattern(@NonNull MenuPattern pattern) {
        pattern.apply(this);
    }

    /**
     * Removes the menu item at the specified slot.
     *
     * @param slot The slot to remove the item from.
     */
    public void removeItem(int slot) {
        this.items.remove(slot);
        if (this.inventory != null) {
            this.inventory.setItem(slot, null);
        }
    }

    /**
     * Gets the menu item at the specified slot.
     *
     * @param slot The slot to get the item from.
     * @return The menu item, or null if no item exists at that slot.
     */
    public @Nullable MenuItem getMenuItem(int slot) {
        return this.items.get(slot);
    }

    /**
     * Handles a click on a menu item with cooldown protection.
     *
     * @param slot  The slot that was clicked.
     * @param event The inventory click event.
     */
    public void handleClick(int slot, @NonNull InventoryClickEvent event) {
        // Cooldown check to prevent spam clicking
        long now = System.currentTimeMillis();
        if (now - this.lastInteraction < INTERACTION_COOLDOWN) {
            event.setCancelled(true);
            return;
        }
        this.lastInteraction = now;

        Player player = (Player) event.getWhoClicked();

        if (!this.allowItemMovement) {
            event.setCancelled(true);
        }

        MenuItem menuItem = this.items.get(slot);
        if (menuItem != null) {
            menuItem.onClick(player, event);
        }

        this.onClick(player, slot, event);
    }

    /**
     * Handles the menu being closed.
     *
     * @param event The inventory close event.
     */
    public void handleClose(@NonNull InventoryCloseEvent event) {
        if (this.preventClose) {
            // Reopen the menu after a tick
            Player player = (Player) event.getPlayer();
            Tools.getInstance().getSchedulerAdapter().syncLater(() -> {
                this.open();
            }, 1, java.util.concurrent.TimeUnit.MILLISECONDS);
            return;
        }

        this.stopAutoRefresh();
        Player player = (Player) event.getPlayer();
        this.onClose(player, event);
        MenuManager.getInstance().unregisterMenu(player);
    }

    /**
     * Adds an action to execute when the menu opens.
     *
     * @param action The action to execute.
     * @return This menu instance for chaining.
     */
    public @NonNull Menu addOpenAction(@NonNull Consumer<Player> action) {
        this.openActions.add(action);
        return this;
    }

    /**
     * Adds an action to execute when the menu closes.
     *
     * @param action The action to execute.
     * @return This menu instance for chaining.
     */
    public @NonNull Menu addCloseAction(@NonNull Consumer<Player> action) {
        this.closeActions.add(action);
        return this;
    }

    /**
     * Sets whether players can move items in this menu.
     *
     * @param allow True to allow item movement, false to prevent it.
     * @return This menu instance for chaining.
     */
    public @NonNull Menu setAllowItemMovement(boolean allow) {
        this.allowItemMovement = allow;
        return this;
    }

    /**
     * Enables auto-refresh for this menu.
     *
     * @param interval The refresh interval in ticks (20 ticks = 1 second).
     * @return This menu instance for chaining.
     */
    public @NonNull Menu enableAutoRefresh(int interval) {
        this.autoRefresh = true;
        this.refreshInterval = interval;
        return this;
    }

    /**
     * Sets the previous menu for back navigation.
     *
     * @param menu The previous menu.
     * @return This menu instance for chaining.
     */
    public @NonNull Menu setPreviousMenu(@Nullable Menu menu) {
        this.previousMenu = menu;
        return this;
    }

    /**
     * Opens the previous menu if one exists.
     */
    public void openPreviousMenu() {
        if (this.previousMenu != null) {
            this.close();
            this.previousMenu.open();
        }
    }

    /**
     * Prevents the menu from being closed by the player.
     *
     * @param prevent True to prevent closing, false to allow it.
     * @return This menu instance for chaining.
     */
    public @NonNull Menu setPreventClose(boolean prevent) {
        this.preventClose = prevent;
        return this;
    }

    /**
     * Sets whether sound effects should play on interactions.
     *
     * @param playSounds True to play sounds, false to disable them.
     * @return This menu instance for chaining.
     */
    public @NonNull Menu setPlaySounds(boolean playSounds) {
        this.playSounds = playSounds;
        return this;
    }

    /**
     * Gets whether sound effects are enabled.
     *
     * @return True if sounds are enabled, false otherwise.
     */
    public boolean isPlaySounds() {
        return this.playSounds;
    }

    /**
     * Gets the previous menu if one exists.
     *
     * @return The previous menu, or null.
     */
    public @Nullable Menu getPreviousMenu() {
        return this.previousMenu;
    }

    /**
     * Gets the player viewing this menu.
     *
     * @return The player, or null if they are offline.
     */
    public @Nullable Player getViewer() {
        return Bukkit.getPlayer(this.viewerId);
    }

    /**
     * Gets the viewer's UUID.
     *
     * @return The viewer's UUID.
     */
    public @NonNull UUID getViewerId() {
        return this.viewerId;
    }

    /**
     * Gets all items in this menu.
     *
     * @return Unmodifiable map of slot to menu item.
     */
    public @NonNull Map<Integer, MenuItem> getItems() {
        return Collections.unmodifiableMap(this.items);
    }

    @Override
    public @NonNull Inventory getInventory() {
        if (this.inventory == null) {
            throw new IllegalStateException("Menu has not been opened yet");
        }
        return this.inventory;
    }

    /**
     * Creates the inventory for this menu.
     *
     * @return The created inventory.
     */
    private @NonNull Inventory createInventory() {
        InventoryType type = this.getInventoryType();

        if (type == InventoryType.CHEST) {
            int size = this.getRows() * 9;
            return Bukkit.createInventory(this, size, this.getTitle());
        } else {
            return Bukkit.createInventory(this, type, this.getTitle());
        }
    }

    /**
     * Renders all menu items into the inventory.
     */
    private void render() {
        if (this.inventory == null) {
            return;
        }

        for (Map.Entry<Integer, MenuItem> entry : this.items.entrySet()) {
            int slot = entry.getKey();
            MenuItem menuItem = entry.getValue();
            ItemStack itemStack = menuItem.getItemStack();

            if (slot >= 0 && slot < this.inventory.getSize()) {
                this.inventory.setItem(slot, itemStack);
            }
        }
    }

    /**
     * Starts auto-refresh task for this menu.
     */
    private void startAutoRefresh() {
        if (this.taskId != null) {
            return;
        }

        this.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                Tools.getInstance(),
                this::refresh,
                this.refreshInterval,
                this.refreshInterval
        );
    }

    /**
     * Stops auto-refresh task for this menu.
     */
    private void stopAutoRefresh() {
        if (this.taskId != null) {
            Bukkit.getScheduler().cancelTask(this.taskId);
            this.taskId = null;
        }
    }
}