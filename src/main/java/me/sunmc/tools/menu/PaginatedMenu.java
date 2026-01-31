package me.sunmc.tools.menu;

import me.sunmc.tools.menu.item.MenuItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enhanced paginated menu with advanced features:
 * - Customizable page navigation
 * - Search and filter support
 * - Sort support
 * - Page jump functionality
 * - Empty state handling
 * - Loading state support
 *
 * @version 1.0.0
 */
public abstract class PaginatedMenu extends Menu {

    private final @NonNull List<MenuItem> allContentItems;
    private @NonNull List<MenuItem> filteredContentItems;
    private int currentPage;
    private int itemsPerPage;
    private @Nullable Function<MenuItem, Integer> sortFunction;
    private @Nullable String searchQuery;
    private boolean showPageInfo = true;
    private boolean showNavigation = true;
    private @Nullable MenuItem emptyStateItem;
    private NavigationPosition navigationPosition = NavigationPosition.BOTTOM;

    /**
     * Navigation position in the menu.
     */
    public enum NavigationPosition {
        TOP,
        BOTTOM,
        BOTH
    }

    /**
     * Constructs a new PaginatedMenu for the specified player.
     *
     * @param viewer The player who will view this menu.
     */
    public PaginatedMenu(@NonNull Player viewer) {
        super(viewer);
        this.allContentItems = new ArrayList<>();
        this.filteredContentItems = new ArrayList<>();
        this.currentPage = 0;
        this.itemsPerPage = this.calculateItemsPerPage();
    }

    /**
     * Gets the slots that should be used for content items.
     * By default, excludes the bottom row for navigation.
     *
     * @return List of slot indices for content.
     */
    public @NonNull List<Integer> getContentSlots() {
        List<Integer> slots = new ArrayList<>();
        int totalSlots = this.getRows() * 9;
        int excludeStart = this.navigationPosition == NavigationPosition.TOP ? 9 :
                (this.getRows() - 1) * 9;
        int excludeEnd = this.navigationPosition == NavigationPosition.TOP ? 9 : totalSlots;

        for (int i = 0; i < totalSlots; i++) {
            if (this.navigationPosition == NavigationPosition.BOTH) {
                // Exclude first and last row
                if (i >= 9 && i < (this.getRows() - 1) * 9) {
                    slots.add(i);
                }
            } else if (this.navigationPosition == NavigationPosition.BOTTOM) {
                // Exclude last row
                if (i < excludeStart) {
                    slots.add(i);
                }
            } else if (this.navigationPosition == NavigationPosition.TOP) {
                // Exclude first row
                if (i >= 9) {
                    slots.add(i);
                }
            }
        }
        return slots;
    }

    /**
     * Adds a content item to the paginated menu.
     *
     * @param item The menu item to add.
     */
    public void addContentItem(@NonNull MenuItem item) {
        this.allContentItems.add(item);
        this.applyFiltersAndSort();
    }

    /**
     * Adds multiple content items to the paginated menu.
     *
     * @param items The menu items to add.
     */
    public void addContentItems(@NonNull List<MenuItem> items) {
        this.allContentItems.addAll(items);
        this.applyFiltersAndSort();
    }

    /**
     * Adds content items using a function to convert objects to menu items.
     *
     * @param objects  The objects to convert.
     * @param converter The conversion function.
     * @param <T>      The type of objects.
     */
    public <T> void addContentItems(@NonNull Collection<T> objects, @NonNull Function<T, MenuItem> converter) {
        objects.stream()
                .map(converter)
                .forEach(this.allContentItems::add);
        this.applyFiltersAndSort();
    }

    /**
     * Clears all content items.
     */
    public void clearContentItems() {
        this.allContentItems.clear();
        this.filteredContentItems.clear();
        this.currentPage = 0;
    }

    /**
     * Sets a search query to filter items.
     *
     * @param query The search query (null to clear).
     */
    public void setSearchQuery(@Nullable String query) {
        this.searchQuery = query;
        this.applyFiltersAndSort();
        this.currentPage = 0; // Reset to first page
    }

