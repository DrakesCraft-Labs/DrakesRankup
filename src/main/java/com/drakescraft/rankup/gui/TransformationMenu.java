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
        return inv;
    }

    public void open() {
        this.inv = Bukkit.createInventory(this, 54, "§8§l⚡ TRANSFORMACIONES & PODERES");

        // Glass background fill
        ItemStack grayGlass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, grayGlass);
        }

        Rank rank = plugin.getRankManager().getPlayerRank(player.getUniqueId());
        int tier = (rank != null) ? rank.getTier() : 0;
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(player.getUniqueId());
        String active = settings.getActiveTransformation();
        if (active == null || active.isEmpty()) active = "NINGUNA";

        // Slot 4: Player Info Header
        inv.setItem(4, createItem(Material.NETHER_STAR, "&e&lPERFIL DE TRANSFORMACIÓN",
                "&7Jugador: &f" + player.getName(),
                "&7Rango Actual: " + (rank != null ? rank.getDisplayName() : "&7Ninguno") + " &8(Tier " + tier + ")",
                "&7Transformación Activa: &a" + active,
                "&7Vuelo de Ki: " + (settings.isKiFlightEnabled() ? "&aHabilitado" : "&cDeshabilitado"),
                "",
                "&eHaz clic en una transformación desbloqueada para equiparla."
        ));

        // Transformations
        // 1. SSJ God (Tier 35)
        inv.setItem(10, buildTransformationItem(
                tier >= 35,
                "SSJ_GOD".equalsIgnoreCase(active),
                Material.RED_DYE,
                "&c&lSuper Saiyajin God",
                35,
                Arrays.asList(
                        "&8▪ &eTipo: &fKi Divino Carmesí",
                        "&8▪ &aEfectos Positivos: &fRegeneración y Velocidad Divina por 20s.",
                        "&8▪ &cContrapartida: &7Requiere cargar Ki agachado (1.8s) inmóvil.",
                        "&8▪ &bActivación: &fSneak (Shift) sostenido hasta detonar."
                )
        ));

        // 2. SSJ Blue (Tier 36)
        inv.setItem(11, buildTransformationItem(
                tier >= 36,
                "SSJ_BLUE".equalsIgnoreCase(active),
                Material.CYAN_DYE,
                "&9&lSuper Saiyajin Blue",
                36,
                Arrays.asList(
                        "&8▪ &eTipo: &fKi Divino de Plasma",
                        "&8▪ &aEfectos Positivos: &fFuerza I y Velocidad II por 20 segundos.",
                        "&8▪ &cContrapartida: &7Agotamiento de energía con cooldown de 45s.",
                        "&8▪ &bActivación: &fSneak (Shift) sostenido hasta detonar."
                )
        ));

        // 3. Ultra Instinto Dominado (Tier 46)
        inv.setItem(12, buildTransformationItem(
                tier >= 46,
                "MUI".equalsIgnoreCase(active),
                Material.FEATHER,
                "&f&lUltra Instinto Dominado (MUI)",
                46,
                Arrays.asList(
                        "&8▪ &eTipo: &fDoctrina Egoísta Suprema",
                        "&8▪ &aEfectos Positivos: &f100% de esquive visual de golpes y flechas por 5s.",
                        "&8▪ &cContrapartida: &c¡FATIGA MORTAL! Si se usa >2 veces en 2 min,",
                        "  &7al terminar recibes 4 corazones de daño, Ceguera y Lentitud III.",
                        "&8▪ &bActivación: &fSneak (Shift) sostenido hasta detonar."
                )
        ));

        // 4. Mega Instinto / Ultra Ego (Tier 47)
        inv.setItem(13, buildTransformationItem(
                tier >= 47,
                "ULTRA_EGO".equalsIgnoreCase(active),
                Material.PURPLE_DYE,
                "&5&lMega Instinto (Ultra Ego)",
                47,
                Arrays.asList(
                        "&8▪ &eTipo: &fPoder Hakai de la Destrucción",
                        "&8▪ &aEfectos Positivos: &fA menor vida, mayor daño destructivo (+60%)",
                        "  &fy devuelve 3.5 corazones de daño puro al atacante.",
                        "&8▪ &cContrapartida: &7Recibe +15% de daño en impactos iniciales.",
                        "&8▪ &bActivación: &fSneak (Shift) sostenido hasta detonar."
                )
        ));

        // 5. Gohan Beast (Tier 45)
        inv.setItem(14, buildTransformationItem(
                tier >= 45,
                "GOHAN_BEAST".equalsIgnoreCase(active),
                Material.AMETHYST_SHARD,
                "&d&lGohan Bestia (Beast)",
                45,
                Arrays.asList(
                        "&8▪ &eTipo: &fEvolución Bestial Suprema",
                        "&8▪ &aEfectos Positivos: &fExplosión crítica devastadora (+75% daño)",
                        "  &fy Makankosappo con rayos magentas fulminantes.",
                        "&8▪ &cContrapartida: &cMetabolismo voraz: &7Consume 4 muslos de comida",
                        "  &fde golpe al activarse e induce Hambre II por 10s.",
                        "&8▪ &bActivación: &fSneak (Shift) sostenido hasta detonar."
                )
        ));

        // 6. Broly LSSJ Berserk (Tier 34)
        inv.setItem(15, buildTransformationItem(
                tier >= 34,
                "BROLY_LSSJ".equalsIgnoreCase(active),
                Material.LIME_DYE,
                "&a&lBroly Super Saiyajin Legendario",
                34,
                Arrays.asList(
                        "&8▪ &eTipo: &fFuria Berserker Ilimitada",
                        "&8▪ &aEfectos Positivos: &fFuerza III, Resistencia II e inmunidad total al empuje.",
                        "&8▪ &cContrapartida: &cLentitud I por 6s al apagarse la furia y +20%",
                        "  &7de vulnerabilidad ante flechas y ataques a distancia.",
                        "&8▪ &bActivación: &fSneak (Shift) sostenido hasta detonar."
                )
        ));

        // 7. Espada de Haz de Luz de Vegetto (Tier 48)
        inv.setItem(16, buildTransformationItem(
                tier >= 48,
                "VEGETTO_SWORD".equalsIgnoreCase(active),
                Material.GOLDEN_SWORD,
                "&e&lEspada de Haz de Luz (Vegetto)",
                48,
                Arrays.asList(
                        "&8▪ &eTipo: &fTécnica Ki Perforante de Fusión",
                        "&8▪ &aEfectos Positivos: &fDispara un rayo láser de energía dorada",
                        "  &fque perfora a todos los enemigos a su paso hasta 14 bloques.",
                        "&8▪ &cContrapartida: &7Cooldown de técnica de 12 segundos.",
                        "&8▪ &bActivación: &fShift + Clic Derecho empuñando cualquier espada."
                )
        ));

        // Row 2: One Piece & Special Powers
        // 8. Fruta Gomu Gomu (Luffy) (Tier 30)
        inv.setItem(28, buildTransformationItem(
                tier >= 30,
                "GOMU_GOMU".equalsIgnoreCase(active),
                Material.MAGMA_CREAM,
                "&c&lFruta del Diablo: Gomu Gomu no Mi",
                30,
                Arrays.asList(
                        "&8▪ &eTipo: &fDespertar Pirata (JoyBoy / Luffy)",
                        "&8▪ &aEfectos Positivos: &fGear Second con vapor corporal y Velocidad III,",
                        "  &fmás ráfagas ígneas de golpes Gatling / Red Hawk.",
                        "&8▪ &cContrapartida: &c¡Maldición del Océano! &7El contacto con el agua",
                        "  &fte impone Debilidad mortal, Lentitud III y fatiga inmediata.",
                        "&8▪ &bActivación: &fSneak (Shift) sostenido hasta detonar."
                )
        ));

        // 9. Maestría Santoryu (Zoro) (Tier 24)
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
                        "&8▪ &cContrapartida: &7Requiere blandir espada; técnica con recarga de 8s.",
                        "&8▪ &bActivación: &fClic Derecho con espada en combate."
                )
        ));

        // 10. Vuelo de Ki Supersónico (Tier 31+)
        inv.setItem(31, createItem(
                tier >= 31 ? Material.FIREWORK_ROCKET : Material.GUNPOWDER,
                tier >= 31 ? "&b&l⚡ Vuelo de Ki Supersónico" : "&c🔒 Vuelo de Ki (Requiere Tier 31)",
                "&8▪ &eTipo: &fPropulsión Aérea Shonen",
                "&8▪ &aEfectos Positivos: &fVuelo supersónico a 2.5x velocidad con onda sónica.",
                "&8▪ &cContrapartida: &cAltamente limitado: &7Dura máximo 4 segundos,",
                "  &7con un tiempo de recarga de 30 segundos (evita vuelo libre ilimitado).",
                "&8▪ &bActivación: &fDoble toque de espacio o Esprintar + Salto.",
                "",
                tier >= 31 ? (settings.isKiFlightEnabled() ? "&a✔ HABILITADO &7(Clic para alternar)" : "&c✖ DESHABILITADO &7(Clic para activar)") : "&cBloqueado por rango."
        ));

        // 11. Desequipar / Resetear a Aura Base (Slot 40)
        inv.setItem(40, createItem(Material.BARRIER, "&c&lDesequipar Transformación Activa",
                "&7Quita la transformación seleccionada y regresa",
                "&7al aura natural y pasiva de tu rango actual.",
                "",
                "&eClic para desequipar"
        ));

        // 12. Volver al menú de Rangos (Slot 49)
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
            lore.add("&a✔ ¡EQUIPADA Y LISTA PARA DETONAR!");
            lore.add("&7Agáchate para iniciar la carga de Ki.");
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
        else if (slot == 28) { newTrans = "GOMU_GOMU"; reqTier = 30; }
        else if (slot == 29) { newTrans = "SANTORYU"; reqTier = 24; }

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
            p.sendMessage("§7Usa Shift sostenido (o los controles de la habilidad) para desatar su poder.");
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
