package com.drakescraft.rankup.ability;

import com.drakescraft.rankup.DrakesRankupPlugin;
import net.kyori.adventure.text.Component;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CustomKitItemListener implements Listener {

    private final DrakesRankupPlugin plugin;
    private final Map<UUID, Long> medusaCooldown = new HashMap<>();

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

    // Senku (Dr. Stone): Dispositivo de Petrificacion "Medusa"
    @EventHandler(priority = EventPriority.HIGH)
    public void onMedusaPetrify(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!plugin.isWorldAllowed(player.getWorld())) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getItemMeta() == null || !item.getItemMeta().hasDisplayName()) return;

        String name = item.getItemMeta().getDisplayName();
        if (!name.contains("Medusa") && !name.contains("Petrificaci")) return;

        event.setCancelled(true);

        long now = System.currentTimeMillis();
        long ready = medusaCooldown.getOrDefault(player.getUniqueId(), 0L);
        if (now < ready) {
            long rem = Math.max(1, (ready - now) / 1000L);
            player.sendActionBar(Component.text("§c⏳ Medusa en recarga: §e" + rem + "s"));
            return;
        }

        // Raycast target entity up to 12 blocks
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        LivingEntity target = null;

        for (double d = 1.0; d <= 12.0; d += 0.5) {
            Location point = eye.clone().add(dir.clone().multiply(d));
            if (!point.getBlock().isPassable()) break;
            for (LivingEntity nearby : point.getWorld().getNearbyLivingEntities(point, 1.2)) {
                if (!nearby.equals(player)) {
                    target = nearby;
                    break;
                }
            }
            if (target != null) break;
        }

        if (target == null) {
            player.sendActionBar(Component.text("§cNo hay ninguna entidad objetivo en rango (12m)."));
            return;
        }

        medusaCooldown.put(player.getUniqueId(), now + 45000L); // 45s cooldown

        // Apply Petrification: Slowness 100, Weakness, Blindness
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 160, 99, false, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 160, 4, false, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0, false, true, true));

        Location tLoc = target.getLocation().add(0, 1.0, 0);
        try {
            tLoc.getWorld().playSound(tLoc, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.8f);
            tLoc.getWorld().playSound(tLoc, Sound.BLOCK_STONE_PLACE, 1.2f, 0.5f);
            tLoc.getWorld().spawnParticle(Particle.BLOCK, tLoc, 30, 0.4, 0.6, 0.4, Material.STONE.createBlockData());
            tLoc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, tLoc, 20, 0.5, 0.5, 0.5, 0.1);
        } catch (Exception ignored) {}

        player.sendTitle("§a§l¡RAYO PETRIFICADOR!", "§7Entidad convertida en piedra por 8s", 0, 30, 10);
        player.sendActionBar(Component.text("§a[Medusa] §f¡Has petrificado a §e" + target.getName() + "§f por 8 segundos!"));
        if (target instanceof Player victim) {
            victim.sendTitle("§7§l¡HAS SIDO PETRIFICADO!", "§cInmovilizado por la Medusa de Senku", 5, 40, 15);
            victim.sendMessage("§7[Medusa] §fTu cuerpo se ha endurecido en piedra pura. ¡Usa §eFluido Nital§f para liberarte!");
        }
    }

    // Senku (Dr. Stone): Liquido Despetrificador (Fluido Nital)
    @EventHandler(priority = EventPriority.HIGH)
    public void onRevivalFluidConsume(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!plugin.isWorldAllowed(player.getWorld())) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getItemMeta() == null || !item.getItemMeta().hasDisplayName()) return;

        String name = item.getItemMeta().getDisplayName();
        if (!name.contains("Despetrificador") && !name.contains("Nital")) return;

        event.setCancelled(true);

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        // Cure petrification & debuffs
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);

        // Buffs
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 300, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 300, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 300, 0));

        Location loc = player.getLocation();
        try {
            loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
            loc.getWorld().playSound(loc, Sound.ITEM_BOTTLE_EMPTY, 1.0f, 1.2f);
            loc.getWorld().spawnParticle(Particle.BLOCK, loc.clone().add(0, 1.0, 0), 25, 0.3, 0.5, 0.3, Material.STONE.createBlockData());
            loc.getWorld().spawnParticle(Particle.CRIT, loc.clone().add(0, 1.0, 0), 15, 0.3, 0.5, 0.3, 0.1);
        } catch (Exception ignored) {}

        player.sendTitle("§e§l¡DESPETRIFICACIÓN!", "§fCuerpo liberado con Fluido Nital", 0, 30, 10);
        player.sendMessage("§e[Senku] §f¡La coraza de piedra ha sido quebrada! Movilidad y reflejos restaurados con éxito.");
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
