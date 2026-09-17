package com.drakescraft.rankup.manager;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.Rank;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PermissionNode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

public class RankManager {

    private final DrakesRankupPlugin plugin;
    private final Map<Integer, Rank> ranksByTier = new TreeMap<>();
    private final Map<String, Rank> ranksById = new HashMap<>();
    private final Map<UUID, Integer> playerTiers = new HashMap<>();
    private File playersFile;
    private FileConfiguration playersConfig;
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###,###,###.##");

    public RankManager(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
        loadRanks();
        loadPlayerData();
    }

    public void loadRanks() {
        ranksByTier.clear();
        ranksById.clear();
        File ranksFile = new File(plugin.getDataFolder(), "ranks.yml");
        if (!ranksFile.exists()) {
            plugin.saveResource("ranks.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(ranksFile);
        ConfigurationSection sec = config.getConfigurationSection("ranks");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                int tier = sec.getInt(key + ".tier");
                String displayName = ChatColor.translateAlternateColorCodes('&', sec.getString(key + ".display-name", key));
                String division = ChatColor.translateAlternateColorCodes('&', sec.getString(key + ".division", "General"));
                double cost = sec.getDouble(key + ".cost", 0.0);
                String matName = sec.getString(key + ".icon", "STONE");
                Material mat = Material.matchMaterial(matName);
                if (mat == null) mat = Material.STONE;
                List<String> perms = sec.getStringList(key + ".permissions");
                List<String> perks = sec.getStringList(key + ".perks");

                Rank rank = new Rank(tier, key, displayName, division, cost, mat, perms, perks);
                ranksByTier.put(tier, rank);
                ranksById.put(key.toLowerCase(), rank);
            }
        }
        plugin.getLogger().info("Se han cargado " + ranksByTier.size() + " rangos de DrakesRankup exitosamente.");
    }

    private void loadPlayerData() {
        playersFile = new File(plugin.getDataFolder(), "players.yml");
        if (!playersFile.exists()) {
            try {
                playersFile.getParentFile().mkdirs();
                playersFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("No se pudo crear players.yml: " + e.getMessage());
            }
        }
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);
        for (String uuidStr : playersConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                playerTiers.put(uuid, playersConfig.getInt(uuidStr));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void savePlayerData() {
        if (playersConfig == null || playersFile == null) return;
        for (Map.Entry<UUID, Integer> entry : playerTiers.entrySet()) {
            playersConfig.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            playersConfig.save(playersFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error guardando players.yml: " + e.getMessage());
        }
    }

    public int getPlayerTier(UUID uuid) {
        return playerTiers.getOrDefault(uuid, 0);
    }

    public Rank getPlayerRank(UUID uuid) {
        int tier = getPlayerTier(uuid);
        return ranksByTier.get(tier);
    }

    public Rank getNextRank(UUID uuid) {
        int currentTier = getPlayerTier(uuid);
        return ranksByTier.get(currentTier + 1);
    }

    public void setPlayerTier(UUID uuid, int tier) {
        playerTiers.put(uuid, tier);
        savePlayerData();
    }

    public boolean processRankup(Player player) {
        UUID uuid = player.getUniqueId();
        Rank next = getNextRank(uuid);
        if (next == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.max-rank")));
            return false;
        }

        Economy eco = plugin.getEconomy();
        if (eco != null) {
            double balance = eco.getBalance(player);
            if (balance < next.getCost()) {
                double missing = next.getCost() - balance;
                String msg = plugin.getConfig().getString("messages.insufficient-funds")
                        .replace("{rank}", next.getDisplayName())
                        .replace("{missing}", MONEY_FORMAT.format(missing));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                return false;
            }

            EconomyResponse r = eco.withdrawPlayer(player, next.getCost());
            if (!r.transactionSuccess()) {
                player.sendMessage(ChatColor.RED + "Error procesando el cobro en Vault: " + r.errorMessage);
                return false;
            }
        }

        int previousTier = getPlayerTier(uuid);
        setPlayerTier(uuid, next.getTier());

        applyLuckPermsRank(player, previousTier, next);

        String successMsg = plugin.getConfig().getString("messages.success")
                .replace("{rank}", next.getDisplayName())
                .replace("{cost}", MONEY_FORMAT.format(next.getCost()));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', successMsg));

        if (plugin.getConfig().getBoolean("settings.title.enabled", true)) {
            String title = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("settings.title.title", "&6&l¡RANGO DESBLOQUEADO!"));
            String sub = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("settings.title.subtitle", "&eHas ascendido a &b{rank}").replace("{rank}", next.getDisplayName()));
            player.sendTitle(title, sub, 10, 50, 10);
        }

        if (plugin.getConfig().getBoolean("settings.sound.enabled", true)) {
            try {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            } catch (Exception ignored) {}
        }

        if (plugin.getConfig().getBoolean("settings.broadcast.enabled", true)) {
            List<String> broadcastLines = plugin.getConfig().getStringList("settings.broadcast.message");
            for (String line : broadcastLines) {
                String formatted = ChatColor.translateAlternateColorCodes('&', line
                        .replace("{player}", player.getName())
                        .replace("{rank}", next.getDisplayName())
                        .replace("{tier}", String.valueOf(next.getTier()))
                        .replace("{division}", next.getDivision()));
                Bukkit.broadcastMessage(formatted);
            }
        }

        return true;
    }

    private void applyLuckPermsRank(Player player, int prevTier, Rank newRank) {
        if (!Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) return;
        try {
            LuckPerms lp = LuckPermsProvider.get();
            User user = lp.getUserManager().getUser(player.getUniqueId());
            if (user == null) return;

            String groupPrefix = plugin.getConfig().getString("settings.luckperms.group-prefix", "rankup_");

            if (plugin.getConfig().getBoolean("settings.luckperms.remove-previous-rankup-group", true) && prevTier > 0) {
                Rank prev = ranksByTier.get(prevTier);
                if (prev != null) {
                    user.data().remove(InheritanceNode.builder(groupPrefix + prev.getId()).build());
                }
            }

            if (plugin.getConfig().getBoolean("settings.luckperms.apply-group", true)) {
                user.data().add(InheritanceNode.builder(groupPrefix + newRank.getId()).build());
            }

            for (String perm : newRank.getPermissions()) {
                user.data().add(PermissionNode.builder(perm).build());
            }

            lp.getUserManager().saveUser(user);
        } catch (Throwable t) {
            plugin.getLogger().warning("No se pudo sincronizar LuckPerms: " + t.getMessage());
        }
    }

    public Collection<Rank> getAllRanks() {
        return ranksByTier.values();
    }

    public Rank getRankByTier(int tier) {
        return ranksByTier.get(tier);
    }
}
