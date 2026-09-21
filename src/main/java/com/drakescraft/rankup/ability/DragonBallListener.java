package com.drakescraft.rankup.ability;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.AbilityType;
import com.drakescraft.rankup.model.PlayerSettings;
import com.drakescraft.rankup.model.Rank;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Color;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
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
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class DragonBallListener implements Listener {

    private final DrakesRankupPlugin plugin;

    // Active transformations: UUID -> expiration timestamp in millis
    private final Map<UUID, Long> activeMUI = new HashMap<>();
    private final Map<UUID, Long> activeUltraEgo = new HashMap<>();
    private final Map<UUID, Long> activeSSJGod = new HashMap<>();
    private final Map<UUID, Long> activeSSJBlue = new HashMap<>();
    private final Map<UUID, Long> activeGohanBeast = new HashMap<>();
    private final Map<UUID, Long> activeBroly = new HashMap<>();

    // Cooldowns: UUID -> available timestamp in millis
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Long> spiritSwordCooldown = new HashMap<>();
    private final Map<UUID, Long> kiFlightCooldown = new HashMap<>();
    private final Map<UUID, Long> kiFlightImmunity = new HashMap<>();

    // Overuse tracking: UUID -> list of activation timestamps in last 120 seconds
    private final Map<UUID, List<Long>> muiUsageHistory = new HashMap<>();

    // Charging tasks: UUID -> task
    private final Map<UUID, BukkitTask> chargingTasks = new HashMap<>();

    public DragonBallListener(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isMuiActive(UUID uuid) {
        Long exp = activeMUI.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    public boolean isUltraEgoActive(UUID uuid) {
        Long exp = activeUltraEgo.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    public boolean isSSJGodActive(UUID uuid) {
        Long exp = activeSSJGod.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    public boolean isSSJBlueActive(UUID uuid) {
        Long exp = activeSSJBlue.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    public boolean isGohanBeastActive(UUID uuid) {
        Long exp = activeGohanBeast.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    public boolean isBrolyActive(UUID uuid) {
        Long exp = activeBroly.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    public void revertTransformations(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        activeMUI.remove(uuid);
        activeUltraEgo.remove(uuid);
        activeSSJGod.remove(uuid);
        activeSSJBlue.remove(uuid);
        activeGohanBeast.remove(uuid);
        activeBroly.remove(uuid);
        BukkitTask task = chargingTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isWorldAllowed(player.getWorld())) return;
        UUID uuid = player.getUniqueId();

        // If stopped sneaking, cancel any charging
        if (!event.isSneaking()) {
            BukkitTask task = chargingTasks.remove(uuid);
            if (task != null) {
                task.cancel();
                player.sendActionBar(Component.text("§7Concentración de Ki interrumpida."));
            }
            return;
        }

        if (!player.isOnGround()) return;

        Rank rank = plugin.getRankManager().getPlayerRank(uuid);
        if (rank == null) return;
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(uuid);
        if (!settings.isAbilitiesEnabled()) return;

        int tier = rank.getTier();
        AbilityType ability = rank.getAbilityType();
        String equipped = settings.getActiveTransformation();

        // Check if player has access to any transformation
        boolean hasGod = (tier >= 35 || ability == AbilityType.SSJ_GOD);
        boolean hasBlue = (tier >= 36 || ability == AbilityType.SSJ_BLUE);
        boolean hasMUI = (tier >= 46 || ability == AbilityType.MASTERED_ULTRA_INSTINCT || ability == AbilityType.ULTRA_INSTINCT);
        boolean hasEgo = (tier >= 47 || ability == AbilityType.ULTRA_EGO || ability == AbilityType.HAKAI_AURA);
        boolean hasGohan = (tier >= 45 || ability == AbilityType.GOHAN_BEAST);
        boolean hasBroly = (tier >= 34 || ability == AbilityType.BROLY_LSSJ);
        boolean isKami = (tier >= 50);

        if (!hasGod && !hasBlue && !hasMUI && !hasEgo && !hasGohan && !hasBroly && !isKami && (equipped == null || equipped.isEmpty())) {
            return;
        }

        // Determine transformation type based on equipped setting or highest unlocked
        String transName;
        long cdSeconds;
        Particle kiParticle;

        if (equipped != null && !equipped.equalsIgnoreCase("NINGUNA")) {
            transName = equipped.toUpperCase();
            if (transName.equals("GOHAN_BEAST")) {
                cdSeconds = 40;
                kiParticle = Particle.CRIMSON_SPORE;
            } else if (transName.equals("BROLY_LSSJ")) {
                cdSeconds = 45;
                kiParticle = Particle.HAPPY_VILLAGER;
            } else if (transName.equals("MUI")) {
                cdSeconds = 40;
                kiParticle = Particle.END_ROD;
            } else if (transName.equals("ULTRA_EGO")) {
                cdSeconds = 50;
                kiParticle = Particle.WITCH;
            } else if (transName.equals("SSJ_BLUE")) {
                cdSeconds = 45;
                kiParticle = Particle.SOUL_FIRE_FLAME;
            } else if (transName.equals("SSJ_GOD")) {
                cdSeconds = 35;
                kiParticle = Particle.FLAME;
            } else {
                return; // Handled by other listeners (e.g. OnePiece)
            }
        } else {
            // Default based on rank
            if (hasMUI) {
                transName = "MUI";
                cdSeconds = 40;
                kiParticle = Particle.END_ROD;
            } else if (hasEgo) {
                transName = "ULTRA_EGO";
                cdSeconds = 50;
                kiParticle = Particle.WITCH;
            } else if (hasGohan) {
                transName = "GOHAN_BEAST";
                cdSeconds = 40;
                kiParticle = Particle.CRIMSON_SPORE;
            } else if (hasBlue) {
                transName = "SSJ_BLUE";
                cdSeconds = 45;
                kiParticle = Particle.SOUL_FIRE_FLAME;
            } else if (hasGod) {
                transName = "SSJ_GOD";
                cdSeconds = 35;
                kiParticle = Particle.FLAME;
            } else if (hasBroly) {
                transName = "BROLY_LSSJ";
                cdSeconds = 45;
                kiParticle = Particle.HAPPY_VILLAGER;
            } else {
                return;
            }
        }

        long now = System.currentTimeMillis();
        long nextReady = cooldowns.getOrDefault(uuid, 0L);
        if (now < nextReady) {
            long remaining = Math.max(1, (nextReady - now) / 1000L);
            player.sendActionBar(Component.text("§c⏳ Ki en reposo: §e" + remaining + "s"));
            return;
        }

        // Start Ki charging runnable (runs every 4 ticks for 36 ticks total = 1.8 seconds)
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !player.isSneaking()) {
                    chargingTasks.remove(uuid);
                    cancel();
                    return;
                }

                ticks += 4;
                float progress = Math.min(1.0f, ticks / 36.0f);
                int bars = (int) (progress * 10);
                StringBuilder barStr = new StringBuilder("§e⚡ Ki: §6[");
                for (int b = 0; b < 10; b++) {
                    if (b < bars) barStr.append("▮");
                    else barStr.append("§7▯");
                }
                barStr.append("§6]");
                player.sendActionBar(Component.text(barStr.toString()));

                Location loc = player.getLocation();
                try {
                    player.getWorld().spawnParticle(kiParticle, loc.clone().add(0, 0.2, 0), 10, 0.3, 0.2, 0.3, 0.05);
                    player.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 0.5f, 0.8f + (progress * 1.2f));
                } catch (Exception ignored) {}

                if (ticks >= 36) {
                    chargingTasks.remove(uuid);
                    cancel();
                    detonateTransformation(player, transName, cdSeconds);
                }
            }
        }.runTaskTimer(plugin, 0L, 4L);

        chargingTasks.put(uuid, task);
    }

    private void detonateTransformation(Player player, String type, long cdSeconds) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        cooldowns.put(uuid, now + (cdSeconds * 1000L));

        Location loc = player.getLocation();

        try {
            player.getWorld().spawnParticle(Particle.EXPLOSION, loc.clone().add(0, 1.0, 0), 2);
            player.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.4f);
        } catch (Exception ignored) {}

        if (type.equals("MUI")) {
            activeMUI.put(uuid, now + 5000L); // 5 seconds of total dodge
            playMuiActivationFx(player); // animacion cinematografica de despertar
            player.sendTitle("§f§lDOCTRINA EGOÍSTA", "§bUltra Instinto Dominado activado", 5, 40, 5);
            player.sendActionBar(Component.text("§f⚡ ¡EVASIÓN TOTAL ACTIVADA POR 5 SEGUNDOS!"));

            List<Long> history = muiUsageHistory.computeIfAbsent(uuid, k -> new ArrayList<>());
            history.removeIf(t -> now - t > 120_000L);
            history.add(now);

            final boolean willFatigue = history.size() >= 2;

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                activeMUI.remove(uuid);
                if (player.isOnline()) {
                    if (willFatigue) {
                        applyMuiFatigue(player);
                    } else {
                        player.sendMessage("§7[Rankup] El estado de Ultra Instinto ha concluido.");
                    }
                }
            }, 100L);

        } else if (type.equals("ULTRA_EGO")) {
            activeUltraEgo.put(uuid, now + 15000L); // 15 seconds
            playAwakeningBurst(player, Color.fromRGB(160,60,220), Color.fromRGB(90,20,140), Particle.WITCH, Sound.ENTITY_ELDER_GUARDIAN_CURSE);
            player.sendTitle("§5§lMEGA INSTINTO (ULTRA EGO)", "§dEl daño recibido aumenta tu poder de destrucción", 5, 40, 5);
            player.sendActionBar(Component.text("§5⚡ ¡AURA HAKAI ACTIVA POR 15 SEGUNDOS!"));

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                activeUltraEgo.remove(uuid);
                if (player.isOnline()) {
                    player.sendMessage("§5[Rankup] El Mega Instinto se ha disipado.");
                }
            }, 300L);

        } else if (type.equals("GOHAN_BEAST")) {
            activeGohanBeast.put(uuid, now + 20000L); // 20 seconds
            playAwakeningBurst(player, Color.fromRGB(245,245,255), Color.fromRGB(230,70,160), Particle.ELECTRIC_SPARK, Sound.ENTITY_RAVAGER_ROAR);
            player.sendTitle("§d§lGOHAN BESTIA (BEAST)", "§f¡Furia desatada! Explosión crítica al máximo", 5, 40, 5);
            player.sendActionBar(Component.text("§d⚡ ¡EXPLOSIÓN CRÍTICA +75% ACTIVADA POR 20S!"));

            // Negative side effect: severe metabolic hunger burn
            try {
                player.setFoodLevel(Math.max(2, player.getFoodLevel() - 8));
                player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 200, 1));
            } catch (Exception ignored) {}
            player.sendMessage("§c[Gohan Beast] §7El esfuerzo bestial ha consumido tu energía vital (Hambre aumentada).");

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                activeGohanBeast.remove(uuid);
                if (player.isOnline()) {
                    player.sendMessage("§d[Rankup] La forma Bestia de Gohan ha concluido.");
                }
            }, 400L);

        } else if (type.equals("BROLY_LSSJ")) {
            activeBroly.put(uuid, now + 20000L); // 20 seconds
            playAwakeningBurst(player, Color.fromRGB(120,230,80), Color.fromRGB(60,150,40), Particle.HAPPY_VILLAGER, Sound.ENTITY_WARDEN_ROAR);
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 400, 1)); // Fuerza II (berserker equilibrado)
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 400, 1)); // Resistance II
            player.sendTitle("§a§lBROLY BERSERKER (LSSJ)", "§2Furia destructiva incontrolable", 5, 40, 5);
            player.sendActionBar(Component.text("§a⚡ ¡FUERZA III & RESISTENCIA II ACTIVAS!"));

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                activeBroly.remove(uuid);
                if (player.isOnline()) {
                    // Negative side effect: Slowness post-rage
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 0));
                    player.sendMessage("§c[Broly LSSJ] §7La cólera berserker se disipa: sufres agotamiento muscular (Lentitud).");
                }
            }, 400L);

        } else if (type.equals("SSJ_BLUE")) {
            activeSSJBlue.put(uuid, now + 20000L);
            playAwakeningBurst(player, Color.fromRGB(70,150,255), Color.fromRGB(150,230,255), Particle.SOUL_FIRE_FLAME, Sound.ENTITY_ENDER_DRAGON_GROWL);
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 400, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 400, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 400, 0)); // Resistencia I: temple divino
            player.sendTitle("§9§lSUPER SAIYAJIN BLUE", "§bFuerza, velocidad y temple de los dioses", 5, 40, 5);
            player.sendActionBar(Component.text("§9⚡ ¡Fuerza I · Velocidad II · Resistencia I por 20s!"));

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                activeSSJBlue.remove(uuid);
                if (player.isOnline()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 0)); // coste: ki agotado
                    player.sendMessage("§9[Rankup] El Super Saiyajin Blue se disipa: ki agotado (Lentitud).");
                }
            }, 400L);

        } else {
            activeSSJGod.put(uuid, now + 20000L);
            playAwakeningBurst(player, Color.fromRGB(255,80,80), Color.fromRGB(255,200,90), Particle.FLAME, Sound.ENTITY_LIGHTNING_BOLT_THUNDER);
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 400, 1)); // Regen II: gracia divina
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 400, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 400, 1)); // Absorcion II: escudo de ki
            player.sendTitle("§c§lSUPER SAIYAJIN GOD", "§eKi divino de sanación y gracia", 5, 40, 5);
            player.sendActionBar(Component.text("§c⚡ ¡Regeneración II · Absorción · Velocidad por 20s!"));

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                activeSSJGod.remove(uuid);
                if (player.isOnline()) {
                    player.sendMessage("§c[Rankup] El ki divino del Super Saiyajin God se ha disipado.");
                }
            }, 400L);
        }
    }

    private void applyMuiFatigue(Player player) {
        Location loc = player.getLocation();
        try {
            player.getWorld().spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 1.0, 0), 30, 0.4, 0.5, 0.4, 0.08);
            player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, loc.clone().add(0, 1.0, 0), 15, 0.3, 0.4, 0.3, 0.05);
            player.getWorld().playSound(loc, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 0.6f);
            player.getWorld().playSound(loc, Sound.BLOCK_ANVIL_LAND, 0.8f, 0.5f);
        } catch (Exception ignored) {}

        // Desgaste de hasta 4 corazones, pero NUNCA letal (deja minimo medio corazon).
        double desgaste = Math.min(8.0, Math.max(0.0, player.getHealth() - 1.0));
        if (desgaste > 0) player.damage(desgaste);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2)); // Level III for 5s
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 50, 0)); // 2.5s

        player.sendTitle("§c§l¡EL CUERPO PASÓ FACTURA!", "§eEl límite divino desgarró tus músculos", 10, 60, 15);
        player.sendMessage("§c§l¡EL CUERPO HA PASADO FACTURA! §7El desgaste del Ultra Instinto supera el límite de un mortal.");
    }

    // ==========================================
    // ESPADA DE HAZ DE LUZ DE VEGETTO
    // ==========================================
    @EventHandler
    public void onVegettoSwordInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!plugin.isWorldAllowed(player.getWorld())) return;
        if (!player.isSneaking()) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR || !item.getType().name().endsWith("_SWORD")) return;

        UUID uuid = player.getUniqueId();
        Rank rank = plugin.getRankManager().getPlayerRank(uuid);
        int tier = (rank != null) ? rank.getTier() : 0;
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(uuid);
        String equipped = settings.getActiveTransformation();

        boolean hasVegetto = (tier >= 48 || "VEGETTO_SWORD".equalsIgnoreCase(equipped));
        if (!hasVegetto) return;

        long now = System.currentTimeMillis();
        long ready = spiritSwordCooldown.getOrDefault(uuid, 0L);
        if (now < ready) {
            long rem = Math.max(1, (ready - now) / 1000L);
            player.sendActionBar(Component.text("§c⏳ Espada de Luz en recarga: §e" + rem + "s"));
            return;
        }

        spiritSwordCooldown.put(uuid, now + 12000L); // 12s cooldown

        // Launch piercing beam of light
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();

        try {
            player.getWorld().playSound(eye, Sound.ITEM_TRIDENT_THUNDER, 1.0f, 1.6f);
            player.getWorld().playSound(eye, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.6f);
        } catch (Exception ignored) {}

        Set<LivingEntity> damaged = new HashSet<>();
        for (double d = 1.0; d <= 14.0; d += 0.5) {
            Location point = eye.clone().add(dir.clone().multiply(d));
            if (!point.getBlock().isPassable()) break;

            try {
                point.getWorld().spawnParticle(Particle.END_ROD, point, 2, 0.05, 0.05, 0.05, 0.01);
                point.getWorld().spawnParticle(Particle.SWEEP_ATTACK, point, 1, 0, 0, 0, 0);
            } catch (Exception ignored) {}

            for (LivingEntity entity : point.getWorld().getNearbyLivingEntities(point, 1.3)) {
                if (entity.equals(player)) continue;
                if (!damaged.contains(entity)) {
                    damaged.add(entity);
                    try {
                        entity.damage(14.0, player); // 7 hearts piercing damage
                        entity.getWorld().spawnParticle(Particle.FLASH, entity.getLocation().add(0, 1.0, 0), 1);
                    } catch (Exception ignored) {}
                }
            }
        }

        player.sendActionBar(Component.text("§e⚡ ¡ESPADA DE HAZ DE LUZ DE VEGETTO! §7(Perforación colosal)"));
    }

    // ==========================================
    // VUELO DE KI SUPERSÓNICO
    // ==========================================
    @EventHandler
    public void onKiFlightToggle(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        if (!plugin.isWorldAllowed(player.getWorld())) return;

        UUID uuid = player.getUniqueId();
        Rank rank = plugin.getRankManager().getPlayerRank(uuid);
        int tier = (rank != null) ? rank.getTier() : 0;
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(uuid);

        if (tier < 31 || !settings.isKiFlightEnabled()) return;

        // Si el jugador tiene vuelo real (rango/Essentials/staff), el doble-salto es para
        // VOLAR: no lo convertimos en dash. Sin esto, el vuelo de rango era inusable.
        if (plugin.hasExternalFlight(player)) return;

        event.setCancelled(true);
        player.setFlying(false);

        long now = System.currentTimeMillis();
        long ready = kiFlightCooldown.getOrDefault(uuid, 0L);
        if (now < ready) {
            long rem = Math.max(1, (ready - now) / 1000L);
            player.sendActionBar(Component.text("§c⏳ Vuelo de Ki en enfriamiento: §e" + rem + "s"));
            return;
        }

        kiFlightCooldown.put(uuid, now + 30000L); // 30s cooldown
        kiFlightImmunity.put(uuid, now + 6000L);  // 6s fall damage immunity

        // Supersonic directional dash
        Vector dir = player.getLocation().getDirection().normalize().multiply(2.2).setY(0.45);
        player.setVelocity(dir);
        player.setFallDistance(0);

        Location loc = player.getLocation();
        try {
            loc.getWorld().spawnParticle(Particle.SONIC_BOOM, loc, 1);
            loc.getWorld().spawnParticle(Particle.FIREWORK, loc, 25, 0.3, 0.3, 0.3, 0.1);
            loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.2f);
            loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.8f);
        } catch (Exception ignored) {}

        player.sendTitle("§b§l¡VUELO DE KI!", "§eImpulso sónico desatado", 0, 30, 10);
        player.sendActionBar(Component.text("§b⚡ ¡VUELO DE KI SUPERSÓNICO DESATADO! §7(Recarga: 30s)"));
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        if (!plugin.isWorldAllowed(player.getWorld())) return;

        UUID uuid = player.getUniqueId();
        Rank rank = plugin.getRankManager().getPlayerRank(uuid);
        int tier = (rank != null) ? rank.getTier() : 0;
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(uuid);

        if (tier >= 31 && settings.isKiFlightEnabled() && !plugin.hasExternalFlight(player)) {
            // Con vuelo externo NO armamos el doble-salto del dash: pisaria el allowFlight
            // que Essentials mantiene para el fly de rango y lo apagaria en cada paso.
            long now = System.currentTimeMillis();
            long ready = kiFlightCooldown.getOrDefault(uuid, 0L);
            if (player.isOnGround() && now >= ready) {
                if (!player.getAllowFlight()) {
                    player.setAllowFlight(true);
                }
            } else if (now < ready && player.getAllowFlight()) {
                player.setAllowFlight(false);
            }
        }
    }

    // ==========================================
    // COMBATE & DAÑO
    // ==========================================
    @EventHandler(priority = EventPriority.LOWEST)
    public void onIncomingDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!plugin.isWorldAllowed(player.getWorld())) return;
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // Ki Flight Fall Immunity
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (now < kiFlightImmunity.getOrDefault(uuid, 0L)) {
                event.setCancelled(true);
                return;
            }
        }

        // Esquiva total durante Ultra Instinto Dominado -- SOLO ataques de combate.
        // (No ignora lava, void, caida ni ahogo: el Ultra Instinto esquiva golpes, no el entorno.)
        if (isMuiActive(uuid)) {
            switch (event.getCause()) {
                case ENTITY_ATTACK, ENTITY_SWEEP_ATTACK, PROJECTILE, MAGIC, ENTITY_EXPLOSION, THORNS -> {
                    event.setCancelled(true);
                    triggerMuiAfterimage(player);
                    return;
                }
                default -> { /* el dano ambiental si afecta */ }
            }
        }

        // Broly takes +20% damage from projectiles (tradeoff)
        if (isBrolyActive(uuid) && event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
            event.setDamage(event.getDamage() * 1.20);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onCombatDamage(EntityDamageByEntityEvent event) {
        if (!plugin.isWorldAllowed(event.getEntity().getWorld())) return;
        // Attacker is Gohan Beast (+75% critical burst)
        if (event.getDamager() instanceof Player attacker) {
            if (!plugin.isWorldAllowed(attacker.getWorld())) return;
            if (isGohanBeastActive(attacker.getUniqueId())) {
                event.setDamage(event.getDamage() * 1.75);
                try {
                    Location loc = event.getEntity().getLocation().add(0, 1.0, 0);
                    attacker.getWorld().spawnParticle(Particle.CRIMSON_SPORE, loc, 25, 0.3, 0.5, 0.3, 0.1);
                    attacker.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 15, 0.3, 0.5, 0.3, 0.1);
                    attacker.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.8f);
                } catch (Exception ignored) {}
            }
        }

        // Attacker is Ultra Ego Vegeta
        if (event.getDamager() instanceof Player attacker) {
            if (isUltraEgoActive(attacker.getUniqueId())) {
                double maxHp = 20.0;
                try {
                    if (attacker.getAttribute(Attribute.MAX_HEALTH) != null) {
                        maxHp = attacker.getAttribute(Attribute.MAX_HEALTH).getValue();
                    }
                } catch (Throwable ignored) {}

                double missingFraction = Math.max(0.0, Math.min(0.9, (maxHp - attacker.getHealth()) / maxHp));
                double multiplier = 1.0 + (missingFraction * 0.60); // up to +60% damage
                event.setDamage(event.getDamage() * multiplier);

                try {
                    Location loc = event.getEntity().getLocation().add(0, 1.0, 0);
                    attacker.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 15, 0.2, 0.3, 0.2, 0.05);
                    attacker.getWorld().spawnParticle(Particle.CRIT, loc, 10, 0.2, 0.2, 0.2, 0.1);
                    attacker.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.9f, 1.6f);
                } catch (Exception ignored) {}
            }
        }

        // Victim is Gohan Beast -- furia temeraria: recibe +20% de dano (glass cannon con riesgo real)
        if (event.getEntity() instanceof Player gVictim && isGohanBeastActive(gVictim.getUniqueId())) {
            event.setDamage(event.getDamage() * 1.20);
        }

        // Victim is Ultra Ego Vegeta
        if (event.getEntity() instanceof Player victim) {
            if (isUltraEgoActive(victim.getUniqueId())) {
                if (event.getDamager() instanceof LivingEntity damager) {
                    try {
                        damager.damage(3.5, victim);
                        Location loc = damager.getLocation().add(0, 1.0, 0);
                        damager.getWorld().spawnParticle(Particle.WITCH, loc, 15, 0.2, 0.3, 0.2, 0.05);
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    // Animacion cinematografica del despertar de Ultra Instinto (plata fluida):
    // FASE 1 implosion -> FASE 2 estallido con pilar y onda -> FASE 3 aura en doble helice.
    // Estallido cinematografico de despertar (reusable): pilar de luz tematico + onda de choque + rugido.
    private void playAwakeningBurst(Player player, Color c1, Color c2, Particle accent, Sound roar) {
        Location loc = player.getLocation();
        var w = player.getWorld();
        Particle.DustOptions d1 = new Particle.DustOptions(c1, 1.4f);
        Particle.DustOptions d2 = new Particle.DustOptions(c2, 1.1f);
        try {
            w.spawnParticle(Particle.FLASH, loc.clone().add(0, 1, 0), 1);
            w.spawnParticle(Particle.EXPLOSION_EMITTER, loc.clone().add(0, 1, 0), 1);
            for (double dy = 0; dy < 5.5; dy += 0.25) {
                w.spawnParticle(accent, loc.clone().add(0, dy, 0), 1, 0.08, 0, 0.08, 0.01);
                w.spawnParticle(Particle.DUST, loc.clone().add(0, dy, 0), 1, 0.15, 0, 0.15, 0, d1);
            }
            for (int i = 0; i < 40; i++) {
                double a = (Math.PI * 2 / 40) * i;
                w.spawnParticle(Particle.DUST, loc.clone().add(Math.cos(a) * 1.8, 0.2, Math.sin(a) * 1.8), 1, 0, 0, 0, 0, d2);
            }
            w.playSound(loc, roar, 1.0f, 1.0f);
            w.playSound(loc, Sound.ITEM_TRIDENT_THUNDER, 0.6f, 1.5f);
        } catch (Exception ignored) {}
    }

    private void playMuiActivationFx(Player player) {
        final Particle.DustOptions PLATA = new Particle.DustOptions(Color.fromRGB(232, 236, 245), 1.3f);
        final Particle.DustOptions CELESTE = new Particle.DustOptions(Color.fromRGB(150, 210, 255), 1.1f);
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (!player.isOnline()) { cancel(); return; }
                Location loc = player.getLocation();
                var w = player.getWorld();
                try {
                    if (t < 10) {
                        // FASE 1 - anillo plateado que colapsa hacia el jugador
                        double radius = 3.0 * (1.0 - t / 10.0) + 0.3;
                        double y = 0.1 + (t / 10.0) * 1.3;
                        for (int i = 0; i < 14; i++) {
                            double ang = (Math.PI * 2 / 14) * i + t * 0.35;
                            double x = Math.cos(ang) * radius, z = Math.sin(ang) * radius;
                            w.spawnParticle(Particle.DUST, loc.clone().add(x, y, z), 1, 0, 0, 0, 0, PLATA);
                            if (t % 2 == 0) w.spawnParticle(Particle.END_ROD, loc.clone().add(x, y, z), 1, 0, 0, 0, 0.01);
                        }
                        if (t % 3 == 0) w.playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 0.7f, 0.6f + t * 0.04f);
                    } else if (t == 10) {
                        // FASE 2 - despertar: flash, pilar de luz y onda de choque
                        w.spawnParticle(Particle.FLASH, loc.clone().add(0, 1, 0), 2);
                        w.spawnParticle(Particle.EXPLOSION_EMITTER, loc.clone().add(0, 1, 0), 1);
                        for (double dy = 0; dy < 6.5; dy += 0.22)
                            w.spawnParticle(Particle.END_ROD, loc.clone().add(0, dy, 0), 2, 0.09, 0, 0.09, 0.0);
                        for (int i = 0; i < 44; i++) {
                            double ang = (Math.PI * 2 / 44) * i;
                            w.spawnParticle(Particle.SWEEP_ATTACK, loc.clone().add(Math.cos(ang) * 1.6, 0.2, Math.sin(ang) * 1.6), 1);
                        }
                        w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.6f);
                        w.playSound(loc, Sound.ITEM_TRIDENT_THUNDER, 0.8f, 1.9f);
                        w.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.35f, 2.0f);
                    } else {
                        // FASE 3 - aura sostenida en doble helice + chispas y ceniza flotante
                        double phase = t * 0.55;
                        double yy = ((t - 11) % 34) / 34.0 * 2.4;
                        for (int s = 0; s < 2; s++) {
                            double ang = phase + Math.PI * s;
                            double x = Math.cos(ang) * 0.95, z = Math.sin(ang) * 0.95;
                            w.spawnParticle(Particle.DUST, loc.clone().add(x, yy, z), 1, 0, 0, 0, 0, s == 0 ? PLATA : CELESTE);
                        }
                        if (t % 5 == 0) w.spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0, 1.0, 0), 3, 0.4, 0.6, 0.4, 0.02);
                        if (t % 9 == 0) w.spawnParticle(Particle.WHITE_ASH, loc.clone().add(0, 1.3, 0), 6, 0.5, 0.9, 0.5, 0.01);
                    }
                } catch (Exception ignored) {}
                if (t++ >= 118) cancel(); // ~6s: cubre los 5s de MUI
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void triggerMuiAfterimage(Player player) {
        Location loc = player.getLocation();

        // Micro-teleport 0.8 blocks backward/sideways
        Vector back = loc.getDirection().normalize().multiply(-0.8).setY(0);
        Location target = loc.clone().add(back);
        if (target.getBlock().isPassable() && target.clone().add(0, 1, 0).getBlock().isPassable()) {
            player.teleport(target); // paso lateral solo si pies Y cabeza quedan libres (evita sofocacion)
        }

        try {
            loc.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0, 0.8, 0), 12, 0.2, 0.4, 0.2, 0.02);
            loc.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(0, 1.0, 0), 15, 0.2, 0.3, 0.2, 0.05);
            loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.8f);
            player.sendActionBar(Component.text("§f⚡ ¡ESQUIVE INSTINTIVO! §7(Doctrina Egoísta)"));
        } catch (Exception ignored) {}
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        BukkitTask task = chargingTasks.remove(uuid);
        if (task != null) task.cancel();
        activeMUI.remove(uuid);
        activeUltraEgo.remove(uuid);
        activeSSJGod.remove(uuid);
        activeSSJBlue.remove(uuid);
        activeGohanBeast.remove(uuid);
        activeBroly.remove(uuid);
        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        BukkitTask task = chargingTasks.remove(uuid);
        if (task != null) task.cancel();
        activeMUI.remove(uuid);
        activeUltraEgo.remove(uuid);
        activeSSJGod.remove(uuid);
        activeSSJBlue.remove(uuid);
        activeGohanBeast.remove(uuid);
        activeBroly.remove(uuid);
        cooldowns.remove(uuid);
        spiritSwordCooldown.remove(uuid);
        kiFlightCooldown.remove(uuid);
        kiFlightImmunity.remove(uuid);
    }
}
