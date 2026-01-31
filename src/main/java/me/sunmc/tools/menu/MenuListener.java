package me.sunmc.tools.menu;

import me.sunmc.tools.Tools;
import me.sunmc.tools.registry.AutoRegister;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Listener for handling menu-related events.
 * Automatically registered by the library's auto-registration system.
 */
@AutoRegister(Listener.class)
public class MenuListener implements Listener {

    public MenuListener(@NonNull Tools plugin) {
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(@NonNull InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();

        if (!(holder instanceof Menu menu)) {
            return;
        }

        int slot = event.getRawSlot();

        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        menu.handleClick(slot, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(@NonNull InventoryDragEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();

        if (holder instanceof Menu) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(@NonNull InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();

        if (!(holder instanceof Menu menu)) {
            return;
        }

        menu.handleClose(event);
    }
}