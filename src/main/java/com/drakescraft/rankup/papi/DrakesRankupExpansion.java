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
            default:
                return null;
        }
    }
}
