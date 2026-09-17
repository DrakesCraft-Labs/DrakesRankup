package com.drakescraft.rankup.listener;

import com.drakescraft.rankup.DrakesRankupPlugin;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class RankDecayListener implements Listener {

    private final DrakesRankupPlugin plugin;

    public RankDecayListener(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // 1. Revisar si su rango expiró mientras estaba desconectado
        plugin.getRankManager().checkDecay(player.getUniqueId(), true);

        // 2. Avisar con gracia si le quedan pocas horas de estabilidad (solo si está en un mundo custom permitido)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && plugin.isWorldAllowed(player.getWorld())) {
                plugin.getRankManager().checkWarning(player);
            }
        }, 40L); // 2 segundos tras conectar
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isWorldAllowed(player.getWorld())) {
            // El jugador ingresó a un mundo deshabilitado (ej. clasico)
            // 1. Revertir transformaciones de anime activas
            if (plugin.getDragonBallListener() != null) {
                plugin.getDragonBallListener().revertTransformations(player);
            }
            // 2. Desactivar vuelo en supervivencia si no es admin ni staff ángel
            if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
                if (!player.hasPermission("drakesrankup.admin") &&
                        (plugin.getStaffManager() == null || !plugin.getStaffManager().isAngel(player.getUniqueId()))) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                }
            }
        } else {
            // Si regresa a un mundo custom, recordar estabilidad si está próximo al vencimiento
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && plugin.isWorldAllowed(player.getWorld())) {
                    plugin.getRankManager().checkWarning(player);
                }
            }, 20L);
        }
    }
}
