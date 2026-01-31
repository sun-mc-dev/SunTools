package me.sunmc.tools.utils.bukkit;

import lombok.experimental.UtilityClass;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Utility class for working with {@link Player}.
 * Updated to use modern Paper API without deprecated methods.
 */
@UtilityClass
public class PlayerUtil {

    public static final double DEFAULT_RESET_AMOUNT = 20;
    public static final float DEFAULT_WALK_SPEED = 0.2f;

    /**
     * Performs a simple reset on the target {@link Player}.
     * <p>
     * The difference of a simple reset and a {@link #fullResetPlayer(Player)} is the values the method resets.
     * This makes it possible for the parent plugin to be more selective in which values they want to reset for the player.
     *
     * @param player Instance of the {@link Player} to reset.
     */
    public static void simpleResetPlayer(@NonNull Player player) {

        AttributeInstance healthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null) {
            player.setHealth(healthAttr.getValue());
        }

        player.setFoodLevel((int) DEFAULT_RESET_AMOUNT);
        player.setSaturation(5.0f);
        player.setExhaustion(0.0f);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setSneaking(false);
        player.setSprinting(false);
        player.setWalkSpeed(DEFAULT_WALK_SPEED);
        player.setLevel(0);
        player.setExp(0);
        player.setFireTicks(0);
        player.setFreezeTicks(0);
    }

    /**
     * Performs a full reset on the target {@link Player}.
     * <p>
     * The difference of a full reset and a {@link #simpleResetPlayer(Player)} is the values the method resets.
     * This makes it possible for the parent plugin to be more selective in which values they want to reset for the player.
     *
     * @param player Instance of the {@link Player} to reset.
     * @see #simpleResetPlayer(Player)
     */
    public static void fullResetPlayer(@NonNull Player player) {
        simpleResetPlayer(player);

        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();
        player.getActivePotionEffects().forEach(effect ->
                player.removePotionEffect(effect.getType()));

        player.setArrowsInBody(0);
        player.setFallDistance(0);
    }

    /**
     * Heals the player to full health.
     *
     * @param player The player to heal.
     */
    public static void healPlayer(@NonNull Player player) {
        AttributeInstance healthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null) {
            player.setHealth(healthAttr.getValue());
        }
    }

    /**
     * Feeds the player to full hunger.
     *
     * @param player The player to feed.
     */
    public static void feedPlayer(@NonNull Player player) {
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setExhaustion(0.0f);
    }

    /**
     * Checks if the player's inventory is full.
     *
     * @param player The player to check.
     * @return True if the inventory is full, false otherwise.
     */
    public static boolean isInventoryFull(@NonNull Player player) {
        return player.getInventory().firstEmpty() == -1;
    }

    /**
     * Checks if the player has a specific amount of empty slots.
     *
     * @param player    The player to check.
     * @param slotsNeeded The number of empty slots needed.
     * @return True if the player has enough empty slots, false otherwise.
     */
    public static boolean hasEmptySlots(@NonNull Player player, int slotsNeeded) {
        int emptySlots = 0;
        for (int i = 0; i < 36; i++) {
            if (player.getInventory().getItem(i) == null) {
                emptySlots++;
                if (emptySlots >= slotsNeeded) {
                    return true;
                }
            }
        }
        return false;
    }
}