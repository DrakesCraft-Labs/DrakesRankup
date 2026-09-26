package com.drakescraft.rankup.gui;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.PlayerSettings;
import com.drakescraft.rankup.model.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TransformationMenu implements InventoryHolder {

    private final DrakesRankupPlugin plugin;
    private final Player player;
    private Inventory inv;

    public TransformationMenu(DrakesRankupPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    @Override
    public Inventory getInventory() {
        return this.inv;
    }

    public void open() {
        this.inv = Bukkit.createInventory(this, 54, ChatColor.translateAlternateColorCodes('&', "&8⚡ &6&lTRANSFORMACIONES Y HABILIDADES"));

        // Fill background with black glass panes
        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, bg);
        }

        Rank rank = plugin.getRankManager().getPlayerRank(player.getUniqueId());
        int tier = (rank != null) ? rank.getTier() : 0;
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(player.getUniqueId());
        String active = settings.getActiveTransformation();

        // 1. SSJ God (Tier 35)
        inv.setItem(10, buildTransformationItem(
                tier >= 35,
                "SSJ_GOD".equalsIgnoreCase(active),
                Material.REDSTONE_BLOCK,
                "&c&lSuper Saiyajin God",
                35,
                Arrays.asList(
                        "&8▪ &eTipo: &fKi Divino Carmesí",
                        "&8▪ &aEfectos: &fRegeneración II, Absorción II y Agilidad Divina (5m).",
                        "&8▪ &bActivación: &fSneak (Shift) sostenido hasta detonar."
                )
        ));

        // 2. SSJ Blue (Tier 36)
        inv.setItem(11, buildTransformationItem(
                tier >= 36,
                "SSJ_BLUE".equalsIgnoreCase(active),
                Material.LAPIS_BLOCK,
                "&9&lSuper Saiyajin Blue",
                36,
                Arrays.asList(
                        "&8▪ &eTipo: &fKi Divino Azul Super Saiyajin",
                        "&8▪ &aEfectos: &fFuerza II, Velocidad II y Resistencia I por 5 minutos.",
                        "&8▪ &bActivación: &fSneak (Shift) sostenido hasta detonar."
                )
        ));

        // 3. Ultra Instinto (MUI) (Tier 46)
        inv.setItem(12, buildTransformationItem(
                tier >= 46,
                "MUI".equalsIgnoreCase(active),
                Material.NETHER_STAR,
                "&f&lDoctrina Egoísta (MUI)",
                46,
                Arrays.asList(
                        "&8▪ &eTipo: &fEstado de los Ángeles",
                        "&8▪ &aEfectos: &fEvasión total milagrosa de ataques por 60 segundos.",
                        "&8▪ &cDesgaste: &7El uso repetido en menos de 2m induce fatiga corporal.",
                        "&8▪ &bActivación: &fSneak (Shift) sostenido hasta detonar."
                )
        ));

        // 4. Ultra Ego (Mega Instinto) (Tier 47)
        inv.setItem(13, buildTransformationItem(
                tier >= 47,
                "ULTRA_EGO".equalsIgnoreCase(active),
                Material.PURPLE_GLAZED_TERRACOTTA,
                "&5&lMega Instinto (Ultra Ego)",
                47,
                Arrays.asList(
                        "&8▪ &eTipo: &fPoder del Dios de la Destrucción",
                        "&8▪ &aEfectos: &fA menor vida, mayor daño destructivo por 5 minutos.",
                        "&8▪ &bActivación: &fSneak (Shift) sostenido hasta detonar."
                )
        ));

        // 5. Gohan Beast (Tier 45)
        inv.setItem(14, buildTransformationItem(
                tier >= 45,
                "GOHAN_BEAST".equalsIgnoreCase(active),
                Material.AMETHYST_CLUSTER,
                "&d&lGohan Bestia (Beast)",
                45,
                Arrays.asList(
                        "&8▪ &eTipo: &fFuria Híbrida Desatada",
                        "&8▪ &aEfectos: &f+75% probabilidad crítica y rayos magentas por 5m.",
                        "&8▪ &bActivación: &fSneak (Shift) sostenido hasta detonar."
                )
        ));

        // 6. Broly Berserker (Tier 34)
        inv.setItem(15, buildTransformationItem(
                tier >= 34,
                "BROLY_LSSJ".equalsIgnoreCase(active),
                Material.EMERALD_BLOCK,
                "&a&lBroly Legendario (LSSJ)",
                34,
                Arrays.asList(
                        "&8▪ &eTipo: &fPoder Berserker Destructivo",
                        "&8▪ &aEfectos: &fFuerza III, Resistencia II e Inmunidad a empuje (5m).",
                        "&8▪ &bActivación: &fSneak (Shift) sostenido hasta detonar."
                )
        ));

        // 7. Espada de Vegetto (Tier 48)
        inv.setItem(16, buildTransformationItem(
                tier >= 48,
                "VEGETTO_SWORD".equalsIgnoreCase(active),
                Material.GOLDEN_SWORD,
                "&e&lEspada de Luz (Vegetto)",
                48,
                Arrays.asList(
                        "&8▪ &eTipo: &fTécnica de Fusión Saiyajin",
                        "&8▪ &aEfectos: &fDispara un haz perforante dorado a 14 bloques.",
                        "&8▪ &bActivación: &fShift + Clic Derecho con espada en mano."
                )
        ));

        // 8. Super Saiyajin 2 (Tier 33)
        inv.setItem(17, buildTransformationItem(
                tier >= 33,
                "SSJ_2".equalsIgnoreCase(active),
                Material.LIGHTNING_ROD,
                "&e&lSuper Saiyajin 2",
                33,
                Arrays.asList(
                        "&8▪ &eTipo: &fKi Dorado Ascendido con Bio-Electricidad",
                        "&8▪ &aEfectos: &fArcos eléctricos permanentes, Fuerza II y Velocidad II (5m).",
                        "&8▪ &bActivación: &fSneak (Shift) sostenido hasta detonar."
                )
        ));

        // 9. Fruta Mera Mera (Tier 55)
        inv.setItem(19, buildTransformationItem(
                tier >= 55,
                "MERA_MERA".equalsIgnoreCase(active),
                Material.BLAZE_POWDER,
                "&6&lFruta Mera Mera (Ace / Sabo)",
                55,
                Arrays.asList(
                        "&8▪ &eTipo: &fFruta del Diablo Tipo Logia (Fuego)",
                        "&8▪ &aEfectos: &fInmunidad a fuego, Puño de Fuego (Hiken) y Entei solar.",
                        "&8▪ &bActivación: &fClic Derecho (Hiken) / Shift + Clic Der (Entei)."
                )
        ));

        // 10. Fruta Ope Ope (Tier 65)
        inv.setItem(20, buildTransformationItem(
                tier >= 65,
                "OPE_OPE".equalsIgnoreCase(active),
                Material.HEART_OF_THE_SEA,
                "&b&lFruta Ope Ope (Trafalgar Law)",
                65,
                Arrays.asList(
                        "&8▪ &eTipo: &fFruta de la Operación Quirúrgica",
                        "&8▪ &aEfectos: &fDomo ROOM y Shambles (intercambio de posición).",
                        "&8▪ &bActivación: &fClic Derecho (ROOM) / Shift + Clic Der (Shambles)."
                )
        ));

        // 11. Fruta Pika Pika (Tier 75)
        inv.setItem(21, buildTransformationItem(
                tier >= 75,
                "PIKA_PIKA".equalsIgnoreCase(active),
                Material.GLOWSTONE,
                "&e&lFruta Pika Pika (Kizaru)",
                75,
                Arrays.asList(
                        "&8▪ &eTipo: &fFruta de la Luz Absoluta",
                        "&8▪ &aEfectos: &fVelocidad fotónica y salto de luz Yata no Kagami.",
                        "&8▪ &bActivación: &fShift + Clic Izquierdo (Salto de Luz)."
                )
        ));

        // 12. Fruta Gura Gura (Tier 85)
        inv.setItem(22, buildTransformationItem(
                tier >= 85,
                "GURA_GURA".equalsIgnoreCase(active),
                Material.IRON_BLOCK,
                "&f&lFruta Gura Gura (Barbablanca)",
                85,
                Arrays.asList(
                        "&8▪ &eTipo: &fFruta del Terremoto Sísmico",
                        "&8▪ &aEfectos: &fGolpe que agrieta el espacio y lanza enemigos por el aire.",
                        "&8▪ &bActivación: &fClic Izquierdo en combate."
                )
        ));

        // 13. Vórtice Magnético (Tier 60)
        inv.setItem(23, buildTransformationItem(
                tier >= 60,
                "JIKI_MAGNET".equalsIgnoreCase(active),
                Material.LODESTONE,
                "&b&lVórtice Magnético (Jiki Jiki)",
                60,
                Arrays.asList(
                        "&8▪ &eTipo: &fMagnetismo Polar Atractor",
                        "&8▪ &aEfectos: &fAtrae al instante todos los ítems en un radio de 200 bloques.",
                        "&8▪ &bActivación: &fShift + Clic Derecho o comando /rankup magnet."
                )
        ));

        // 14. Filos Danzantes / Aura Kill (Tier 70)
        inv.setItem(24, buildTransformationItem(
                tier >= 70,
                "DANCING_BLADES".equalsIgnoreCase(active),
                Material.DIAMOND_SWORD,
                "&c&lFilos Danzantes (Aura Kill)",
                70,
                Arrays.asList(
                        "&8▪ &eTipo: &fMaestría Telequinética de Armas",
                        "&8▪ &aEfectos: &fLas espadas y hachas flotan y golpean solas en 10 bloques.",
                        "&8▪ &bActivación: &fHaz clic para alternar el Aura Kill autónomo."
                )
        ));

        // 15. Sun God Nika (Gear 5) (Tier 88)
        inv.setItem(25, buildTransformationItem(
                tier >= 88,
                "GEAR_FIVE".equalsIgnoreCase(active),
                Material.NETHER_STAR,
                "&f&l☀ Sun God Nika (Gear 5)",
                88,
                Arrays.asList(
                        "&8▪ &eTipo: &fGuerrero de la Liberación Shonen",
                        "&8▪ &aEfectos: &fTambores de júbilo, rebote cartoon sin daño de caída,",
                        "  &fLanzamiento de Rayos Kaminari y Puño Colosal Bajrang Gun (5m).",
                        "&8▪ &bActivación: &fShift + Clic Izquierdo (Bajrang) / Clic Izq (Kaminari)."
                )
        ));

        // 16. Luffy Gomu Gomu (Gear 2, 3 y 4 Bounceman) (Tier 30)
        inv.setItem(28, buildTransformationItem(
                tier >= 30,
                "GOMU_GOMU".equalsIgnoreCase(active),
                Material.SLIME_BALL,
                "&c&lFruta Gomu Gomu (Luffy)",
                30,
                Arrays.asList(
                        "&8▪ &eTipo: &fHombre de Goma",
                        "&8▪ &aEfectos: &fGear 2 (vapor), Gear 3 (gigante 2.8) y Gear 4 (Bounceman).",
                        "  &f¡El Gear 4 bota continuamente en el suelo como en el anime!",
                        "&8▪ &bActivación: &fSneak + Clic Derecho (Gear 3) / Sneak + Clic Izq (Gear 4)."
                )
        ));

        // 17. Maestría Santoryu (Zoro) (Tier 24)
        inv.setItem(29, buildTransformationItem(
                tier >= 24,
                "SANTORYU".equalsIgnoreCase(active),
                Material.IRON_SWORD,
                "&2&lEstilo Tres Espadas (Santoryu - Zoro)",
                24,
                Arrays.asList(
                        "&8▪ &eTipo: &fMaestría de Espadachín de Wano",
                        "&8▪ &aEfectos Positivos: &f+25% daño físico permanente con espadas",
                        "  &fy Corte del Dragón Volador de viento a distancia.",
                        "&8▪ &bActivación: &fClic Derecho con espada en combate."
                )
        ));

        // 18. Kaio-ken Aumentado (Tier 92)
        inv.setItem(30, buildTransformationItem(
                tier >= 92,
                "KAIOKEN".equalsIgnoreCase(active),
                Material.RED_CANDLE,
                "&c&lKaio-ken (x3 / x10 / x20)",
                92,
                Arrays.asList(
                        "&8▪ &eTipo: &fMultiplicador Divino Saiyajin",
                        "&8▪ &aEfectos Positivos: &fVelocidad III, Fuerza II, Salto II y Vuelo Libre (5m).",
                        "&8▪ &bActivación: &fSneak (Shift) sostenido hasta detonar."
                )
        ));

        // 19. Vuelo de Ki Supersónico (Tier 31+)
        inv.setItem(31, createItem(
                tier >= 31 ? Material.FIREWORK_ROCKET : Material.GUNPOWDER,
                tier >= 31 ? "&b&l⚡ Vuelo de Ki Supersónico" : "&c🔒 Vuelo de Ki (Requiere Tier 31)",
                "&8▪ &eTipo: &fPropulsión Aérea Shonen",
                "&8▪ &aEfectos: &fSuper impulso a 100 bloques con W + Doble Salto (CD escalonado).",
                "",
                tier >= 31 ? (settings.isKiFlightEnabled() ? "&a✔ HABILITADO &7(Clic para alternar)" : "&c✖ DESHABILITADO &7(Clic para activar)") : "&cBloqueado por rango."
        ));

        // Desequipar (Slot 40)
        inv.setItem(40, createItem(Material.BARRIER, "&c&lDesequipar Transformación Activa",
                "&7Quita la transformación seleccionada y regresa",
                "&7al aura natural y pasiva de tu rango actual.",
                "",
                "&eClic para desequipar"
        ));

        // Volver al menú de Rangos (Slot 49)
        inv.setItem(49, createItem(Material.ARROW, "&a◀ Volver al Menú de Rangos", "&7Abrir la interfaz principal de /rankup"));

        player.openInventory(this.inv);
    }

    private ItemStack buildTransformationItem(boolean unlocked, boolean isEquipped, Material mat, String name, int reqTier, List<String> details) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Nivel Requerido: &6Tier " + reqTier);
        lore.add("");
        lore.addAll(details);
        lore.add("");

        if (isEquipped) {
            lore.add("&a✔ ¡EQUIPADA Y ACTIVA!");
            return createItem(Material.ENCHANTED_BOOK, name + " &8(&aEQUIPADO&8)", lore);
        } else if (unlocked) {
            lore.add("&e▶ Desbloqueada. Haz clic para equiparla como activa.");
            return createItem(mat, name + " &8(&eDisponible&8)", lore);
        } else {
            lore.add("&c🔒 Bloqueada. Debes ascender hasta el Tier " + reqTier + ".");
            return createItem(Material.RED_STAINED_GLASS_PANE, name + " &8(&cBloqueada&8)", lore);
        }
    }

    public void handleClick(InventoryClickEvent e, Player p) {
        int slot = e.getRawSlot();
        Rank rank = plugin.getRankManager().getPlayerRank(p.getUniqueId());
        int tier = (rank != null) ? rank.getTier() : 0;
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(p.getUniqueId());

        // Back to /rankup (Slot 49)
        if (slot == 49) {
            new RankupMenu(plugin, p, 0).open();
            return;
        }

        // Unequip (Slot 40)
        if (slot == 40) {
            settings.setActiveTransformation(null);
            plugin.getRankManager().savePlayerData();
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.8f);
            p.sendMessage("§b[Transformación] §7Has desequipado tu transformación. Aura restablecida a la base de tu rango.");
            open();
            return;
        }

        // Toggle Ki Flight (Slot 31)
        if (slot == 31) {
            if (tier < 31) {
                p.sendMessage("§c[Rankup] Necesitas alcanzar el Tier 31 (Saiyajin) para usar el Vuelo de Ki.");
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            settings.setKiFlightEnabled(!settings.isKiFlightEnabled());
            plugin.getRankManager().savePlayerData();
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
            p.sendMessage("§b[Rankup] §7Vuelo de Ki supersónico: " + (settings.isKiFlightEnabled() ? "§aHabilitado" : "§cDeshabilitado"));
            open();
            return;
        }

        // Selection Slots
        String newTrans = null;
        int reqTier = 0;

        if (slot == 10) { newTrans = "SSJ_GOD"; reqTier = 35; }
        else if (slot == 11) { newTrans = "SSJ_BLUE"; reqTier = 36; }
        else if (slot == 12) { newTrans = "MUI"; reqTier = 46; }
        else if (slot == 13) { newTrans = "ULTRA_EGO"; reqTier = 47; }
        else if (slot == 14) { newTrans = "GOHAN_BEAST"; reqTier = 45; }
        else if (slot == 15) { newTrans = "BROLY_LSSJ"; reqTier = 34; }
        else if (slot == 16) { newTrans = "VEGETTO_SWORD"; reqTier = 48; }
        else if (slot == 17) { newTrans = "SSJ_2"; reqTier = 33; }
        else if (slot == 19) { newTrans = "MERA_MERA"; reqTier = 55; }
        else if (slot == 20) { newTrans = "OPE_OPE"; reqTier = 65; }
        else if (slot == 21) { newTrans = "PIKA_PIKA"; reqTier = 75; }
        else if (slot == 22) { newTrans = "GURA_GURA"; reqTier = 85; }
        else if (slot == 23) { newTrans = "JIKI_MAGNET"; reqTier = 60; }
        else if (slot == 24) { newTrans = "DANCING_BLADES"; reqTier = 70; }
        else if (slot == 25) { newTrans = "GEAR_FIVE"; reqTier = 88; }
        else if (slot == 28) { newTrans = "GOMU_GOMU"; reqTier = 30; }
        else if (slot == 29) { newTrans = "SANTORYU"; reqTier = 24; }
        else if (slot == 30) { newTrans = "KAIOKEN"; reqTier = 92; }

        if (newTrans != null) {
            if (tier < reqTier) {
                p.sendMessage("§c[Transformación] Requiere Tier " + reqTier + " para desbloquear esta técnica.");
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            settings.setActiveTransformation(newTrans);
            plugin.getRankManager().savePlayerData();
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1.0f, 1.2f);
            p.sendMessage("§a[Transformación] §f¡Has equipado la transformación: §e" + newTrans + "§f!");
            p.sendMessage("§7Usa los controles de la habilidad para desatar su poder.");

            // Activación inmediata para Gear 5 si se selecciona directamente
            if ("GEAR_FIVE".equalsIgnoreCase(newTrans)) {
                plugin.getOnePieceListener().activateGear5(p);
            }
            open();
        }
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        return createItem(mat, name, Arrays.asList(lore));
    }

    private ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat != null ? mat : Material.STONE);
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
}
