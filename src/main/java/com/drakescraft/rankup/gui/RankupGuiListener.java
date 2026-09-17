package com.drakescraft.rankup.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class RankupGuiListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof RankupMenu menu) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                menu.handleClick(event, player);
            }
        }
    }
}