    /**
     * Sets a sort function for items.
     *
     * @param sortFunction The sort function (null to clear).
     */
    public void setSortFunction(@Nullable Function<MenuItem, Integer> sortFunction) {
        this.sortFunction = sortFunction;
        this.applyFiltersAndSort();
    }

    /**
     * Sets the navigation position.
     *
     * @param position The navigation position.
     */
    public void setNavigationPosition(@NonNull NavigationPosition position) {
        this.navigationPosition = position;
        this.itemsPerPage = this.calculateItemsPerPage();
    }

    /**
     * Sets whether to show page info.
     *
     * @param show True to show, false to hide.
     */
    public void setShowPageInfo(boolean show) {
        this.showPageInfo = show;
    }

    /**
     * Sets whether to show navigation buttons.
     *
     * @param show True to show, false to hide.
     */
    public void setShowNavigation(boolean show) {
        this.showNavigation = show;
    }

    /**
     * Sets the item to display when there are no items.
     *
     * @param item The empty state item.
     */
    public void setEmptyStateItem(@Nullable MenuItem item) {
        this.emptyStateItem = item;
    }

    /**
     * Gets the current page number (0-indexed).
     *
     * @return The current page.
     */
    public int getCurrentPage() {
        return this.currentPage;
    }

    /**
     * Sets the current page number (0-indexed).
     *
     * @param page The page to set.
     */
    public void setCurrentPage(int page) {
        int maxPage = this.getMaxPage();
        this.currentPage = Math.max(0, Math.min(page, maxPage));
    }

    /**
     * Gets the maximum page number (0-indexed).
     *
     * @return The maximum page.
     */
    public int getMaxPage() {
        return Math.max(0, (int) Math.ceil((double) this.filteredContentItems.size() / this.itemsPerPage) - 1);
    }

    /**
     * Gets the total number of items (after filtering).
     *
     * @return The total item count.
     */
    public int getTotalItems() {
        return this.filteredContentItems.size();
    }

    /**
     * Checks if there is a next page.
     *
     * @return True if there is a next page, false otherwise.
     */
    public boolean hasNextPage() {
        return this.currentPage < this.getMaxPage();
    }

    /**
     * Checks if there is a previous page.
     *
     * @return True if there is a previous page, false otherwise.
     */
    public boolean hasPreviousPage() {
        return this.currentPage > 0;
    }

    /**
     * Navigates to the next page.
     */
    public void nextPage() {
        if (this.hasNextPage()) {
            this.currentPage++;
            this.refresh();
        }
    }

    /**
     * Navigates to the previous page.
     */
    public void previousPage() {
        if (this.hasPreviousPage()) {
            this.currentPage--;
            this.refresh();
        }
    }

    /**
     * Jumps to the first page.
     */
    public void firstPage() {
        if (this.currentPage != 0) {
            this.currentPage = 0;
            this.refresh();
        }
    }

    /**
     * Jumps to the last page.
     */
    public void lastPage() {
        int maxPage = this.getMaxPage();
        if (this.currentPage != maxPage) {
            this.currentPage = maxPage;
            this.refresh();
        }
    }

    @Override
    public void init() {
        this.applyFiltersAndSort();

        if (this.filteredContentItems.isEmpty() && this.emptyStateItem != null) {
            this.displayEmptyState();
        } else {
            this.populateContent();
        }

        if (this.showNavigation) {
            this.addNavigationItems();
        }
    }

    /**
     * Displays the empty state.
     */
    protected void displayEmptyState() {
        int centerSlot = (this.getRows() / 2) * 9 + 4;
        this.setItem(centerSlot, this.emptyStateItem);
    }

    /**
     * Populates the content items for the current page.
     */
    protected void populateContent() {
        List<Integer> contentSlots = this.getContentSlots();
        int startIndex = this.currentPage * this.itemsPerPage;
        int endIndex = Math.min(startIndex + this.itemsPerPage, this.filteredContentItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            int slotIndex = i - startIndex;
            if (slotIndex < contentSlots.size()) {
                int slot = contentSlots.get(slotIndex);
                this.setItem(slot, this.filteredContentItems.get(i));
            }
        }
    }

