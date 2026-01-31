package me.sunmc.tools.item.util;

import me.sunmc.tools.utils.bukkit.legacy.LegacyPlayerMessenger;
import me.sunmc.tools.utils.java.ContentVariable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.Color;
import org.bukkit.OfflinePlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;
import java.util.function.Consumer;

/**
 * Enhanced builder for creating and modifying {@link ItemStack} with advanced features.
 * <p>
 * Features:
 * - Fluent API with method chaining
 * - Adventure Component support
 * - Attribute modifiers
 * - Leather armor dyeing
 * - Skull/head textures
 * - Item rarity
 * - Damage/durability
 * - Unbreakable items
 * - Item flags
 * - Enchantments (safe and unsafe)
 * - Custom model data
 * - And more!
 *
 * @version 1.0.0
 */
public class ItemStackBuilder {

    private final @NonNull ItemStack item;

    /**
     * Creates a new builder with a {@link Material} for the {@link ItemStack}.
     *
     * @param material The {@link Material} for the item.
     */
    public ItemStackBuilder(@NonNull Material material) {
        this.item = new ItemStack(material);
    }

    /**
     * Creates a new builder based on an existing {@link ItemStack}.
     *
     * @param item The {@link ItemStack} to create the builder for.
     */
    public ItemStackBuilder(@NonNull ItemStack item) {
        this.item = item.clone();
    }

    /**
     * Sets the amount of items in this {@link ItemStack}.
     *
     * @param amount New amount of items in the stack.
     * @return This builder instance.
     */
    public @NonNull ItemStackBuilder amount(int amount) {
        this.item.setAmount(amount);
        return this;
    }

    /**
     * Sets the display name of this {@link ItemStack}.
     *
     * @param name The name to set as a {@link String}.
     * @return This builder instance.
     * @see #name(String, ContentVariable...)
     */
    public @NonNull ItemStackBuilder name(@NonNull String name) {
        return this.name(name, (ContentVariable) null);
    }

    /**
     * Sets the display name with italic disabled by default.
     *
     * @param name The name to set.
     * @param noItalic If true, disables italic formatting.
     * @return This builder instance.
     */
    public @NonNull ItemStackBuilder name(@NonNull String name, boolean noItalic) {
        this.item.editMeta(meta -> {
            Component component = LegacyPlayerMessenger.handleToComponent(name);
            if (noItalic) {
                component = component.decoration(TextDecoration.ITALIC, false);
            }
            meta.displayName(component);
        });
        return this;
    }

    /**
     * Sets the display name of this {@link ItemStack}.
     *
     * @param name      The name to set as a {@link String}.
     * @param variables Optional {@link ContentVariable content variables}.
     * @return This builder instance.
     */
    public @NonNull ItemStackBuilder name(@NonNull String name, @Nullable ContentVariable... variables) {
        this.item.editMeta(meta -> meta.displayName(LegacyPlayerMessenger.handleToComponent(name, variables)));
        return this;
    }

    /**
     * Sets the display name using a Component.
     *
     * @param name The name component.
     * @return This builder instance.
     */
    public @NonNull ItemStackBuilder name(@NonNull Component name) {
        this.item.editMeta(meta -> meta.displayName(name));
        return this;
    }

    /**
     * Sets the lore for this {@link ItemStack}.
     *
     * @param lines The lines to set as lore.
     * @return This builder instance.
     */
    public @NonNull ItemStackBuilder lore(@NonNull Component... lines) {
        this.item.editMeta(meta -> meta.lore(List.of(lines)));
        return this;
    }

    /**
     * Sets the lore for this {@link ItemStack}.
     *
     * @param lines The lines to set as lore.
     * @return This builder instance.
     */
    public @NonNull ItemStackBuilder lore(@NonNull String... lines) {
        return this.lore(List.of(lines), (ContentVariable) null);
    }

    /**
     * Sets the lore with italic disabled by default.
     *
     * @param noItalic If true, disables italic formatting.
     * @param lines The lines to set as lore.
     * @return This builder instance.
     */
    public @NonNull ItemStackBuilder lore(boolean noItalic, @NonNull String... lines) {
        this.item.editMeta(meta -> {
            final List<Component> components = Arrays.stream(lines)
                    .map(LegacyPlayerMessenger::handleToComponent)
                    .map(comp -> noItalic ? comp.decoration(TextDecoration.ITALIC, false) : comp)
                    .toList();
            meta.lore(components);
        });
        return this;
    }

