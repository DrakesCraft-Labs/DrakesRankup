package com.drakescraft.rankup.ability;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.AbilityType;
import com.drakescraft.rankup.model.PlayerSettings;
import com.drakescraft.rankup.model.Rank;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
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

    // Cooldowns: UUID -> available timestamp in millis
    private final Map<UUID, Long> cooldowns = new HashMap<>();

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

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
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

        // Check if player has access to any Dragon Ball transformation
        boolean hasGod = (tier >= 35 || ability == AbilityType.SSJ_GOD);
        boolean hasBlue = (tier >= 36 || ability == AbilityType.SSJ_BLUE);
        boolean hasMUI = (tier >= 46 || ability == AbilityType.MASTERED_ULTRA_INSTINCT || ability == AbilityType.ULTRA_INSTINCT);
        boolean hasEgo = (tier >= 47 || ability == AbilityType.ULTRA_EGO || ability == AbilityType.HAKAI_AURA);
        boolean isKami = (tier >= 50);

        if (!hasGod && !hasBlue && !hasMUI && !hasEgo && !isKami) return;

        // Determine transformation type based on highest unlocked or specific ability
        String transName;
        long cdSeconds;
        Particle kiParticle;

        if (hasMUI) {
            transName = "MUI";
            cdSeconds = 40;
            kiParticle = Particle.END_ROD;
        } else if (hasEgo) {
            transName = "ULTRA_EGO";
            cdSeconds = 50;
            kiParticle = Particle.WITCH;
        } else if (hasBlue) {
            transName = "SSJ_BLUE";
            cdSeconds = 45;
            kiParticle = Particle.SOUL_FIRE_FLAME;
        } else {
            transName = "SSJ_GOD";
            cdSeconds = 35;
            kiParticle = Particle.FLAME;
        }

        long now = System.currentTimeMillis();
        long nextReady = cooldowns.getOrDefault(uuid, 0L);
        if (now < nextReady) {
            long remaining = Math.max(1, (nextReady - now) / 1000L);
            player.sendActionBar(Component.text("§c⏳ Ki en reposo: §e" + remaining + "s"));
            return;
        }

        // Start Ki charging runnable (runs every 4 ticks for 36 ticks total = 1.8 seconds)
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !player.isSneaking()) {
                    chargingTasks.remove(uuid);
                    cancel();
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

                Location loc = player.getLocation();
                try {
                    player.getWorld().spawnParticle(kiParticle, loc.clone().add(0, 0.2, 0), 10, 0.3, 0.2, 0.3, 0.05);
                    player.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 0.5f, 0.8f + (progress * 1.2f));
                } catch (Exception ignored) {}

                if (ticks >= 36) {
                    chargingTasks.remove(uuid);
                    cancel();
                    detonateTransformation(player, transName, cdSeconds);
                }
            }
        }.runTaskTimer(plugin, 0L, 4L);

        chargingTasks.put(uuid, task);
    }

    private void detonateTransformation(Player player, String type, long cdSeconds) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        cooldowns.put(uuid, now + (cdSeconds * 1000L));

        Location loc = player.getLocation();

        try {
            player.getWorld().spawnParticle(Particle.EXPLOSION, loc.clone().add(0, 1.0, 0), 2);
            player.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.4f);
        } catch (Exception ignored) {}

        if (type.equals("MUI")) {
            activeMUI.put(uuid, now + 5000L); // 5 seconds of total dodge
            player.sendTitle("§f§lDOCTRINA EGOÍSTA", "§bUltra Instinto Dominado activado", 5, 40, 5);
            player.sendActionBar(Component.text("§f⚡ ¡EVASIÓN TOTAL ACTIVADA POR 5 SEGUNDOS!"));

            // Record usage for fatigue
            List<Long> history = muiUsageHistory.computeIfAbsent(uuid, k -> new ArrayList<>());
            history.removeIf(t -> now - t > 120_000L);
            history.add(now);

            final boolean willFatigue = history.size() >= 2;

            // Schedule end of MUI
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                activeMUI.remove(uuid);
                if (player.isOnline()) {
                    if (willFatigue) {
                        applyMuiFatigue(player);
                    } else {
                        player.sendMessage("§7[Rankup] El estado de Ultra Instinto ha concluido.");
                    }
                }
            }, 100L);

        } else if (type.equals("ULTRA_EGO")) {
            activeUltraEgo.put(uuid, now + 15000L); // 15 seconds
            player.sendTitle("§5§lMEGA INSTINTO (ULTRA EGO)", "§dEl daño recibido aumenta tu poder de destrucción", 5, 40, 5);
            player.sendActionBar(Component.text("§5⚡ ¡AURA HAKAI ACTIVA POR 15 SEGUNDOS!"));

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                activeUltraEgo.remove(uuid);
                if (player.isOnline()) {
                    player.sendMessage("§5[Rankup] El Mega Instinto se ha disipado.");
                }
            }, 300L);

        } else if (type.equals("SSJ_BLUE")) {
            activeSSJBlue.put(uuid, now + 20000L);
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 400, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 400, 1));
            player.sendTitle("§9§lSUPER SAIYAJIN BLUE", "§bFuerza y velocidad de los dioses", 5, 40, 5);

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                activeSSJBlue.remove(uuid);
            }, 400L);

        } else {
            activeSSJGod.put(uuid, now + 20000L);
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 400, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 400, 0));
            player.sendTitle("§c§lSUPER SAIYAJIN GOD", "§eKi divino de sanación y gracia", 5, 40, 5);

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                activeSSJGod.remove(uuid);
            }, 400L);
        }
    }

    private void applyMuiFatigue(Player player) {
        Location loc = player.getLocation();
        try {
            player.getWorld().spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 1.0, 0), 30, 0.4, 0.5, 0.4, 0.08);
            player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, loc.clone().add(0, 1.0, 0), 15, 0.3, 0.4, 0.3, 0.05);
            player.getWorld().playSound(loc, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 0.6f);
            player.getWorld().playSound(loc, Sound.BLOCK_ANVIL_LAND, 0.8f, 0.5f);
        } catch (Exception ignored) {}

        player.damage(8.0); // 4 hearts pure strain damage
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2)); // Level III for 5s
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 50, 0)); // 2.5s

        player.sendTitle("§c§l¡EL CUERPO PASÓ FACTURA!", "§eEl límite divino desgarró tus músculos", 10, 60, 15);
        player.sendMessage("§c§l¡EL CUERPO HA PASADO FACTURA! §7El desgaste del Ultra Instinto supera el límite de un mortal.");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onIncomingDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();

        // 100% Dodge during Mastered Ultra Instinct
        if (isMuiActive(uuid)) {
            event.setCancelled(true);
            triggerMuiAfterimage(player);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onCombatDamage(EntityDamageByEntityEvent event) {
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

    private void triggerMuiAfterimage(Player player) {
        Location loc = player.getLocation();

        // Micro-teleport 0.8 blocks backward/sideways
        Vector back = loc.getDirection().normalize().multiply(-0.8).setY(0);
        Location target = loc.clone().add(back);
        if (target.getBlock().isPassable()) {
            player.teleport(target);
        }

        try {
            // Afterimage particles at previous location
            loc.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0, 0.8, 0), 12, 0.2, 0.4, 0.2, 0.02);
            loc.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(0, 1.0, 0), 15, 0.2, 0.3, 0.2, 0.05);
            loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.8f);
            player.sendActionBar(Component.text("§f⚡ ¡ESQUIVE INSTINTIVO! §7(Doctrina Egoísta)"));
        } catch (Exception ignored) {}
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
        cooldowns.remove(uuid);
    }
}
