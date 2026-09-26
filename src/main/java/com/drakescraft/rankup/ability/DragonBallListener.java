package com.drakescraft.rankup.ability;

import org.bukkit.World;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.AbilityType;
import com.drakescraft.rankup.model.PlayerSettings;
import com.drakescraft.rankup.model.Rank;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Color;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class DragonBallListener implements Listener {

    private final DrakesRankupPlugin plugin;

    // Active transformations: UUID -> expiration timestamp in millis
    private final Map<UUID, Long> activeMUI = new HashMap<>();
    private final Map<UUID, Long> activeUltraEgo = new HashMap<>();
    private final Map<UUID, Long> activeSSJGod = new HashMap<>();
    private final Map<UUID, Long> activeSSJBlue = new HashMap<>();
    private final Map<UUID, Long> activeGohanBeast = new HashMap<>();
    private final Map<UUID, Long> activeBroly = new HashMap<>();
    private final Map<UUID, Long> activeKaioken = new HashMap<>();
    private final Map<UUID, Long> activeSSJ2 = new HashMap<>();

    // Cooldowns: UUID -> available timestamp in millis
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Long> spiritSwordCooldown = new HashMap<>();
    private final Map<UUID, Long> kiFlightCooldown = new HashMap<>();
    private final Map<UUID, Long> kiFlightImmunity = new HashMap<>();
    private final Map<UUID, Long> hakaiCooldown = new HashMap<>();
    private final Map<UUID, Long> zenoEraseCooldown = new HashMap<>();

    // Overuse tracking: UUID -> list of activation timestamps in last 120 seconds
    private final Map<UUID, List<Long>> muiUsageHistory = new HashMap<>();

    // Charging tasks: UUID -> task
    private final Map<UUID, BukkitTask> chargingTasks = new HashMap<>();

    public DragonBallListener(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isMuiActive(UUID uuid) {
        Long exp = activeMUI.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    public boolean isUltraEgoActive(UUID uuid) {
        Long exp = activeUltraEgo.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    public boolean isSSJGodActive(UUID uuid) {
        Long exp = activeSSJGod.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    public boolean isSSJBlueActive(UUID uuid) {
        Long exp = activeSSJBlue.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    public boolean isGohanBeastActive(UUID uuid) {
        Long exp = activeGohanBeast.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    public boolean isKaiokenActive(UUID uuid) {
        Long exp = activeKaioken.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    public boolean isBrolyActive(UUID uuid) {
        Long exp = activeBroly.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    public boolean isSSJ2Active(UUID uuid) {
        Long exp = activeSSJ2.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    public void revertTransformations(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        activeMUI.remove(uuid);
        activeUltraEgo.remove(uuid);
        activeSSJGod.remove(uuid);
        activeSSJBlue.remove(uuid);
        activeGohanBeast.remove(uuid);
        activeBroly.remove(uuid);
        activeKaioken.remove(uuid);
        activeSSJ2.remove(uuid);
        BukkitTask task = chargingTasks.remove(uuid);
        if (task != null) task.cancel();

        player.setFlySpeed(0.10f);
        if (!plugin.hasExternalFlight(player) && player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isWorldAllowed(player.getWorld())) return;
        UUID uuid = player.getUniqueId();

        // If stopped sneaking, cancel any charging
        if (!event.isSneaking()) {
            BukkitTask task = chargingTasks.remove(uuid);
            if (task != null) {
                task.cancel();
                player.sendActionBar(Component.text("§7Concentración de Ki interrumpida."));
            }
            return;
        }

        if (!player.isOnGround()) return;

        Rank rank = plugin.getRankManager().getPlayerRank(uuid);
        if (rank == null) return;
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(uuid);
        if (!settings.isAbilitiesEnabled()) return;

        int tier = rank.getTier();
        AbilityType ability = rank.getAbilityType();
        String equipped = settings.getActiveTransformation();

        // Check if player has access to any transformation
        boolean hasGod = (tier >= 35 || ability == AbilityType.SSJ_GOD);
        boolean hasBlue = (tier >= 36 || ability == AbilityType.SSJ_BLUE);
        boolean hasMUI = (tier >= 46 || ability == AbilityType.MASTERED_ULTRA_INSTINCT || ability == AbilityType.ULTRA_INSTINCT);
        boolean hasEgo = (tier >= 47 || ability == AbilityType.ULTRA_EGO || ability == AbilityType.HAKAI_AURA);
        boolean hasGohan = (tier >= 45 || ability == AbilityType.GOHAN_BEAST);
        boolean hasBroly = (tier >= 34 || ability == AbilityType.BROLY_LSSJ);
        boolean isKami = (tier >= 50);
        boolean hasKaioken = (tier >= 92 || ability == AbilityType.KAIOKEN);

        if (!hasGod && !hasBlue && !hasMUI && !hasEgo && !hasGohan && !hasBroly && !isKami && !hasKaioken && (equipped == null || equipped.isEmpty())) {
            return;
        }

        // Determine transformation type based on equipped setting or highest unlocked
        String transName;
        long cdSeconds;
        Particle kiParticle;

        if (equipped != null && !equipped.equalsIgnoreCase("NINGUNA")) {
            transName = equipped.toUpperCase();
            if (transName.equals("GOHAN_BEAST")) {
                cdSeconds = 40;
                kiParticle = Particle.CRIMSON_SPORE;
            } else if (transName.equals("BROLY_LSSJ")) {
                cdSeconds = 45;
                kiParticle = Particle.HAPPY_VILLAGER;
            } else if (transName.equals("MUI")) {
                cdSeconds = 40;
                kiParticle = Particle.END_ROD;
            } else if (transName.equals("ULTRA_EGO")) {
                cdSeconds = 50;
                kiParticle = Particle.WITCH;
            } else if (transName.equals("SSJ_BLUE")) {
                cdSeconds = 45;
                kiParticle = Particle.SOUL_FIRE_FLAME;
            } else if (transName.equals("KAIOKEN")) {
                cdSeconds = 35;
                kiParticle = Particle.DUST;
            } else if (transName.equals("SSJ_GOD")) {
                cdSeconds = 35;
                kiParticle = Particle.FLAME;
            } else if (transName.equals("SSJ_2")) {
                cdSeconds = 35;
                kiParticle = Particle.ELECTRIC_SPARK;
            } else {
                return; // Handled by other listeners (e.g. OnePiece)
            }
        } else {
            // Default based on rank
            if (hasMUI) {
                transName = "MUI";
                cdSeconds = 40;
                kiParticle = Particle.END_ROD;
            } else if (hasEgo) {
                transName = "ULTRA_EGO";
                cdSeconds = 50;
                kiParticle = Particle.WITCH;
            } else if (hasGohan) {
                transName = "GOHAN_BEAST";
                cdSeconds = 40;
                kiParticle = Particle.CRIMSON_SPORE;
            } else if (hasBlue) {
                transName = "SSJ_BLUE";
                cdSeconds = 45;
                kiParticle = Particle.SOUL_FIRE_FLAME;
            } else if (hasGod) {
                transName = "SSJ_GOD";
                cdSeconds = 35;
                kiParticle = Particle.FLAME;
            } else if (hasKaioken) {
                transName = "KAIOKEN";
                cdSeconds = 35;
                kiParticle = Particle.DUST;
            } else if (tier == 33 || (rank != null && rank.getAbilityType() == AbilityType.SSJ_2)) {
                transName = "SSJ_2";
                cdSeconds = 35;
                kiParticle = Particle.ELECTRIC_SPARK;
            } else if (hasBroly) {
                transName = "BROLY_LSSJ";
                cdSeconds = 45;
                kiParticle = Particle.HAPPY_VILLAGER;
            } else {
                return;
            }
        }

        long now = System.currentTimeMillis();
        long nextReady = cooldowns.getOrDefault(uuid, 0L);
        if (now < nextReady) {
            long remaining = Math.max(1, (nextReady - now) / 1000L);
            player.sendActionBar(Component.text("§c⏳ Ki en reposo: §e" + remaining + "s"));
            return;
        }

        Location initialGroundLoc = player.getLocation().clone();

        // Start Ki charging runnable con levitación de 1 bloque y vórtice de doble hélice
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !player.isSneaking()) {
                    chargingTasks.remove(uuid);
                    cancel();
                    // Restaurar suavemente al suelo si se interrumpe
                    if (player.isOnline()) {
                        player.teleport(initialGroundLoc.clone().setDirection(player.getLocation().getDirection()));
                    }
                    return;
                }

                ticks += 4;
                float progress = Math.min(1.0f, ticks / 36.0f);
                int bars = (int) (progress * 10);
                StringBuilder barStr = new StringBuilder("§e⚡ Ki: §6[");
                for (int b = 0; b < 10; b++) {
                    if (b < bars) barStr.append("▮");
                    else barStr.append("§7▯");
                }
                barStr.append("§6]");
                player.sendActionBar(Component.text(barStr.toString()));

                // Flotar exactamente 1 bloque encima del suelo con suave oscilación
                double hover = 1.0 + Math.sin(ticks * 0.45) * 0.08;
                Location floatLoc = initialGroundLoc.clone().add(0, hover, 0);
                floatLoc.setDirection(player.getLocation().getDirection());
                player.teleport(floatLoc);

                var w = player.getWorld();
                try {
                    // Vórtice ascendente de doble hélice alrededor del cuerpo
                    for (int s = 0; s < 2; s++) {
                        double ang = (ticks * 0.42) + (s * Math.PI);
                        double rad = 1.3 - (progress * 0.35);
                        double px = Math.cos(ang) * rad;
                        double pz = Math.sin(ang) * rad;
                        double py = ((ticks % 20) / 20.0) * 2.2;
                        if (kiParticle == Particle.DUST) {
                            w.spawnParticle(kiParticle, floatLoc.clone().add(px, py, pz), 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(255, 20, 30), 1.4f));
                        } else {
                            w.spawnParticle(kiParticle, floatLoc.clone().add(px, py, pz), 1, 0, 0, 0, 0);
                        }
                    }
                    // Arcos de rayos y chispas de energía concentrada
                    w.spawnParticle(Particle.ELECTRIC_SPARK, floatLoc.clone().add(0, 0.5, 0), 4, 0.35, 0.4, 0.35, 0.05);
                    player.playSound(floatLoc, Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, 0.8f + (progress * 1.2f));
                } catch (Exception ignored) {}

                if (ticks >= 36) {
                    chargingTasks.remove(uuid);
                    cancel();
                    try {
                        w.spawnParticle(Particle.FLASH, floatLoc.clone().add(0, 0.5, 0), 2);
                        w.spawnParticle(Particle.SONIC_BOOM, floatLoc.clone().add(0, 0.5, 0), 1);
                        w.playSound(floatLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.8f);
                    } catch (Exception ignored) {}
                    detonateTransformation(player, transName, cdSeconds);
                }
            }
        }.runTaskTimer(plugin, 0L, 4L);

        chargingTasks.put(uuid, task);
    }

    private void detonateTransformation(Player player, String type, long cdSeconds) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        int rebirths = plugin.getRankManager().getRebirthCount(uuid);
        double cdr = 1.0 - Math.min(0.5, rebirths * 0.01);
        cooldowns.put(uuid, now + (long)((cdSeconds * 1000L) * cdr));

        Location loc = player.getLocation();

        try {
            player.getWorld().spawnParticle(Particle.EXPLOSION, loc.clone().add(0, 1.0, 0), 2);
            player.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.4f);
        } catch (Exception ignored) {}

        if (type.equals("SSJ_2")) {
            activeSSJ2.put(uuid, now + 300000L); // 5 minutes
            playAwakeningBurst(player, Color.fromRGB(255, 230, 40), Color.fromRGB(255, 255, 100), Particle.ELECTRIC_SPARK, Sound.ENTITY_LIGHTNING_BOLT_THUNDER);
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 6000, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 6000, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 6000, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 6000, 0));
            player.sendTitle("§e§lSUPER SAIYAJIN 2", "§6¡Arcos de bio-electricidad y fuerza colosal!", 5, 50, 10);
            player.sendActionBar(Component.text("§e⚡ ¡SSJ2 ACTIVO! Arcos de bio-electricidad, Fuerza II & Velocidad II por 5 minutos"));

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                activeSSJ2.remove(uuid);
                if (player.isOnline()) {
                    player.sendMessage("§e[Rankup] El estado de Super Saiyajin 2 ha concluido.");
                }
            }, 6000L);

        } else if (type.equals("MUI")) {
            activeMUI.put(uuid, now + 60000L); // 60 seconds
            playMuiActivationFx(player);
            player.sendTitle("§f§lDOCTRINA EGOÍSTA", "§bUltra Instinto Dominado activado", 5, 40, 5);
            player.sendActionBar(Component.text("§f⚡ ¡EVASIÓN TOTAL ACTIVADA POR 60 SEGUNDOS!"));

            List<Long> history = muiUsageHistory.computeIfAbsent(uuid, k -> new ArrayList<>());
            history.removeIf(t -> now - t > 120_000L);
            history.add(now);

            final boolean willFatigue = history.size() >= 2;

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                activeMUI.remove(uuid);
                if (player.isOnline()) {
                    if (willFatigue) {
                        applyMuiFatigue(player);
                    } else {
                        player.sendMessage("§7[Rankup] El estado de Ultra Instinto ha concluido.");
                    }
                }
            }, 1200L);

        } else if (type.equals("ULTRA_EGO")) {
            activeUltraEgo.put(uuid, now + 300000L); // 5 minutes
            playAwakeningBurst(player, Color.fromRGB(160,60,220), Color.fromRGB(90,20,140), Particle.WITCH, Sound.ENTITY_ELDER_GUARDIAN_CURSE);
            player.sendTitle("§5§lMEGA INSTINTO (ULTRA EGO)", "§dEl daño recibido aumenta tu poder de destrucción", 5, 40, 5);
            player.sendActionBar(Component.text("§5⚡ ¡AURA HAKAI ACTIVA POR 5 MINUTOS!"));

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                activeUltraEgo.remove(uuid);
                if (player.isOnline()) {
                    player.sendMessage("§5[Rankup] El Mega Instinto se ha disipado.");
                }
            }, 6000L);

        } else if (type.equals("GOHAN_BEAST")) {
            activeGohanBeast.put(uuid, now + 300000L); // 5 minutes
            playAwakeningBurst(player, Color.fromRGB(245,245,255), Color.fromRGB(230,70,160), Particle.ELECTRIC_SPARK, Sound.ENTITY_RAVAGER_ROAR);
            player.sendTitle("§d§lGOHAN BESTIA (BEAST)", "§f¡Furia desatada! Explosión crítica al máximo", 5, 40, 5);
            player.sendActionBar(Component.text("§d⚡ ¡EXPLOSIÓN CRÍTICA +75% ACTIVADA POR 5 MINUTOS!"));

            // Negative side effect: severe metabolic hunger burn
            try {
                player.setFoodLevel(Math.max(2, player.getFoodLevel() - 8));
                player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 200, 1));
            } catch (Exception ignored) {}
            player.sendMessage("§c[Gohan Beast] §7El esfuerzo bestial ha consumido tu energía vital (Hambre aumentada).");

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                activeGohanBeast.remove(uuid);
                if (player.isOnline()) {
                    player.sendMessage("§d[Rankup] La forma Bestia de Gohan ha concluido.");
                }
            }, 400L);

        } else if (type.equals("BROLY_LSSJ")) {
            activeBroly.put(uuid, now + 300000L); // 5 minutes
            playAwakeningBurst(player, Color.fromRGB(120,230,80), Color.fromRGB(60,150,40), Particle.HAPPY_VILLAGER, Sound.ENTITY_WARDEN_ROAR);
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 6000, 1)); // Fuerza II (berserker equilibrado)
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 6000, 1)); // Resistance II
            player.sendTitle("§a§lBROLY BERSERKER (LSSJ)", "§2Furia destructiva incontrolable", 5, 40, 5);
            player.sendActionBar(Component.text("§a⚡ ¡FUERZA III & RESISTENCIA II ACTIVAS!"));

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                activeBroly.remove(uuid);
        activeKaioken.remove(uuid);
        activeSSJ2.remove(uuid);
                if (player.isOnline()) {
                    // Negative side effect: Slowness post-rage
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 0));
                    player.sendMessage("§c[Broly LSSJ] §7La cólera berserker se disipa: sufres agotamiento muscular (Lentitud).");
                }
            }, 400L);

        } else if (type.equals("SSJ_BLUE")) {
            activeSSJBlue.put(uuid, now + 300000L);
            playAwakeningBurst(player, Color.fromRGB(70,150,255), Color.fromRGB(150,230,255), Particle.SOUL_FIRE_FLAME, Sound.ENTITY_ENDER_DRAGON_GROWL);
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 6000, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 6000, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 6000, 0)); // Resistencia I: temple divino
            player.sendTitle("§9§lSUPER SAIYAJIN BLUE", "§bFuerza, velocidad y temple de los dioses", 5, 40, 5);
            player.sendActionBar(Component.text("§9⚡ ¡Fuerza I · Velocidad II · Resistencia I por 20s!"));

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                activeSSJBlue.remove(uuid);
                if (player.isOnline()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 0)); // coste: ki agotado
                    player.sendMessage("§9[Rankup] El Super Saiyajin Blue se disipa: ki agotado (Lentitud).");
                }
            }, 400L);

        } else if (type.equals("SSJ_GOD")) {
            activeSSJGod.put(uuid, now + 300000L);
            playAwakeningBurst(player, Color.fromRGB(255,80,80), Color.fromRGB(255,200,90), Particle.FLAME, Sound.ENTITY_LIGHTNING_BOLT_THUNDER);
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 6000, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 6000, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 6000, 1));
            player.sendTitle("§c§lSUPER SAIYAJIN GOD", "§eKi divino de sanación y gracia", 5, 40, 5);
            player.sendActionBar(Component.text("§c⚡ ¡Regeneración II · Absorción · Velocidad por 35s!"));

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                activeSSJGod.remove(uuid);
                if (player.isOnline()) {
                    player.sendMessage("§c[Rankup] El ki divino del Super Saiyajin God se ha disipado.");
                }
            }, 700L);
        } else if (type.equals("KAIOKEN")) {
            activeKaioken.put(uuid, now + 300000L); // 5 minutes
            playAwakeningBurst(player, Color.fromRGB(255,20,30), Color.fromRGB(180,10,10), Particle.FLAME, Sound.ENTITY_WARDEN_HEARTBEAT);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 6000, 2));
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 6000, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 6000, 1));
            player.sendTitle("§c§l¡KAIO-KEN AUMENTADO!", "§4Multiplicador de poder divino x20", 5, 45, 10);
            player.sendActionBar(Component.text("§c⚡ ¡KAIO-KEN ACTIVO! Velocidad III · Fuerza II · Vuelo libre (35s)"));

            // Drenaje gradual de vitalidad por sobreesfuerzo cardiaco
            new BukkitRunnable() {
                int count = 0;
                @Override
                public void run() {
                    if (!player.isOnline() || !isKaiokenActive(uuid)) {
                        cancel();
                        return;
                    }
                    count++;
                    if (count % 2 == 0) {
                        double maxDrain = Math.min(2.0, Math.max(0.0, player.getHealth() - 2.0));
                        if (maxDrain > 0) {
                            player.damage(maxDrain);
                            try {
                                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.8f, 0.5f);
                                player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, player.getLocation().add(0, 1.0, 0), 2);
                            } catch (Exception ignored) {}
                        }
                    }
                    if (count >= 17) cancel();
                }
            }.runTaskTimer(plugin, 40L, 40L);

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                activeKaioken.remove(uuid);
        activeSSJ2.remove(uuid);
                if (player.isOnline()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 0));
                    player.sendMessage("§c[Rankup] El Kaio-ken concluye: tus fibras musculares descansan.");
                }
            }, 700L);
        }

        // Conceder vuelo libre de 15 segundos a toda transformación
        grantTransformationFlight(player, 15);
    }

    private void applyMuiFatigue(Player player) {
        Location loc = player.getLocation();
        try {
            player.getWorld().spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 1.0, 0), 30, 0.4, 0.5, 0.4, 0.08);
            player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, loc.clone().add(0, 1.0, 0), 15, 0.3, 0.4, 0.3, 0.05);
            player.getWorld().playSound(loc, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 0.6f);
            player.getWorld().playSound(loc, Sound.BLOCK_ANVIL_LAND, 0.8f, 0.5f);
        } catch (Exception ignored) {}

        // Desgaste de hasta 4 corazones, pero NUNCA letal (deja minimo medio corazon).
        double desgaste = Math.min(8.0, Math.max(0.0, player.getHealth() - 1.0));
        if (desgaste > 0) player.damage(desgaste);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2)); // Level III for 5s
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 50, 0)); // 2.5s

        player.sendTitle("§c§l¡EL CUERPO PASÓ FACTURA!", "§eEl límite divino desgarró tus músculos", 10, 60, 15);
        player.sendMessage("§c§l¡EL CUERPO HA PASADO FACTURA! §7El desgaste del Ultra Instinto supera el límite de un mortal.");
    }

    // ==========================================
    // ESPADA DE HAZ DE LUZ DE VEGETTO
    // ==========================================
    @EventHandler
    public void onVegettoSwordInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!plugin.isWorldAllowed(player.getWorld())) return;
        if (!player.isSneaking()) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR || !item.getType().name().endsWith("_SWORD")) return;

        UUID uuid = player.getUniqueId();
        Rank rank = plugin.getRankManager().getPlayerRank(uuid);
        int tier = (rank != null) ? rank.getTier() : 0;
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(uuid);
        String equipped = settings.getActiveTransformation();

        boolean isEquipped = "VEGETTO_SWORD".equalsIgnoreCase(equipped);
        boolean isCurrentRankPrimary = (rank != null && rank.getAbilityType() == AbilityType.VEGETTO_SPIRIT_SWORD && (equipped == null || equipped.isEmpty()));
        if (!isEquipped && !isCurrentRankPrimary) return;

        long now = System.currentTimeMillis();
        long ready = spiritSwordCooldown.getOrDefault(uuid, 0L);
        if (now < ready) {
            long rem = Math.max(1, (ready - now) / 1000L);
            player.sendActionBar(Component.text("§c⏳ Espada de Luz en recarga: §e" + rem + "s"));
            return;
        }

        spiritSwordCooldown.put(uuid, now + 12000L); // 12s cooldown

        // Launch piercing beam of light
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();

        try {
            player.getWorld().playSound(eye, Sound.ITEM_TRIDENT_THUNDER, 1.0f, 1.6f);
            player.getWorld().playSound(eye, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.6f);
        } catch (Exception ignored) {}

        Set<LivingEntity> damaged = new HashSet<>();
        for (double d = 1.0; d <= 14.0; d += 0.5) {
            Location point = eye.clone().add(dir.clone().multiply(d));
            if (!point.getBlock().isPassable()) break;

            try {
                point.getWorld().spawnParticle(Particle.END_ROD, point, 2, 0.05, 0.05, 0.05, 0.01);
                point.getWorld().spawnParticle(Particle.SWEEP_ATTACK, point, 1, 0, 0, 0, 0);
            } catch (Exception ignored) {}

            for (LivingEntity entity : point.getWorld().getNearbyLivingEntities(point, 1.3)) {
                if (entity.equals(player)) continue;
                if (!damaged.contains(entity)) {
                    damaged.add(entity);
                    try {
                        entity.damage(14.0, player); // 7 hearts piercing damage
                        entity.getWorld().spawnParticle(Particle.FLASH, entity.getLocation().add(0, 1.0, 0), 1);
                    } catch (Exception ignored) {}
                }
            }
        }

        player.sendActionBar(Component.text("§e⚡ ¡ESPADA DE HAZ DE LUZ DE VEGETTO! §7(Perforación colosal)"));
    }

    // ==========================================
    // HAKAI DE BILLS / BEERUS (DESINTEGRACIÓN DESTRUCTORA)
    // ==========================================
    @EventHandler
    public void onBillsHakai(PlayerInteractEvent event) {
        // Trigger: Sneak + Left Click o Sneak + Right Click con mano vacia/aura destructora
        boolean isLeft = (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK);
        boolean isRightEmpty = (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                && event.getPlayer().getInventory().getItemInMainHand().getType() == Material.AIR;

        if (!isLeft && !isRightEmpty) return;

        Player player = event.getPlayer();
        if (!plugin.isWorldAllowed(player.getWorld())) return;
        if (!player.isSneaking()) return;

        UUID uuid = player.getUniqueId();
        Rank rank = plugin.getRankManager().getPlayerRank(uuid);
        int tier = (rank != null) ? rank.getTier() : 0;
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(uuid);
        if (!settings.isAbilitiesEnabled()) return;

        boolean hasHakai = (tier >= 47 || (rank != null && (rank.getAbilityType() == AbilityType.HAKAI_AURA
                || rank.getAbilityType() == AbilityType.ULTRA_EGO)) || isUltraEgoActive(uuid));
        if (!hasHakai) return;

        long now = System.currentTimeMillis();
        long ready = hakaiCooldown.getOrDefault(uuid, 0L);
        if (now < ready) {
            long rem = Math.max(1, (ready - now) / 1000L);
            player.sendActionBar(Component.text("§c⏳ Hakai en recarga: §5" + rem + "s"));
            return;
        }

        hakaiCooldown.put(uuid, now + 35000L); // 35s cooldown

        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();

        try {
            player.getWorld().playSound(eye, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.8f);
            player.getWorld().playSound(eye, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.6f);
        } catch (Exception ignored) {}

        Set<LivingEntity> damaged = new HashSet<>();
        for (double d = 1.0; d <= 14.0; d += 0.5) {
            Location point = eye.clone().add(dir.clone().multiply(d));
            if (!point.getBlock().isPassable()) break;

            try {
                point.getWorld().spawnParticle(Particle.WITCH, point, 3, 0.08, 0.08, 0.08, 0.02);
                point.getWorld().spawnParticle(Particle.DRAGON_BREATH, point, 2, 0.05, 0.05, 0.05, 0.01);
                point.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, point, 1, 0.02, 0.02, 0.02, 0.01);
            } catch (Exception ignored) {}

            for (LivingEntity entity : point.getWorld().getNearbyLivingEntities(point, 1.4)) {
                if (entity.equals(player)) continue;
                if (!damaged.contains(entity)) {
                    damaged.add(entity);
                    try {
                        Location eLoc = entity.getLocation().add(0, 1.0, 0);
                        eLoc.getWorld().spawnParticle(Particle.ASH, eLoc, 35, 0.3, 0.5, 0.3, 0.1);
                        eLoc.getWorld().spawnParticle(Particle.FLASH, eLoc, 1);
                        eLoc.getWorld().playSound(eLoc, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1.0f, 0.7f);

                        if (entity instanceof Player victim) {
                            // PvP balance: NO instakill a jugadores (14.0 dano = 7 corazones + oscuridad)
                            victim.damage(14.0, player);
                            victim.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 80, 0));
                            victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 1));
                            victim.sendMessage("§5§l¡HAKAI! §7Has sido alcanzado por la energia destructora de un Dios.");
                        } else {
                            // Mobs y jefes: Desintegracion colosal
                            entity.damage(50.0, player);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        player.sendTitle("§5§l¡HAKAI!", "§dEnergia de la Destruccion desatada", 0, 25, 10);
        player.sendActionBar(Component.text("§5⚡ ¡HAKAI DEPREDADOR DESATADO! §7(Recarga: 35s)"));
    }

    // ==========================================
    // BORRADO UNIVERSAL DE ZENO-SAMA (TIER 50+)
    // ==========================================
    @EventHandler
    public void onZenoErase(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!plugin.isWorldAllowed(player.getWorld())) return;
        if (!player.isSneaking()) return;

        UUID uuid = player.getUniqueId();
        Rank rank = plugin.getRankManager().getPlayerRank(uuid);
        int tier = (rank != null) ? rank.getTier() : 0;
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(uuid);
        if (!settings.isAbilitiesEnabled()) return;

        ItemStack hand = player.getInventory().getItemInMainHand();
        boolean hasTotem = hand.hasItemMeta() && hand.getItemMeta().hasDisplayName()
                && hand.getItemMeta().getDisplayName().contains("Zeno-Sama");
        boolean isKami = (tier >= 50 || (rank != null && rank.getAbilityType() == AbilityType.KAMI_DIVINE));

        if (!isKami && !hasTotem) return;

        long now = System.currentTimeMillis();
        long ready = zenoEraseCooldown.getOrDefault(uuid, 0L);
        if (now < ready) {
            long rem = Math.max(1, (ready - now) / 1000L);
            player.sendActionBar(Component.text("§c⏳ Borrado Universal en enfriamiento: §e" + rem + "s"));
            return;
        }

        zenoEraseCooldown.put(uuid, now + 90000L); // 90s cooldown
        event.setCancelled(true);

        Location center = player.getLocation();
        try {
            center.getWorld().playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.9f);
            center.getWorld().playSound(center, Sound.ITEM_TOTEM_USE, 0.8f, 1.5f);
            center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.5f);
            center.getWorld().spawnParticle(Particle.FLASH, center.clone().add(0, 1.0, 0), 3);
            center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(0, 1.0, 0), 80, 2.0, 1.0, 2.0, 0.1);
        } catch (Exception ignored) {}

        int erased = 0;
        for (LivingEntity target : center.getWorld().getNearbyLivingEntities(center, 16.0)) {
            if (target instanceof Monster || target instanceof Phantom
                    || target instanceof Slime || target instanceof Ghast) {
                Location tLoc = target.getLocation();
                try {
                    tLoc.getWorld().spawnParticle(Particle.POOF, tLoc.add(0, 0.5, 0), 15, 0.3, 0.3, 0.3, 0.05);
                } catch (Exception ignored) {}
                target.remove();
                erased++;
            }
        }

        player.sendTitle("§b§l¡BORRADO OMNI-UNIVERSAL!", "§f" + erased + " entidades hostiles desvanecidas de la existencia", 5, 40, 15);
        player.sendActionBar(Component.text("§b⚡ ¡BORRADO DE ZENO-SAMA! §e" + erased + " mobs erradicados §7(Recarga: 90s)"));
    }

    // ==========================================
    // SUPER IMPULSO SÓNICO (W + DOBLE SALTO) & VUELO DE KI
    // ==========================================
    @EventHandler
    public void onKiFlightToggle(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        if (!plugin.isWorldAllowed(player.getWorld())) return;

        UUID uuid = player.getUniqueId();
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(uuid);
        if (!settings.isAbilitiesEnabled() || !settings.isKiFlightEnabled()) {
            if (!plugin.hasExternalFlight(player) && player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setFlying(false);
                player.setAllowFlight(false);
            }
            if (player.getFlySpeed() != 0.10f) {
                player.setFlySpeed(0.10f);
            }
            return;
        }

        Rank rank = plugin.getRankManager().getPlayerRank(uuid);
        int tier = (rank != null) ? rank.getTier() : 0;
        boolean hasTransform = isAnyTransformationActive(uuid);
        boolean hasStaff = (player.hasPermission("drakesrankup.staff") || plugin.getStaffManager().isAngel(uuid));
        boolean eligible = (tier >= 31 || hasTransform || hasStaff);

        if (!eligible) {
            if (!plugin.hasExternalFlight(player) && player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setFlying(false);
                player.setAllowFlight(false);
            }
            return;
        }

        event.setCancelled(true);
        player.setFlying(false);

        long now = System.currentTimeMillis();
        long ready = kiFlightCooldown.getOrDefault(uuid, 0L);
        if (now < ready) {
            long remMs = ready - now;
            double remSec = remMs / 1000.0;
            player.sendActionBar(Component.text(String.format("§c⏳ Super Impulso en enfriamiento: §e%.1fs", remSec)));
            return;
        }

        // Cooldown escalonado estricto:
        // Tier 100 -> 2.0 segundos
        // Tier 15  -> 20.0 segundos
        // NUNCA supera 20 segundos de recarga
        int effectiveTier = Math.min(100, Math.max(15, tier));
        double tRatio = (effectiveTier - 15) / 85.0;
        double rawCdSec = 20.0 - (tRatio * 18.0);
        int rebirths = plugin.getRankManager().getRebirthCount(uuid);
        double cdr = 1.0 - Math.min(0.50, rebirths * 0.01);
        long cdMillis = Math.max(2000L, (long)(rawCdSec * cdr * 1000L));

        kiFlightCooldown.put(uuid, now + cdMillis);
        kiFlightImmunity.put(uuid, now + 15000L); // 15s de inmunidad total a caídas

        // Lanzamiento sónico direccional a donde mira (~80 a 100 bloques de distancia)
        Vector dir = player.getLocation().getDirection().normalize();
        double yLaunch = Math.max(0.42, dir.getY() * 1.15 + 0.35);
        Vector launch = dir.clone().multiply(3.75).setY(yLaunch);
        player.setVelocity(launch);
        player.setFallDistance(0);

        Location loc = player.getLocation();
        try {
            loc.getWorld().spawnParticle(Particle.SONIC_BOOM, loc, 2);
            loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
            loc.getWorld().spawnParticle(Particle.FIREWORK, loc, 35, 0.4, 0.4, 0.4, 0.15);
            loc.getWorld().spawnParticle(Particle.FLASH, loc, 1);
            loc.getWorld().playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.4f);
            loc.getWorld().playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.2f, 1.0f);
            loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.8f);
        } catch (Exception ignored) {}

        player.sendTitle("§b§l¡SUPER IMPULSO SÓNICO!", "§ePropulsión aérea a 100 bloques", 5, 25, 10);
        player.sendActionBar(Component.text(String.format("§b⚡ ¡SUPER IMPULSO SÓNICO! §7(Enfriamiento: §e%.1fs§7)", cdMillis / 1000.0)));

        // Conceder aprox 10 segundos de vuelo controlado aerodinámico y propulsión sónica
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFlySpeed(0.18f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                ticks += 2;
                player.setFallDistance(0);

                if (player.isFlying() || player.getVelocity().lengthSquared() > 0.05) {
                    Location pLoc = player.getLocation();
                    try {
                        pLoc.getWorld().spawnParticle(Particle.FIREWORK, pLoc, 2, 0.15, 0.05, 0.15, 0.02);
                        pLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, pLoc, 2, 0.1, 0.05, 0.1, 0.02);
                        pLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, pLoc.clone().add(0, 0.5, 0), 2, 0.2, 0.2, 0.2, 0.05);
                    } catch (Exception ignored) {}
                }

                if (ticks >= 200 || !settings.isKiFlightEnabled() || !settings.isAbilitiesEnabled()) { // 10s o cancelacion por toggle
                    cancel();
                    if (player.isOnline()) {
                        player.setFlySpeed(0.10f); // Restaurar velocidad por defecto
                        if (!plugin.hasExternalFlight(player) && player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                            player.setFlying(false);
                            player.setAllowFlight(false);
                            player.sendMessage("§e[Impulso Sónico] §7Propulsión sónica finalizada. Vuelo normal restaurado.");
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 2L, 2L);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        if (!plugin.isWorldAllowed(player.getWorld())) return;

        UUID uuid = player.getUniqueId();
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(uuid);

        if (!settings.isAbilitiesEnabled() || !settings.isKiFlightEnabled()) {
            if (!plugin.hasExternalFlight(player) && player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                if (player.isFlying()) {
                    player.setFlying(false);
                }
                if (player.getAllowFlight()) {
                    player.setAllowFlight(false);
                }
            }
            if (player.getFlySpeed() != 0.10f) {
                player.setFlySpeed(0.10f);
            }
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to != null && (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ())) {
                spawnMovementTrail(player, uuid);
            }
            return;
        }

        Rank rank = plugin.getRankManager().getPlayerRank(uuid);
        int tier = (rank != null) ? rank.getTier() : 0;
        boolean hasTransform = isAnyTransformationActive(uuid);
        boolean hasStaff = (player.hasPermission("drakesrankup.staff") || plugin.getStaffManager().isAngel(uuid));
        boolean eligible = (tier >= 31 || hasTransform || hasStaff);

        if (eligible && !plugin.hasExternalFlight(player)) {
            long now = System.currentTimeMillis();
            long ready = kiFlightCooldown.getOrDefault(uuid, 0L);
            if (now >= ready) {
                if (!player.getAllowFlight()) {
                    player.setAllowFlight(true);
                }
            }
        } else if (!eligible && !plugin.hasExternalFlight(player)) {
            if (player.getAllowFlight()) {
                player.setAllowFlight(false);
            }
            if (player.isFlying()) {
                player.setFlying(false);
            }
        }

        // ==========================================
        // ESTELA DE COLOR AL CORRER O CAMINAR
        // ==========================================
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to != null && (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ())) {
            spawnMovementTrail(player, uuid);
        }
    }

    public boolean isAnyTransformationActive(UUID uuid) {
        if (isSSJGodActive(uuid) || isSSJBlueActive(uuid) || isKaiokenActive(uuid) ||
            isUltraEgoActive(uuid) || isGohanBeastActive(uuid) || isBrolyActive(uuid) || isMuiActive(uuid)) {
            return true;
        }
        if (plugin.getOnePieceListener() != null) {
            if (plugin.getOnePieceListener().isGear3Active(uuid) ||
                plugin.getOnePieceListener().isGear4Active(uuid) ||
                plugin.getOnePieceListener().isGearSecondActive(uuid)) {
                return true;
            }
        }
        return false;
    }

    public void spawnMovementTrail(Player player, UUID uuid) {
        Location loc = player.getLocation().add(0, 0.15, 0);
        World world = loc.getWorld();
        if (world == null) return;

        try {
            if (isSSJ2Active(uuid)) {
                // Estela SSJ 2: Oro radiante con chispas de bio-electricidad
                world.spawnParticle(Particle.DUST, loc, 3, 0.15, 0.1, 0.15, 0, new Particle.DustOptions(Color.fromRGB(255, 230, 30), 1.4f));
                world.spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0, 0.4, 0), 2, 0.2, 0.2, 0.2, 0.05);
            } else if (isKaiokenActive(uuid)) {
                // Estela Kaio-ken: Rojo carmesí y humo de calor cardíaco
                world.spawnParticle(Particle.DUST, loc, 3, 0.15, 0.1, 0.15, 0, new Particle.DustOptions(Color.fromRGB(255, 10, 20), 1.4f));
                world.spawnParticle(Particle.FLAME, loc, 1, 0.05, 0.05, 0.05, 0.01);
            } else if (isSSJGodActive(uuid)) {
                // Estela SSJ God: Fuego divino carmesí-anaranjado
                world.spawnParticle(Particle.DUST, loc, 3, 0.15, 0.1, 0.15, 0, new Particle.DustOptions(Color.fromRGB(255, 65, 45), 1.3f));
                world.spawnParticle(Particle.FLAME, loc, 2, 0.1, 0.05, 0.1, 0.02);
            } else if (isSSJBlueActive(uuid)) {
                // Estela SSJ Blue: Azul eléctrico / fuego celestial
                world.spawnParticle(Particle.DUST, loc, 3, 0.15, 0.1, 0.15, 0, new Particle.DustOptions(Color.fromRGB(40, 180, 255), 1.3f));
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 1, 0.05, 0.05, 0.05, 0.01);
                world.spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0, 0.3, 0), 1, 0.1, 0.1, 0.1, 0.03);
            } else if (isUltraEgoActive(uuid)) {
                // Estela Ultra Ego: Púrpura de la Destrucción (Hakai)
                world.spawnParticle(Particle.DUST, loc, 3, 0.15, 0.1, 0.15, 0, new Particle.DustOptions(Color.fromRGB(150, 20, 220), 1.4f));
                world.spawnParticle(Particle.WITCH, loc, 2, 0.1, 0.1, 0.1, 0.02);
            } else if (isGohanBeastActive(uuid)) {
                // Estela Gohan Beast: Blanco plateado con relámpagos magenta
                world.spawnParticle(Particle.DUST, loc, 3, 0.15, 0.1, 0.15, 0, new Particle.DustOptions(Color.fromRGB(245, 245, 255), 1.3f));
                world.spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0, 0.4, 0), 2, 0.15, 0.15, 0.15, 0.05);
            } else if (isBrolyActive(uuid)) {
                // Estela Broly LSSJ: Verde neón radiactivo berserker
                world.spawnParticle(Particle.DUST, loc, 3, 0.15, 0.1, 0.15, 0, new Particle.DustOptions(Color.fromRGB(60, 255, 40), 1.4f));
                world.spawnParticle(Particle.ITEM_SLIME, loc, 2, 0.1, 0.1, 0.1, 0.02);
            } else if (isMuiActive(uuid)) {
                // Estela MUI: Plateado cósmico / End Rod celestial
                world.spawnParticle(Particle.DUST, loc, 3, 0.15, 0.1, 0.15, 0, new Particle.DustOptions(Color.fromRGB(225, 235, 255), 1.2f));
                world.spawnParticle(Particle.END_ROD, loc.clone().add(0, 0.3, 0), 1, 0.05, 0.05, 0.05, 0.01);
            } else if (plugin.getOnePieceListener() != null && plugin.getOnePieceListener().isGear3Active(uuid)) {
                // Estela Gear 3: Nubes de vapor y rebote elástico
                world.spawnParticle(Particle.CLOUD, loc, 2, 0.2, 0.1, 0.2, 0.01);
                world.spawnParticle(Particle.ITEM_SLIME, loc, 2, 0.1, 0.1, 0.1, 0.02);
            } else if (plugin.getOnePieceListener() != null && plugin.getOnePieceListener().isGear4Active(uuid)) {
                // Estela Gear 4: Vapor negro y carmesí de armadura Haki
                world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 1, 0.1, 0.1, 0.1, 0.01);
                world.spawnParticle(Particle.DUST, loc, 2, 0.15, 0.1, 0.15, 0, new Particle.DustOptions(Color.fromRGB(150, 15, 25), 1.3f));
            } else {
                // Si tiene Rebirths activos, dejar una estela dorada celestial sutil
                int rebirths = plugin.getRankManager().getRebirthCount(uuid);
                if (rebirths > 0) {
                    world.spawnParticle(Particle.DUST, loc, 1, 0.1, 0.05, 0.1, 0, new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.0f));
                }
            }
        } catch (Exception ignored) {}
    }

    // ==========================================
    // COMBATE & DAÑO
    // ==========================================
    @EventHandler(priority = EventPriority.LOWEST)
    public void onIncomingDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!plugin.isWorldAllowed(player.getWorld())) return;
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // Ki Flight Fall Immunity
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (now < kiFlightImmunity.getOrDefault(uuid, 0L)) {
                event.setCancelled(true);
                return;
            }
        }

        // Esquiva total durante Ultra Instinto Dominado -- SOLO ataques de combate.
        // (No ignora lava, void, caida ni ahogo: el Ultra Instinto esquiva golpes, no el entorno.)
        if (isMuiActive(uuid)) {
            switch (event.getCause()) {
                case ENTITY_ATTACK, ENTITY_SWEEP_ATTACK, PROJECTILE, MAGIC, ENTITY_EXPLOSION, THORNS -> {
                    event.setCancelled(true);
                    triggerMuiAfterimage(player);
                    return;
                }
                default -> { /* el dano ambiental si afecta */ }
            }
        }

        // Broly takes +20% damage from projectiles (tradeoff)
        if (isBrolyActive(uuid) && event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
            event.setDamage(event.getDamage() * 1.20);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onCombatDamage(EntityDamageByEntityEvent event) {
        if (!plugin.isWorldAllowed(event.getEntity().getWorld())) return;
        // Attacker is Gohan Beast (+75% critical burst)
        if (event.getDamager() instanceof Player attacker) {
            if (!plugin.isWorldAllowed(attacker.getWorld())) return;
            if (isGohanBeastActive(attacker.getUniqueId())) {
                event.setDamage(event.getDamage() * 1.75);
                try {
                    Location loc = event.getEntity().getLocation().add(0, 1.0, 0);
                    attacker.getWorld().spawnParticle(Particle.CRIMSON_SPORE, loc, 25, 0.3, 0.5, 0.3, 0.1);
                    attacker.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 15, 0.3, 0.5, 0.3, 0.1);
                    attacker.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.8f);
                } catch (Exception ignored) {}
            }
        }

        // Attacker is Ultra Ego Vegeta
        if (event.getDamager() instanceof Player attacker) {
            if (isUltraEgoActive(attacker.getUniqueId())) {
                double maxHp = 20.0;
                try {
                    if (attacker.getAttribute(Attribute.MAX_HEALTH) != null) {
                        maxHp = attacker.getAttribute(Attribute.MAX_HEALTH).getValue();
                    }
                } catch (Throwable ignored) {}

                double missingFraction = Math.max(0.0, Math.min(0.9, (maxHp - attacker.getHealth()) / maxHp));
                double multiplier = 1.0 + (missingFraction * 0.60); // up to +60% damage
                event.setDamage(event.getDamage() * multiplier);

                try {
                    Location loc = event.getEntity().getLocation().add(0, 1.0, 0);
                    attacker.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 15, 0.2, 0.3, 0.2, 0.05);
                    attacker.getWorld().spawnParticle(Particle.CRIT, loc, 10, 0.2, 0.2, 0.2, 0.1);
                    attacker.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.9f, 1.6f);
                } catch (Exception ignored) {}
            }
        }

        // Victim is Gohan Beast -- furia temeraria: recibe +20% de dano (glass cannon con riesgo real)
        if (event.getEntity() instanceof Player gVictim && isGohanBeastActive(gVictim.getUniqueId())) {
            event.setDamage(event.getDamage() * 1.20);
        }

        // Victim is Ultra Ego Vegeta
        if (event.getEntity() instanceof Player victim) {
            if (isUltraEgoActive(victim.getUniqueId())) {
                if (event.getDamager() instanceof LivingEntity damager) {
                    try {
                        damager.damage(3.5, victim);
                        Location loc = damager.getLocation().add(0, 1.0, 0);
                        damager.getWorld().spawnParticle(Particle.WITCH, loc, 15, 0.2, 0.3, 0.2, 0.05);
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    // Animacion cinematografica del despertar de Ultra Instinto (plata fluida):
    // FASE 1 implosion -> FASE 2 estallido con pilar y onda -> FASE 3 aura en doble helice.
    // Estallido cinematografico de despertar (reusable): pilar de luz tematico + onda de choque + rugido.
    private void playAwakeningBurst(Player player, Color c1, Color c2, Particle accent, Sound roar) {
        Location loc = player.getLocation();
        var w = player.getWorld();
        Particle.DustOptions d1 = new Particle.DustOptions(c1, 1.4f);
        Particle.DustOptions d2 = new Particle.DustOptions(c2, 1.1f);
        try {
            w.spawnParticle(Particle.FLASH, loc.clone().add(0, 1, 0), 1);
            w.spawnParticle(Particle.EXPLOSION_EMITTER, loc.clone().add(0, 1, 0), 1);
            for (double dy = 0; dy < 5.5; dy += 0.25) {
                w.spawnParticle(accent, loc.clone().add(0, dy, 0), 1, 0.08, 0, 0.08, 0.01);
                w.spawnParticle(Particle.DUST, loc.clone().add(0, dy, 0), 1, 0.15, 0, 0.15, 0, d1);
            }
            for (int i = 0; i < 40; i++) {
                double a = (Math.PI * 2 / 40) * i;
                w.spawnParticle(Particle.DUST, loc.clone().add(Math.cos(a) * 1.8, 0.2, Math.sin(a) * 1.8), 1, 0, 0, 0, 0, d2);
            }
            w.playSound(loc, roar, 1.0f, 1.0f);
            w.playSound(loc, Sound.ITEM_TRIDENT_THUNDER, 0.6f, 1.5f);
        } catch (Exception ignored) {}
    }

    private void playMuiActivationFx(Player player) {
        final Particle.DustOptions PLATA = new Particle.DustOptions(Color.fromRGB(232, 236, 245), 1.3f);
        final Particle.DustOptions CELESTE = new Particle.DustOptions(Color.fromRGB(150, 210, 255), 1.1f);
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (!player.isOnline()) { cancel(); return; }
                Location loc = player.getLocation();
                var w = player.getWorld();
                try {
                    if (t < 10) {
                        // FASE 1 - anillo plateado que colapsa hacia el jugador
                        double radius = 3.0 * (1.0 - t / 10.0) + 0.3;
                        double y = 0.1 + (t / 10.0) * 1.3;
                        for (int i = 0; i < 14; i++) {
                            double ang = (Math.PI * 2 / 14) * i + t * 0.35;
                            double x = Math.cos(ang) * radius, z = Math.sin(ang) * radius;
                            w.spawnParticle(Particle.DUST, loc.clone().add(x, y, z), 1, 0, 0, 0, 0, PLATA);
                            if (t % 2 == 0) w.spawnParticle(Particle.END_ROD, loc.clone().add(x, y, z), 1, 0, 0, 0, 0.01);
                        }
                        if (t % 3 == 0) w.playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 0.7f, 0.6f + t * 0.04f);
                    } else if (t == 10) {
                        // FASE 2 - despertar: flash, pilar de luz y onda de choque
                        w.spawnParticle(Particle.FLASH, loc.clone().add(0, 1, 0), 2);
                        w.spawnParticle(Particle.EXPLOSION_EMITTER, loc.clone().add(0, 1, 0), 1);
                        for (double dy = 0; dy < 6.5; dy += 0.22)
                            w.spawnParticle(Particle.END_ROD, loc.clone().add(0, dy, 0), 2, 0.09, 0, 0.09, 0.0);
                        for (int i = 0; i < 44; i++) {
                            double ang = (Math.PI * 2 / 44) * i;
                            w.spawnParticle(Particle.SWEEP_ATTACK, loc.clone().add(Math.cos(ang) * 1.6, 0.2, Math.sin(ang) * 1.6), 1);
                        }
                        w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.6f);
                        w.playSound(loc, Sound.ITEM_TRIDENT_THUNDER, 0.8f, 1.9f);
                        w.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.35f, 2.0f);
                    } else {
                        // FASE 3 - aura sostenida en doble helice + chispas y ceniza flotante
                        double phase = t * 0.55;
                        double yy = ((t - 11) % 34) / 34.0 * 2.4;
                        for (int s = 0; s < 2; s++) {
                            double ang = phase + Math.PI * s;
                            double x = Math.cos(ang) * 0.95, z = Math.sin(ang) * 0.95;
                            w.spawnParticle(Particle.DUST, loc.clone().add(x, yy, z), 1, 0, 0, 0, 0, s == 0 ? PLATA : CELESTE);
                        }
                        if (t % 5 == 0) w.spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0, 1.0, 0), 3, 0.4, 0.6, 0.4, 0.02);
                        if (t % 9 == 0) w.spawnParticle(Particle.WHITE_ASH, loc.clone().add(0, 1.3, 0), 6, 0.5, 0.9, 0.5, 0.01);
                    }
                } catch (Exception ignored) {}
                if (t++ >= 118) cancel(); // ~6s: cubre los 5s de MUI
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void triggerMuiAfterimage(Player player) {
        Location loc = player.getLocation();

        // Micro-teleport 0.8 blocks backward/sideways
        Vector back = loc.getDirection().normalize().multiply(-0.8).setY(0);
        Location target = loc.clone().add(back);
        if (target.getBlock().isPassable() && target.clone().add(0, 1, 0).getBlock().isPassable()) {
            player.teleport(target); // paso lateral solo si pies Y cabeza quedan libres (evita sofocacion)
        }

        try {
            loc.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0, 0.8, 0), 12, 0.2, 0.4, 0.2, 0.02);
            loc.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(0, 1.0, 0), 15, 0.2, 0.3, 0.2, 0.05);
            loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.8f);
            player.sendActionBar(Component.text("§f⚡ ¡ESQUIVE INSTINTIVO! §7(Doctrina Egoísta)"));
        } catch (Exception ignored) {}
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        BukkitTask task = chargingTasks.remove(uuid);
        if (task != null) task.cancel();
        activeMUI.remove(uuid);
        activeUltraEgo.remove(uuid);
        activeSSJGod.remove(uuid);
        activeSSJBlue.remove(uuid);
        activeGohanBeast.remove(uuid);
        activeBroly.remove(uuid);
        activeKaioken.remove(uuid);
        activeSSJ2.remove(uuid);
        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        BukkitTask task = chargingTasks.remove(uuid);
        if (task != null) task.cancel();
        activeMUI.remove(uuid);
        activeUltraEgo.remove(uuid);
        activeSSJGod.remove(uuid);
        activeSSJBlue.remove(uuid);
        activeGohanBeast.remove(uuid);
        activeBroly.remove(uuid);
        activeKaioken.remove(uuid);
        activeSSJ2.remove(uuid);
        cooldowns.remove(uuid);
        spiritSwordCooldown.remove(uuid);
        kiFlightCooldown.remove(uuid);
        kiFlightImmunity.remove(uuid);
    }

    public void grantTransformationFlight(Player player, int seconds) {
        if (player == null || !player.isOnline()) return;
        UUID uuid = player.getUniqueId();
        if (!player.getAllowFlight()) {
            player.setAllowFlight(true);
        }
        player.setFlying(true);
        kiFlightImmunity.put(uuid, System.currentTimeMillis() + (seconds + 8) * 1000L);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                ticks += 5;
                if (player.isFlying()) {
                    Location loc = player.getLocation();
                    try {
                        player.getWorld().spawnParticle(Particle.FIREWORK, loc, 2, 0.1, 0.05, 0.1, 0.02);
                        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 1, 0.05, 0.05, 0.05, 0.01);
                    } catch (Exception ignored) {}
                }
                if (ticks >= seconds * 20) {
                    cancel();
                    if (player.isOnline() && !plugin.hasExternalFlight(player) && player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                        player.setFlying(false);
                        player.setAllowFlight(false);
                        player.sendMessage("§e[Vuelo Saiyajin] §7Tu impulso de vuelo por transformación ha expirado (Inmunidad a caídas: 8s).");
                    }
                }
            }
        }.runTaskTimer(plugin, 5L, 5L);
    }

}
