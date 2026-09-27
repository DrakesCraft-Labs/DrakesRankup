package com.drakescraft.rankup.ability;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.AbilityType;
import com.drakescraft.rankup.model.PlayerSettings;
import com.drakescraft.rankup.model.Rank;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class RankAbilityListener implements Listener {

    private final DrakesRankupPlugin plugin;
    private final Random random = new Random();
    private final Map<UUID, Long> zenkaiCooldowns = new HashMap<>();

    public RankAbilityListener(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!plugin.isWorldAllowed(event.getEntity().getWorld())) return;

        if (event.getEntity() instanceof Player player) {
            Rank rank = plugin.getRankManager().getPlayerRank(player.getUniqueId());
            if (rank == null) return;
            PlayerSettings settings = plugin.getRankManager().getPlayerSettings(player.getUniqueId());
            if (!settings.isAbilitiesEnabled()) return;

            AbilityType ability = rank.getAbilityType();

            // Pasivas defensivas por daño ambiental
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                if (ability == AbilityType.FEATHER_STEP || rank.getTier() >= 5) {
                    event.setDamage(event.getDamage() * 0.5);
                }
            }

            if (event.getCause() == EntityDamageEvent.DamageCause.FIRE 
                    || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK 
                    || event.getCause() == EntityDamageEvent.DamageCause.LAVA) {
                if (ability == AbilityType.HAKAI_AURA || rank.getTier() >= 47 || ability == AbilityType.ZANKA_NO_TACHI) {
                    event.setCancelled(true);
                    player.setFireTicks(0);
                }
            }

            if (rank.getTier() >= 15) {
                event.setDamage(event.getDamage() * 0.90);
            }

            if (ability == AbilityType.KYOKA_SUIGETSU || rank.getTier() >= 48) {
                if (random.nextDouble() < 0.20) {
                    event.setCancelled(true);
                    try {
                        Location loc = player.getLocation();
                        player.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc.add(0, 1.0, 0), 15, 0.3, 0.5, 0.3, 0.02);
                        player.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
                        player.sendActionBar(Component.text("§b⚡ ¡ILUSIÓN KYOKA SUIGETSU! §7Ataque enemigo desviado."));
                    } catch (Exception ignored) {}
                    return;
                }
            }

            if (ability == AbilityType.ULTRA_INSTINCT || rank.getTier() >= 49) {
                if (random.nextDouble() < 0.15) {
                    event.setCancelled(true);
                    try {
                        Location dloc = player.getLocation().add(0, 0.9, 0);
                        player.getWorld().spawnParticle(Particle.CLOUD, dloc, 14, 0.3, 0.3, 0.3, 0.03);
                        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, dloc, 6, 0.25, 0.4, 0.25, 0.02);
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.9f);
                        player.sendActionBar(Component.text("§f⚡ §7Esquive instintivo"));
                    } catch (Exception ignored) {}
                    return;
                }
            }

            if (rank.getTier() >= 25) {
                if (player.getWorld().isDayTime() && !player.getWorld().hasStorm()) {
                    event.setDamage(event.getDamage() * 0.85);
                }
            }

            if (ability == AbilityType.WATER_GRACE || rank.getTier() >= 10) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 120, 0, false, false, false));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!plugin.isWorldAllowed(event.getEntity().getWorld())) return;

        // Atacante es un Jugador
        if (event.getDamager() instanceof Player player) {
            // Protección contra bucles infinitos y activación por auras/daño indirecto
            if (event.getEntity().hasMetadata("DRAKES_ABILITY_DAMAGE")) return;
            if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK &&
                event.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
                return;
            }

            Rank rank = plugin.getRankManager().getPlayerRank(player.getUniqueId());
            if (rank == null) return;
            PlayerSettings settings = plugin.getRankManager().getPlayerSettings(player.getUniqueId());
            if (!settings.isAbilitiesEnabled()) return;

            // Bonificador permanente por Rebirth: +3% por nivel
            int rebirths = plugin.getRankManager().getRebirthCount(player.getUniqueId());
            if (rebirths > 0) {
                event.setDamage(event.getDamage() * (1.0 + rebirths * 0.03));
            }

            AbilityType ability = rank.getAbilityType();
            Location hitLoc = event.getEntity().getLocation();

            if (ability == AbilityType.BLACK_FLASH || rank.getTier() >= 20) {
                if (random.nextDouble() < 0.15) {
                    event.setDamage(event.getDamage() * 1.5);
                    try {
                        Location loc = hitLoc.clone().add(0, 1.0, 0);
                        player.getWorld().spawnParticle(Particle.SQUID_INK, loc, 20, 0.3, 0.3, 0.3, 0.08);
                        player.getWorld().spawnParticle(Particle.CRIT, loc, 15, 0.2, 0.2, 0.2, 0.1);
                        player.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 1.8f);
                    } catch (Exception ignored) {}
                }
            }

            if (ability == AbilityType.CURSED_FLAME || rank.getTier() >= 43) {
                event.getEntity().setFireTicks(80);
                try {
                    event.getEntity().getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, hitLoc.clone().add(0, 1.0, 0), 15, 0.2, 0.4, 0.2, 0.05);
                } catch (Exception ignored) {}
            }

            // Habilidades Tiers 51 - 100
            if (ability == AbilityType.DISMANTLE_CLEAVE || rank.getTier() >= 58) {
                if (random.nextDouble() < 0.20) {
                    event.setDamage(event.getDamage() * 1.4);
                    try {
                        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, hitLoc.clone().add(0, 1.0, 0), 4, 0.3, 0.3, 0.3, 0);
                        player.getWorld().playSound(hitLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.5f);
                    } catch (Exception ignored) {}
                    if (plugin.getProtectionGate() != null) {
                        plugin.getProtectionGate().applyTerrainExplosion(player, hitLoc, 2.0f, false);
                    }
                }
            }

            if (ability == AbilityType.HOLLOW_PURPLE || rank.getTier() >= 59) {
                if (random.nextDouble() < 0.15) {
                    event.setDamage(event.getDamage() * 1.6);
                    try {
                        player.getWorld().spawnParticle(Particle.DRAGON_BREATH, hitLoc.clone().add(0, 1.0, 0), 25, 0.5, 0.5, 0.5, 0.05);
                        player.getWorld().spawnParticle(Particle.FLASH, hitLoc.clone().add(0, 1.0, 0), 1);
                        player.getWorld().playSound(hitLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.6f);
                    } catch (Exception ignored) {}
                    if (plugin.getProtectionGate() != null) {
                        plugin.getProtectionGate().applyTerrainExplosion(player, hitLoc, 3.2f, false);
                    }
                }
            }

            if (ability == AbilityType.GURA_GURA_TREMOR || rank.getTier() >= 89) {
                if (random.nextDouble() < 0.18) {
                    event.setDamage(event.getDamage() * 1.5);
                    for (Entity nearby : hitLoc.getWorld().getNearbyEntities(hitLoc, 5.0, 3.0, 5.0)) {
                        if (!nearby.isValid() || !nearby.getLocation().isChunkLoaded()) continue;
                        if (nearby instanceof LivingEntity target && !nearby.equals(player)) {
                            try {
                                target.setMetadata("DRAKES_ABILITY_DAMAGE", new FixedMetadataValue(plugin, true));
                                target.damage(8.0, player);
                            } finally {
                                target.removeMetadata("DRAKES_ABILITY_DAMAGE", plugin);
                            }
                            target.setVelocity(target.getLocation().toVector().subtract(hitLoc.toVector()).normalize().multiply(1.3).setY(0.5));
                        }
                    }
                    try {
                        player.getWorld().playSound(hitLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.2f, 0.5f);
                        player.getWorld().spawnParticle(Particle.SONIC_BOOM, hitLoc.clone().add(0, 1.0, 0), 1);
                    } catch (Exception ignored) {}
                    if (plugin.getProtectionGate() != null) {
                        plugin.getProtectionGate().applyTerrainExplosion(player, hitLoc, 2.8f, false);
                    }
                }
            }

            if (ability == AbilityType.CHAINSAW_DEVIL || rank.getTier() >= 71) {
                try {
                    player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 2.0));
                    player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, player.getLocation().add(0, 1.0, 0), 3, 0.2, 0.2, 0.2, 0.02);
                    player.getWorld().playSound(hitLoc, Sound.ENTITY_MINECART_RIDING, 0.7f, 1.9f);
                } catch (Exception ignored) {}
            }

            if (ability == AbilityType.BOOGIE_WOOGIE) {
                if (random.nextDouble() < 0.15) {
                    Location pLoc = player.getLocation().clone();
                    Location tLoc = event.getEntity().getLocation().clone();
                    player.teleport(tLoc);
                    event.getEntity().teleport(pLoc);
                    try {
                        player.getWorld().playSound(pLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.8f);
                        player.getWorld().spawnParticle(Particle.PORTAL, pLoc.add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.1);
                    } catch (Exception ignored) {}
                }
            }
        }

        // Víctima es un Jugador
        if (event.getEntity() instanceof Player player) {
            Rank rank = plugin.getRankManager().getPlayerRank(player.getUniqueId());
            if (rank == null) return;
            PlayerSettings settings = plugin.getRankManager().getPlayerSettings(player.getUniqueId());
            if (!settings.isAbilitiesEnabled()) return;

            AbilityType ability = rank.getAbilityType();

            if (ability == AbilityType.HAKI_CONQUEROR || rank.getTier() >= 30) {
                Location loc = player.getLocation();
                for (Entity nearby : player.getNearbyEntities(6.0, 3.0, 6.0)) {
                    if (nearby instanceof Monster monster) {
                        monster.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1));
                        monster.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 1));
                    }
                }
                try {
                    player.getWorld().spawnParticle(Particle.SONIC_BOOM, loc.add(0, 1.0, 0), 1);
                    player.getWorld().playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.4f, 1.5f);
                } catch (Exception ignored) {}
            }

            if (ability == AbilityType.KAMI_DIVINE || rank.getTier() >= 50) {
                if (event.getDamager() instanceof Monster monster) {
                    if (random.nextDouble() < 0.25) {
                        monster.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2));
                        monster.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 2));
                        try {
                            player.getWorld().spawnParticle(Particle.FLASH, monster.getLocation().add(0, 1.0, 0), 1);
                            player.getWorld().playSound(monster.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 2.0f);
                        } catch (Exception ignored) {}
                    }
                }
            }

            if (ability == AbilityType.ZENKAI_BOOST || (rank.getTier() >= 31 && rank.getTier() <= 36)) {
                double remainingHealth = player.getHealth() - event.getFinalDamage();
                if (remainingHealth > 0 && remainingHealth <= 6.0) {
                    long now = System.currentTimeMillis();
                    long lastZenkai = zenkaiCooldowns.getOrDefault(player.getUniqueId(), 0L);
                    if (now - lastZenkai > 90_000L) {
                        zenkaiCooldowns.put(player.getUniqueId(), now);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 160, 1));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 160, 0));
                        try {
                            player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1.0, 0), 30, 0.4, 0.5, 0.4, 0.1);
                            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.8f);
                        } catch (Exception ignored) {}
                        player.sendMessage("§c§l¡ZENKAI BOOST ACTIVADO! §eEl poder Saiyajin despierta ante el peligro.");
                    }
                }
            }

            if (ability == AbilityType.HAKAI_AURA || rank.getTier() >= 47) {
                if (event.getDamager() instanceof LivingEntity damager) {
                    try {
                        damager.setMetadata("DRAKES_ABILITY_DAMAGE", new FixedMetadataValue(plugin, true));
                        damager.damage(3.0, player);
                    } finally {
                        damager.removeMetadata("DRAKES_ABILITY_DAMAGE", plugin);
                    }
                    try {
                        damager.getWorld().spawnParticle(Particle.WITCH, damager.getLocation().add(0, 1.0, 0), 15, 0.2, 0.3, 0.2, 0.05);
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        if (!plugin.isWorldAllowed(killer.getWorld())) return;
        Rank rank = plugin.getRankManager().getPlayerRank(killer.getUniqueId());
        if (rank == null) return;

        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(killer.getUniqueId());
        if (!settings.isAbilitiesEnabled()) return;

        if (rank.getAbilityType() == AbilityType.SHADOW_EXTRACTION || rank.getAbilityType() == AbilityType.SHADOW_MONARCH_ARMY || (rank.getTier() >= 39 && rank.getTier() <= 40) || rank.getTier() >= 80) {
            double chance = rank.getTier() >= 80 ? 0.30 : 0.12;
            if (event.getEntity() instanceof Monster && random.nextDouble() < chance) {
                Location loc = event.getEntity().getLocation();
                try {
                    Wolf shadowWolf = (Wolf) loc.getWorld().spawnEntity(loc, EntityType.WOLF);
                    shadowWolf.setOwner(killer);
                    shadowWolf.setCustomName("§8§l[Sombra de " + killer.getName() + "]");
                    shadowWolf.setCustomNameVisible(true);
                    shadowWolf.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 500, 1));
                    shadowWolf.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 500, 1));
                    shadowWolf.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 500, 0));

                    loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc.add(0, 0.5, 0), 25, 0.3, 0.3, 0.3, 0.05);
                    killer.playSound(loc, Sound.ENTITY_WITHER_SHOOT, 0.6f, 0.5f);

                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (shadowWolf.isValid()) {
                            shadowWolf.getWorld().spawnParticle(Particle.SMOKE, shadowWolf.getLocation(), 15, 0.2, 0.2, 0.2, 0.05);
                            shadowWolf.remove();
                        }
                    }, 500L);
                } catch (Exception ignored) {}
            }
        }
    }
}
