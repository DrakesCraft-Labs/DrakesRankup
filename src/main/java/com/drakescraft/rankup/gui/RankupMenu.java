package com.drakescraft.rankup.gui;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.PlayerSettings;
import com.drakescraft.rankup.model.Rank;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.*;

public class RankupMenu implements InventoryHolder {

    private final DrakesRankupPlugin plugin;
    @Getter
    private final Player player;
    @Getter
    private final int page; // 0 to 4 (Divisions I to V)
    public static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###,###,###.##");

    private static final String[] DIVISION_NAMES = {
            "División I: Ciencia & Nen",
            "División II: Shinobi & Hechicería",
            "División III: Ruta Pirata & Segadores",
            "División IV: Guerreros Z & Monarca",
            "División V: Trascendencia Suprema",
            "División VI: Maldiciones Supremas",
            "División VII: Shinigami Reiatsu",
            "División VIII: Monarcas & Demonios",
            "División IX: Leyendas Cósmicas",
            "División X: Dioses Multiversales"
    };

    public RankupMenu(DrakesRankupPlugin plugin, Player player, int page) {
        this.plugin = plugin;
        this.player = player;
        this.page = Math.max(0, Math.min(9, page));
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    public void open() {
        Inventory inv = Bukkit.createInventory(this, 54, "§8§lDRAKES RANKUP §8- " + DIVISION_NAMES[page]);

        // Background fillers
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, glass);
        }

        // Division Tabs for current 5-division cluster (0-4 or 5-9)
        int clusterStart = (page / 5) * 5;
        int[] tabSlots = {0, 1, 2, 6, 7};
        for (int i = 0; i < 5; i++) {
            int d = clusterStart + i;
            if (d >= 10) break;
            boolean current = (d == page);
            Material tabMat = current ? Material.GOLD_BLOCK : Material.IRON_BARS;
            String prefix = current ? "&6&l▶ " : "&7";
            inv.setItem(tabSlots[i], createItem(tabMat, prefix + DIVISION_NAMES[d],
                    "&7Niveles: &f" + ((d * 10) + 1) + " al " + ((d * 10) + 10),
                    current ? "&aPestaña activa" : "&eClic para ver esta división"
            ));
        }

        // Player Info (Slot 4)
        Rank currentRank = plugin.getRankManager().getPlayerRank(player.getUniqueId());
        int currentTier = currentRank != null ? currentRank.getTier() : 0;
        double monedero = plugin.getEconomy() != null ? plugin.getEconomy().getBalance(player) : 0.0;
        double bolsa = plugin.getRankManager().getBolsa(player.getUniqueId());
        double balance = monedero + bolsa; // fondos de ascenso: bolsa + monedero
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(player.getUniqueId());

        inv.setItem(4, createItem(Material.PLAYER_HEAD, "&e&l" + player.getName(),
                "&7Rango actual: " + (currentRank != null ? currentRank.getDisplayName() : "&7Ninguno"),
                "&7Nivel / Tier: &6" + currentTier + "/100",
                "&7Rebirth: &e" + settings.getRebirthCount() + "/50 &7(+" + (settings.getRebirthCount() * 3) + "% daño)", 
                "&7Monedero: &a$" + MONEY_FORMAT.format(monedero),
                "&7Bolsa de ascenso: &6$" + MONEY_FORMAT.format(bolsa) + " &8(/rankup bolsa)",
                "&7Habilidad: " + (currentRank != null && currentRank.getAbilityType() != null ? "&b" + currentRank.getAbilityType().getName() : "&7Ninguna"),
                "&7Empuje Cinético: " + (currentRank != null && currentRank.isHasKineticPush() ? "&aDesbloqueado" : "&cBloqueado"),
                "",
                "&8Ajustes personales:",
                "&7Empujes: " + (settings.isKineticPushEnabled() ? "&aACTIVO" : "&cDESACTIVADO"),
                "&7Partículas: " + (settings.isParticlesEnabled() ? "&aACTIVO" : "&cDESACTIVADO"),
                "&7Habilidades: " + (settings.isAbilitiesEnabled() ? "&aACTIVO" : "&cDESACTIVADO")
        ));

