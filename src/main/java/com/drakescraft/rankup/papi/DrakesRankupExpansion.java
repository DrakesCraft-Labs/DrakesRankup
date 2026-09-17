package com.drakescraft.rankup.papi;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.Rank;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;

public class DrakesRankupExpansion extends PlaceholderExpansion {

    private final DrakesRankupPlugin plugin;
    private static final DecimalFormat DF = new DecimalFormat("#,###,###,###.##");

    public DrakesRankupExpansion(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "drakesrankup";
    }

    @Override
    public @NotNull String getAuthor() {
        return "DrakesCraft-Labs";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        int currentTier = plugin.getRankManager().getPlayerTier(player.getUniqueId());
        Rank currentRank = plugin.getRankManager().getPlayerRank(player.getUniqueId());
        Rank nextRank = plugin.getRankManager().getNextRank(player.getUniqueId());

        switch (params.toLowerCase()) {
            case "tier":
                return String.valueOf(currentTier);
            case "rank":
            case "display":
                return currentRank != null ? currentRank.getDisplayName() : "Sin Rango";
            case "division":
                return currentRank != null ? currentRank.getDivision() : "Ninguna";
            case "next_rank":
                return nextRank != null ? nextRank.getDisplayName() : "Maximo";
            case "next_cost":
                return nextRank != null ? DF.format(nextRank.getCost()) : "0";
            case "progress":
                return String.valueOf((currentTier * 100) / 50);
            case "tag":
                return currentRank != null ? "&8[" + currentRank.getDisplayName() + "&8]" : "";
            case "permanent":
                return currentRank != null ? String.valueOf(currentRank.isPermanent()) : "true";
            case "maintenance_cost":
                return (currentRank != null && !currentRank.isPermanent()) ? DF.format(currentRank.getMaintenanceCost()) : "0";
            case "maintenance_status": {
                if (currentRank == null || currentRank.getTier() <= 0) return "NINGUNO";
                if (currentRank.isPermanent()) return "PERMANENTE";
                long remMs = plugin.getRankManager().getRemainingMaintenanceMs(player.getUniqueId());
                long days = remMs / 86400000L;
                if (days >= 3) return "ÓPTIMO";
                if (days >= 1) return "INESTABLE";
                return "CRÍTICO";
            }
            case "maintenance_time": {
                if (currentRank == null || currentRank.getTier() <= 0) return "N/A";
                if (currentRank.isPermanent()) return "Permanente";
                long remMs = plugin.getRankManager().getRemainingMaintenanceMs(player.getUniqueId());
                long days = remMs / 86400000L;
                long hours = (remMs % 86400000L) / 3600000L;
                return days + "d " + hours + "h";
            }
            default:
                return null;
        }
    }
}
