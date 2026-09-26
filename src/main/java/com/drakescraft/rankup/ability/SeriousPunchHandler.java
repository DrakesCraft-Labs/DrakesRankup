package com.drakescraft.rankup.ability;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.AbilityType;
import com.drakescraft.rankup.model.PlayerSettings;
import com.drakescraft.rankup.model.Rank;
import com.drakescraft.rankup.protection.ProtectionGate;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Habilidad Legendaria: GOLPE SERIO / PUÑO DE LA MUERTE (DEATH PUNCH - SAITAMA & JIREN).
 * - Provoca daño real y masivo a montañas y terreno (cono sónico colosal).
 * - Umbral Telúrico (400 bloques):
 *   * Si la destrucción es <= 400 bloques: Los bloques se regeneran progresivamente con el tiempo.
 *   * Si la destrucción supera 400 bloques: "¡Que se quede así nomás!" La destrucción es PERMANENTE,
 *     dejando un cráter / cañón perpetuo en el mundo.
 * - Respeto estricto de Claims (ProtectionStones/WorldGuard), Bedrock y Contenedores de jugadores.
 */
public class SeriousPunchHandler implements Listener {

    private final DrakesRankupPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    // Umbral de bloques destruidos para regeneración vs permanencia
    public static final int PERMANENT_CRATER_THRESHOLD = 400;

    // Colas de bloques para regeneración progresiva
    private final Map<UUID, Queue<BlockState>> activeRegenQueues = new ConcurrentHashMap<>();

    public SeriousPunchHandler(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean hasSeriousPunch(Player player) {
        if (!plugin.isWorldAllowed(player.getWorld())) return false;
        Rank rank = plugin.getRankManager().getPlayerRank(player.getUniqueId());
        if (rank == null) return false;

        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(player.getUniqueId());
        if (!settings.isAbilitiesEnabled()) return false;

        // Saitama (Tier 41), Jiren (Tier 91), Tiers 50+, o habilidad SERIOUS_PUNCH
        if (rank.getAbilityType() == AbilityType.SERIOUS_PUNCH) return true;
        if (rank.getTier() == 41 || rank.getTier() == 91 || rank.getTier() >= 50) return true;
        if ("SAITAMA".equalsIgnoreCase(settings.getActiveTransformation()) || "SERIOUS_PUNCH".equalsIgnoreCase(settings.getActiveTransformation())) return true;

        return false;
    }

    public long getCooldownRemainingMs(Player player) {
        long ready = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long now = System.currentTimeMillis();
        return Math.max(0L, ready - now);
    }

    private void applyCooldown(Player player) {
        int rebirths = plugin.getRankManager().getRebirthCount(player.getUniqueId());
        // Cooldown base 6 segundos, reducido por rebirths hasta 3 segundos
        long cd = Math.max(3000L, 6000L - (rebirths * 80L));
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + cd);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!hasSeriousPunch(player)) return;

        long remaining = getCooldownRemainingMs(player);
        if (remaining > 0) return; // Si está en cooldown, no activa el golpe masivo

        applyCooldown(player);
        Location punchOrigin = player.getEyeLocation();
        Vector direction = player.getEyeLocation().getDirection().normalize();

        executeSeriousDeathPunch(player, punchOrigin, direction, event.getEntity());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        if (!hasSeriousPunch(player)) return;

        long remaining = getCooldownRemainingMs(player);
        if (remaining > 0) {
            long sec = (remaining + 999L) / 1000L;
            player.sendActionBar(Component.text("§c⏳ Golpe Serio en recarga: §e" + sec + "s"));
            return;
        }

        applyCooldown(player);
        Location punchOrigin = player.getEyeLocation();
        Vector direction = player.getEyeLocation().getDirection().normalize();

