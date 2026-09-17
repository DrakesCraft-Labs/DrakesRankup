package com.drakescraft.rankup.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RankupGuiListener implements Listener {

    private final Map<UUID, Long> clickDebounce = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RankupMenu menu)) return;

        // Unconditionally cancel to prevent extracting items or ghost dupes
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Block unsafe click types and rapid packet dupes
        ClickType click = event.getClick();
        if (click.isKeyboardClick() || click.isShiftClick() || click == ClickType.DOUBLE_CLICK || click == ClickType.DROP || click == ClickType.CONTROL_DROP) {
            return;
        }

        // Anti-spam debounce: 250ms
        long now = System.currentTimeMillis();
        long last = clickDebounce.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 250) {
            return;
        }
        clickDebounce.put(player.getUniqueId(), now);

        if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
            menu.handleClick(event, player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof RankupMenu) {
            event.setCancelled(true);
        }
    }
}