    /**
     * Sets the lore for this {@link ItemStack}.
     *
     * @param lines     The lines to set as lore.
     * @param variables Optional {@link ContentVariable content variables}.
     * @return This builder instance.
     */
    public @NonNull ItemStackBuilder lore(@NonNull List<String> lines, @Nullable ContentVariable... variables) {
        this.item.editMeta(meta -> {
            final List<Component> array = lines.stream()
                    .map(s -> LegacyPlayerMessenger.handleToComponent(s, variables))
                    .toList();
            meta.lore(array);
        });
        return this;
    }

    /**
     * Adds lines to existing lore.
     *
     * @param lines The lines to add.
     * @return This builder instance.
     */
    public @NonNull ItemStackBuilder addLore(@NonNull String... lines) {
        this.item.editMeta(meta -> {
            List<Component> currentLore = meta.lore();
            if (currentLore == null) {
                currentLore = new ArrayList<>();
            }

            List<Component> newLines = Arrays.stream(lines)
                    .map(LegacyPlayerMessenger::handleToComponent)
                    .toList();

            currentLore.addAll(newLines);
            meta.lore(currentLore);
        });
        return this;
    }

    /**
     * Adds {@link ItemFlag} to this {@link ItemStack}.
     *
     * @param flags The {@link ItemFlag} to add.
     * @return This builder instance.
     */
    public @NonNull ItemStackBuilder addItemFlag(@NonNull ItemFlag... flags) {
        this.item.editMeta(meta -> meta.addItemFlags(flags));
        return this;
    }

    /**
     * Removes {@link ItemFlag} from this {@link ItemStack}.
     *
     * @param flags The {@link ItemFlag} to remove.
     * @return This builder instance.
     */
    public @NonNull ItemStackBuilder removeItemFlag(@NonNull ItemFlag... flags) {
        this.item.editMeta(meta -> meta.removeItemFlags(flags));
        return this;
    }

    /**
     * Hides all item flags.
     *
     * @return This builder instance.
     */
    public @NonNull ItemStackBuilder hideAllFlags() {
        return this.addItemFlag(ItemFlag.values());
    }

    /**
     * Adds an {@link Enchantment} to this {@link ItemStack}.
     *
     * @param enchantment The {@link Enchantment} to add.
     * @param level       The level to set for the enchantment.
     * @return This builder instance.
     */
    public @NonNull ItemStackBuilder addEnchantment(@NonNull Enchantment enchantment, int level) {
        this.item.addUnsafeEnchantment(enchantment, level);
        return this;
    }

    /**
     * Adds multiple enchantments at once.
     *
     * @param enchantments Map of enchantments to levels.
     * @return This builder instance.
     */
    public @NonNull ItemStackBuilder addEnchantments(@NonNull Map<Enchantment, Integer> enchantments) {
        enchantments.forEach((ench, level) -> this.item.addUnsafeEnchantment(ench, level));
        return this;
    }

    /**
     * Removes an already applied {@link Enchantment}.
     *
     * @param enchantment The {@link Enchantment} to remove.
     * @return This builder instance.
     */
    public @NonNull ItemStackBuilder removeEnchantment(@NonNull Enchantment enchantment) {
        this.item.removeEnchantment(enchantment);
        return this;
    }

    /**
     * Removes all enchantments.
     *
     * @return This builder instance.
     */
    public @NonNull ItemStackBuilder clearEnchantments() {
        new HashMap<>(this.item.getEnchantments()).keySet().forEach(this.item::removeEnchantment);
        return this;
    }

    /**
     * Sets the custom model data.
     *
     * @param data The custom model data value.
     * @return This builder instance.
     */
    public @NonNull ItemStackBuilder customModelData(int data) {
        this.item.editMeta(meta -> meta.setCustomModelData(data));
        return this;
    }

    /**
     * Sets whether this item should be unbreakable.
     *
     * @param unbreakable True if the item should be unbreakable.
     * @return This builder instance.
     */
    public @NonNull ItemStackBuilder unbreakable(boolean unbreakable) {
        this.item.editMeta(meta -> meta.setUnbreakable(unbreakable));
        return this;
    }

    /**
     * Sets whether this item should hide the tooltip.
     *
     * @param hideTooltip True to hide the tooltip.
     * @return This builder instance.
     */
    public @NonNull ItemStackBuilder hideTooltip(boolean hideTooltip) {
        this.item.editMeta(meta -> meta.setHideTooltip(hideTooltip));
        return this;
    }

