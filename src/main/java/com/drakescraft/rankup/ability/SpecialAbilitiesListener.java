package com.drakescraft.rankup.ability;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.AbilityType;
import com.drakescraft.rankup.model.PlayerSettings;
import com.drakescraft.rankup.model.Rank;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Ghast;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Gestor de Habilidades Especiales Avanzadas:
 * 1. Vórtice Magnético (Imán Atractor de 200 bloques).
 * 2. Filos Danzantes / Kill Aura de Armas (Espadas y hachas que golpean solas).
 */
public class SpecialAbilitiesListener implements Listener {

    private final DrakesRankupPlugin plugin;

    // Cooldown de Imán Magnético: UUID -> timestamp
    private final Map<UUID, Long> magnetCooldown = new HashMap<>();

    // Estado activo de Filos Danzantes (Aura Kill)
    private final Map<UUID, Long> activeDancingBlades = new HashMap<>();
    private final Map<UUID, Long> bladesCooldown = new HashMap<>();

    private double bladeOrbitAngle = 0;

    public SpecialAbilitiesListener(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
        startDancingBladesTask();
    }

    public boolean isDancingBladesActive(UUID uuid) {
        Long exp = activeDancingBlades.get(uuid);
        if (exp != null && System.currentTimeMillis() < exp) return true;
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(uuid);
        return "DANCING_BLADES".equalsIgnoreCase(settings.getActiveTransformation());
    }

