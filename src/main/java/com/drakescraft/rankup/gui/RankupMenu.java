package com.drakescraft.rankup.gui;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.*;

public class RankupMenu implements Listener, InventoryHolder {

    private final DrakesRankupPlugin plugin;
    private final Player player;
    private int page = 0;
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###,###,###.##");

    public RankupMenu(DrakesRankupPlugin plugin, Player player, int page) {
        this.plugin = plugin;
        this.player = player;
        this.page = page;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    public void open() {
        int currentTier = plugin.getRankManager().getPlayerTier(player.getUniqueId());
        String title = ChatColor.DARK_GRAY + "DrakesRankup ✦ Pagina " + (page + 1) + "/5";
        Inventory inv = Bukkit.createInventory(this, 54, title);

        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inv.setItem(i, glass);
        for (int i = 45; i < 54; i++) inv.setItem(i, glass);
        inv.setItem(9, glass); inv.setItem(18, glass); inv.setItem(27, glass); inv.setItem(36, glass);
        inv.setItem(17, glass); inv.setItem(26, glass); inv.setItem(35, glass); inv.setItem(44, glass);

        Rank currentRank = plugin.getRankManager().getPlayerRank(player.getUniqueId());
        String currentName = currentRank != null ? currentRank.getDisplayName() : "&7Sin Rango";
        double balance = plugin.getEconomy() != null ? plugin.getEconomy().getBalance(player) : 0.0;
        ItemStack skull = createItem(Material.PLAYER_HEAD, "&e&lTu Perfil de Rango",
                "&7Rango Actual: " + currentName,
                "&7Progreso: &a" + currentTier + " &7/ &e50",
                "&7Saldo en Vault: &a$" + MONEY_FORMAT.format(balance)
        );
        inv.setItem(4, skull);

        int[] slots = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33};
        int startTier = (page * 10) + 1;
        int endTier = Math.min(startTier + 9, 50);

        int slotIdx = 0;
        for (int t = startTier; t <= endTier; t++) {
            Rank r = plugin.getRankManager().getRankByTier(t);
            if (r == null) continue;
            int targetSlot = slots[slotIdx++];

            String statusTitle;
            List<String> lore = new ArrayList<>();
            lore.add("&8" + r.getDivision());
            lore.add("&7Costo: &6$" + MONEY_FORMAT.format(r.getCost()));
            lore.add("");
            lore.add("&e&nVentajas y Beneficios:&r");
            for (String perk : r.getPerks()) {
                lore.add(" &8▪ &f" + ChatColor.translateAlternateColorCodes('&', perk));
            }
            lore.add("");

            if (currentTier >= t) {
                statusTitle = "&a✔ " + r.getDisplayName() + " &8(&aCompletado&8)";
                lore.add("&aYa has desbloqueado este rango.");
                inv.setItem(targetSlot, createItem(Material.LIME_DYE, statusTitle, lore));
            } else if (t == currentTier + 1) {
                statusTitle = "&e⚡ " + r.getDisplayName() + " &8(&e¡Disponible!&8)";
                boolean canAfford = balance >= r.getCost();
                if (canAfford) {
                    lore.add("&a▶ ¡Tienes suficiente dinero! Haz clic para ascender.");
                } else {
                    lore.add("&c✖ Te faltan &e$" + MONEY_FORMAT.format(r.getCost() - balance) + " &cpara ascender.");
                }
                inv.setItem(targetSlot, createItem(r.getIcon(), statusTitle, lore));
            } else {
                statusTitle = "&c🔒 " + r.getDisplayName() + " &8(&cBloqueado&8)";
                lore.add("&cDebes desbloquear los rangos anteriores primero.");
                inv.setItem(targetSlot, createItem(Material.RED_STAINED_GLASS_PANE, statusTitle, lore));
            }
        }

        if (page > 0) {
            inv.setItem(48, createItem(Material.ARROW, "&a◀ Pagina Anterior"));
        }
        Rank next = plugin.getRankManager().getNextRank(player.getUniqueId());
        if (next != null) {
            boolean afford = balance >= next.getCost();
            inv.setItem(49, createItem(Material.NETHER_STAR, "&6&lSUBIR AL SIGUIENTE RANGO",
                    "&7Siguiente: " + next.getDisplayName(),
                    "&7Precio: &6$" + MONEY_FORMAT.format(next.getCost()),
                    afford ? "&a✔ Haz clic para pagar y subir ahora" : "&c✖ No tienes fondos suficientes"
            ));
        } else {
            inv.setItem(49, createItem(Material.BEACON, "&6&l¡RANGO MAXIMO ALCANZADO!", "&aHas completado los 50 rangos."));
        }
        if (page < 4) {
            inv.setItem(50, createItem(Material.ARROW, "&aPagina Siguiente ▶"));
        }

        player.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        return createItem(mat, name, Arrays.asList(lore));
    }

    private ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> coloredLore = new ArrayList<>();
            for (String l : lore) {
                coloredLore.add(ChatColor.translateAlternateColorCodes('&', l));
            }
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof RankupMenu)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!p.getUniqueId().equals(player.getUniqueId())) return;

        int slot = e.getRawSlot();
        if (slot == 48 && page > 0) {
            new RankupMenu(plugin, p, page - 1).open();
        } else if (slot == 50 && page < 4) {
            new RankupMenu(plugin, p, page + 1).open();
        } else if (slot == 49) {
            if (plugin.getRankManager().processRankup(p)) {
                new RankupMenu(plugin, p, page).open();
            }
        }
    }
}
