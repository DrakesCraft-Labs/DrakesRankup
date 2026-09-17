package com.drakescraft.rankup.listener;

import com.drakescraft.rankup.DrakesRankupPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
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

        // 2. Avisar con gracia si le quedan pocas horas de estabilidad
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getRankManager().checkWarning(player);
            }
        }, 40L); // 2 segundos tras conectar
    }
}
