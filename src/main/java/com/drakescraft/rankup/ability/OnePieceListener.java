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
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Suite Completa One Piece:
 * - Gear 2, Gear 3 (Gigante), Gear 4 (Bounceman con rebote continuo realista) y Gear 5 (Sun God Nika).
 * - Frutas del Diablo adicionales: Mera Mera (Fuego), Ope Ope (ROOM), Pika Pika (Luz), Gura Gura (Terremoto).
 * - Trilogía de Haki: Armamento (Busoshoku), Observación (Kenbunshoku) y Conquistador (Haoshoku).
 * - Duración extendida de transformaciones (3 a 5 minutos) y resolución de conflictos de espadas.
 */
public class OnePieceListener implements Listener {

    private final DrakesRankupPlugin plugin;

    // Cooldown de Zoro Flying Slash (Santoryu)
    private final Map<UUID, Long> santoryuCooldown = new HashMap<>();

    // Estados Activos de Transformaciones (UUID -> timestamp expiración)
    private final Map<UUID, Long> activeGearSecond = new HashMap<>();
    private final Map<UUID, Long> activeGear3 = new HashMap<>();
    private final Map<UUID, Long> gear3Cooldown = new HashMap<>();
    private final Map<UUID, Long> activeGear4 = new HashMap<>();
    private final Map<UUID, Long> gear4Cooldown = new HashMap<>();
    private final Map<UUID, Long> activeGear5 = new HashMap<>();
    private final Map<UUID, Long> gear5Cooldown = new HashMap<>();

    // Frutas adicionales activas
    private final Map<UUID, Long> activeMera = new HashMap<>();
    private final Map<UUID, Long> activeOpe = new HashMap<>();
    private final Map<UUID, Long> activePika = new HashMap<>();
    private final Map<UUID, Long> activeGura = new HashMap<>();
    private final Map<UUID, Long> fruitSkillCooldown = new HashMap<>();

    // Cadencia de rebote de Gear 4 (bouncing constante como Luffy)
    private final Map<UUID, Long> lastGear4Bounce = new HashMap<>();

    // Cooldown de Haki del Conquistador
    private final Map<UUID, Long> conquerorCooldown = new HashMap<>();

    public OnePieceListener(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
        startJoyboyHeartbeatTask();
    }

