package me.sunmc.tools.menu.confirmation;

import me.sunmc.tools.menu.Menu;
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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A pre-built confirmation dialog menu.
 * Displays a question with Confirm and Cancel options.
 *
 * <p>Example usage:
 * <pre>{@code
 * ConfirmationMenu.builder(player)
 *     .title("Delete Item?")
 *     .question("Are you sure you want to delete this item?")
 *     .warning("This action cannot be undone!")
 *     .onConfirm(() -> {
 *         // Delete the item
 *         player.sendMessage("Item deleted!");
 *     })
 *     .onCancel(() -> {
 *         // Return to previous menu
 *         previousMenu.open();
 *     })
 *     .build()
 *     .open();
 * }</pre>
 *
 * @version 1.0.0
 */
public class ConfirmationMenu extends Menu {

    private final @NonNull Component titleComponent;
    private final @NonNull Component questionComponent;
    private final @NonNull List<Component> descriptionLines;
    private final @Nullable Runnable onConfirm;
    private final @Nullable Runnable onCancel;
    private final @NonNull Material confirmMaterial;
    private final @NonNull Material cancelMaterial;
    private final @NonNull ConfirmationStyle style;

    /**
     * Confirmation menu style.
     */
    public enum ConfirmationStyle {
        /** Simple layout with confirm on left, cancel on right */
        SIMPLE,
        /** Center layout with confirm and cancel in the middle */
        CENTER,
        /** Large layout with more spacing */
        LARGE
    }

    private ConfirmationMenu(@NonNull Builder builder) {
        super(builder.viewer);
        this.titleComponent = builder.titleComponent;
        this.questionComponent = builder.questionComponent;
        this.descriptionLines = new ArrayList<>(builder.descriptionLines);
        this.onConfirm = builder.onConfirm;
        this.onCancel = builder.onCancel;
        this.confirmMaterial = builder.confirmMaterial;
        this.cancelMaterial = builder.cancelMaterial;
        this.style = builder.style;

        if (builder.previousMenu != null) {
            this.setPreviousMenu(builder.previousMenu);
        }
    }

    @Override
    public @NonNull Component getTitle() {
        return this.titleComponent;
    }

    @Override
    public int getRows() {
        return this.style == ConfirmationStyle.LARGE ? 5 : 3;
    }

    @Override
    public void init() {
        // Add border
        ItemStack borderItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        borderItem.editMeta(meta -> meta.displayName(Component.empty()));
        this.fillBorder(MenuItem.placeholder(borderItem));

        // Add question item in center
        this.addQuestionItem();

        // Add confirm and cancel buttons based on style
        switch (this.style) {
            case SIMPLE -> this.addSimpleButtons();
            case CENTER -> this.addCenterButtons();
            case LARGE -> this.addLargeButtons();
        }
    }

    /**
     * Adds the question display item.
     */
    private void addQuestionItem() {
        int row = this.style == ConfirmationStyle.LARGE ? 1 : 1;
        int col = 4;

        ItemStack questionItem = new ItemStack(Material.PAPER);
        questionItem.editMeta(meta -> {
            meta.displayName(this.questionComponent);

            if (!this.descriptionLines.isEmpty()) {
                List<Component> lore = new ArrayList<>();
                lore.add(Component.empty());
                lore.addAll(this.descriptionLines);
                meta.lore(lore);
            }
        });

        this.setItem(row, col, MenuItem.placeholder(questionItem));
    }

    /**
     * Adds buttons in simple layout.
     */
    private void addSimpleButtons() {
        // Confirm button (left)
        this.setItem(1, 2, this.createConfirmButton());

        // Cancel button (right)
        this.setItem(1, 6, this.createCancelButton());
    }

    /**
     * Adds buttons in center layout.
     */
    private void addCenterButtons() {
        // Confirm button
        this.setItem(1, 3, this.createConfirmButton());

        // Cancel button
        this.setItem(1, 5, this.createCancelButton());
    }

    /**
     * Adds buttons in large layout.
     */
    private void addLargeButtons() {
        // Confirm button
        this.setItem(3, 3, this.createConfirmButton());

        // Cancel button
        this.setItem(3, 5, this.createCancelButton());
    }

