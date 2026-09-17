package com.drakescraft.rankup.manager;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.Rank;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import java.util.*;

public class StaffManager implements Listener {

    private final DrakesRankupPlugin plugin;
    private final Set<UUID> angelPlayers = new HashSet<>();
    private final Map<UUID, Integer> originalTiers = new HashMap<>();

    public StaffManager(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isAngel(UUID uuid) {
        return angelPlayers.contains(uuid);
    }

    public boolean toggleAngel(Player player) {
        UUID uuid = player.getUniqueId();
        if (angelPlayers.contains(uuid)) {
            angelPlayers.remove(uuid);
            player.setInvulnerable(false);
            player.sendMessage("§c✦ [STAFF] Modo Ángel / Zeno-Sama §cDESACTIVADO.");
            player.sendTitle("§cMODO ÁNGEL OFF", "§7Has regresado al estado mortal", 5, 30, 5);
            return false;
        } else {
            angelPlayers.add(uuid);
            player.setInvulnerable(true);
            player.setAllowFlight(true);
            player.setFlying(true);
            player.sendMessage("§b✦ [STAFF] Modo Ángel / Zeno-Sama §aACTIVADO§b. Invulnerabilidad total y Ultra Instinto Angelical infinito.");
            player.sendTitle("§b✦ MODO ÁNGEL ✦", "§eWhis / Daishinkan · Invulnerable & Evasión 100%", 10, 50, 10);
            try {
                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1.0, 0), 40, 0.5, 0.8, 0.5, 0.2);
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.5f);
            } catch (Exception ignored) {}
            return true;
        }
    }

    public boolean setTestTier(Player player, int targetTier) {
        UUID uuid = player.getUniqueId();
        int currentTier = plugin.getRankManager().getPlayerTier(uuid);
        originalTiers.putIfAbsent(uuid, currentTier);

        Rank rank = plugin.getRankManager().getRankByTier(targetTier);
        if (rank == null) return false;

        plugin.getRankManager().setPlayerTier(uuid, targetTier);
        player.sendMessage("§a✦ [Staff Test] Has cambiado temporalmente al rango: §eNivel " + targetTier + " §6(" + rank.getDisplayName() + "§6)§a.");
        player.sendMessage("§7Usa §e/rankup test reset §7para volver a tu nivel original (" + originalTiers.get(uuid) + ").");
        try {
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.8f);
            player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation().add(0, 1.0, 0), 20, 0.3, 0.4, 0.3, 0.05);
        } catch (Exception ignored) {}
        return true;
    }

    public boolean resetTestTier(Player player) {
        UUID uuid = player.getUniqueId();
        Integer original = originalTiers.remove(uuid);
        if (original == null) {
            player.sendMessage("§c[Staff Test] No estabas en ningún modo de prueba.");
            return false;
        }

        plugin.getRankManager().setPlayerTier(uuid, original);
        Rank rank = plugin.getRankManager().getRankByTier(original);
        player.sendMessage("§a✦ [Staff Test] Tu rango original ha sido restaurado: §eNivel " + original + " (" + (rank != null ? rank.getDisplayName() : "Sin Rango") + "§e).");
        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAngelDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isAngel(player.getUniqueId())) return;

        event.setCancelled(true);
        executeAngelDodge(player, null);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAngelDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isAngel(player.getUniqueId())) return;

        event.setCancelled(true);

        if (event.getDamager() instanceof Projectile proj) {
            try {
                // Deflect or vaporize projectile in slow motion / flash
                Vector reversed = proj.getVelocity().multiply(-0.8);
                proj.setVelocity(reversed);
                proj.getWorld().spawnParticle(Particle.FLASH, proj.getLocation(), 1);
            } catch (Exception ignored) {}
        }

        executeAngelDodge(player, event.getDamager() instanceof Player ? (Player) event.getDamager() : null);
    }

    private void executeAngelDodge(Player player, Player attacker) {
        Location loc = player.getLocation();

        // Calculate smooth evasion vector (sideways or backwards)
        Vector dir = loc.getDirection().normalize();
        Vector side = new Vector(-dir.getZ(), 0, dir.getX()).normalize().multiply(0.75);

        Location target = loc.clone().add(side);
        if (target.getBlock().isPassable()) {
            player.teleport(target);
        }

        try {
            // High-speed afterimage celestial particles
            loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 1.0, 0), 12, 0.2, 0.5, 0.2, 0.05);
            loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0, 0.5, 0), 15, 0.3, 0.3, 0.3, 0.1);
            loc.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(0, 0.8, 0), 10, 0.2, 0.2, 0.2, 0.05);

            player.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 2.0f);
            player.playSound(loc, Sound.ITEM_ARMOR_EQUIP_ELYTRA, 0.8f, 1.8f);

            player.sendActionBar(Component.text("§b✦ ¡ESQUIVE ANGELICAL ABSOLUTO! §7(Doctrina de los Ángeles)"));
        } catch (Exception ignored) {}
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        angelPlayers.remove(uuid);
        Integer original = originalTiers.remove(uuid);
        if (original != null) {
            plugin.getRankManager().setPlayerTier(uuid, original);
        }
    }
}
