package com.drakescraft.rankup.ability;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.PlayerSettings;
import com.drakescraft.rankup.model.Rank;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class KineticPushListener implements Listener {

    private final DrakesRankupPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> fallProtection = new HashSet<>();

    public KineticPushListener(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        Rank rank = plugin.getRankManager().getPlayerRank(player.getUniqueId());
        if (rank == null || !rank.isHasKineticPush()) return;

        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(player.getUniqueId());
        if (!settings.isKineticPushEnabled()) return;

        long now = System.currentTimeMillis();
        long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long cdMillis = Math.max(1, rank.getPushCooldownSeconds()) * 1000L;
        if (now - lastUsed < cdMillis) {
            long remaining = Math.max(1, (cdMillis - (now - lastUsed)) / 1000L);
            event.setCancelled(true);
            player.setFlying(false);
            player.setAllowFlight(false);
            player.sendMessage("§c⏳ Empuje cinético en enfriamiento: §e" + remaining + "s");
            return;
        }

        event.setCancelled(true);
        player.setFlying(false);
        player.setAllowFlight(false);

        cooldowns.put(player.getUniqueId(), now);
        fallProtection.add(player.getUniqueId());

        Vector direction = player.getLocation().getDirection().normalize();
        double mult = rank.getPushMultiplier();
        if (mult <= 0.1) mult = 1.2;

        Vector velocity = direction.multiply(mult);
        velocity.setY(Math.min(0.55, Math.max(0.35, direction.getY() * 0.5 + 0.38)));
        player.setVelocity(velocity);

        Location loc = player.getLocation();
        int tier = rank.getTier();
        try {
            if (tier <= 20) {
                player.getWorld().spawnParticle(Particle.SMOKE, loc, 25, 0.3, 0.3, 0.3, 0.05);
            } else if (tier <= 30) {
                player.getWorld().spawnParticle(Particle.CLOUD, loc, 20, 0.4, 0.2, 0.4, 0.05);
                player.getWorld().playSound(loc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.4f);
            } else if (tier <= 40) {
                player.getWorld().spawnParticle(Particle.FLAME, loc, 25, 0.3, 0.3, 0.3, 0.08);
                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 15, 0.3, 0.3, 0.3, 0.1);
                player.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 1.8f);
            } else {
                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 35, 0.4, 0.4, 0.4, 0.15);
                player.getWorld().spawnParticle(Particle.PORTAL, loc, 25, 0.3, 0.3, 0.3, 0.2);
                player.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);
            }
        } catch (Exception ignored) {}
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        if (player.isOnGround()) {
            fallProtection.remove(player.getUniqueId());
            Rank rank = plugin.getRankManager().getPlayerRank(player.getUniqueId());
            if (rank != null && rank.isHasKineticPush()) {
                PlayerSettings settings = plugin.getRankManager().getPlayerSettings(player.getUniqueId());
                if (settings.isKineticPushEnabled()) {
                    long now = System.currentTimeMillis();
                    long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);
                    long cdMillis = Math.max(1, rank.getPushCooldownSeconds()) * 1000L;
                    if (now - lastUsed >= cdMillis) {
                        player.setAllowFlight(true);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && event.getEntity() instanceof Player player) {
            if (fallProtection.remove(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cooldowns.remove(event.getPlayer().getUniqueId());
        fallProtection.remove(event.getPlayer().getUniqueId());
    }
}