    /**
     * Creates the confirm button.
     */
    private @NonNull MenuItem createConfirmButton() {
        ItemStack item = new ItemStack(this.confirmMaterial);
        item.editMeta(meta -> {
            meta.displayName(Component.text("✔ Confirm", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false)
                    .decoration(TextDecoration.BOLD, true));
            meta.lore(List.of(
                    Component.empty(),
                    Component.text("Click to confirm", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
        });

        return MenuItem.of(item, event -> {
            event.setCancelled(true);
            Player player = event.player();

            this.close();

            if (this.onConfirm != null) {
                this.onConfirm.run();
            }
        }).setClickSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
    }

    /**
     * Creates the cancel button.
     */
    private @NonNull MenuItem createCancelButton() {
        ItemStack item = new ItemStack(this.cancelMaterial);
        item.editMeta(meta -> {
            meta.displayName(Component.text("✖ Cancel", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false)
                    .decoration(TextDecoration.BOLD, true));
            meta.lore(List.of(
                    Component.empty(),
                    Component.text("Click to cancel", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
        });

        return MenuItem.of(item, event -> {
            event.setCancelled(true);

            this.close();

            if (this.onCancel != null) {
                this.onCancel.run();
            }
        }).setClickSound(Sound.ENTITY_VILLAGER_NO);
    }

    /**
     * Creates a new builder for a confirmation menu.
     *
     * @param viewer The player who will view the menu.
     * @return A new builder instance.
     */
    public static @NonNull Builder builder(@NonNull Player viewer) {
        return new Builder(viewer);
    }

    /**
     * Builder for creating confirmation menus.
     */
    public static class Builder {
        private final @NonNull Player viewer;
        private @NonNull Component titleComponent = Component.text("Confirm Action");
        private @NonNull Component questionComponent = Component.text("Are you sure?");
        private final @NonNull List<Component> descriptionLines = new ArrayList<>();
        private @Nullable Runnable onConfirm;
        private @Nullable Runnable onCancel;
        private @Nullable Menu previousMenu;
        private @NonNull Material confirmMaterial = Material.LIME_WOOL;
        private @NonNull Material cancelMaterial = Material.RED_WOOL;
        private @NonNull ConfirmationStyle style = ConfirmationStyle.SIMPLE;

        private Builder(@NonNull Player viewer) {
            this.viewer = viewer;
        }

        /**
         * Sets the menu title.
         */
        public @NonNull Builder title(@NonNull String title) {
            this.titleComponent = Component.text(title);
            return this;
        }

        /**
         * Sets the menu title.
         */
        public @NonNull Builder title(@NonNull Component title) {
            this.titleComponent = title;
            return this;
        }

        /**
         * Sets the question text.
         */
        public @NonNull Builder question(@NonNull String question) {
            this.questionComponent = Component.text(question, NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false);
            return this;
        }

        /**
         * Sets the question text.
         */
        public @NonNull Builder question(@NonNull Component question) {
            this.questionComponent = question;
            return this;
        }

        /**
         * Adds a description line.
         */
        public @NonNull Builder description(@NonNull String line) {
            this.descriptionLines.add(Component.text(line, NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            return this;
        }

        /**
         * Adds a warning line (red text).
         */
        public @NonNull Builder warning(@NonNull String warning) {
            this.descriptionLines.add(Component.text("⚠ " + warning, NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false)
                    .decoration(TextDecoration.BOLD, true));
            return this;
        }

        /**
         * Adds multiple description lines.
         */
        public @NonNull Builder description(@NonNull String @NotNull ... lines) {
            for (String line : lines) {
                this.description(line);
            }
            return this;
        }

        /**
         * Sets the confirm action.
         */
        public @NonNull Builder onConfirm(@NonNull Runnable action) {
            this.onConfirm = action;
            return this;
        }

        /**
         * Sets the cancel action.
         */
        public @NonNull Builder onCancel(@NonNull Runnable action) {
            this.onCancel = action;
            return this;
        }

        /**
         * Sets the previous menu to return to.
         */
        public @NonNull Builder previousMenu(@NonNull Menu menu) {
            this.previousMenu = menu;
            return this;
        }

        /**
         * Sets the confirmation style.
         */
        public @NonNull Builder style(@NonNull ConfirmationStyle style) {
            this.style = style;
            return this;
        }

        /**
         * Sets custom materials for buttons.
         */
        public @NonNull Builder materials(@NonNull Material confirm, @NonNull Material cancel) {
            this.confirmMaterial = confirm;
            this.cancelMaterial = cancel;
            return this;
        }

        /**
         * Builds the confirmation menu.
         */
        public @NonNull ConfirmationMenu build() {
            return new ConfirmationMenu(this);
        }
    }
}