        executeSeriousDeathPunch(player, punchOrigin, direction, null);
    }

    /**
     * Ejecuta el golpe serio estilo Saitama vs Genos.
     * Crea un cono supersónico masivo que ahueca montañas y perfora el terreno.
     */
    public void executeSeriousDeathPunch(Player player, Location origin, Vector dir, Entity directTarget) {
        World world = origin.getWorld();
        if (world == null) return;

        ProtectionGate gate = plugin.getProtectionGate();
        boolean actorInClaim = gate != null && gate.isProtected(origin, player);

        // Sonidos y explosión inicial al detonar el puño
        world.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.6f);
        world.playSound(origin, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 0.8f);
        world.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.8f, 0.7f);

        player.sendTitle("§c§l¡GOLPE SERIO!", "§fPUÑO DE LA MUERTE §8(ᴅᴇᴀᴛʜ ᴘᴜɴᴄʜ)", 5, 40, 10);
        player.sendActionBar(Component.text("§c💥 ¡GOLPE SERIO! §7Onda de choque supersónica desplegada."));

        // Lista de bloques que serán destruidos
        List<BlockState> candidateBlocks = new ArrayList<>();
        Set<Location> visitedLocs = new HashSet<>();

        double maxDistance = 34.0;
        double step = 1.5;

        // Trazado del cono expansivo
        for (double d = 2.0; d <= maxDistance; d += step) {
            Location center = origin.clone().add(dir.clone().multiply(d));
            double radius = 1.8 + (d * 0.22); // Aumenta hasta ~9.2 bloques de ancho

            // Efectos visuales a lo largo del eje central
            try {
                if (((int)(d / step)) % 2 == 0) {
                    world.spawnParticle(Particle.SONIC_BOOM, center, 1);
                    world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
                }
                world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, center, 8, radius * 0.4, radius * 0.4, radius * 0.4, 0.05);
                world.spawnParticle(Particle.SWEEP_ATTACK, center, 3, 0.5, 0.5, 0.5, 0);
                world.spawnParticle(Particle.FLASH, center, 1);
            } catch (Exception ignored) {}

            // Si el actor está en un claim ajeno, no rompe terreno
            if (actorInClaim) continue;

            // Escanear cilindro/esfera en este radio
            int rCeil = (int) Math.ceil(radius);
            for (int dx = -rCeil; dx <= rCeil; dx++) {
                for (int dy = -rCeil; dy <= rCeil; dy++) {
                    for (int dz = -rCeil; dz <= rCeil; dz++) {
                        double distSq = dx * dx + dy * dy + dz * dz;
                        if (distSq > radius * radius) continue;

                        Block b = center.clone().add(dx, dy, dz).getBlock();
                        Location bLoc = b.getLocation();
                        if (visitedLocs.contains(bLoc)) continue;
                        visitedLocs.add(bLoc);

                        if (gate != null && !gate.canDestroyBlock(player, b)) {
                            continue; // Protegido por claims, o bedrock, portal, contenedor
                        }

                        if (!b.getType().isAir()) {
                            candidateBlocks.add(b.getState());
                        }
                    }
                }
            }
        }

        // Daño cinético masivo a entidades en el cono
        int rebirths = plugin.getRankManager().getRebirthCount(player.getUniqueId());
        double entityDamage = 45.0 + (rebirths * 2.5);

        for (Entity e : world.getNearbyEntities(origin.clone().add(dir.clone().multiply(16.0)), 20.0, 15.0, 20.0)) {
            if (e.equals(player)) continue;
            if (e instanceof Tameable t && player.getUniqueId().equals(t.getOwnerUniqueId())) continue;

            if (e instanceof LivingEntity living) {
                // Comprobar si está en el cono
                Vector toTarget = living.getLocation().toVector().subtract(origin.toVector());
                double dot = toTarget.normalize().dot(dir);
                if (dot > 0.65 || e.equals(directTarget)) {
                    living.damage(entityDamage, player);
                    Vector launch = dir.clone().multiply(2.8).setY(0.65);
                    living.setVelocity(launch);
                    try {
                        world.spawnParticle(Particle.EXPLOSION, living.getLocation().add(0, 1.0, 0), 2);
                        world.playSound(living.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.2f, 1.4f);
                    } catch (Exception ignored) {}
                }
            }
        }

        int totalBlocks = candidateBlocks.size();
        if (totalBlocks == 0) return;

        // Destruir físicamente los bloques
        for (BlockState state : candidateBlocks) {
            Block b = state.getBlock();
            b.setType(Material.AIR, false);
        }

        // Evaluar Umbral Telúrico (400 bloques)
        if (totalBlocks <= PERMANENT_CRATER_THRESHOLD) {
            // MODO REGENERACIÓN: Se restaura progresivamente tras 12 segundos
            player.sendActionBar(Component.text("§e[Saitama] §7Cráter telúrico (" + totalBlocks + " bloques) en fase de regeneración temporal."));

            // Ordenar de abajo hacia arriba (menor Y a mayor Y) para que la montaña se reconstruya con naturalidad
            candidateBlocks.sort(Comparator.comparingInt(BlockState::getY));

            UUID queueId = UUID.randomUUID();
            Queue<BlockState> queue = new ConcurrentLinkedQueue<>(candidateBlocks);
            activeRegenQueues.put(queueId, queue);

            // Programar reconstrucción progresiva tras 12 segundos (240 ticks)
            new BukkitRunnable() {
                @Override
                public void run() {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Queue<BlockState> q = activeRegenQueues.get(queueId);
                            if (q == null || q.isEmpty()) {
                                activeRegenQueues.remove(queueId);
                                cancel();
                                return;
                            }

                            // Regenerar 12 bloques por pulso (cada 2 ticks)
                            for (int i = 0; i < 12; i++) {
                                BlockState bs = q.poll();
                                if (bs == null) break;

                                Block b = bs.getBlock();
                                bs.update(true, false);

                                try {
                                    b.getWorld().spawnParticle(Particle.BLOCK, b.getLocation().add(0.5, 0.5, 0.5), 4, 0.2, 0.2, 0.2, bs.getBlockData());
                                    if (i == 0) {
                                        b.getWorld().playSound(b.getLocation(), Sound.BLOCK_STONE_PLACE, 0.6f, 1.2f);
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                    }.runTaskTimer(plugin, 1L, 2L);
                }
            }.runTaskLater(plugin, 240L); // 12 segundos de retardo
        } else {
            // MODO CRÁTER PERMANENTE ("si pasa de cierta cantidad de bloques la destruccion que se quede asi nomas")
            world.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.5f);
            player.sendTitle("§c§l💥 ¡CRÁTER PERMANENTE!", "§e" + totalBlocks + " bloques devastados · Se queda así nomás", 10, 60, 20);
            Bukkit.broadcast(Component.text("§6[Rankup] §c¡El Golpe Serio de §e" + player.getName() + " §cha alterado permanentemente la geografía! §7(" + totalBlocks + " bloques destruidos)"));
        }
    }

    /**
     * En caso de apagado o recarga del servidor, restaurar inmediatamente colas pendientes
     * para evitar pérdida accidental de terreno en proceso de regeneración.
     */
    public void restoreAllPending() {
        for (Queue<BlockState> q : activeRegenQueues.values()) {
            while (!q.isEmpty()) {
                BlockState bs = q.poll();
                if (bs != null) {
                    bs.update(true, false);
                }
            }
        }
        activeRegenQueues.clear();
    }
}
