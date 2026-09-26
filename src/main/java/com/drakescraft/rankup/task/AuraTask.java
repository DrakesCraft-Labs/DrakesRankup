package com.drakescraft.rankup.task;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.PlayerSettings;
import com.drakescraft.rankup.model.Rank;
import org.bukkit.Bukkit;
import org.bukkit.Color;
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
            if (!plugin.isWorldAllowed(player.getWorld())) {
                continue;
            }
            Location loc = player.getLocation();

            // Staff Angel Mode Double Halo Aura (takes precedence)
            if (plugin.getStaffManager() != null && plugin.getStaffManager().isAngel(player.getUniqueId())) {
                try {
                    double r = 0.55;
                    double x1 = r * Math.cos(angle);
                    double z1 = r * Math.sin(angle);
                    double x2 = r * Math.cos(angle + Math.PI);
                    double z2 = r * Math.sin(angle + Math.PI);

                    loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(x1, 2.25, z1), 1, 0, 0, 0, 0);
                    loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(x2, 2.25, z2), 1, 0, 0, 0, 0);
                    loc.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(0, 0.15, 0), 1, 0.1, 0.05, 0.1, 0.01);
                } catch (Exception ignored) {}
                continue;
            }

            Rank rank = plugin.getRankManager().getPlayerRank(player.getUniqueId());
            if (rank == null) continue;

            PlayerSettings settings = plugin.getRankManager().getPlayerSettings(player.getUniqueId());
            if (!settings.isParticlesEnabled() || settings.getParticleLevel() <= 0) continue;
            if (settings.getParticleLevel() == 1 && ((int) Math.round(angle / (Math.PI / 8)) % 2 != 0)) continue;

            int tier = rank.getTier();
            String activeTrans = settings.getActiveTransformation();
            int rebirths = settings.getRebirthCount();

            try {
                // Aura celestial de Rebirth permanente
                if (rebirths > 0) {
                    double rRad = 0.50;
                    double rx = rRad * Math.cos(angle * 1.5);
                    double rz = rRad * Math.sin(angle * 1.5);
                    loc.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(rx, 2.35, rz), 1, 0, 0, 0, 0.01);
                    if (rebirths >= 10) {
                        loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(-rx, 2.35, -rz), 1, 0, 0, 0, 0);
                    }
                    if (rebirths >= 25) {
                        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(rx * 0.7, 2.45, rz * 0.7), 1, 0, 0, 0, 0);
                    }
                }

                // Active Dragon Ball & Anime Transformation Auras
                if (plugin.getDragonBallListener() != null) {
                    if (plugin.getDragonBallListener().isKaiokenActive(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 0.8, 0), 6, 0.35, 0.5, 0.35, 0, new Particle.DustOptions(Color.fromRGB(255, 20, 30), 1.4f));
                        loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 0.3, 0), 4, 0.2, 0.3, 0.2, 0.03);
                        continue;
                    } else if (plugin.getDragonBallListener().isMuiActive(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 1.0, 0), 4, 0.3, 0.5, 0.3, 0.05);
                        loc.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(0, 0.2, 0), 3, 0.2, 0.1, 0.2, 0.02);
                        continue;
                    } else if (plugin.getDragonBallListener().isUltraEgoActive(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.WITCH, loc.clone().add(0, 0.8, 0), 5, 0.3, 0.5, 0.3, 0.05);
                        loc.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc.clone().add(0, 0.2, 0), 3, 0.2, 0.1, 0.2, 0.03);
                        continue;
                    } else if (plugin.getDragonBallListener().isGohanBeastActive(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.CRIMSON_SPORE, loc.clone().add(0, 0.8, 0), 5, 0.25, 0.4, 0.25, 0.05);
                        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0, 1.1, 0), 3, 0.3, 0.4, 0.3, 0.05);
                        continue;
                    } else if (plugin.getDragonBallListener().isBrolyActive(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0, 0.5, 0), 6, 0.35, 0.5, 0.35, 0.02);
                        continue;
                    } else if (plugin.getDragonBallListener().isSSJBlueActive(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 0.3, 0), 4, 0.25, 0.4, 0.25, 0.03);
                        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0, 0.9, 0), 3, 0.3, 0.4, 0.3, 0.08);
                        continue;
                    } else if (plugin.getDragonBallListener().isSSJGodActive(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 0.4, 0), 5, 0.25, 0.4, 0.25, 0.03);
                        continue;
                    } else if (plugin.getDragonBallListener().isSSJ2Active(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 0.8, 0), 6, 0.35, 0.5, 0.35, 0, new Particle.DustOptions(Color.fromRGB(255, 230, 20), 1.5f));
                        loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 0.4, 0), 4, 0.25, 0.4, 0.25, 0.03);
                        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0, 0.9, 0), 5, 0.35, 0.5, 0.35, 0.08);
                        continue;
                    }
                }

                // One Piece: Gear 5, Gear 4, Gear 3 y Frutas del Diablo
                if (plugin.getOnePieceListener() != null) {
                    if (plugin.getOnePieceListener().isGear5Active(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0, 1.8, 0), 5, 0.35, 0.15, 0.35, 0.02);
                        loc.getWorld().spawnParticle(Particle.WAX_OFF, loc.clone().add(0, 1.2, 0), 3, 0.3, 0.3, 0.3, 0.05);
                        loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 0.5, 0), 2, 0.2, 0.3, 0.2, 0.01);
                        continue;
                    } else if (plugin.getOnePieceListener().isGear4Active(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc.clone().add(0, 0.7, 0), 4, 0.3, 0.4, 0.3, 0.02);
                        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 0.9, 0), 5, 0.35, 0.4, 0.35, 0, new Particle.DustOptions(Color.fromRGB(180, 20, 20), 1.3f));
                        continue;
                    } else if (plugin.getOnePieceListener().isGear3Active(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.ITEM_SLIME, loc.clone().add(0, 0.6, 0), 4, 0.6, 0.4, 0.6, 0.05);
                        loc.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0, 0.4, 0), 3, 0.5, 0.2, 0.5, 0.02);
                        continue;
                    } else if (plugin.getOnePieceListener().isMeraActive(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 0.6, 0), 6, 0.3, 0.4, 0.3, 0.03);
                        loc.getWorld().spawnParticle(Particle.LAVA, loc.clone().add(0, 0.2, 0), 2, 0.2, 0.1, 0.2, 0);
                        continue;
                    } else if (plugin.getOnePieceListener().isOpeActive(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 0.8, 0), 3, 0.25, 0.3, 0.25, 0.02);
                        continue;
                    } else if (plugin.getOnePieceListener().isPikaActive(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(0, 0.8, 0), 4, 0.25, 0.4, 0.25, 0.03);
                        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0, 1.0, 0), 3, 0.25, 0.4, 0.25, 0.05);
                        continue;
                    } else if (plugin.getOnePieceListener().isGuraActive(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.CRIT, loc.clone().add(0, 0.6, 0), 5, 0.3, 0.4, 0.3, 0.04);
                        continue;
                    }
                }

                // Equipped Static Aura from Settings
                if ("GOMU_GOMU".equalsIgnoreCase(activeTrans)) {
                    loc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc.clone().add(0, 0.3, 0), 2, 0.15, 0.3, 0.15, 0.01);
                } else if ("SANTORYU".equalsIgnoreCase(activeTrans)) {
                    loc.getWorld().spawnParticle(Particle.CRIT, loc.clone().add(0, 0.2, 0), 2, 0.2, 0.2, 0.2, 0.02);
                } else if ("KAIOKEN".equalsIgnoreCase(activeTrans)) {
                    loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 0.3, 0), 2, 0.2, 0.3, 0.2, 0, new Particle.DustOptions(Color.fromRGB(240, 20, 30), 1.2f));
                }

                // Auras pasivas por rango continuo (Tiers 1 a 100)
                spawnPassiveAura(loc, tier);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Aura pasiva continua para los 100 tiers de DrakesRankup.
     */
    private void spawnPassiveAura(Location loc, int tier) {
        if (tier < 1) return;
        Location base = loc.clone().add(0, 0.2, 0);
        Location head = loc.clone().add(0, 1.0, 0);
        try {
            if (tier <= 10) {                 // División I: Ciencia & Nen
                loc.getWorld().spawnParticle(Particle.CRIT, base, 1 + tier / 5, 0.2, 0.2, 0.2, 0.01);
            } else if (tier <= 20) {          // División II: Shinobi & Hechicería
                int n = 1 + (tier - 10) / 4;
                loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, base, n, 0.2, 0.25, 0.2, 0.01);
                if (tier >= 16) loc.getWorld().spawnParticle(Particle.END_ROD, head, 1, 0.2, 0.4, 0.2, 0.01);
            } else if (tier <= 30) {          // División III: Piratas & Segadores
                int n = 1 + (tier - 20) / 4;
                loc.getWorld().spawnParticle(Particle.CLOUD, base, n, 0.2, 0.15, 0.2, 0.01);
                if (tier >= 26) loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, head, 1, 0.3, 0.4, 0.3, 0.03);
            } else if (tier <= 40) {          // División IV: Guerreros Z & Monarca
                int n = 2 + (tier - 30) / 3;
                loc.getWorld().spawnParticle(Particle.FLAME, base, n, 0.25, 0.3, 0.25, 0.02);
                if (tier >= 33) loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, head, 1 + (tier - 33) / 3, 0.3, 0.5, 0.3, 0.05);
                if (tier >= 37) loc.getWorld().spawnParticle(Particle.SQUID_INK, base, 1, 0.2, 0.1, 0.2, 0.01);
            } else if (tier <= 50) {          // División V: Trascendencia Suprema
                int n = 2 + (tier - 40) / 2;
                loc.getWorld().spawnParticle(Particle.WITCH, base, n, 0.25, 0.4, 0.25, 0.02);
                if (tier >= 44) loc.getWorld().spawnParticle(Particle.FIREWORK, base, 1 + (tier - 44) / 2, 0.3, 0.5, 0.3, 0.02);
                double x = 0.45 * Math.cos(angle);
                double z = 0.45 * Math.sin(angle);
                loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(x, 2.15, z), 1, 0, 0, 0, 0);
            } else if (tier <= 60) {          // División VI: Maldiciones Supremas
                int n = 2 + (tier - 50) / 3;
                loc.getWorld().spawnParticle(Particle.SQUID_INK, base, n, 0.3, 0.3, 0.3, 0.02);
                loc.getWorld().spawnParticle(Particle.DRAGON_BREATH, head, 1, 0.2, 0.3, 0.2, 0.02);
            } else if (tier <= 70) {          // División VII: Shinigami Reiatsu
                int n = 2 + (tier - 60) / 3;
                loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, base, n, 0.3, 0.3, 0.3, 0.03);
                loc.getWorld().spawnParticle(Particle.END_ROD, head, 1 + (tier - 60) / 4, 0.2, 0.4, 0.2, 0.01);
            } else if (tier <= 80) {          // División VIII: Monarcas & Demonios
                int n = 3 + (tier - 70) / 3;
                loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, base, n, 0.35, 0.4, 0.35, 0.02);
                loc.getWorld().spawnParticle(Particle.PORTAL, head, 2, 0.2, 0.3, 0.2, 0.1);
            } else if (tier <= 90) {          // División IX: Leyendas Cósmicas
                int n = 3 + (tier - 80) / 2;
                loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, base, n, 0.4, 0.5, 0.4, 0.05);
                loc.getWorld().spawnParticle(Particle.FLAME, head, 2, 0.3, 0.3, 0.3, 0.02);
            } else {                          // División X: Dioses Multiversales (91-100)
                int n = 4 + (tier - 90) / 2;
                loc.getWorld().spawnParticle(Particle.FLASH, base, 1);
                loc.getWorld().spawnParticle(Particle.END_ROD, base, n, 0.4, 0.6, 0.4, 0.05);
                double x1 = 0.50 * Math.cos(angle);
                double z1 = 0.50 * Math.sin(angle);
                double x2 = 0.50 * Math.cos(angle + Math.PI);
                double z2 = 0.50 * Math.sin(angle + Math.PI);
                loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(x1, 2.25, z1), 1, 0, 0, 0, 0);
                loc.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(x2, 2.25, z2), 1, 0, 0, 0, 0);
            }
        } catch (Exception ignored) {}
    }
}