    public boolean isGear5Active(UUID uuid) {
        Long exp = activeGear5.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    public boolean isGear4Active(UUID uuid) {
        Long exp = activeGear4.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    public boolean isGear3Active(UUID uuid) {
        Long exp = activeGear3.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    public boolean isGearSecondActive(UUID uuid) {
        Long exp = activeGearSecond.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    public boolean isMeraActive(UUID uuid) {
        Long exp = activeMera.get(uuid);
        if (exp != null && System.currentTimeMillis() < exp) return true;
        PlayerSettings s = plugin.getRankManager().getPlayerSettings(uuid);
        return "MERA_MERA".equalsIgnoreCase(s.getActiveTransformation());
    }

    public boolean isOpeActive(UUID uuid) {
        Long exp = activeOpe.get(uuid);
        if (exp != null && System.currentTimeMillis() < exp) return true;
        PlayerSettings s = plugin.getRankManager().getPlayerSettings(uuid);
        return "OPE_OPE".equalsIgnoreCase(s.getActiveTransformation());
    }

    public boolean isPikaActive(UUID uuid) {
        Long exp = activePika.get(uuid);
        if (exp != null && System.currentTimeMillis() < exp) return true;
        PlayerSettings s = plugin.getRankManager().getPlayerSettings(uuid);
        return "PIKA_PIKA".equalsIgnoreCase(s.getActiveTransformation());
    }

    public boolean isGuraActive(UUID uuid) {
        Long exp = activeGura.get(uuid);
        if (exp != null && System.currentTimeMillis() < exp) return true;
        PlayerSettings s = plugin.getRankManager().getPlayerSettings(uuid);
        return "GURA_GURA".equalsIgnoreCase(s.getActiveTransformation());
    }

    private void resetScale(Player player) {
        try {
            var attr = player.getAttribute(Attribute.SCALE);
            if (attr != null) attr.setBaseValue(1.0);
        } catch (Throwable ignored) {}
    }

    // ==========================================
    // ESTILO TRES ESPADAS DE ZORO (SANTORYU)
    // Sin choques: solo se ejecuta si está activamente equipado o es habilidad de rango principal
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH)
    public void onSantoryuInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!plugin.isWorldAllowed(player.getWorld())) return;
        if (player.isSneaking()) return; // Shift + Clic reservado para técnicas de agachado

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR || !item.getType().name().endsWith("_SWORD")) return;

        UUID uuid = player.getUniqueId();
        Rank rank = plugin.getRankManager().getPlayerRank(uuid);
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(uuid);
        String equipped = settings.getActiveTransformation();

        boolean isEquipped = "SANTORYU".equalsIgnoreCase(equipped);
        boolean isCurrentRankPrimary = (rank != null && rank.getAbilityType() == AbilityType.SANTORYU_ZORO && (equipped == null || equipped.isEmpty()));
        if (!isEquipped && !isCurrentRankPrimary) return;

        long now = System.currentTimeMillis();
        long ready = santoryuCooldown.getOrDefault(uuid, 0L);
        if (now < ready) {
            long rem = Math.max(1, (ready - now) / 1000L);
            player.sendActionBar(Component.text("§c⏳ Corte del Dragón en recarga: §e" + rem + "s"));
            return;
        }

        int rebirths = plugin.getRankManager().getRebirthCount(uuid);
        double cdr = 1.0 - Math.min(0.5, rebirths * 0.01);
        santoryuCooldown.put(uuid, now + (long)(8000L * cdr));

        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();

        try {
            player.getWorld().playSound(eye, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.8f);
            player.getWorld().playSound(eye, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.0f, 1.9f);
            player.getWorld().playSound(eye, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.9f, 0.8f);
        } catch (Exception ignored) {}

        // Arco de viento volador
        Set<LivingEntity> hitList = new HashSet<>();
        for (double d = 1.2; d <= 12.0; d += 0.6) {
            Location pt = eye.clone().add(dir.clone().multiply(d));
            if (!pt.getBlock().isPassable()) break;

            try {
                pt.getWorld().spawnParticle(Particle.SWEEP_ATTACK, pt, 2, 0.1, 0.1, 0.1, 0);
                pt.getWorld().spawnParticle(Particle.CRIT, pt, 3, 0.1, 0.1, 0.1, 0.05);
                pt.getWorld().spawnParticle(Particle.DUST, pt, 4, 0.25, 0.5, 0.25, 0, new Particle.DustOptions(Color.fromRGB(60, 200, 90), 1.3f));
            } catch (Exception ignored) {}

            for (LivingEntity entity : pt.getWorld().getNearbyLivingEntities(pt, 1.3)) {
                if (entity.equals(player)) continue;
                if (!hitList.contains(entity)) {
                    hitList.add(entity);
                    try {
                        double dmg = 12.0 * (1.0 + rebirths * 0.03);
                        entity.damage(dmg, player);
                        Vector knock = dir.clone().multiply(0.85).setY(0.28);
                        entity.setVelocity(knock);
                    } catch (Exception ignored) {}
                }
            }
        }

        player.sendActionBar(Component.text("§2⚔ ¡ESTILO TRES ESPADAS: CORTE DEL DRAGÓN VOLADOR! §7(Recarga: 8s)"));
    }

