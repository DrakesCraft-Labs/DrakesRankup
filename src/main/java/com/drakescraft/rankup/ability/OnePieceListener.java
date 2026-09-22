package com.drakescraft.rankup.ability;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.AbilityType;
import com.drakescraft.rankup.model.PlayerSettings;
import com.drakescraft.rankup.model.Rank;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Color;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class OnePieceListener implements Listener {

    private final DrakesRankupPlugin plugin;

    // Zoro Flying Slash Cooldown: UUID -> timestamp
    private final Map<UUID, Long> santoryuCooldown = new HashMap<>();

    // Active Gear Second: UUID -> expiration timestamp
    private final Map<UUID, Long> activeGearSecond = new HashMap<>();

    // Luffy Gear 3 (forma gigante): estado activo y cooldown
    private final Map<UUID, Long> activeGear3 = new HashMap<>();
    private final Map<UUID, Long> gear3Cooldown = new HashMap<>();

    public boolean isGear3Active(UUID uuid) {
        Long exp = activeGear3.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    private void resetScale(Player player) {
        try {
            var attr = player.getAttribute(Attribute.SCALE);
            if (attr != null) attr.setBaseValue(1.0);
        } catch (Throwable ignored) {}
    }

    public OnePieceListener(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isGearSecondActive(UUID uuid) {
        Long exp = activeGearSecond.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    // ==========================================
    // ESTILO TRES ESPADAS DE ZORO (SANTORYU)
    // ==========================================
    @EventHandler
    public void onSantoryuInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!plugin.isWorldAllowed(player.getWorld())) return;
        if (player.isSneaking()) return; // Shift + Right Click reserved for Vegetto beam

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR || !item.getType().name().endsWith("_SWORD")) return;

        UUID uuid = player.getUniqueId();
        Rank rank = plugin.getRankManager().getPlayerRank(uuid);
        int tier = (rank != null) ? rank.getTier() : 0;
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(uuid);
        String equipped = settings.getActiveTransformation();

        boolean hasSantoryu = (tier >= 24 || "SANTORYU".equalsIgnoreCase(equipped) || (rank != null && rank.getAbilityType() == AbilityType.SANTORYU_ZORO));
        if (!hasSantoryu) return;

        long now = System.currentTimeMillis();
        long ready = santoryuCooldown.getOrDefault(uuid, 0L);
        if (now < ready) {
            long rem = Math.max(1, (ready - now) / 1000L);
            player.sendActionBar(Component.text("§c⏳ Corte del Dragón en recarga: §e" + rem + "s"));
            return;
        }

        santoryuCooldown.put(uuid, now + 8000L); // 8s cooldown

        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();

        try {
            player.getWorld().playSound(eye, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.8f);
            player.getWorld().playSound(eye, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.0f, 1.9f);
            player.getWorld().playSound(eye, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.9f, 0.8f);
        } catch (Exception ignored) {}

        // Launch flying wind blade arc
        Set<LivingEntity> hitList = new HashSet<>();
        for (double d = 1.2; d <= 9.0; d += 0.6) {
            Location pt = eye.clone().add(dir.clone().multiply(d));
            if (!pt.getBlock().isPassable()) break;

            try {
                pt.getWorld().spawnParticle(Particle.SWEEP_ATTACK, pt, 2, 0.1, 0.1, 0.1, 0);
                pt.getWorld().spawnParticle(Particle.CRIT, pt, 3, 0.1, 0.1, 0.1, 0.05);
                pt.getWorld().spawnParticle(Particle.DUST, pt, 4, 0.25, 0.5, 0.25, 0, new Particle.DustOptions(Color.fromRGB(60, 200, 90), 1.3f)); // viento verde de Zoro
                pt.getWorld().spawnParticle(Particle.CLOUD, pt, 1, 0.1, 0.1, 0.1, 0.01);
            } catch (Exception ignored) {}

            for (LivingEntity entity : pt.getWorld().getNearbyLivingEntities(pt, 1.2)) {
                if (entity.equals(player)) continue;
                if (!hitList.contains(entity)) {
                    hitList.add(entity);
                    try {
                        entity.damage(8.5, player);
                        Vector knock = dir.clone().multiply(0.8).setY(0.25);
                        entity.setVelocity(knock);
                    } catch (Exception ignored) {}
                }
            }
        }

        player.sendActionBar(Component.text("§2⚔ ¡ESTILO TRES ESPADAS: CORTE DEL DRAGÓN VOLADOR! §7(Recarga: 8s)"));
    }

    // ==========================================
    // MAESTRÍA DE ESPADAS Y GATLING RED HAWK
    // ==========================================
    // ==========================================
    // LUFFY GEAR 3 (forma gigante con Attribute.SCALE)
    // ==========================================
    @EventHandler
    public void onGear3Activate(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR) return;
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        if (!plugin.isWorldAllowed(player.getWorld())) return;
        UUID uuid = player.getUniqueId();
        Rank rank = plugin.getRankManager().getPlayerRank(uuid);
        int tier = (rank != null) ? rank.getTier() : 0;
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(uuid);
        String equipped = settings.getActiveTransformation();
        boolean hasGomu = (tier >= 30 || "GOMU_GOMU".equalsIgnoreCase(equipped)
                || (rank != null && rank.getAbilityType() == AbilityType.DEVIL_FRUIT_GOMU));
        if (!hasGomu || isGear3Active(uuid)) return;
        long now = System.currentTimeMillis();
        Long cd = gear3Cooldown.get(uuid);
        if (cd != null && now < cd) {
            player.sendActionBar(Component.text("§c⏳ Gear 3 en recarga: §e" + ((cd - now) / 1000 + 1) + "s"));
            return;
        }
        gear3Cooldown.put(uuid, now + 35000L);
        activeGear3.put(uuid, now + 12000L); // 12s
        try {
            var attr = player.getAttribute(Attribute.SCALE);
            if (attr != null) attr.setBaseValue(1.8); // gigantificacion
        } catch (Throwable ignored) {}
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 240, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 240, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 240, 0)); // gigante = lento
        Location loc = player.getLocation();
        try {
            player.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0, 1, 0), 40, 0.6, 1.0, 0.6, 0.05);
            player.getWorld().spawnParticle(Particle.POOF, loc.clone().add(0, 1, 0), 20, 0.5, 0.8, 0.5, 0.02);
            player.getWorld().playSound(loc, Sound.ENTITY_PUFFER_FISH_BLOW_UP, 1.0f, 0.6f);
            player.getWorld().playSound(loc, Sound.ENTITY_IRON_GOLEM_REPAIR, 0.8f, 0.6f);
        } catch (Exception ignored) {}
        player.sendTitle("§c§lGEAR THIRD", "§e¡Hone Fuusen! Musculo de globo, golpes descomunales", 5, 40, 5);
        player.sendActionBar(Component.text("§c⚡ ¡GEAR 3 ACTIVO! Mas grande, +50% daño, mas lento (12s)"));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            activeGear3.remove(uuid);
            resetScale(player);
            if (player.isOnline()) {
                try {
                    player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, player.getLocation().add(0, 1, 0), 15, 0.4, 0.6, 0.4, 0.02);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PUFFER_FISH_BLOW_OUT, 1.0f, 0.7f);
                } catch (Exception ignored) {}
                player.sendMessage("§c[Rankup] El Gear 3 se desinfla: recuperas tu tamaño.");
            }
        }, 240L);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onCombatMelee(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!plugin.isWorldAllowed(attacker.getWorld())) return;
        UUID uuid = attacker.getUniqueId();
        Rank rank = plugin.getRankManager().getPlayerRank(uuid);
        int tier = (rank != null) ? rank.getTier() : 0;
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(uuid);
        String equipped = settings.getActiveTransformation();

        // 1. Zoro Sword Mastery (+25% damage with swords)
        boolean hasSantoryu = (tier >= 24 || "SANTORYU".equalsIgnoreCase(equipped) || (rank != null && rank.getAbilityType() == AbilityType.SANTORYU_ZORO));
        if (hasSantoryu) {
            ItemStack item = attacker.getInventory().getItemInMainHand();
            if (item.getType().name().endsWith("_SWORD")) {
                event.setDamage(event.getDamage() * 1.25); // +25% damage
                try {
                    Location loc = event.getEntity().getLocation().add(0, 1.0, 0);
                    attacker.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 1);
                } catch (Exception ignored) {}
            }
        }

        // Gear 3 activo: puño gigante +50% de daño
        if (isGear3Active(uuid)) {
            event.setDamage(event.getDamage() * 1.5);
        }

        // 2. Luffy Gear Second Red Hawk / Gatling burst
        boolean hasGomu = (tier >= 30 || "GOMU_GOMU".equalsIgnoreCase(equipped) || (rank != null && rank.getAbilityType() == AbilityType.DEVIL_FRUIT_GOMU));
        if (hasGomu) {
            if (event.getEntity() instanceof LivingEntity victim) {
                try {
                    victim.setFireTicks(60); // 3s fire
                    Location loc = victim.getLocation().add(0, 1.0, 0);
                    victim.getWorld().spawnParticle(Particle.FLAME, loc, 18, 0.3, 0.4, 0.3, 0.05);
                    victim.getWorld().spawnParticle(Particle.LAVA, loc, 3, 0.2, 0.3, 0.2, 0);
                    victim.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 5, 0.3, 0.4, 0.3, 0.01); // vapor del Gear Second
                    victim.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 0.9f, 1.5f);
                    victim.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.7f, 1.4f);
                } catch (Exception ignored) {}
            }
        }
    }

    // ==========================================
    // MALDICIÓN DEL OCÉANO (DEBILIDAD AL AGUA)
    // ==========================================
    @EventHandler
    public void onOceanCurse(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isWorldAllowed(player.getWorld())) return;
        UUID uuid = player.getUniqueId();
        Rank rank = plugin.getRankManager().getPlayerRank(uuid);
        int tier = (rank != null) ? rank.getTier() : 0;
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(uuid);
        String equipped = settings.getActiveTransformation();

        boolean hasFruit = ("GOMU_GOMU".equalsIgnoreCase(equipped) || (rank != null && rank.getAbilityType() == AbilityType.DEVIL_FRUIT_GOMU));
        if (!hasFruit) return;

        // Check if player is in water
        Material mat = player.getLocation().getBlock().getType();
        if (player.isInWater() || mat == Material.WATER) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 1, false, false));
            player.sendActionBar(Component.text("§c🌊 ¡MALDICIÓN DEL OCÉANO! §7El agua drena toda tu fuerza vital."));
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        activeGearSecond.remove(event.getPlayer().getUniqueId());
        activeGear3.remove(event.getPlayer().getUniqueId());
        resetScale(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        santoryuCooldown.remove(uuid);
        activeGearSecond.remove(uuid);
        activeGear3.remove(uuid);
        resetScale(event.getPlayer());
    }
}
