package com.drakescraft.rankup.ability;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.AbilityType;
import com.drakescraft.rankup.model.PlayerSettings;
import com.drakescraft.rankup.model.Rank;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Ghast;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor de Habilidades Especiales Avanzadas:
 * 1. Vórtice Magnético (Imán Atractor de 200 bloques).
 * 2. Filos Danzantes / Kill Aura de Armas (Espadas y hachas flotantes reales con animaciones 3D).
 */
public class SpecialAbilitiesListener implements Listener {

    public static final String BLADE_TAG = "DRAKES_FLYING_BLADE";
    public static final String ABILITY_DAMAGE_META = "DRAKES_ABILITY_DAMAGE";

    private final DrakesRankupPlugin plugin;

    // Cooldown de Imán Magnético: UUID -> timestamp
    private final Map<UUID, Long> magnetCooldown = new HashMap<>();

    // Estado activo de Filos Danzantes (Aura Kill)
    private final Map<UUID, Long> activeDancingBlades = new HashMap<>();

    // Entidades visuales de espadas flotantes por jugador: UUID -> Lista de ItemDisplays
    private final Map<UUID, List<ItemDisplay>> bladeDisplays = new ConcurrentHashMap<>();

    private double bladeOrbitAngle = 0;
    private BukkitTask renderTask;
    private BukkitTask combatTask;

    public SpecialAbilitiesListener(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
        startDancingBladesTasks();
    }

