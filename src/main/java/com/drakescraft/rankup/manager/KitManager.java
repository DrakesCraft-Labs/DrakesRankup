package com.drakescraft.rankup.manager;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

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

            // Determine division (1 to 10)
            int division = ((tier - 1) / 10) + 1;
            division = Math.max(1, Math.min(10, division));

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

            list.add(new ItemStack(Material.GOLDEN_APPLE, 4));
            list.add(new ItemStack(Material.COOKED_BEEF, 32));

        } else if (division == 2) {
            // División II: Héroes & Cazadores
            ItemStack gauntlet = createCustomItem(Material.BOW, "&6&lArco de Precisión de Hawkeye",
                    Arrays.asList(
                            "&8▪ &6Encantamiento: Puntería Smash IV",
                            "&8▪ &7Poder IV · Infinidad · Irrompibilidad III",
                            "&7Arma de apoyo táctico calibrada por la U.A. High School."
                    ));
            gauntlet.addUnsafeEnchantment(Enchantment.POWER, 4);
            gauntlet.addUnsafeEnchantment(Enchantment.INFINITY, 1);
            gauntlet.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);
            list.add(gauntlet);
            list.add(new ItemStack(Material.ARROW, 1));

            ItemStack chest = createCustomItem(Material.DIAMOND_CHESTPLATE, "&2&lChaleco Táctico ANBU",
                    Arrays.asList(
                            "&8▪ &2Encantamiento: Blindaje de Chakra IV",
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

        } else if (division == 5) {
            // División V: Trascendencia Suprema
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

        } else if (division == 6) {
            // División VI: Hechicería & Maldición (Jujutsu Kaisen)
            ItemStack tojiBlade = createCustomItem(Material.NETHERITE_SWORD, "&d&lCuchilla Invertida del Cielo (Amahoko)",
                    Arrays.asList(
                            "&8▪ &dHerramienta Maldita de Grado Especial",
                            "&8▪ &7Filo VI · Filo Arrasador III · Irrompibilidad V",
                            "&7Anula y corta cualquier técnica ritual o barrera defensiva."
                    ));
            tojiBlade.addUnsafeEnchantment(Enchantment.SHARPNESS, 6);
            tojiBlade.addUnsafeEnchantment(Enchantment.SWEEPING_EDGE, 3);
            tojiBlade.addUnsafeEnchantment(Enchantment.UNBREAKING, 5);
            list.add(tojiBlade);

            ItemStack sukunaFinger = createCustomItem(Material.NETHER_WART, "&4&lDedo Sagrado de Sukuna",
                    Arrays.asList(
                            "&8▪ &4Reliquia Indestructible del Rey de las Maldiciones",
                            "&7Concentra 1/20 del poder del Heian original."
                    ));
            sukunaFinger.setAmount(2);
            list.add(sukunaFinger);

            ItemStack senzu = createSenzuBeans(8);
            list.add(senzu);
            list.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 2));

        } else if (division == 7) {
            // División VII: Sociedad de Almas & Arrancar (Bleach)
            ItemStack bankaiSword = createCustomItem(Material.NETHERITE_SWORD, "&8&lTensa Zangetsu: Corte Trascendental",
                    Arrays.asList(
                            "&8▪ &8Espada Bankai de Reiatsu Condensado",
                            "&8▪ &7Filo VII · Aspecto Ígneo II · Irrompibilidad V · Reparación",
                            "&7Libera una ráfaga devastadora de energía espiritual pura."
                    ));
            bankaiSword.addUnsafeEnchantment(Enchantment.SHARPNESS, 7);
            bankaiSword.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 2);
            bankaiSword.addUnsafeEnchantment(Enchantment.UNBREAKING, 5);
            bankaiSword.addUnsafeEnchantment(Enchantment.MENDING, 1);
            list.add(bankaiSword);

            ItemStack hogyoku = createCustomItem(Material.CONDUIT, "&b&lFragmento del Hōgyoku de Aizen",
                    Arrays.asList(
                            "&8▪ &bEsfera de los Deseos Espirituales",
                            "&7Materializa los límites evolutivos del alma inmortal."
                    ));
            list.add(hogyoku);

            ItemStack senzu = createSenzuBeans(10);
            list.add(senzu);
            list.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 3));

        } else if (division == 8) {
            // División VIII: Demonios & Cazadores de Sombras (CSM & Solo Leveling)
            ItemStack monarchDagger = createCustomItem(Material.NETHERITE_SWORD, "&5&lColmillo del Monarca de las Sombras (Kamish)",
                    Arrays.asList(
                            "&8▪ &5Forjada con el colmillo del Dragón de la Calamidad",
                            "&8▪ &7Filo VII · Saqueo IV · Irrompibilidad VI",
                            "&7Corta la mismísima sombra del enemigo drenando vitalidad."
                    ));
            monarchDagger.addUnsafeEnchantment(Enchantment.SHARPNESS, 7);
            monarchDagger.addUnsafeEnchantment(Enchantment.LOOTING, 4);
            monarchDagger.addUnsafeEnchantment(Enchantment.UNBREAKING, 6);
            list.add(monarchDagger);

            ItemStack chainsawHeart = createCustomItem(Material.REDSTONE_BLOCK, "&c&lMotor del Demonio Motosierra (Pochita)",
                    Arrays.asList(
                            "&8▪ &cCorazón del Héroe del Infierno",
                            "&7Late con sed de sangre y aniquilación de pesadillas."
                    ));
            list.add(chainsawHeart);

            ItemStack senzu = createSenzuBeans(12);
            list.add(senzu);
            list.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 4));

        } else if (division == 9) {
            // División IX: Leyendas Cósmicas & Seis Caminos (Naruto / OP / HxH)
            ItemStack gudodama = createCustomItem(Material.NETHERITE_SWORD, "&8&lBáculo de la Verdad de los Seis Caminos (Gudōdama)",
                    Arrays.asList(
                            "&8▪ &8Esferas Negras de Yin-Yang Trascendental",
                            "&8▪ &7Filo VII · Irrompibilidad VI · Reparación",
                            "&7Desintegra el ninjutsu y la materia elemental ordinaria."
                    ));
            gudodama.addUnsafeEnchantment(Enchantment.SHARPNESS, 7);
            gudodama.addUnsafeEnchantment(Enchantment.UNBREAKING, 6);
            gudodama.addUnsafeEnchantment(Enchantment.MENDING, 1);
            list.add(gudodama);

            ItemStack nikaFruit = createCustomItem(Material.GOLDEN_APPLE, "&e&lFruta Hito Hito no Mi: Modelo Nika",
                    Arrays.asList(
                            "&8▪ &eEl Dios del Sol y los Tambores de la Liberación",
                            "&7La fruta más ridícula del mundo que concede libertad absoluta."
                    ));
            list.add(nikaFruit);

            ItemStack senzu = createSenzuBeans(14);
            list.add(senzu);
            list.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 4));

        } else {
            // División X: Dioses Multiversales & Singularidad Endgame (Tier 91 - 100)
            // =========================================================================
            // ULTRA ARMADURA OMNI-SUPREMA (4 PIEZAS CUSTOM PAPER 1.21 TRIM + ATRIBUTOS)
            // =========================================================================
            list.add(buildOmniHelmet());
            list.add(buildOmniChestplate());
            list.add(buildOmniLeggings());
            list.add(buildOmniBoots());

            // =========================================================================
            // ARTEFACTOS ENDGAME DE SLIMEFUN (INFINITY, SUPREME, SLIMETINKER, DAXIS)
            // =========================================================================
            // 1. Infinity Expansion: Lingote del Infinito
            ItemStack infinityItem = getSlimefunOrFallback("INFINITE_INGOT",
                    createCustomItem(Material.NETHERITE_INGOT, "&b&lLingote del Infinito &7(Infinity Expansion)",
                            Arrays.asList(
                                    "&8▪ &bSingularidad: &fCondensación de 100,000 átomos multiversales",
                                    "&8▪ &7Material insigne del endgame de Slimefun",
                                    "&eCapaz de alimentar reactores cósmicos y forjar armaduras infinitas."
                            )));
            infinityItem.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
            list.add(infinityItem);

            // 2. Supreme Addon: Singularidad Suprema
            ItemStack supremeItem = getSlimefunOrFallback("SUPREME_SINGULARITY",
                    createCustomItem(Material.NETHER_STAR, "&d&lSingularidad Suprema &7(Supreme Addon)",
                            Arrays.asList(
                                    "&8▪ &dSingularidad de Materia Máxima",
                                    "&8▪ &7Forjada en el Generador de Singularidades de Supreme",
                                    "&6La cúspide tecnológica de Slimefun en DrakesCraft."
                            )));
            supremeItem.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
            list.add(supremeItem);

            // 3. SlimeTinker: Núcleo Modular Divino
            ItemStack tinkerItem = getSlimefunOrFallback("TINKER_CORE",
                    createCustomItem(Material.HEART_OF_THE_SEA, "&e&lNúcleo Modular Divino &7(SlimeTinker)",
                            Arrays.asList(
                                    "&8▪ &eModificador Divino de Forja Modular",
                                    "&8▪ &7Permite encajar gemas y atributos legendarios en herramientas Tinker",
                                    "&aCompatibilidad total con la mesa de ensamblaje SlimeTinker."
                            )));
            tinkerItem.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
            list.add(tinkerItem);

            // 4. Daxten's Additions (Daxis): Reliquia Ancestral de Daxis
            ItemStack daxisItem = getSlimefunOrFallback("DAXIS_RELIC",
                    createCustomItem(Material.AMETHYST_SHARD, "&5&lReliquia Ancestral de Daxis &7(Daxten's Additions)",
                            Arrays.asList(
                                    "&8▪ &5Matriz Resonante de Daxis",
                                    "&8▪ &7Artefacto milenario recuperado de los archivos de Daxten",
                                    "&dDesbloquea el potencial oculto de las máquinas del vacío."
                            )));
            daxisItem.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
            list.add(daxisItem);

            // Suministros celestiales supremos
            ItemStack senzu = createSenzuBeans(16);
            list.add(senzu);

            ItemStack totem = createCustomItem(Material.TOTEM_OF_UNDYING, "&e&lTotem Supremo del Omni-Rey Zeno",
                    Arrays.asList(
                            "&8▪ &eSalvación Inmortal Multiversal",
                            "&7Protege de la muerte con Regeneración IV y Absorción Divina."
                    ));
            list.add(totem);

            list.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 4));
        }

        return list;
    }

    private ItemStack buildOmniHelmet() {
        ItemStack helmet = new ItemStack(Material.NETHERITE_HELMET);
        ItemMeta rawMeta = helmet.getItemMeta();
        if (rawMeta instanceof ArmorMeta meta) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6&lCorona Omni-Suprema del Infinito"));
            List<String> lore = Arrays.asList(
                    ChatColor.translateAlternateColorCodes('&', "&8▪ &6Jerarquía: &eTier 100 Endgame Multiversal"),
                    ChatColor.translateAlternateColorCodes('&', "&8▪ &eProtección VII · Irrompibilidad X · Reparación"),
                    ChatColor.translateAlternateColorCodes('&', "&8▪ &eRespiración V · Afinidad Acuática"),
                    ChatColor.translateAlternateColorCodes('&', "&8▪ &aAtributo: +4 Corazones de Salud Divina (+8 HP)"),
                    ChatColor.translateAlternateColorCodes('&', "&7Corona celestial que refracta la luz cósmica.")
            );
            meta.setLore(lore);
            meta.setTrim(new ArmorTrim(TrimMaterial.GOLD, TrimPattern.SPIRE));

            AttributeModifier hpModifier = new AttributeModifier(
                    new NamespacedKey(plugin, "omni_helmet_hp"),
                    8.0,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.HEAD
            );
            meta.addAttributeModifier(Attribute.MAX_HEALTH, hpModifier);
            helmet.setItemMeta(meta);
        }
        helmet.addUnsafeEnchantment(Enchantment.PROTECTION, 7);
        helmet.addUnsafeEnchantment(Enchantment.UNBREAKING, 10);
        helmet.addUnsafeEnchantment(Enchantment.MENDING, 1);
        helmet.addUnsafeEnchantment(Enchantment.RESPIRATION, 5);
        helmet.addUnsafeEnchantment(Enchantment.AQUA_AFFINITY, 1);
        return helmet;
    }

    private ItemStack buildOmniChestplate() {
        ItemStack chest = new ItemStack(Material.NETHERITE_CHESTPLATE);
        ItemMeta rawMeta = chest.getItemMeta();
        if (rawMeta instanceof ArmorMeta meta) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6&lPechera Omni-Suprema del Infinito"));
            List<String> lore = Arrays.asList(
                    ChatColor.translateAlternateColorCodes('&', "&8▪ &6Jerarquía: &eTier 100 Endgame Multiversal"),
                    ChatColor.translateAlternateColorCodes('&', "&8▪ &eProtección VII · Irrompibilidad X · Reparación · Espinas IV"),
                    ChatColor.translateAlternateColorCodes('&', "&8▪ &aAtributo: +8 Corazones de Salud Divina (+16 HP)"),
                    ChatColor.translateAlternateColorCodes('&', "&8▪ &aAtributo: +20% Resistencia al Empuje Cósmico"),
                    ChatColor.translateAlternateColorCodes('&', "&7Placas forjadas en la Singularidad Suprema del Vacío.")
            );
            meta.setLore(lore);
            meta.setTrim(new ArmorTrim(TrimMaterial.GOLD, TrimPattern.SILENCE));

            AttributeModifier hpModifier = new AttributeModifier(
                    new NamespacedKey(plugin, "omni_chest_hp"),
                    16.0,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.CHEST
            );
            AttributeModifier kbModifier = new AttributeModifier(
                    new NamespacedKey(plugin, "omni_chest_kb"),
                    0.20,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.CHEST
            );
            meta.addAttributeModifier(Attribute.MAX_HEALTH, hpModifier);
            meta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, kbModifier);
            chest.setItemMeta(meta);
        }
        chest.addUnsafeEnchantment(Enchantment.PROTECTION, 7);
        chest.addUnsafeEnchantment(Enchantment.UNBREAKING, 10);
        chest.addUnsafeEnchantment(Enchantment.MENDING, 1);
        chest.addUnsafeEnchantment(Enchantment.THORNS, 4);
        return chest;
    }

    private ItemStack buildOmniLeggings() {
        ItemStack legs = new ItemStack(Material.NETHERITE_LEGGINGS);
        ItemMeta rawMeta = legs.getItemMeta();
        if (rawMeta instanceof ArmorMeta meta) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6&lGrebas Omni-Supremas del Infinito"));
            List<String> lore = Arrays.asList(
                    ChatColor.translateAlternateColorCodes('&', "&8▪ &6Jerarquía: &eTier 100 Endgame Multiversal"),
                    ChatColor.translateAlternateColorCodes('&', "&8▪ &eProtección VII · Irrompibilidad X · Reparación · Sigilo Veloz III"),
                    ChatColor.translateAlternateColorCodes('&', "&8▪ &aAtributo: +4 Armadura Base · +3 Dureza Celestial"),
                    ChatColor.translateAlternateColorCodes('&', "&7Grebas imbuidas con la gravedad del Big Crunch.")
            );
            meta.setLore(lore);
            meta.setTrim(new ArmorTrim(TrimMaterial.AMETHYST, TrimPattern.SILENCE));

            AttributeModifier armorMod = new AttributeModifier(
                    new NamespacedKey(plugin, "omni_legs_armor"),
                    4.0,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.LEGS
            );
            AttributeModifier toughMod = new AttributeModifier(
                    new NamespacedKey(plugin, "omni_legs_tough"),
                    3.0,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.LEGS
            );
            meta.addAttributeModifier(Attribute.ARMOR, armorMod);
            meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, toughMod);
            legs.setItemMeta(meta);
        }
        legs.addUnsafeEnchantment(Enchantment.PROTECTION, 7);
        legs.addUnsafeEnchantment(Enchantment.UNBREAKING, 10);
        legs.addUnsafeEnchantment(Enchantment.MENDING, 1);
        legs.addUnsafeEnchantment(Enchantment.SWIFT_SNEAK, 3);
        return legs;
    }

    private ItemStack buildOmniBoots() {
        ItemStack boots = new ItemStack(Material.NETHERITE_BOOTS);
        ItemMeta rawMeta = boots.getItemMeta();
        if (rawMeta instanceof ArmorMeta meta) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6&lBotas Omni-Supremas del Infinito"));
            List<String> lore = Arrays.asList(
                    ChatColor.translateAlternateColorCodes('&', "&8▪ &6Jerarquía: &eTier 100 Endgame Multiversal"),
                    ChatColor.translateAlternateColorCodes('&', "&8▪ &eProtección VII · Caída de Pluma V · Paso Ágil III · Reparación"),
                    ChatColor.translateAlternateColorCodes('&', "&8▪ &aAtributo: +20% Velocidad Cósmica Permanente"),
                    ChatColor.translateAlternateColorCodes('&', "&7Calzado divino capaz de caminar sobre líneas temporales.")
            );
            meta.setLore(lore);
            meta.setTrim(new ArmorTrim(TrimMaterial.GOLD, TrimPattern.SPIRE));

            AttributeModifier speedMod = new AttributeModifier(
                    new NamespacedKey(plugin, "omni_boots_spd"),
                    0.02,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.FEET
            );
            meta.addAttributeModifier(Attribute.MOVEMENT_SPEED, speedMod);
            boots.setItemMeta(meta);
        }
        boots.addUnsafeEnchantment(Enchantment.PROTECTION, 7);
        boots.addUnsafeEnchantment(Enchantment.UNBREAKING, 10);
        boots.addUnsafeEnchantment(Enchantment.MENDING, 1);
        boots.addUnsafeEnchantment(Enchantment.FEATHER_FALLING, 5);
        boots.addUnsafeEnchantment(Enchantment.DEPTH_STRIDER, 3);
        boots.addUnsafeEnchantment(Enchantment.SOUL_SPEED, 3);
        return boots;
    }

    private ItemStack createSenzuBeans(int amount) {
        ItemStack senzu = createCustomItem(Material.SLIME_BALL, "&a&lSemilla del Ermitaño (Senzu Bean)",
                Arrays.asList(
                        "&8▪ &aEfecto Místico: Curación Divina Inmediata",
                        "&7Clic derecho para consumir:",
                        "&8▪ &fRestaura instantáneamente el 100% de Salud (20 HP)",
                        "&8▪ &fRestaura al 100% el Hambre y Saturación",
                        "&8▪ &fPurifica y elimina todos los efectos negativos",
                        "&eCultivada en lo alto de la Torre de Karin."
                ));
        senzu.setAmount(amount);
        return senzu;
    }

    private ItemStack getSlimefunOrFallback(String sfId, ItemStack fallback) {
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("Slimefun")) {
                Class<?> sfClass = Class.forName("io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem");
                Method getById = sfClass.getMethod("getById", String.class);
                Object sfItem = getById.invoke(null, sfId);
                if (sfItem != null) {
                    Method getItem = sfClass.getMethod("getItem");
                    ItemStack is = (ItemStack) getItem.invoke(sfItem);
                    if (is != null) {
                        return is.clone();
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return fallback;
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
