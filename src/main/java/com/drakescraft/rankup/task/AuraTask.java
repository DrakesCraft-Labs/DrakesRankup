package com.drakescraft.rankup.task;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.PlayerSettings;
import com.drakescraft.rankup.model.Rank;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class AuraTask extends BukkitRunnable {

    private final DrakesRankupPlugin plugin;
    private double angle = 0;

    public AuraTask(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        angle += Math.PI / 8;
        if (angle >= Math.PI * 2) angle = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            Rank rank = plugin.getRankManager().getPlayerRank(player.getUniqueId());
            if (rank == null) continue;

            PlayerSettings settings = plugin.getRankManager().getPlayerSettings(player.getUniqueId());
            if (!settings.isParticlesEnabled()) continue;

            int tier = rank.getTier();
            Location loc = player.getLocation();

            try {
                if (tier >= 32 && tier <= 36) {
                    loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 0.2, 0), 3, 0.25, 0.3, 0.25, 0.02);
                    if (tier >= 33) {
                        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0, 1.0, 0), 2, 0.3, 0.5, 0.3, 0.05);
                    }
                } else if (tier >= 37 && tier <= 40) {
                    loc.getWorld().spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 0.1, 0), 2, 0.2, 0.1, 0.2, 0.01);
                } else if (tier == 46) {
                    loc.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(0, 0.3, 0), 3, 0.3, 0.5, 0.3, 0.02);
                } else if (tier == 47) {
                    loc.getWorld().spawnParticle(Particle.WITCH, loc.clone().add(0, 0.2, 0), 3, 0.25, 0.4, 0.25, 0.02);
                } else if (tier >= 50) {
                    double x = 0.4 * Math.cos(angle);
                    double z = 0.4 * Math.sin(angle);
                    loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(x, 2.15, z), 1, 0, 0, 0, 0);
                } else if (tier == 19) {
                    loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 1.0, 0), 1, 0.2, 0.4, 0.2, 0.01);
                } else if (tier == 20) {
                    loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 0.2, 0), 2, 0.2, 0.2, 0.2, 0.01);
                } else if (tier == 30) {
                    loc.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0, 0.1, 0), 2, 0.2, 0.1, 0.2, 0.01);
                }
            } catch (Exception ignored) {}
        }
    }
}