    public boolean isDancingBladesActive(UUID uuid) {
        Long exp = activeDancingBlades.get(uuid);
        if (exp != null && System.currentTimeMillis() < exp) return true;
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(uuid);
        if (settings != null && "DANCING_BLADES".equalsIgnoreCase(settings.getActiveTransformation())) {
            return true;
        }
        return false;
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

        player.sendTitle("§b§lVÓRTICE MAGNÉTICO", "§7Atracción masiva desplegada (200 bloques)", 5, 30, 10);
        player.sendActionBar(Component.text("§b🧲 ¡VÓRTICE ACTIVO! Absorbiendo todos los ítems del sector."));
        player.playSound(pLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 1.6f);
        player.playSound(pLoc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.2f, 0.8f);

        int attractedCount = 0;
        double radius = 200.0;
        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof Item item && item.isValid() && !item.isDead()) {
                Vector dir = pLoc.toVector().subtract(item.getLocation().toVector());
                double dist = dir.length();
                if (dist > 0.8) {
                    double speed = Math.min(2.8, Math.max(0.6, dist * 0.15));
                    item.setVelocity(dir.normalize().multiply(speed));
                }
                attractedCount++;
            }
        }

        player.sendMessage("§b[Imán] §fSe atrajeron §e" + attractedCount + " §fgrupos de ítems hacia tu posición.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        Rank rank = plugin.getRankManager().getPlayerRank(player.getUniqueId());
        if (rank == null) return;
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(player.getUniqueId());
        if (!settings.isAbilitiesEnabled()) return;

        if (rank.getAbilityType() != AbilityType.MAGNETIC_ATTRACTOR && rank.getTier() < 60) return;

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() != Material.AIR) return;

        event.setCancelled(true);
        triggerMagnetVortex(player);
    }

    // ==========================================
    // 2. FILOS DANZANTES / ARSENAL CELESTIAL 3D
    // ==========================================
    public void toggleDancingBlades(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (isDancingBladesActive(uuid)) {
            activeDancingBlades.remove(uuid);
            clearPlayerBlades(uuid);
            player.sendActionBar(Component.text("§c✖ Filos Danzantes (Espadas Voladoras) desvanecidos."));
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.2f);
        } else {
            activeDancingBlades.put(uuid, now + 300000L); // 5 minutos de uso
            spawnBladesForPlayer(player);
            player.sendTitle("§c§lFILOS DANZANTES", "§7Arsenal de espadas flotantes desplegado (5m)", 5, 40, 10);
            player.sendActionBar(Component.text("§c⚔ ¡ESPADAS ESPECTRALES ACTIVAS! Orbitando y aniquilando hostiles."));
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1.0f, 1.5f);
            player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RETURN, 1.0f, 1.2f);
        }
    }

    private void spawnBladesForPlayer(Player player) {
        clearPlayerBlades(player.getUniqueId());
        if (!player.isOnline() || player.isDead()) return;

        List<ItemDisplay> list = new ArrayList<>();
        Location center = player.getLocation().add(0, 1.2, 0);

        List<ItemStack> weapons = new ArrayList<>();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand != null && (hand.getType().name().endsWith("_SWORD") || hand.getType().name().endsWith("_AXE") || hand.getType() == Material.TRIDENT || hand.getType() == Material.MACE)) {
            weapons.add(hand.clone());
        } else {
            weapons.add(createSpectralWeapon(Material.NETHERITE_SWORD, "§4⚔ §cHoja Abisal de Netherite"));
        }
        weapons.add(createSpectralWeapon(Material.DIAMOND_SWORD, "§b✦ §fEspada Divina Celestial"));
        weapons.add(createSpectralWeapon(Material.NETHERITE_AXE, "§6⚡ §eHacha de Juicio Multiversal"));
        weapons.add(createSpectralWeapon(Material.TRIDENT, "§3🔱 §bTridente de la Singularidad"));

        for (ItemStack weapon : weapons) {
            try {
                ItemDisplay display = player.getWorld().spawn(center, ItemDisplay.class, d -> {
                    d.setItemStack(weapon);
                    d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                    d.setBillboard(Display.Billboard.FIXED);
                    d.setInvulnerable(true);
                    d.setPersistent(false);
                    d.setTeleportDuration(2);
                    d.addScoreboardTag(BLADE_TAG);
                });
                list.add(display);
            } catch (Exception e) {
                plugin.getLogger().warning("Error spawneando espada flotante: " + e.getMessage());
            }
        }
        bladeDisplays.put(player.getUniqueId(), list);
    }

    private ItemStack createSpectralWeapon(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void clearPlayerBlades(UUID uuid) {
        List<ItemDisplay> list = bladeDisplays.remove(uuid);
        if (list != null) {
            for (ItemDisplay d : list) {
                if (d != null && d.isValid()) {
                    d.remove();
                }
            }
        }
    }

    public void cleanupAll() {
        if (renderTask != null) renderTask.cancel();
        if (combatTask != null) combatTask.cancel();

        for (List<ItemDisplay> list : bladeDisplays.values()) {
            for (ItemDisplay d : list) {
                if (d != null && d.isValid()) {
                    d.remove();
                }
            }
        }
        bladeDisplays.clear();
        activeDancingBlades.clear();

        for (World world : Bukkit.getWorlds()) {
            for (Entity e : world.getEntitiesByClass(ItemDisplay.class)) {
                if (e.getScoreboardTags().contains(BLADE_TAG)) {
                    e.remove();
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clearPlayerBlades(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        clearPlayerBlades(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        clearPlayerBlades(event.getPlayer().getUniqueId());
    }

    private void startDancingBladesTasks() {
        // Tarea 1: RENDER Y ANIMACIÓN 3D FLUIDA DE LAS ESPADAS
        renderTask = new BukkitRunnable() {
            @Override
            public void run() {
                bladeOrbitAngle += 0.22;
                if (bladeOrbitAngle > Math.PI * 2) bladeOrbitAngle -= Math.PI * 2;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.isOnline() || player.isDead()) continue;
                    UUID uuid = player.getUniqueId();
                    if (!isDancingBladesActive(uuid)) {
                        clearPlayerBlades(uuid);
                        continue;
                    }
                    if (!plugin.isWorldAllowed(player.getWorld())) {
                        clearPlayerBlades(uuid);
                        continue;
                    }

                    List<ItemDisplay> displays = bladeDisplays.get(uuid);
                    if (displays == null || displays.isEmpty() || displays.stream().anyMatch(d -> !d.isValid())) {
                        spawnBladesForPlayer(player);
                        displays = bladeDisplays.get(uuid);
                        if (displays == null || displays.isEmpty()) continue;
                    }

                    Location pLoc = player.getLocation();
                    int count = displays.size();

                    for (int i = 0; i < count; i++) {
                        ItemDisplay display = displays.get(i);
                        if (!display.isValid()) continue;

                        double angle = bladeOrbitAngle + (i * (Math.PI * 2.0 / count));
                        double radius = 1.95;
                        double ox = Math.cos(angle) * radius;
                        double oz = Math.sin(angle) * radius;
                        double oy = 1.15 + Math.sin(angle * 2.0) * 0.28;

                        Location targetLoc = pLoc.clone().add(ox, oy, oz);
                        targetLoc.setYaw((float) Math.toDegrees(-angle) + 90.0f);
                        targetLoc.setPitch(40.0f);

                        display.teleport(targetLoc);

                        if (i % 2 == 0) {
                            pLoc.getWorld().spawnParticle(Particle.CRIT, targetLoc, 1, 0.02, 0.02, 0.02, 0.01);
                            pLoc.getWorld().spawnParticle(Particle.ENCHANT, targetLoc, 2, 0.08, 0.08, 0.08, 0.02);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 4L, 2L);

        // Tarea 2: COMBATE AUTÓNOMO (AURA KILL CONTROLADA)
        combatTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.isOnline() || player.isDead()) continue;
                    UUID uuid = player.getUniqueId();
                    if (!isDancingBladesActive(uuid)) continue;
                    if (!plugin.isWorldAllowed(player.getWorld())) continue;

                    Location pLoc = player.getLocation();
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    double baseDamage = 16.0;
                    if (hand.getType().name().endsWith("_SWORD") || hand.getType().name().endsWith("_AXE")) {
                        baseDamage = 26.0;
                    }
                    int rebirths = plugin.getRankManager().getRebirthCount(uuid);
                    double finalDamage = baseDamage * (1.0 + rebirths * 0.04);

                    int attacked = 0;
                    for (Entity entity : player.getNearbyEntities(11.0, 5.0, 11.0)) {
                        if (!entity.isValid() || !entity.getLocation().isChunkLoaded()) continue;
                        if (isHostileTarget(entity)) {
                            LivingEntity target = (LivingEntity) entity;
                            if (target.isDead()) continue;

                            // MARCAR METADATA PARA EVITAR BUCLES RECURSIVOS (Saitama, Gura Gura, etc.)
                            try {
                                target.setMetadata(ABILITY_DAMAGE_META, new FixedMetadataValue(plugin, true));
                                target.damage(finalDamage, player);
                            } finally {
                                target.removeMetadata(ABILITY_DAMAGE_META, plugin);
                            }

                            try {
                                Location tLoc = target.getLocation().add(0, 1.0, 0);
                                tLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, tLoc, 1, 0, 0, 0, 0);
                                tLoc.getWorld().spawnParticle(Particle.CRIT, tLoc, 5, 0.2, 0.3, 0.2, 0.08);
                                tLoc.getWorld().playSound(tLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.4f);
                            } catch (Exception ignored) {}

                            attacked++;
                            if (attacked >= 5) break;
                        }
                    }

                    if (attacked > 0) {
                        try {
                            player.getWorld().playSound(pLoc, Sound.ITEM_TRIDENT_THROW, 0.6f, 1.6f);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    private boolean isHostileTarget(Entity e) {
        return (e instanceof Monster || e instanceof Slime || e instanceof Phantom || e instanceof Ghast);
    }
}