    // ==========================================
    // 1. IMÁN ATRACTOR MAGNÉTICO (200 BLOQUES)
    // ==========================================
    public void triggerMagnetVortex(Player player) {
        if (!plugin.isWorldAllowed(player.getWorld())) return;
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long ready = magnetCooldown.getOrDefault(uuid, 0L);
        if (now < ready) {
            long rem = Math.max(1, (ready - now) / 1000L);
            player.sendActionBar(Component.text("§c⏳ Imán Magnético en recarga: §e" + rem + "s"));
            return;
        }

        int rebirths = plugin.getRankManager().getRebirthCount(uuid);
        double cdr = 1.0 - Math.min(0.5, rebirths * 0.01);
        magnetCooldown.put(uuid, now + (long)(8000L * cdr)); // 8s cooldown

        Location pLoc = player.getLocation();
        double radius = 200.0;
        int count = 0;

        try {
            pLoc.getWorld().playSound(pLoc, Sound.BLOCK_BEACON_POWER_SELECT, 1.2f, 1.8f);
            pLoc.getWorld().playSound(pLoc, Sound.BLOCK_CONDUIT_ACTIVATE, 1.2f, 1.4f);
            pLoc.getWorld().playSound(pLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.9f);
            pLoc.getWorld().spawnParticle(Particle.SONIC_BOOM, pLoc.clone().add(0, 1.0, 0), 1);
            pLoc.getWorld().spawnParticle(Particle.FLASH, pLoc.clone().add(0, 1.2, 0), 2);
        } catch (Exception ignored) {}

        // Escanear todos los items caídos en el mundo dentro del radio de 200 bloques
        for (Entity entity : player.getWorld().getEntitiesByClass(Item.class)) {
            if (!(entity instanceof Item droppedItem)) continue;
            if (!droppedItem.isValid() || droppedItem.isDead()) continue;

            Location itemLoc = droppedItem.getLocation();
            double dist = itemLoc.distance(pLoc);
            if (dist <= radius) {
                count++;

                // Si está lejos (> 30 bloques), teletransportarlo suavemente a órbita cercana
                if (dist > 30.0) {
                    Vector offset = itemLoc.toVector().subtract(pLoc.toVector()).normalize().multiply(12.0);
                    Location near = pLoc.clone().add(offset).add(0, 0.5, 0);
                    droppedItem.teleport(near);
                    try {
                        itemLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, near, 4, 0.2, 0.2, 0.2, 0.05);
                    } catch (Exception ignored) {}
                }

                // Vector hacia los pies del jugador
                Vector toPlayer = pLoc.toVector().subtract(droppedItem.getLocation().toVector());
                double speed = Math.min(2.2, Math.max(0.9, toPlayer.length() * 0.25));
                Vector pull = toPlayer.normalize().multiply(speed).setY(0.28);
                droppedItem.setVelocity(pull);

                try {
                    pLoc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, droppedItem.getLocation(), 2, 0.1, 0.1, 0.1, 0.01);
                } catch (Exception ignored) {}
            }
        }

        player.sendTitle("§b§l¡VÓRTICE MAGNÉTICO!", "§f" + count + " ítems atraídos desde 200 bloques", 5, 35, 10);
        player.sendActionBar(Component.text("§b🧲 ¡POLARIDAD MAGNÉTICA ACTIVADA! §e" + count + " §7ítems caídos atraídos hacia ti."));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMagnetInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        Material mat = item.getType();
        if (mat != Material.IRON_INGOT && mat != Material.IRON_BLOCK && mat != Material.LODESTONE && mat != Material.COMPASS) {
            UUID uuid = player.getUniqueId();
            PlayerSettings settings = plugin.getRankManager().getPlayerSettings(uuid);
            if (!"JIKI_MAGNET".equalsIgnoreCase(settings.getActiveTransformation())) return;
        }

        event.setCancelled(true);
        triggerMagnetVortex(player);
    }

    // ==========================================
    // 2. FILOS DANZANTES / AURA KILL DE ARMAS
    // ==========================================
    public void toggleDancingBlades(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (isDancingBladesActive(uuid)) {
            activeDancingBlades.remove(uuid);
            player.sendActionBar(Component.text("§c✖ Filos Danzantes (Aura Kill) desvanecidos."));
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.2f);
        } else {
            activeDancingBlades.put(uuid, now + 300000L); // 5 minutos de uso
            player.sendTitle("§c§lFILOS DANZANTES", "§7Aura de exterminio autónomo activada (5m)", 5, 40, 10);
            player.sendActionBar(Component.text("§c⚔ ¡ARMAS ESPECTRALES ACTIVAS! Las espadas y hachas golpean solas en 10 bloques."));
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1.0f, 1.5f);
            player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RETURN, 1.0f, 1.2f);
        }
    }

    private void startDancingBladesTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                bladeOrbitAngle += 0.35;
                if (bladeOrbitAngle > Math.PI * 2) bladeOrbitAngle -= Math.PI * 2;

                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (!player.isOnline() || player.isDead()) continue;
                    UUID uuid = player.getUniqueId();
                    if (!isDancingBladesActive(uuid)) continue;
                    if (!plugin.isWorldAllowed(player.getWorld())) continue;

                    Location pLoc = player.getLocation();

                    // Partículas de armas flotantes orbitando al jugador
                    try {
                        for (int i = 0; i < 3; i++) {
                            double offsetAngle = bladeOrbitAngle + (i * (Math.PI * 2.0 / 3.0));
                            double ox = Math.cos(offsetAngle) * 2.2;
                            double oz = Math.sin(offsetAngle) * 2.2;
                            double oy = 1.0 + Math.sin(offsetAngle * 2.0) * 0.4;
                            Location bladeLoc = pLoc.clone().add(ox, oy, oz);

                            pLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, bladeLoc, 1, 0, 0, 0, 0);
                            pLoc.getWorld().spawnParticle(Particle.CRIT, bladeLoc, 2, 0.1, 0.1, 0.1, 0.02);
                            pLoc.getWorld().spawnParticle(Particle.ENCHANT, bladeLoc, 3, 0.15, 0.15, 0.15, 0.05);
                        }
                    } catch (Exception ignored) {}

                    // Daño autónomo (Aura Kill) contra todos los monstruos / hostiles en 10 bloques
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    double baseDamage = 14.0;
                    if (hand.getType().name().endsWith("_SWORD") || hand.getType().name().endsWith("_AXE")) {
                        baseDamage = 22.0; // Potenciado si empuña arma
                    }
                    int rebirths = plugin.getRankManager().getRebirthCount(uuid);
                    double finalDamage = baseDamage * (1.0 + rebirths * 0.03);

                    for (Entity entity : player.getNearbyEntities(10.0, 4.0, 10.0)) {
                        if (isHostileTarget(entity)) {
                            LivingEntity target = (LivingEntity) entity;
                            if (target.isDead()) continue;

                            target.damage(finalDamage, player);
                            try {
                                Location tLoc = target.getLocation().add(0, 1.0, 0);
                                tLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, tLoc, 1, 0, 0, 0, 0);
                                tLoc.getWorld().spawnParticle(Particle.CRIT, tLoc, 6, 0.2, 0.3, 0.2, 0.1);
                                tLoc.getWorld().playSound(tLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.4f);
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 10L, 10L); // Cada medio segundo (10 ticks)
    }

    private boolean isHostileTarget(Entity e) {
        return (e instanceof Monster || e instanceof Slime || e instanceof Phantom || e instanceof Ghast);
    }
}