    /**
     * Adds navigation items (previous, next, page info).
     */
    protected void addNavigationItems() {
        int navRow = this.navigationPosition == NavigationPosition.TOP ? 0 : this.getRows() - 1;

        // First page button
        if (this.currentPage > 0) {
            this.setItem(navRow, 0, this.createFirstPageItem());
        }

        // Previous page button
        if (this.hasPreviousPage()) {
            this.setItem(navRow, 3, this.createPreviousPageItem());
        }

        // Page info
        if (this.showPageInfo) {
            this.setItem(navRow, 4, this.createPageInfoItem());
        }

        // Next page button
        if (this.hasNextPage()) {
            this.setItem(navRow, 5, this.createNextPageItem());
        }

        // Last page button
        if (this.currentPage < this.getMaxPage()) {
            this.setItem(navRow, 8, this.createLastPageItem());
        }
    }

    /**
     * Creates the first page navigation item.
     *
     * @return The first page menu item.
     */
    protected @NonNull MenuItem createFirstPageItem() {
        ItemStack item = new ItemStack(Material.SPECTRAL_ARROW);
        item.editMeta(meta -> {
            meta.displayName(Component.text("⏮ First Page", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
        });

        return MenuItem.of(item, event -> {
            event.setCancelled(true);
            this.firstPage();
        }).setClickSound(Sound.UI_BUTTON_CLICK);
    }

    /**
     * Creates the previous page navigation item.
     *
     * @return The previous page menu item.
     */
    protected @NonNull MenuItem createPreviousPageItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        item.editMeta(meta -> {
            meta.displayName(Component.text("← Previous Page", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Page " + this.currentPage + " ◀", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
        });

        return MenuItem.of(item, event -> {
            event.setCancelled(true);
            this.previousPage();
        }).setClickSound(Sound.UI_BUTTON_CLICK);
    }

    /**
     * Creates the next page navigation item.
     *
     * @return The next page menu item.
     */
    protected @NonNull MenuItem createNextPageItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        item.editMeta(meta -> {
            meta.displayName(Component.text("Next Page →", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("▶ Page " + (this.currentPage + 2), NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
        });

        return MenuItem.of(item, event -> {
            event.setCancelled(true);
            this.nextPage();
        }).setClickSound(Sound.UI_BUTTON_CLICK);
    }

    /**
     * Creates the last page navigation item.
     *
     * @return The last page menu item.
     */
    protected @NonNull MenuItem createLastPageItem() {
        ItemStack item = new ItemStack(Material.SPECTRAL_ARROW);
        item.editMeta(meta -> {
            meta.displayName(Component.text("Last Page ⏭", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
        });

        return MenuItem.of(item, event -> {
            event.setCancelled(true);
            this.lastPage();
        }).setClickSound(Sound.UI_BUTTON_CLICK);
    }

    /**
     * Creates the page info item.
     *
     * @return The page info menu item.
     */
    protected @NonNull MenuItem createPageInfoItem() {
        ItemStack item = new ItemStack(Material.PAPER);
        item.editMeta(meta -> {
            meta.displayName(Component.text("Page Information", NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Page: " + (this.currentPage + 1) + " / " + (this.getMaxPage() + 1), NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Items: " + this.getTotalItems(), NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
        });

        return MenuItem.of(item, event -> event.setCancelled(true));
    }

    /**
     * Applies filters and sorting to content items.
     */
    private void applyFiltersAndSort() {
        this.filteredContentItems = new ArrayList<>(this.allContentItems);

        // Apply search filter (basic implementation - override for custom filtering)
        if (this.searchQuery != null && !this.searchQuery.isEmpty()) {
            String query = this.searchQuery.toLowerCase();
            this.filteredContentItems = this.filteredContentItems.stream()
                    .filter(item -> {
                        Component displayName = item.getItemStack().getItemMeta().displayName();
                        return displayName != null &&
                                net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                                        .serialize(displayName).toLowerCase().contains(query);
                    })
                    .collect(Collectors.toList());
        }

        // Apply sorting
        if (this.sortFunction != null) {
            this.filteredContentItems.sort(Comparator.comparing(this.sortFunction));
        }
    }

    /**
     * Calculates the number of items per page based on content slots.
     *
     * @return The number of items per page.
     */
    private int calculateItemsPerPage() {
        return this.getContentSlots().size();
    }
}