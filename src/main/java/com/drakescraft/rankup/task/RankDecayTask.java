package com.drakescraft.rankup.task;

import com.drakescraft.rankup.DrakesRankupPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class RankDecayTask extends BukkitRunnable {

    private final DrakesRankupPlugin plugin;

    public RankDecayTask(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("settings.maintenance.enabled", true)) {
            return;
        }

        // 1. Revisar jugadores online
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            plugin.getRankManager().checkDecay(uuid, true);
            plugin.getRankManager().checkWarning(player);
        }

        // 2. Revisar jugadores offline registrados en players.yml
        for (UUID uuid : plugin.getRankManager().getAllRegisteredPlayerUuids()) {
            if (Bukkit.getPlayer(uuid) == null) {
                plugin.getRankManager().checkDecay(uuid, false);
            }
        }
    }
}
