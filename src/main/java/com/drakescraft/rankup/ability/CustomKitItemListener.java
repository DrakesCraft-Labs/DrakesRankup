package com.drakescraft.rankup.ability;

import com.drakescraft.rankup.DrakesRankupPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

public class CustomKitItemListener implements Listener {

    private final DrakesRankupPlugin plugin;

    public CustomKitItemListener(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
    }

    // Senku (Dr. Stone): fx de transmutacion cientifica al minar con el Pico Transmutador.
    @EventHandler(ignoreCancelled = true)
    public void onSenkuTransmute(BlockBreakEvent event) {
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (tool.getType() != Material.DIAMOND_PICKAXE || !tool.hasItemMeta()) return;
        if (!tool.getItemMeta().hasDisplayName()) return;
        String name = tool.getItemMeta().getDisplayName();
        if (name == null || !name.contains("Senku")) return;
        try {
            Location loc = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
            loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 8, 0.3, 0.3, 0.3, 0.05);
            loc.getWorld().spawnParticle(Particle.GLOW, loc, 5, 0.2, 0.2, 0.2, 0.02);
            loc.getWorld().spawnParticle(Particle.END_ROD, loc, 3, 0.1, 0.1, 0.1, 0.02);
            loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.6f);
        } catch (Exception ignored) {}
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSenzuBeanConsume(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!plugin.isWorldAllowed(player.getWorld())) return;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getItemMeta() == null || !item.getItemMeta().hasDisplayName()) return;
        String name = item.getItemMeta().getDisplayName();
        if (!name.contains("Semilla del Ermitaño") && !name.contains("Senzu Bean")) return;

        event.setCancelled(true);

        // Consume 1 bean
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        // 100% Health
        double maxHp = 20.0;
        try {
            if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
                maxHp = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            }
        } catch (Throwable ignored) {}
        player.setHealth(maxHp);

        // 100% Food & Saturation
        player.setFoodLevel(20);
        player.setSaturation(20.0f);

        // Cure negative potion effects
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.POISON);
        player.removePotionEffect(PotionEffectType.WITHER);
        player.removePotionEffect(PotionEffectType.HUNGER);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        player.removePotionEffect(PotionEffectType.DARKNESS);

        Location loc = player.getLocation();
        try {
            loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_BURP, 1.0f, 1.2f);
            loc.getWorld().playSound(loc, Sound.ITEM_TOTEM_USE, 0.6f, 1.6f);
            loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0, 1.0, 0), 25, 0.3, 0.5, 0.3, 0.1);
            loc.getWorld().spawnParticle(Particle.HEART, loc.clone().add(0, 1.5, 0), 8, 0.4, 0.4, 0.4, 0.1);
        } catch (Exception ignored) {}

        player.sendTitle("§a§l¡SEMILLA DEL ERMITAÑO!", "§eSalud y energía recuperadas al 100%", 5, 35, 10);
        player.sendMessage("§a[Karin] §f¡Has consumido una §eSemilla del Ermitaño§f! Tu cuerpo ha sanado instantáneamente de toda fatiga.");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onCustomWeaponCombat(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!plugin.isWorldAllowed(attacker.getWorld())) return;
        ItemStack item = attacker.getInventory().getItemInMainHand();
        if (item.getItemMeta() == null || !item.getItemMeta().hasDisplayName()) return;

        String name = item.getItemMeta().getDisplayName();

        // 1. Katana Maldita Sukuna (15% chance to Black Flash)
        if (name.contains("Katana Maldita")) {
            if (Math.random() < 0.15) {
                event.setDamage(event.getDamage() * 1.5);
                try {
                    Location loc = event.getEntity().getLocation().add(0, 1.0, 0);
                    attacker.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 15, 0.2, 0.3, 0.2, 0.05);
                    attacker.getWorld().spawnParticle(Particle.CRIT, loc, 10, 0.2, 0.2, 0.2, 0.1);
                    attacker.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 1.9f);
                } catch (Exception ignored) {}
            }
        }

        // 2. Daga del Monarca (Lifesteal: 1 heart)
        if (name.contains("Daga del Monarca")) {
            double maxHp = 20.0;
            try {
                if (attacker.getAttribute(Attribute.MAX_HEALTH) != null) {
                    maxHp = attacker.getAttribute(Attribute.MAX_HEALTH).getValue();
                }
            } catch (Throwable ignored) {}
            attacker.setHealth(Math.min(maxHp, attacker.getHealth() + 2.0));
            try {
                Location loc = attacker.getLocation().add(0, 1.0, 0);
                attacker.getWorld().spawnParticle(Particle.SQUID_INK, loc, 5, 0.2, 0.3, 0.2, 0.02);
            } catch (Exception ignored) {}
        }
    }
}