    // ==========================================
    // 1. GEAR 4 (BOUNCEMAN: REBOTE REALISTA CONTINUO + 4 MINUTOS)
    // ==========================================
    @EventHandler
    public void onGear4Activate(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        if (!plugin.isWorldAllowed(player.getWorld())) return;
        UUID uuid = player.getUniqueId();
        Rank rank = plugin.getRankManager().getPlayerRank(uuid);
        int tier = (rank != null) ? rank.getTier() : 0;
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(uuid);
        String equipped = settings.getActiveTransformation();

        boolean hasGear4 = "GEAR_4".equalsIgnoreCase(equipped) || "GOMU_GOMU".equalsIgnoreCase(equipped) || tier >= 40;
        if (!hasGear4 || isGear4Active(uuid)) return;

        long now = System.currentTimeMillis();
        Long cd = gear4Cooldown.get(uuid);
        if (cd != null && now < cd) {
            player.sendActionBar(Component.text("§4⏳ Gear 4 en recarga: §e" + ((cd - now) / 1000 + 1) + "s"));
            return;
        }

        int rebirths = plugin.getRankManager().getRebirthCount(uuid);
        double cdr = 1.0 - Math.min(0.5, rebirths * 0.01);
        gear4Cooldown.put(uuid, now + (long)(60000L * cdr));
        activeGear4.put(uuid, now + 240000L); // 4 MINUTOS (240s)

        try {
            var attr = player.getAttribute(Attribute.SCALE);
            if (attr != null) attr.setBaseValue(1.35); // Inflado Bounceman
        } catch (Throwable ignored) {}

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 4800, 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 4800, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 4800, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 4800, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 4800, 0));

        Location loc = player.getLocation();
        try {
            var d1 = new Particle.DustOptions(Color.fromRGB(200, 30, 30), 1.5f);
            var d2 = new Particle.DustOptions(Color.fromRGB(30, 20, 20), 1.3f);
            player.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 60, 0.8, 1.0, 0.8, 0, d1);
            player.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 40, 0.6, 0.8, 0.6, 0, d2);
            player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc.clone().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.02);
            player.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.4f);
            player.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.2f);
            player.getWorld().playSound(loc, Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1.0f, 0.6f);
        } catch (Exception ignored) {}

        player.sendTitle("§4§lGEAR FOURTH: BOUNCEMAN", "§c¡Haki de Armamento colosal + Rebote elástico continuo!", 5, 50, 10);
        player.sendActionBar(Component.text("§4⚡ ¡GEAR 4 BOUNCEMAN ACTIVO! Rebotes automáticos, Velocidad III y Fuerza II por 4 minutos"));

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            activeGear4.remove(uuid);
            resetScale(player);
            if (player.isOnline()) {
                player.sendMessage("§4[Rankup] El Gear 4 ha concluido: el haki consumido se disipa.");
            }
        }, 4800L);
    }

    // ==========================================
    // 2. GEAR 5 (SUN GOD NIKA: TAMBORES DE JÚBILO + 5 MINUTOS)
    // ==========================================
    public void activateGear5(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long cd = gear5Cooldown.get(uuid);
        if (cd != null && now < cd) {
            player.sendActionBar(Component.text("§f⏳ Gear 5 en recarga: §e" + ((cd - now) / 1000 + 1) + "s"));
            return;
        }

        int rebirths = plugin.getRankManager().getRebirthCount(uuid);
        double cdr = 1.0 - Math.min(0.5, rebirths * 0.01);
        gear5Cooldown.put(uuid, now + (long)(60000L * cdr));
        activeGear5.put(uuid, now + 300000L); // 5 MINUTOS (300s)

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 6000, 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 6000, 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 6000, 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 6000, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 6000, 1));

        Location loc = player.getLocation();
        try {
            loc.getWorld().spawnParticle(Particle.FLASH, loc.clone().add(0, 1, 0), 3);
            loc.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0, 1, 0), 80, 0.8, 1.2, 0.8, 0.05);
            loc.getWorld().spawnParticle(Particle.WAX_OFF, loc.clone().add(0, 1, 0), 40, 0.8, 1.0, 0.8, 0.1);
            loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.05);
            loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL, 1.2f, 1.8f);
            loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.6f);
        } catch (Exception ignored) {}

        player.sendTitle("§f§l☀ SUN GOD NIKA ︱ GEAR 5", "§e¡Los Tambores de la Liberación resuenan! (5m)", 5, 50, 15);
        player.sendActionBar(Component.text("§f☀ ¡JAJAJAJA! ¡ESTE ES EL GUERRERO DE LA LIBERACIÓN: GEAR 5! §e(5 minutos)"));

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            activeGear5.remove(uuid);
            if (player.isOnline()) {
                player.sendMessage("§f[Rankup] El Gear 5 se apaga: el Guerrero de la Liberación descansa.");
            }
        }, 6000L);
    }

    private void startJoyboyHeartbeatTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (!player.isOnline() || player.isDead()) continue;
                    UUID uuid = player.getUniqueId();
                    if (!isGear5Active(uuid)) continue;

                    Location loc = player.getLocation();
                    try {
                        // Tambores de la Liberación: Doom-dutta-da!
                        loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.9f, 0.6f);
                        loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.9f, 0.8f);
                        loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL, 0.7f, 1.5f);

                        // Corona de nubes y estela de luz pura
                        loc.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0, 1.8, 0), 6, 0.35, 0.15, 0.35, 0.02);
                        loc.getWorld().spawnParticle(Particle.WAX_OFF, loc.clone().add(0, 1.5, 0), 4, 0.3, 0.3, 0.3, 0.05);
                    } catch (Exception ignored) {}
                }
            }
        }.runTaskTimer(plugin, 60L, 60L); // Cada 3 segundos
    }

    // ==========================================
    // REBOTE CONTINUO REALISTA DE GEAR 4 & CARTOON DE GEAR 5 EN MOVIMIENTO
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerBounceMovement(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isWorldAllowed(player.getWorld())) return;
        UUID uuid = player.getUniqueId();

        // 1. Maldición del Océano
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(uuid);
        String equipped = settings.getActiveTransformation();
        boolean hasFruit = isGear5Active(uuid) || isGear4Active(uuid) || isGear3Active(uuid) || isGearSecondActive(uuid)
                || isMeraActive(uuid) || isOpeActive(uuid) || isPikaActive(uuid) || isGuraActive(uuid)
                || "GOMU_GOMU".equalsIgnoreCase(equipped) || "MERA_MERA".equalsIgnoreCase(equipped);

        if (hasFruit && (player.isInWater() || player.getLocation().getBlock().getType() == Material.WATER)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 1, false, false));
            return;
        }

        // 2. GEAR 4 BOUNCEMAN: REBOTE CONTINUO SIN TOCAR NADA
        if (isGear4Active(uuid) && player.isOnGround()) {
            long now = System.currentTimeMillis();
            long lastBounce = lastGear4Bounce.getOrDefault(uuid, 0L);
            if (now - lastBounce > 320L) { // Cadencia elástica (~0.32s)
                lastGear4Bounce.put(uuid, now);
                Vector v = player.getVelocity();
                Vector bounce = new Vector(v.getX() * 1.15, 0.45, v.getZ() * 1.15);
                player.setVelocity(bounce);
                player.setFallDistance(0);

                Location loc = player.getLocation();
                try {
                    loc.getWorld().playSound(loc, Sound.BLOCK_SLIME_BLOCK_FALL, 0.7f, 1.3f);
                    loc.getWorld().playSound(loc, Sound.ENTITY_SLIME_JUMP, 0.6f, 0.9f);
                    loc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 5, 0.2, 0.05, 0.2, 0.01);
                    loc.getWorld().spawnParticle(Particle.DUST, loc, 8, 0.3, 0.05, 0.3, 0, new Particle.DustOptions(Color.fromRGB(180, 20, 20), 1.3f));
                } catch (Exception ignored) {}
            }
        }
    }

    // ==========================================
    // REBOTE CARTOON DE GOMA (GEAR 3, GEAR 4 Y GEAR 5 SIN DAÑO DE CAÍDA)
    // ==========================================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRubberFallBounce(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        UUID uuid = player.getUniqueId();

        boolean isRubber = isGear5Active(uuid) || isGear4Active(uuid) || isGear3Active(uuid);
        if (!isRubber) return;

        event.setCancelled(true);
        double fallDist = player.getFallDistance();
        double bounceY = Math.min(2.5, Math.max(0.9, fallDist * 0.20));
        Vector v = player.getVelocity();
        player.setVelocity(new Vector(v.getX() * 1.25, bounceY, v.getZ() * 1.25));
        player.setFallDistance(0);

        Location loc = player.getLocation();
        try {
            loc.getWorld().playSound(loc, Sound.BLOCK_SLIME_BLOCK_FALL, 1.4f, 0.7f);
            loc.getWorld().playSound(loc, Sound.ENTITY_SLIME_SQUISH, 1.2f, 0.8f);
            loc.getWorld().spawnParticle(Particle.ITEM_SLIME, loc, 40, 1.0, 0.4, 1.0, 0.1);
            loc.getWorld().spawnParticle(Particle.CLOUD, loc, 30, 0.8, 0.3, 0.8, 0.05);
            player.sendActionBar(Component.text("§a⚾ ¡REBOTE ELÁSTICO DE GOMA! §eImpulso: ↑" + String.format("%.1f", bounceY)));
        } catch (Exception ignored) {}

        // Impacto sísmico en terreno abierto si cayó más de 2 bloques
        if (fallDist >= 2.0) {
            int rebirths = plugin.getRankManager().getRebirthCount(uuid);
            double baseDamage = (12.0 + fallDist * 0.9) * (1.0 + rebirths * 0.03);

            for (Entity e : player.getNearbyEntities(6.0, 3.5, 6.0)) {
                if (e instanceof LivingEntity target && e != player) {
                    target.damage(baseDamage, player);
                    Vector knock = target.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(1.5).setY(0.5);
                    target.setVelocity(knock);
                }
            }
            if (plugin.getProtectionGate() != null) {
                plugin.getProtectionGate().applyTerrainExplosion(player, loc, 2.5f, false);
            }
        }
    }

    // ==========================================
    // TÉCNICAS ACTIVAS DE COMBATE (GEAR 5, MERA, OPE, PIKA, GURA)
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH)
    public void onFruitInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isWorldAllowed(player.getWorld())) return;
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // 1. GEAR 5: KAMINARI (Lanzamiento de Rayo) & BAJRANG GUN (Puño Colosal)
        if (isGear5Active(uuid)) {
            if (event.getAction() == Action.LEFT_CLICK_AIR) {
                if (player.isSneaking()) {
                    // Bajrang Gun: Puño Colosal
                    Long cd = fruitSkillCooldown.get(uuid);
                    if (cd != null && now < cd) return;
                    fruitSkillCooldown.put(uuid, now + 10000L); // 10s CD

                    Location eye = player.getEyeLocation();
                    Vector dir = eye.getDirection().normalize();
                    Location impact = eye.clone().add(dir.multiply(7.0));

                    try {
                        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, impact, 1);
                        player.getWorld().spawnParticle(Particle.CLOUD, impact, 60, 2.0, 2.0, 2.0, 0.1);
                        player.getWorld().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.6f);
                        player.getWorld().playSound(impact, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.8f);
                    } catch (Exception ignored) {}

                    player.sendTitle("§f§lBAJRANG GUN", "§e¡Puño Colosal de la Liberación!", 2, 35, 10);
                    for (Entity e : player.getNearbyEntities(10.0, 5.0, 10.0)) {
                        if (e instanceof LivingEntity target && e != player) {
                            target.damage(28.0, player);
                            target.setVelocity(dir.clone().multiply(2.2).setY(0.8));
                        }
                    }
                    if (plugin.getProtectionGate() != null) {
                        plugin.getProtectionGate().applyTerrainExplosion(player, impact, 3.5f, false);
                    }
                } else {
                    // Kaminari: Lanza un rayo eléctrico
                    Long cd = fruitSkillCooldown.get(uuid);
                    if (cd != null && now < cd) return;
                    fruitSkillCooldown.put(uuid, now + 4000L); // 4s CD

                    Location loc = player.getLocation().add(0, 3, 0);
                    loc.getWorld().strikeLightningEffect(loc);
                    Vector dir = player.getEyeLocation().getDirection().normalize();

                    for (double d = 1.0; d <= 20.0; d += 0.8) {
                        Location p = player.getEyeLocation().add(dir.clone().multiply(d));
                        if (!p.getBlock().isPassable()) break;
                        p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, p, 5, 0.2, 0.2, 0.2, 0.08);
                        p.getWorld().spawnParticle(Particle.FLASH, p, 1, 0, 0, 0, 0);
                        for (LivingEntity target : p.getWorld().getNearbyLivingEntities(p, 1.5)) {
                            if (target != player) {
                                target.damage(16.0, player);
                                target.getWorld().strikeLightningEffect(target.getLocation());
                            }
                        }
                    }
                    player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.4f);
                    player.sendActionBar(Component.text("§f⚡ ¡GOMU GOMU NO KAMINARI! Rayo celestial arrojado."));
                }
                return;
            }
        }

        // 2. FRUTA MERA MERA (FUEGO: HIKEN & ENTEI)
        if (isMeraActive(uuid)) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                Long cd = fruitSkillCooldown.get(uuid);
                if (cd != null && now < cd) return;

                if (player.isSneaking()) {
                    // Dai Enkai: Entei (Emperador de las Llamas)
                    fruitSkillCooldown.put(uuid, now + 12000L); // 12s CD
                    Location eye = player.getEyeLocation();
                    Vector dir = eye.getDirection().normalize();
                    Location target = eye.clone().add(dir.multiply(14.0));

                    try {
                        target.getWorld().spawnParticle(Particle.FLAME, target, 120, 2.5, 2.5, 2.5, 0.08);
                        target.getWorld().spawnParticle(Particle.LAVA, target, 20, 1.0, 1.0, 1.0, 0.05);
                        target.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, target, 1);
                        target.getWorld().playSound(target, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.5f);
                    } catch (Exception ignored) {}

                    player.sendTitle("§6§lDAI ENKAI: ENTEI", "§c¡Sol abrasador desatado!", 5, 35, 10);
                    for (Entity e : player.getNearbyEntities(12.0, 6.0, 12.0)) {
                        if (e instanceof LivingEntity vic && e != player) {
                            vic.setFireTicks(160);
                            vic.damage(24.0, player);
                        }
                    }
                    if (plugin.getProtectionGate() != null) {
                        plugin.getProtectionGate().applyTerrainExplosion(player, target, 3.0f, true);
                    }
                } else {
                    // Hiken: Puño de Fuego
                    fruitSkillCooldown.put(uuid, now + 4000L); // 4s CD
                    Vector dir = player.getEyeLocation().getDirection().normalize();

                    for (double d = 1.0; d <= 18.0; d += 0.7) {
                        Location p = player.getEyeLocation().add(dir.clone().multiply(d));
                        if (!p.getBlock().isPassable()) break;
                        p.getWorld().spawnParticle(Particle.FLAME, p, 15, 0.4, 0.4, 0.4, 0.05);
                        p.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, p, 2, 0.2, 0.2, 0.2, 0.02);
                        for (LivingEntity vic : p.getWorld().getNearbyLivingEntities(p, 1.6)) {
                            if (vic != player) {
                                vic.setFireTicks(100);
                                vic.damage(14.0, player);
                            }
                        }
                    }
                    player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);
                    player.sendActionBar(Component.text("§6🔥 ¡HIKEN! ¡Puño de Fuego desatado!"));
                }
                return;
            }
        }

        // 3. FRUTA OPE OPE (ROOM & SHAMBLES)
        if (isOpeActive(uuid)) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (player.isSneaking()) {
                    // Shambles: Intercambio de posiciones con el objetivo
                    Long cd = fruitSkillCooldown.get(uuid);
                    if (cd != null && now < cd) return;

                    Entity target = getTargetEntity(player, 25.0);
                    if (target instanceof LivingEntity livingTarget) {
                        fruitSkillCooldown.put(uuid, now + 5000L);
                        Location pLoc = player.getLocation();
                        Location tLoc = livingTarget.getLocation();

                        player.teleport(tLoc);
                        livingTarget.teleport(pLoc);

                        pLoc.getWorld().playSound(pLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
                        tLoc.getWorld().playSound(tLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
                        pLoc.getWorld().spawnParticle(Particle.PORTAL, pLoc, 20, 0.5, 1.0, 0.5, 0.1);
                        tLoc.getWorld().spawnParticle(Particle.PORTAL, tLoc, 20, 0.5, 1.0, 0.5, 0.1);
                        player.sendActionBar(Component.text("§b⚡ ¡SHAMBLES! Intercambio de posición ejecutado."));
                    }
                } else {
                    // ROOM: Despliega domo
                    Location loc = player.getLocation();
                    for (int i = 0; i < 24; i++) {
                        double angle = (Math.PI * 2.0 * i) / 24.0;
                        Location edge = loc.clone().add(Math.cos(angle) * 14.0, 0.5, Math.sin(angle) * 14.0);
                        edge.getWorld().spawnParticle(Particle.END_ROD, edge, 1, 0, 0.5, 0, 0.01);
                    }
                    player.playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 1.8f);
                    player.sendActionBar(Component.text("§b🌀 ¡ROOM desplegado! (14 bloques de dominio espacial)"));
                }
                return;
            }
        }

        // 4. FRUTA PIKA PIKA (LUZ: YATA NO KAGAMI)
        if (isPikaActive(uuid)) {
            if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                if (player.isSneaking()) {
                    // Salto a la velocidad de la luz (Yata no Kagami)
                    Long cd = fruitSkillCooldown.get(uuid);
                    if (cd != null && now < cd) return;
                    fruitSkillCooldown.put(uuid, now + 6000L); // 6s CD

                    Vector dir = player.getEyeLocation().getDirection().normalize();
                    Location dest = player.getLocation().add(dir.clone().multiply(30.0));
                    dest.setY(dest.getWorld().getHighestBlockYAt(dest) + 1);

                    for (double d = 0; d < 30.0; d += 1.5) {
                        Location beam = player.getEyeLocation().add(dir.clone().multiply(d));
                        beam.getWorld().spawnParticle(Particle.FIREWORK, beam, 2, 0.1, 0.1, 0.1, 0.02);
                        beam.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, beam, 3, 0.1, 0.1, 0.1, 0.05);
                    }

                    player.teleport(dest);
                    dest.getWorld().playSound(dest, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.5f);
                    dest.getWorld().spawnParticle(Particle.FLASH, dest.clone().add(0, 1, 0), 2);
                    player.sendActionBar(Component.text("§e✨ ¡YATA NO KAGAMI! Teletransporte fotónico de luz."));
                }
            }
        }

        // 5. FRUTA GURA GURA (TERREMOTO SÍSMICO)
        if (isGuraActive(uuid)) {
            if (event.getAction() == Action.LEFT_CLICK_AIR) {
                Long cd = fruitSkillCooldown.get(uuid);
                if (cd != null && now < cd) return;
                fruitSkillCooldown.put(uuid, now + 7000L); // 7s CD

                Location eye = player.getEyeLocation();
                Vector dir = eye.getDirection().normalize();
                Location center = eye.clone().add(dir.multiply(3.0));

                try {
                    center.getWorld().spawnParticle(Particle.SONIC_BOOM, center, 1);
                    center.getWorld().spawnParticle(Particle.EXPLOSION, center, 4, 1.0, 1.0, 1.0, 0.1);
                    center.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.2f, 0.6f);
                    center.getWorld().playSound(center, Sound.BLOCK_ANVIL_LAND, 1.2f, 0.5f);
                } catch (Exception ignored) {}

                for (Entity e : player.getNearbyEntities(12.0, 5.0, 12.0)) {
                    if (e instanceof LivingEntity target && e != player) {
                        target.damage(22.0, player);
                        target.setVelocity(new Vector(0, 1.2, 0).add(dir.clone().multiply(1.4)));
                    }
                }
                player.sendTitle("§f§lGURA GURA: SHIMA YURASHI", "§7¡El aire se agrieta en pedazos!", 2, 30, 8);
                player.sendActionBar(Component.text("§f🌊 ¡FISURA ESPACIAL SÍSMICA! Enemigos lanzados por el aire."));
            }
        }
    }

    // ==========================================
    // 3. HAKI DEL CONQUISTADOR (HAOSHOKU HAKI: SHIFT + F)
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH)
    public void onConquerorHakiTrigger(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        if (!plugin.isWorldAllowed(player.getWorld())) return;

        Rank rank = plugin.getRankManager().getPlayerRank(player.getUniqueId());
        int tier = (rank != null) ? rank.getTier() : 0;
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(player.getUniqueId());
        String equipped = settings.getActiveTransformation();

        boolean hasConqueror = tier >= 30 || "HAKI".equalsIgnoreCase(equipped) || "GEAR_FIVE".equalsIgnoreCase(equipped)
                || (rank != null && rank.getAbilityType() == AbilityType.HAKI_CONQUEROR);
        if (!hasConqueror) return;

        event.setCancelled(true);
        triggerConquerorHaki(player);
    }

    public void triggerConquerorHaki(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long ready = conquerorCooldown.getOrDefault(uuid, 0L);
        if (now < ready) {
            long rem = Math.max(1, (ready - now) / 1000L);
            player.sendActionBar(Component.text("§c⏳ Haki del Conquistador en recarga: §e" + rem + "s"));
            return;
        }

        int rebirths = plugin.getRankManager().getRebirthCount(uuid);
        double cdr = 1.0 - Math.min(0.5, rebirths * 0.01);
        conquerorCooldown.put(uuid, now + (long)(30000L * cdr));

        Location loc = player.getLocation();
        try {
            loc.getWorld().strikeLightningEffect(loc);
            loc.getWorld().spawnParticle(Particle.SONIC_BOOM, loc.clone().add(0, 1.2, 0), 2);
            loc.getWorld().spawnParticle(Particle.FLASH, loc.clone().add(0, 1.0, 0), 2);
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1.0, 0), 80, 4.0, 1.0, 4.0, 0, new Particle.DustOptions(Color.fromRGB(150, 10, 10), 1.6f));
            loc.getWorld().playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.7f);
            loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.6f);
        } catch (Exception ignored) {}

        int knocked = 0;
        for (Entity e : player.getNearbyEntities(16.0, 6.0, 16.0)) {
            if (e instanceof LivingEntity target && e != player) {
                knocked++;
                if (target instanceof Monster) {
                    if (target.getHealth() <= 30.0) {
                        target.damage(999.0, player); // Desmayo/muerte instantánea para débiles
                    } else {
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 140, 3));
                        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 140, 2));
                    }
                } else if (target instanceof Player victim) {
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2));
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
                    victim.sendTitle("§4§l¡VOLUNTAD DEL CONQUISTADOR!", "§7Abrumado por el Haki de " + player.getName(), 5, 30, 10);
                }
            }
        }

        player.sendTitle("§4§lHAOSHOKU HAKI", "§c¡Estallido de Voluntad Divina!", 5, 40, 10);
        player.sendActionBar(Component.text("§4👑 ¡HAKI DEL CONQUISTADOR! §e" + knocked + " §7enemigos paralizados o noqueados."));
    }

    // ==========================================
    // HAKI DE OBSERVACIÓN & ARMAMENTO EN COMBATE
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH)
    public void onHakiCombatDefend(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player defender)) return;
        UUID uuid = defender.getUniqueId();
        Rank rank = plugin.getRankManager().getPlayerRank(uuid);
        int tier = (rank != null) ? rank.getTier() : 0;
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(uuid);
        String equipped = settings.getActiveTransformation();

        // 1. Kenbunshoku Haki (Observación): 30% esquiva automática
        boolean hasObservation = tier >= 28 || "HAKI".equalsIgnoreCase(equipped) || isGear5Active(uuid)
                || (rank != null && rank.getAbilityType() == AbilityType.KENBUNSHOKU_HAKI);

        if (hasObservation && Math.random() < 0.30) {
            event.setCancelled(true);
            try {
                Location dloc = defender.getLocation();
                defender.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, dloc.clone().add(0, 1.0, 0), 12, 0.3, 0.4, 0.3, 0.05);
                defender.getWorld().playSound(dloc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.8f);
                defender.sendActionBar(Component.text("§e⚡ ¡HAKI DE OBSERVACIÓN! §7Ataque predicho y esquivado limpiamente."));
            } catch (Exception ignored) {}
            return;
        }

        // 2. Busoshoku Haki (Armamento): 25% reducción de daño físico
        boolean hasArmament = tier >= 30 || "HAKI".equalsIgnoreCase(equipped) || isGear4Active(uuid)
                || (rank != null && rank.getAbilityType() == AbilityType.BUSOSHOKU_HAKI);

        if (hasArmament) {
            event.setDamage(event.getDamage() * 0.75);
            try {
                defender.getWorld().spawnParticle(Particle.SQUID_INK, defender.getLocation().add(0, 1.0, 0), 4, 0.2, 0.3, 0.2, 0.02);
            } catch (Exception ignored) {}
        }
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

        // Bonificaciones de transformación
        if (isGear4Active(uuid)) {
            event.setDamage(event.getDamage() * 1.6);
        } else if (isGear5Active(uuid)) {
            event.setDamage(event.getDamage() * 1.75);
        } else if (isGear3Active(uuid)) {
            event.setDamage(event.getDamage() * 1.5);
        }

        // Haki de Armamento (Ofensivo: +45% daño físico)
        boolean hasArmament = tier >= 30 || "HAKI".equalsIgnoreCase(equipped) || isGear4Active(uuid)
                || (rank != null && rank.getAbilityType() == AbilityType.BUSOSHOKU_HAKI);

        if (hasArmament) {
            event.setDamage(event.getDamage() * 1.45);
            try {
                Location tLoc = event.getEntity().getLocation().add(0, 1.0, 0);
                attacker.getWorld().spawnParticle(Particle.SQUID_INK, tLoc, 8, 0.25, 0.35, 0.25, 0.03);
                attacker.getWorld().spawnParticle(Particle.CRIT, tLoc, 6, 0.2, 0.3, 0.2, 0.08);
            } catch (Exception ignored) {}
        }
    }

    private Entity getTargetEntity(Player player, double range) {
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        for (double d = 1.0; d <= range; d += 0.5) {
            Location p = eye.clone().add(dir.clone().multiply(d));
            for (Entity e : player.getWorld().getNearbyEntities(p, 1.2, 1.2, 1.2)) {
                if (e != player && e instanceof LivingEntity) return e;
            }
        }
        return null;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        activeGearSecond.remove(uuid);
        activeGear3.remove(uuid);
        activeGear4.remove(uuid);
        activeGear5.remove(uuid);
        activeMera.remove(uuid);
        activeOpe.remove(uuid);
        activePika.remove(uuid);
        activeGura.remove(uuid);
        resetScale(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        santoryuCooldown.remove(uuid);
        activeGearSecond.remove(uuid);
        activeGear3.remove(uuid);
        activeGear4.remove(uuid);
        activeGear5.remove(uuid);
        activeMera.remove(uuid);
        activeOpe.remove(uuid);
        activePika.remove(uuid);
        activeGura.remove(uuid);
        resetScale(event.getPlayer());
    }
}
