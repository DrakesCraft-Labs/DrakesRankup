package com.drakescraft.rankup.manager;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.Rank;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class KitManager {

    private final DrakesRankupPlugin plugin;
    private final File kitsFile;
    private FileConfiguration kitsConfig;

    // Cooldown in millis: 24 hours
    public static final long KIT_COOLDOWN_MS = 24L * 60L * 60L * 1000L;

    // Concurrency lock for kit claims to prevent dupes
    private final Set<UUID> activeKitClaims = Collections.synchronizedSet(new HashSet<>());

    public KitManager(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
        this.kitsFile = new File(plugin.getDataFolder(), "kits.yml");
        loadKitsData();
    }

    public void loadKitsData() {
        if (!kitsFile.exists()) {
            try {
                kitsFile.getParentFile().mkdirs();
                kitsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("No se pudo crear kits.yml: " + e.getMessage());
            }
        }
        kitsConfig = YamlConfiguration.loadConfiguration(kitsFile);
    }

    public synchronized void saveKitsData() {
        if (kitsConfig == null || kitsFile == null) return;
        try {
            kitsConfig.save(kitsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error guardando kits.yml: " + e.getMessage());
        }
    }

    public long getLastClaim(UUID uuid, int division) {
        return kitsConfig.getLong(uuid.toString() + ".div_" + division, 0L);
    }

    public void setLastClaim(UUID uuid, int division, long timestamp) {
        kitsConfig.set(uuid.toString() + ".div_" + division, timestamp);
        saveKitsData();
    }

    public boolean canClaim(UUID uuid, int division) {
        long last = getLastClaim(uuid, division);
        return System.currentTimeMillis() - last >= KIT_COOLDOWN_MS;
    }

    public long getRemainingCooldownMs(UUID uuid, int division) {
        long last = getLastClaim(uuid, division);
        long elapsed = System.currentTimeMillis() - last;
        return Math.max(0, KIT_COOLDOWN_MS - elapsed);
    }

    public boolean claimKit(Player player) {
        if (player == null || !player.isOnline()) return false;
        UUID uuid = player.getUniqueId();

        if (!activeKitClaims.add(uuid)) {
            return false; // Already processing kit transaction
        }

        try {
            Rank rank = plugin.getRankManager().getPlayerRank(uuid);
            int tier = (rank != null) ? rank.getTier() : 0;

            if (tier < 1) {
                player.sendMessage("§c[Kit Rankup] Debes tener al menos el Rango Tier 1 (Senku) para reclamar un kit de rankup.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return false;
            }

            // Determine division (1 to 5)
            int division = ((tier - 1) / 10) + 1;
            division = Math.max(1, Math.min(5, division));

            if (!canClaim(uuid, division)) {
                long remMs = getRemainingCooldownMs(uuid, division);
                long hours = remMs / (1000 * 60 * 60);
                long mins = (remMs % (1000 * 60 * 60)) / (1000 * 60);
                player.sendMessage("§c[Kit Rankup] Ya has reclamado el kit de tu división hoy.");
                player.sendMessage("§7Tiempo de recarga restante: §e" + hours + "h " + mins + "m§7.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return false;
            }

            // Mark claimed atomically
            setLastClaim(uuid, division, System.currentTimeMillis());

            // Build items
            List<ItemStack> items = buildKitItems(division);

            // Give items safely (drop excess at feet)
            Location loc = player.getLocation();
            for (ItemStack it : items) {
                HashMap<Integer, ItemStack> excess = player.getInventory().addItem(it);
                for (ItemStack drop : excess.values()) {
                    player.getWorld().dropItemNaturally(loc, drop);
                }
            }

            player.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
            player.sendTitle("§6§l¡KIT DE RANKUP RECLAMADO!", "§eDivisión " + division + " de DrakesCraft", 10, 50, 10);
            player.sendMessage("§a[Kit Rankup] §f¡Has recibido con éxito el kit de tu §6División " + division + "§f con encantamientos y objetos custom!");
            return true;
        } finally {
            activeKitClaims.remove(uuid);
        }
    }

    public List<ItemStack> buildKitItems(int division) {
        List<ItemStack> list = new ArrayList<>();

        if (division == 1) {
            // División I: Ciencia & Nen
            ItemStack pick = createCustomItem(Material.DIAMOND_PICKAXE, "&e&lPico Transmutador de Senku",
                    Arrays.asList(
                            "&8▪ &eEncantamiento: Transmutación Alquímica",
                            "&8▪ &7Eficiencia IV · Fortuna II · Irrompibilidad III",
                            "&7Herramienta científica imbuida con conocimientos de la Edad de Piedra."
                    ));
            pick.addUnsafeEnchantment(Enchantment.EFFICIENCY, 4);
            pick.addUnsafeEnchantment(Enchantment.FORTUNE, 2);
            pick.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);
            list.add(pick);

            ItemStack sword = createCustomItem(Material.IRON_SWORD, "&b&lBisturí de Cromo",
                    Arrays.asList(
                            "&8▪ &bEncantamiento: Filo Analítico IV",
                            "&8▪ &7Filo IV · Saqueo II · Irrompibilidad III",
                            "&7Hoja forjada con metales puros descubiertos en la expedición."
                    ));
            sword.addUnsafeEnchantment(Enchantment.SHARPNESS, 4);
            sword.addUnsafeEnchantment(Enchantment.LOOTING, 2);
            sword.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);
            list.add(sword);

            list.add(createPotion(PotionEffectType.SPEED, 4800, 0, "&bElixir de Agilidad de Nen"));

            ItemStack medusa = createCustomItem(Material.ECHO_SHARD, "&a&lDispositivo de Petrificación: Medusa",
                    Arrays.asList(
                            "&8▪ &aTecnología Dr. Stone: Rayo Petrificador",
                            "&7Clic derecho para emitir un destello petrificador a 12m:",
                            "&8▪ &fParaliza a la entidad objetivo por 8 segundos",
                            "&8▪ &fAplica Lentitud extrema, Ceguera y Debilidad",
                            "&8▪ &7Recarga: 45s",
                            "&aHerramienta científica definitiva de Why-Man & Senku."
                    ));
            list.add(medusa);

            ItemStack nital = createCustomItem(Material.POTION, "&e&lLíquido Despetrificador (Fluido Nital)",
                    Arrays.asList(
                            "&8▪ &eFórmula: Ácido Nítrico + Etanol (Dr. Stone)",
                            "&7Clic derecho para aplicar o consumir:",
                            "&8▪ &fQuiebra instantáneamente cualquier petrificación o parálisis",
                            "&8▪ &fPurifica Lentitud, Fatiga Minera y Debilidad",
                            "&8▪ &fOtorga Velocidad II y Prisa Minera II por 15 segundos",
                            "&eEl milagro de la ciencia que despertó a la humanidad."
                    ));
            nital.setAmount(3);
            list.add(nital);

            list.add(new ItemStack(Material.BREAD, 32));

        } else if (division == 2) {
            // División II: Shinobi & Hechicería
            ItemStack katana = createCustomItem(Material.NETHERITE_SWORD, "&4&lKatana Maldita de Sukuna & Yuji",
                    Arrays.asList(
                            "&8▪ &cEncantamiento: Destello Negro (Black Flash)",
                            "&8▪ &7Filo V · Aspecto Ígneo II · Irrompibilidad III",
                            "&7Canaliza energía maldita pura al asestar impactos cuerpo a cuerpo."
                    ));
            katana.addUnsafeEnchantment(Enchantment.SHARPNESS, 5);
            katana.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 2);
            katana.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);
            list.add(katana);

            ItemStack chest = createCustomItem(Material.NETHERITE_CHESTPLATE, "&8&lManto Táctico ANBU",
                    Arrays.asList(
                            "&8▪ &bEncantamiento: Protección de las Sombras IV",
                            "&8▪ &7Protección IV · Irrompibilidad III",
                            "&7Chaleco blindado utilizado por los mejores shinobi de operaciones encubiertas."
                    ));
            chest.addUnsafeEnchantment(Enchantment.PROTECTION, 4);
            chest.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);
            list.add(chest);

            list.add(new ItemStack(Material.ENDER_PEARL, 16));
            list.add(new ItemStack(Material.COOKED_BEEF, 32));

        } else if (division == 3) {
            // División III: Ruta Pirata & Segadores
            ItemStack zanpakuto = createCustomItem(Material.NETHERITE_SWORD, "&c&lZanpakuto: Liberación de Capitán",
                    Arrays.asList(
                            "&8▪ &cEncantamiento: Reiatsu Espiritual V",
                            "&8▪ &7Filo V · Aspecto Ígneo II · Golpe V · Irrompibilidad IV",
                            "&7Espada que canaliza la presión espiritual del Gotei 13."
                    ));
            zanpakuto.addUnsafeEnchantment(Enchantment.SHARPNESS, 5);
            zanpakuto.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 2);
            zanpakuto.addUnsafeEnchantment(Enchantment.SMITE, 5);
            zanpakuto.addUnsafeEnchantment(Enchantment.UNBREAKING, 4);
            list.add(zanpakuto);

            ItemStack shield = createCustomItem(Material.SHIELD, "&6&lEscudo del Conquistador Imbuido",
                    Arrays.asList(
                            "&8▪ &6Encantamiento: Imbuido en Haki del Conquistador",
                            "&8▪ &7Irrompibilidad IV · Reparación",
                            "&7Repele proyectiles y ataques devastadores en alta mar."
                    ));
            shield.addUnsafeEnchantment(Enchantment.UNBREAKING, 4);
            shield.addUnsafeEnchantment(Enchantment.MENDING, 1);
            list.add(shield);

            list.add(new ItemStack(Material.GOLDEN_CARROT, 32));
            list.add(createPotion(PotionEffectType.WATER_BREATHING, 9600, 0, "&9Néctar de las Profundidades"));

        } else if (division == 4) {
            // División IV: Guerreros Z & Monarca
            // 4 Semillas del Ermitaño (Senzu Beans)
            ItemStack senzu = createCustomItem(Material.SLIME_BALL, "&a&lSemilla del Ermitaño (Senzu Bean)",
                    Arrays.asList(
                            "&8▪ &aEfecto Místico: Curación Divina Inmediata",
                            "&7Clic derecho para consumir:",
                            "&8▪ &fRestaura instantáneamente el 100% de Salud (20 HP)",
                            "&8▪ &fRestaura al 100% el Hambre y Saturación",
                            "&8▪ &fPurifica y elimina todos los efectos negativos",
                            "&eCultivada en lo alto de la Torre de Karin."
                    ));
            senzu.setAmount(4);
            list.add(senzu);

            ItemStack armor = createCustomItem(Material.NETHERITE_CHESTPLATE, "&e&lArmadura de Batalla Saiyajin",
                    Arrays.asList(
                            "&8▪ &eEncantamiento: Resonancia de Ki Saiyajin V",
                            "&8▪ &7Protección V · Espinas III · Irrompibilidad IV",
                            "&7Placas ultra flexibles capaces de soportar explosiones planetarias."
                    ));
            armor.addUnsafeEnchantment(Enchantment.PROTECTION, 5);
            armor.addUnsafeEnchantment(Enchantment.THORNS, 3);
            armor.addUnsafeEnchantment(Enchantment.UNBREAKING, 4);
            list.add(armor);

            ItemStack dagger = createCustomItem(Material.NETHERITE_SWORD, "&8&lDaga del Monarca de las Sombras",
                    Arrays.asList(
                            "&8▪ &8Encantamiento: Sed de Sangre del Monarca",
                            "&8▪ &7Filo VI · Filo Arrasador III · Irrompibilidad IV",
                            "&7Armamento del Monarca que roba vitalidad con cada tajo."
                    ));
            dagger.addUnsafeEnchantment(Enchantment.SHARPNESS, 6);
            dagger.addUnsafeEnchantment(Enchantment.SWEEPING_EDGE, 3);
            dagger.addUnsafeEnchantment(Enchantment.UNBREAKING, 4);
            list.add(dagger);

            list.add(new ItemStack(Material.GOLDEN_APPLE, 8));

        } else {
            // División V: Trascendencia Suprema
            // 8 Semillas del Ermitaño Divinas
            ItemStack senzu = createCustomItem(Material.SLIME_BALL, "&a&lSemilla del Ermitaño (Senzu Bean)",
                    Arrays.asList(
                            "&8▪ &aEfecto Místico: Curación Divina Inmediata",
                            "&7Clic derecho para consumir:",
                            "&8▪ &fRestaura instantáneamente el 100% de Salud (20 HP)",
                            "&8▪ &fRestaura al 100% el Hambre y Saturación",
                            "&8▪ &fPurifica y elimina todos los efectos negativos",
                            "&eCultivada en lo alto de la Torre de Karin."
                    ));
            senzu.setAmount(8);
            list.add(senzu);

            ItemStack godSword = createCustomItem(Material.NETHERITE_SWORD, "&6&lArmamento Celestial de KamiSama",
                    Arrays.asList(
                            "&8▪ &6Encantamiento: Corte Divino de Hakai VI",
                            "&8▪ &7Filo VI · Botín IV · Aspecto Ígneo II · Irrompibilidad V",
                            "&7Forjada en los límites del multiverso por los Ángeles Supremos."
                    ));
            godSword.addUnsafeEnchantment(Enchantment.SHARPNESS, 6);
            godSword.addUnsafeEnchantment(Enchantment.LOOTING, 4);
            godSword.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 2);
            godSword.addUnsafeEnchantment(Enchantment.UNBREAKING, 5);
            list.add(godSword);

            ItemStack totem = createCustomItem(Material.TOTEM_OF_UNDYING, "&e&lTotem Supremo de Zeno-Sama",
                    Arrays.asList(
                            "&8▪ &eProtección Omni-Universal",
                            "&7Otorga salvación absoluta con Regeneración III al activarse."
                    ));
            list.add(totem);

            ItemStack godChest = createCustomItem(Material.NETHERITE_CHESTPLATE, "&b&lToga Angelical de Whis",
                    Arrays.asList(
                            "&8▪ &bEncantamiento: Gracia Inmortal de los Ángeles V",
                            "&8▪ &7Protección V · Irrompibilidad V · Reparación",
                            "&7Tejido sagrado impenetrable que irradia luz celestial."
                    ));
            godChest.addUnsafeEnchantment(Enchantment.PROTECTION, 5);
            godChest.addUnsafeEnchantment(Enchantment.UNBREAKING, 5);
            godChest.addUnsafeEnchantment(Enchantment.MENDING, 1);
            list.add(godChest);

            list.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 2));
        }

        return list;
    }

    private ItemStack createCustomItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> coloredLore = new ArrayList<>();
            for (String l : lore) {
                coloredLore.add(ChatColor.translateAlternateColorCodes('&', l));
            }
            meta.setLore(coloredLore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPotion(PotionEffectType type, int duration, int amplifier, String name) {
        ItemStack pot = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) pot.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            meta.addCustomEffect(new PotionEffect(type, duration, amplifier), true);
            pot.setItemMeta(meta);
        }
        return pot;
    }
}
