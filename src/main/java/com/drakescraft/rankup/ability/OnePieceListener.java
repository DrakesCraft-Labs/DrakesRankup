package com.drakescraft.rankup.ability;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.AbilityType;
import com.drakescraft.rankup.model.PlayerSettings;
import com.drakescraft.rankup.model.Rank;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
            player.getWorld().playSound(eye, Sound.ITEM_ARMOR_EQUIP_IRON, 1.0f, 1.5f);
        } catch (Exception ignored) {}

        // Launch flying wind blade arc
        Set<LivingEntity> hitList = new HashSet<>();
        for (double d = 1.2; d <= 9.0; d += 0.6) {
            Location pt = eye.clone().add(dir.clone().multiply(d));
            if (!pt.getBlock().isPassable()) break;

            try {
                pt.getWorld().spawnParticle(Particle.SWEEP_ATTACK, pt, 2, 0.1, 0.1, 0.1, 0);
                pt.getWorld().spawnParticle(Particle.CRIT, pt, 3, 0.1, 0.1, 0.1, 0.05);
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

        // 2. Luffy Gear Second Red Hawk / Gatling burst
        boolean hasGomu = (tier >= 30 || "GOMU_GOMU".equalsIgnoreCase(equipped) || (rank != null && rank.getAbilityType() == AbilityType.DEVIL_FRUIT_GOMU));
        if (hasGomu) {
            if (event.getEntity() instanceof LivingEntity victim) {
                try {
                    victim.setFireTicks(60); // 3s fire
                    Location loc = victim.getLocation().add(0, 1.0, 0);
                    victim.getWorld().spawnParticle(Particle.FLAME, loc, 15, 0.3, 0.4, 0.3, 0.05);
                    victim.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.4f);
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
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        santoryuCooldown.remove(uuid);
        activeGearSecond.remove(uuid);
    }
}