        // Settings Toggle Button (Slot 8)
        inv.setItem(8, createItem(Material.REPEATER, "&b&lAjustes Rápidos",
                "&7Clic para alternar tus efectos:",
                "&8▪ &7Empujes: " + (settings.isKineticPushEnabled() ? "&aActivado" : "&cDesactivado"),
                "&8▪ &7Partículas: " + (settings.isParticlesEnabled() ? "&aActivado" : "&cDesactivado"),
                "",
                "&eClic para alternar Empuje Cinético"
        ));

        // Center 10 Ranks for Current Division
        int[] slots = {19, 20, 21, 22, 23, 28, 29, 30, 31, 32};
        int startTier = (page * 10) + 1;
        int endTier = Math.min(startTier + 9, 100);

        int slotIdx = 0;
        for (int t = startTier; t <= endTier; t++) {
            Rank r = plugin.getRankManager().getRankByTier(t);
            if (r == null) continue;
            int targetSlot = slots[slotIdx++];

            String statusTitle;
            List<String> lore = new ArrayList<>();
            lore.add("&8" + r.getDivision());
            lore.add("&7Costo: &6$" + MONEY_FORMAT.format(r.getCost()));
            lore.add("&7Tipo: " + (r.isPermanent() ? "&aPermanente (Ancla)" : "&eTemporal (14 días)"));
            lore.add("");
            lore.add("&e&nVentajas y Beneficios:&r");
            for (String perk : r.getPerks()) {
                lore.add(" &8▪ &f" + ChatColor.translateAlternateColorCodes('&', perk));
            }
            if (r.getAbilityType() != null && !r.getAbilityType().name().equals("NONE")) {
                lore.add(" &8▪ &bHabilidad: " + r.getAbilityType().getName());
            }
            if (r.isHasKineticPush()) {
                lore.add(" &8▪ &aEmpuje Cinético: Potencia x" + r.getPushMultiplier() + " (CD: " + r.getPushCooldownSeconds() + "s)");
            }
            lore.add("");

            if (currentTier >= t) {
                statusTitle = "&a✔ " + r.getDisplayName() + " &8(&aDesbloqueado&8)";
                lore.add("&aYa dominas este rango.");
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

        // Bottom Controls
        // Slot 45: Núcleo de Estabilidad de Rango (Mantención)
        if (currentRank == null || currentTier == 0) {
            inv.setItem(45, createItem(Material.RESPAWN_ANCHOR, "&8🏺 Núcleo de Estabilidad",
                    "&7No posees ningún rango activo.",
                    "&7Asciende al Tier 1 para iniciar tu camino."));
        } else if (currentRank.isPermanent()) {
            inv.setItem(45, createItem(Material.BEACON, "&a&l🏺 Rango Permanente",
                    "&7Tu rango: " + currentRank.getDisplayName(),
                    "&8" + currentRank.getDivision(),
                    "",
                    "&a✔ ¡RANGO ETERNO (ANCLA)!",
                    "&7Este rango actúa como un ancla sagrada.",
                    "&7No requiere tributo ni sufre desgaste."));
        } else {
            long remainingMs = plugin.getRankManager().getRemainingMaintenanceMs(player.getUniqueId());
            long days = remainingMs / 86400000L;
            long hours = (remainingMs % 86400000L) / 3600000L;
            double mCost = currentRank.getMaintenanceCost();
            boolean affordM = balance >= mCost;

            Material mIcon;
            String statusColor;
            String statusText;
            if (days >= 3) {
                mIcon = Material.RESPAWN_ANCHOR;
                statusColor = "&a";
                statusText = "ÓPTIMO";
            } else if (days >= 1) {
                mIcon = Material.CRYING_OBSIDIAN;
                statusColor = "&e";
                statusText = "INESTABLE";
            } else {
                mIcon = Material.REDSTONE_BLOCK;
                statusColor = "&c";
                statusText = "CRÍTICO";
            }

            int filledBars = (int) Math.max(0, Math.min(10, (days * 10) / 14));
            StringBuilder bar = new StringBuilder("&8[");
            for (int b = 0; b < 10; b++) {
                if (b < filledBars) {
                    bar.append(statusColor).append("▮");
                } else {
                    bar.append("&8▯");
                }
            }
            bar.append("&8]");

            inv.setItem(45, createItem(mIcon, "&6&l🏺 Núcleo de Estabilidad de Rango",
                    "&7Rango: " + currentRank.getDisplayName() + " &8(Temporal)",
                    "&7Estabilidad: " + statusColor + statusText + " " + bar.toString(),
                    "&7Tiempo restante: " + statusColor + days + "d " + hours + "h",
                    "",
                    "&7Costo de mantención: &6$" + MONEY_FORMAT.format(mCost) + " &7(+14 días)",
                    "",
                    affordM ? "&a▶ Clic para alimentar el Núcleo y renovar" : "&c✖ Fondos insuficientes para renovar"));
        }

        // Help & Info Button (Slot 46)
        inv.setItem(46, createItem(Material.BOOK, "&e&lInformación de Rankup",
                "&7100 Rangos Anime Shonen",
                "&7Economía 100% in-game (Dragmas)",
                "&7Totalmente compatible con VIP Dioses",
                "",
                "&fComandos útiles: &6/rankup&f, &6/rankup max, &6/rankup mantener"
        ));

        // Transformation Menu Button (Slot 47)
        inv.setItem(47, createItem(Material.DRAGON_BREATH, "&d&l⚡ Menú de Transformaciones",
                "&7Accede al selector de transformaciones anime,",
                "&7habilidades, vuelo de Ki y técnicas especiales.",
                "",
                "&eClic para abrir /transform"
        ));

        if (page > 0) {
            inv.setItem(48, createItem(Material.ARROW, "&a◀ División Anterior", "&7Ir a " + DIVISION_NAMES[page - 1]));
        }

        Rank next = plugin.getRankManager().getNextRank(player.getUniqueId());
        if (next != null) {
            boolean afford = balance >= next.getCost();
            inv.setItem(49, createItem(Material.NETHER_STAR, "&6&lASCENDER AL SIGUIENTE RANGO",
                    "&7Siguiente: " + next.getDisplayName(),
                    "&7Precio: &6$" + MONEY_FORMAT.format(next.getCost()),
                    "",
                    afford ? "&a✔ Haz clic para pagar y subir ahora" : "&c✖ No tienes fondos suficientes"
            ));
        } else {
            inv.setItem(49, createItem(Material.BEACON, "&6&l¡RANGO MAXIMO ALCANZADO!", "&aHas completado los 100 rangos anime de DrakesCraft. Usa /rebirth para renacer."));
        }

        if (page < 9) {
            inv.setItem(50, createItem(Material.ARROW, "&aDivisión Siguiente ▶", "&7Ir a " + DIVISION_NAMES[page + 1]));
        }

        // Rankup Kit Button (Slot 51)
        int div = currentTier > 0 ? Math.min(5, ((currentTier - 1) / 10) + 1) : 0;
        boolean canClaimKit = div > 0 && plugin.getKitManager() != null && plugin.getKitManager().canClaim(player.getUniqueId(), div);
        long remMs = (div > 0 && plugin.getKitManager() != null) ? plugin.getKitManager().getRemainingCooldownMs(player.getUniqueId(), div) : 0;
        long h = remMs / (1000 * 60 * 60);
        long m = (remMs % (1000 * 60 * 60)) / (1000 * 60);

        inv.setItem(51, createItem(Material.CHEST_MINECART, "&6&l🎁 Reclamar Kit de Rankup",
                "&7Obtén el kit diario de tu división actual con",
                "&7armaduras, armas con encantamientos custom y",
                "&7Semillas del Ermitaño curativas.",
                "",
                div == 0 ? "&c✖ Requiere tener al menos Rango Tier 1" :
                        (canClaimKit ? "&a✔ ¡DISPONIBLE! Haz clic para reclamar" : "&c⏳ Disponible en &e" + h + "h " + m + "m"),
                "",
                "&eClic para ejecutar /rankup kit"
        ));

        // Max Rankup Button (Slot 52)
        inv.setItem(52, createItem(Material.EMERALD, "&a&lSUBIR AL MAXIMO POSIBLE",
                "&7Calcula tu saldo bancario y asciende",
                "&7todos los niveles continuos posibles.",
                "",
                "&eClic para ejecutar /rankup max"
        ));

        player.openInventory(inv);
    }

    public void handleClick(InventoryClickEvent e, Player p) {
        int slot = e.getRawSlot();

        // Division Tabs
        int[] tabSlots = {0, 1, 2, 6, 7};
        for (int d = 0; d < 5; d++) {
            if (slot == tabSlots[d]) {
                new RankupMenu(plugin, p, d).open();
                return;
            }
        }

        // Settings Toggle (Slot 8)
        if (slot == 8) {
            PlayerSettings s = plugin.getRankManager().getPlayerSettings(p.getUniqueId());
            s.setKineticPushEnabled(!s.isKineticPushEnabled());
            plugin.getRankManager().savePlayerData();
            p.sendMessage("§b[Rankup] §7Empuje cinético: " + (s.isKineticPushEnabled() ? "§aActivado" : "§cDesactivado"));
            new RankupMenu(plugin, p, page).open();
            return;
        }

        // Maintenance Núcleo Button (Slot 45)
        if (slot == 45) {
            Rank rank = plugin.getRankManager().getPlayerRank(p.getUniqueId());
            if (rank == null || rank.getTier() == 0) {
                p.sendMessage("§cNo tienes un rango activo que requiera mantención.");
                return;
            }
            if (rank.isPermanent()) {
                p.sendMessage("§aTu rango actual (" + rank.getDisplayName() + "§a) es permanente y no requiere mantención.");
                return;
            }
            boolean ok = plugin.getRankManager().processMaintenance(p);
            if (ok) {
                new RankupMenu(plugin, p, page).open();
            }
            return;
        }

        // Transformation Menu Button (Slot 47)
        if (slot == 47) {
            new TransformationMenu(plugin, p).open();
            return;
        }

        // Previous Page
        if (slot == 48 && page > 0) {
            new RankupMenu(plugin, p, page - 1).open();
            return;
        }

        // Next Page
        if (slot == 50 && page < 9) {
            new RankupMenu(plugin, p, page + 1).open();
            return;
        }

        // Claim Kit Button (Slot 51)
        if (slot == 51) {
            if (plugin.getKitManager() != null) {
                plugin.getKitManager().claimKit(p);
                new RankupMenu(plugin, p, page).open();
            }
            return;
        }

        // Max Rankup Button (Slot 52)
        if (slot == 52) {
            p.closeInventory();
            p.performCommand("rankup max");
            return;
        }

        // Rankup Next Button (Slot 49)
        if (slot == 49) {
            p.closeInventory();
            p.performCommand("rankup");
            return;
        }

        // Click on current available rank to buy directly
        int[] rankSlots = {19, 20, 21, 22, 23, 28, 29, 30, 31, 32};
        int startTier = (page * 10) + 1;
        int currentTier = plugin.getRankManager().getPlayerTier(p.getUniqueId());

        for (int i = 0; i < rankSlots.length; i++) {
            if (slot == rankSlots[i]) {
                int clickedTier = startTier + i;
                if (clickedTier == currentTier + 1) {
                    p.closeInventory();
                    p.performCommand("rankup");
                }
                return;
            }
        }
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
            for (String line : lore) {
                coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