    /**
     * Sets the glint override.
     *
     * @param glintOverride True to force glint, false to remove forced glint, null for default.
     * @return This builder instance.
     */
    public @NonNull ItemStackBuilder glintOverride(@Nullable Boolean glintOverride) {
        this.item.editMeta(meta -> meta.setEnchantmentGlintOverride(glintOverride));
        return this;
    }

    /**
     * Sets whether this item is fire resistant.
     *
     * @param fireResistant True if the item should be fire resistant.
     * @return This builder instance.
     */
    public @NonNull ItemStackBuilder fireResistant(boolean fireResistant) {
        this.item.editMeta(meta -> meta.setFireResistant(fireResistant));
        return this;
    }

    /**
     * Sets the maximum stack size.
     *
     * @param maxStackSize The maximum stack size (1-99).
     * @return This builder instance.
     */
    public @NonNull ItemStackBuilder maxStackSize(int maxStackSize) {
        this.item.editMeta(meta -> {
            if (maxStackSize >= 1 && maxStackSize <= 99) {
                meta.setMaxStackSize(maxStackSize);
            }
        });
        return this;
    }

    /**
     * Sets the item rarity.
     *
     * @param rarity The item rarity.
     * @return This builder instance.
     */
    public @NonNull ItemStackBuilder rarity(@NonNull ItemRarity rarity) {
        this.item.editMeta(meta -> meta.setRarity(rarity));
        return this;
    }

    /**
     * Sets the damage/durability of the item.
     *
     * @param damage The damage value.
     * @return This builder instance.
     */
    public @NonNull ItemStackBuilder damage(int damage) {
        this.item.editMeta(meta -> {
            if (meta instanceof Damageable damageable) {
                damageable.setDamage(damage);
            }
        });
        return this;
    }

    /**
     * Adds an attribute modifier.
     *
     * @param attribute The attribute.
     * @param modifier The modifier.
     * @return This builder instance.
     */
    public @NonNull ItemStackBuilder addAttributeModifier(@NonNull Attribute attribute,
                                                          @NonNull AttributeModifier modifier) {
        this.item.editMeta(meta -> meta.addAttributeModifier(attribute, modifier));
        return this;
    }

    /**
     * Removes all attribute modifiers.
     *
     * @return This builder instance.
     */
    public @NonNull ItemStackBuilder clearAttributeModifiers() {
        this.item.editMeta(meta -> {
            if (meta.getAttributeModifiers() != null) {
                meta.getAttributeModifiers().keySet().forEach(meta::removeAttributeModifier);
            }
        });
        return this;
    }

    /**
     * Sets the color for leather armor.
     *
     * @param color The color.
     * @return This builder instance.
     */
    public @NonNull ItemStackBuilder leatherColor(@NonNull Color color) {
        this.item.editMeta(meta -> {
            if (meta instanceof LeatherArmorMeta leatherMeta) {
                leatherMeta.setColor(color);
            }
        });
        return this;
    }

    /**
     * Sets the skull owner for player heads.
     *
     * @param owner The skull owner.
     * @return This builder instance.
     */
    public @NonNull ItemStackBuilder skullOwner(@NonNull OfflinePlayer owner) {
        this.item.editMeta(meta -> {
            if (meta instanceof SkullMeta skullMeta) {
                skullMeta.setOwningPlayer(owner);
            }
        });
        return this;
    }

    /**
     * Applies a custom meta editor function.
     *
     * @param metaEditor The meta editor function.
     * @return This builder instance.
     */
    public @NonNull ItemStackBuilder editMeta(@NonNull Consumer<org.bukkit.inventory.meta.ItemMeta> metaEditor) {
        this.item.editMeta(metaEditor);
        return this;
    }

    /**
     * Returns the built {@link ItemStack}.
     *
     * @return The built {@link ItemStack}.
     */
    public @NonNull ItemStack build() {
        return this.item;
    }

    /**
     * Static factory method to create a new builder.
     *
     * @param material The material for the item.
     * @return A new ItemStackBuilder instance.
     */
    public static @NonNull ItemStackBuilder of(@NonNull Material material) {
        return new ItemStackBuilder(material);
    }

    /**
     * Static factory method to create a builder from an existing item.
     *
     * @param item The existing item.
     * @return A new ItemStackBuilder instance.
     */
    public static @NonNull ItemStackBuilder of(@NonNull ItemStack item) {
        return new ItemStackBuilder(item);
    }
}