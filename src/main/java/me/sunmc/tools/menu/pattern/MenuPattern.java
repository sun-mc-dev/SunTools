package me.sunmc.tools.menu.pattern;

import me.sunmc.tools.menu.Menu;
import me.sunmc.tools.menu.item.MenuItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a pattern that can be applied to a menu.
 * Patterns allow for easy creation of complex menu layouts using a string-based format.
 *
 * <p>Example usage:
 * <pre>{@code
 * MenuPattern pattern = MenuPattern.builder()
 *     .row("XXXXXXXXX")
 *     .row("X       X")
 *     .row("X   C   X")
 *     .row("X       X")
 *     .row("XXXXXXXXX")
 *     .item('X', borderItem)
 *     .item('C', centerItem)
 *     .build();
 *
 * pattern.apply(menu);
 * }</pre>
 *
 * @version 1.0.0
 */
public class MenuPattern {

    private final @NonNull String[] rows;
    private final @NonNull Map<Character, MenuItem> itemMap;

    /**
     * Creates a new menu pattern.
     *
     * @param rows    The pattern rows.
     * @param itemMap The character to menu item mapping.
     */
    private MenuPattern(@NonNull String[] rows, @NonNull Map<Character, MenuItem> itemMap) {
        this.rows = rows;
        this.itemMap = itemMap;
    }

    /**
     * Applies this pattern to a menu.
     *
     * @param menu The menu to apply the pattern to.
     */
    public void apply(@NonNull Menu menu) {
        for (int row = 0; row < rows.length && row < menu.getRows(); row++) {
            String rowPattern = rows[row];
            for (int col = 0; col < rowPattern.length() && col < 9; col++) {
                char c = rowPattern.charAt(col);
                if (c == ' ') continue; // Skip spaces

                MenuItem item = itemMap.get(c);
                if (item != null) {
                    menu.setItem(row, col, item);
                }
            }
        }
    }

    /**
     * Creates a new pattern builder.
     *
     * @return A new builder instance.
     */
    public static @NonNull Builder builder() {
        return new Builder();
    }

    /**
     * Creates a common border pattern.
     *
     * @param rows       Number of rows in the menu.
     * @param borderItem The item to use for the border.
     * @return A border pattern.
     */
    public static @NonNull MenuPattern border(int rows, @NonNull MenuItem borderItem) {
        Builder builder = builder();

        // First row - all border
        builder.row("XXXXXXXXX");

        // Middle rows - border on sides only
        for (int i = 1; i < rows - 1; i++) {
            builder.row("X       X");
        }

        // Last row - all border
        if (rows > 1) {
            builder.row("XXXXXXXXX");
        }

        builder.item('X', borderItem);
        return builder.build();
    }

    /**
     * Creates a checkerboard pattern.
     *
     * @param rows  Number of rows in the menu.
     * @param item1 First item for the pattern.
     * @param item2 Second item for the pattern.
     * @return A checkerboard pattern.
     */
    public static @NonNull MenuPattern checkerboard(int rows, @NonNull MenuItem item1, @NonNull MenuItem item2) {
        Builder builder = builder();

        for (int row = 0; row < rows; row++) {
            StringBuilder rowPattern = new StringBuilder();
            for (int col = 0; col < 9; col++) {
                rowPattern.append((row + col) % 2 == 0 ? 'A' : 'B');
            }
            builder.row(rowPattern.toString());
        }

        builder.item('A', item1);
        builder.item('B', item2);
        return builder.build();
    }

    /**
     * Creates a frame pattern (border with corners).
     *
     * @param rows        Number of rows in the menu.
     * @param cornerItem  Item for corners.
     * @param borderItem  Item for border edges.
     * @return A frame pattern.
     */
    public static @NonNull MenuPattern frame(int rows, @NonNull MenuItem cornerItem, @NonNull MenuItem borderItem) {
        Builder builder = builder();

        // First row
        builder.row("CBBBBBBBC");

        // Middle rows
        for (int i = 1; i < rows - 1; i++) {
            builder.row("B       B");
        }

        // Last row
        if (rows > 1) {
            builder.row("CBBBBBBBC");
        }

        builder.item('C', cornerItem);
        builder.item('B', borderItem);
        return builder.build();
    }

    /**
     * Builder for creating menu patterns.
     */
    public static class Builder {
        private final List<String> rows = new ArrayList<>();
        private final @NonNull Map<Character, MenuItem> itemMap = new HashMap<>();

        /**
         * Adds a row to the pattern.
         *
         * @param pattern The pattern string (max 9 characters).
         * @return This builder instance.
         */
        public @NonNull Builder row(@NonNull String pattern) {
            if (pattern.length() > 9) {
                throw new IllegalArgumentException("Row pattern cannot exceed 9 characters");
            }
            this.rows.add(pattern);
            return this;
        }

        /**
         * Maps a character to a menu item.
         *
         * @param character The character in the pattern.
         * @param item      The menu item to place.
         * @return This builder instance.
         */
        public @NonNull Builder item(char character, @NonNull MenuItem item) {
            this.itemMap.put(character, item);
            return this;
        }

        /**
         * Maps a character to a menu item using an ItemStack.
         *
         * @param character The character in the pattern.
         * @param itemStack The item stack to use.
         * @return This builder instance.
         */
        public @NonNull Builder item(char character, @NonNull ItemStack itemStack) {
            this.itemMap.put(character, MenuItem.of(itemStack));
            return this;
        }

        /**
         * Maps a character to a menu item using a Material.
         *
         * @param character The character in the pattern.
         * @param material  The material to use.
         * @return This builder instance.
         */
        public @NonNull Builder item(char character, @NonNull Material material) {
            this.itemMap.put(character, MenuItem.of(new ItemStack(material)));
            return this;
        }

        /**
         * Builds the menu pattern.
         *
         * @return The created pattern.
         */
        public @NonNull MenuPattern build() {
            if (rows.isEmpty()) {
                throw new IllegalStateException("Pattern must have at least one row");
            }
            return new MenuPattern(rows.toArray(new String[0]), new HashMap<>(itemMap));
        }
    }
}