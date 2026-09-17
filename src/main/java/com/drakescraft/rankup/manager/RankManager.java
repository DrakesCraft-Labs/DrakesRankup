package com.drakescraft.rankup.manager;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.AbilityType;
import com.drakescraft.rankup.model.PlayerSettings;
import com.drakescraft.rankup.model.Rank;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PermissionNode;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
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
    private final Map<UUID, PlayerSettings> playerSettings = new HashMap<>();
    private final Set<UUID> activeTransactions = Collections.synchronizedSet(new HashSet<>());

    private FileConfiguration ranksConfig;
    private File playersFile;
    private FileConfiguration playersConfig;

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###,###,###.##");

    public RankManager(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
        loadRanks();
        loadPlayerData();
    }

    public void loadRanks() {
        File file = new File(plugin.getDataFolder(), "ranks.yml");
        if (!file.exists()) {
            plugin.saveResource("ranks.yml", false);
        }
        ranksConfig = YamlConfiguration.loadConfiguration(file);
        ranksByTier.clear();
        ranksById.clear();

        ConfigurationSection sec = ranksConfig.getConfigurationSection("ranks");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                int tier = sec.getInt(key + ".tier");
                String displayName = sec.getString(key + ".display-name", key);
                String division = sec.getString(key + ".division", "División Desconocida");
                double cost = sec.getDouble(key + ".cost", 0.0);
                String matName = sec.getString(key + ".icon", "STONE");
                Material mat = Material.matchMaterial(matName);
                if (mat == null) mat = Material.STONE;

                List<String> perms = sec.getStringList(key + ".permissions");
                List<String> perks = sec.getStringList(key + ".perks");
                List<String> rewardCommands = sec.getStringList(key + ".reward-commands");
                boolean hasPush = sec.getBoolean(key + ".kinetic-push", false);
                double pushMult = sec.getDouble(key + ".push-multiplier", 1.2);
                int pushCd = sec.getInt(key + ".push-cooldown", 5);

                String abName = sec.getString(key + ".ability", "NONE");
                AbilityType abilityType;
                try {
                    abilityType = AbilityType.valueOf(abName.toUpperCase());
                } catch (Exception e) {
                    abilityType = AbilityType.NONE;
                }

                String particleType = sec.getString(key + ".particle", "NONE");

                Rank rank = new Rank(tier, key, displayName, division, cost, mat, perms, perks, rewardCommands, hasPush, pushMult, pushCd, abilityType, particleType);
                ranksByTier.put(tier, rank);
                ranksById.put(key.toLowerCase(), rank);
            }
        }
        plugin.getLogger().info("Se han cargado " + ranksByTier.size() + " rangos de DrakesRankup exitosamente.");
    }

    public void loadPlayerData() {
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
        playerTiers.clear();
        playerSettings.clear();

        for (String uuidStr : playersConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                if (playersConfig.isConfigurationSection(uuidStr)) {
                    int tier = playersConfig.getInt(uuidStr + ".tier", 0);
                    boolean particles = playersConfig.getBoolean(uuidStr + ".particles", true);
                    boolean push = playersConfig.getBoolean(uuidStr + ".kinetic-push", true);
                    boolean abilities = playersConfig.getBoolean(uuidStr + ".abilities", true);
                    playerTiers.put(uuid, tier);
                    playerSettings.put(uuid, new PlayerSettings(particles, push, abilities));
                } else {
                    int tier = playersConfig.getInt(uuidStr, 0);
                    playerTiers.put(uuid, tier);
                    playerSettings.put(uuid, new PlayerSettings());
                }
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public synchronized void savePlayerData() {
        if (playersConfig == null || playersFile == null) return;
        for (Map.Entry<UUID, Integer> entry : playerTiers.entrySet()) {
            String path = entry.getKey().toString();
            playersConfig.set(path + ".tier", entry.getValue());
            PlayerSettings s = getPlayerSettings(entry.getKey());
            playersConfig.set(path + ".particles", s.isParticlesEnabled());
            playersConfig.set(path + ".kinetic-push", s.isKineticPushEnabled());
            playersConfig.set(path + ".abilities", s.isAbilitiesEnabled());
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
        Player online = Bukkit.getPlayer(uuid);
        if (online != null && plugin.getKineticPushListener() != null) {
            plugin.getKineticPushListener().updatePushEligibility(online);
        }
    }

    public PlayerSettings getPlayerSettings(UUID uuid) {
        return playerSettings.computeIfAbsent(uuid, k -> new PlayerSettings());
    }

    public boolean processRankup(Player player) {
        if (player == null || !player.isOnline()) return false;
        UUID uuid = player.getUniqueId();
        if (!activeTransactions.add(uuid)) {
            return false;
        }
        try {
            return doProcessRankup(player);
        } finally {
            activeTransactions.remove(uuid);
        }
    }

    private boolean doProcessRankup(Player player) {
        UUID uuid = player.getUniqueId();
        Rank next = getNextRank(uuid);
        if (next == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.max-rank", "&aYa has alcanzado el rango máximo.")));
            return false;
        }

        Economy eco = plugin.getEconomy();
        if (eco != null) {
            double balance = eco.getBalance(player);
            if (balance < next.getCost()) {
                double missing = next.getCost() - balance;
                String msg = plugin.getConfig().getString("messages.insufficient-funds", "&cTe faltan ${missing}")
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
        dispatchRewards(player, next);

        if (plugin.getKineticPushListener() != null) {
            plugin.getKineticPushListener().updatePushEligibility(player);
        }

        String successMsg = plugin.getConfig().getString("messages.success", "&a¡Has ascendido a {rank}!")
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

    public int processRankupMax(Player player) {
        if (player == null || !player.isOnline()) return 0;
        UUID uuid = player.getUniqueId();
        if (!activeTransactions.add(uuid)) {
            return 0;
        }
        try {
            return doProcessRankupMax(player);
        } finally {
            activeTransactions.remove(uuid);
        }
    }

    private int doProcessRankupMax(Player player) {
        UUID uuid = player.getUniqueId();
        int currentTier = getPlayerTier(uuid);
        if (currentTier >= 50) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.max-rank", "&aYa has alcanzado el rango máximo.")));
            return 0;
        }

        Economy eco = plugin.getEconomy();
        double balance = eco != null ? eco.getBalance(player) : Double.MAX_VALUE;

        int targetTier = currentTier;
        double totalCost = 0.0;

        for (int t = currentTier + 1; t <= 50; t++) {
            Rank r = ranksByTier.get(t);
            if (r == null) break;
            if (totalCost + r.getCost() <= balance) {
                totalCost += r.getCost();
                targetTier = t;
            } else {
                break;
            }
        }

        if (targetTier == currentTier) {
            Rank next = ranksByTier.get(currentTier + 1);
            double missing = next != null ? next.getCost() - balance : 0.0;
            String msg = plugin.getConfig().getString("messages.insufficient-funds", "&cTe faltan ${missing}")
                    .replace("{rank}", next != null ? next.getDisplayName() : "")
                    .replace("{missing}", MONEY_FORMAT.format(missing));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
            return 0;
        }

        if (eco != null && totalCost > 0) {
            EconomyResponse r = eco.withdrawPlayer(player, totalCost);
            if (!r.transactionSuccess()) {
                player.sendMessage(ChatColor.RED + "Error procesando cobro acumulado en Vault: " + r.errorMessage);
                return 0;
            }
        }

        int previousTier = currentTier;
        setPlayerTier(uuid, targetTier);
        Rank finalRank = ranksByTier.get(targetTier);

        applyLuckPermsRank(player, previousTier, finalRank);

        for (int t = previousTier + 1; t <= targetTier; t++) {
            Rank r = ranksByTier.get(t);
            if (r != null) {
                dispatchRewards(player, r);
            }
        }

        if (plugin.getKineticPushListener() != null) {
            plugin.getKineticPushListener().updatePushEligibility(player);
        }

        int ranksGained = targetTier - previousTier;
        player.sendMessage("§a§l¡ASCENSO MULTIPLE COMPLETADO! §eHas ascendido §6" + ranksGained + " §erangos hasta §b" + (finalRank != null ? finalRank.getDisplayName() : "") + "§e por un total de §a$" + MONEY_FORMAT.format(totalCost) + "§e.");
        try {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        } catch (Exception ignored) {}

        return ranksGained;
    }

    private void dispatchRewards(Player player, Rank rank) {
        if (rank.getRewardCommands() == null || rank.getRewardCommands().isEmpty()) return;
        for (String cmd : rank.getRewardCommands()) {
            if (cmd != null && !cmd.trim().isEmpty()) {
                String formattedCmd = cmd.replace("{player}", player.getName());
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCmd);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error ejecutando comando de recompensa: " + formattedCmd + " (" + e.getMessage() + ")");
                }
            }
        }
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

    public Rank getRankById(String id) {
        return ranksById.get(id != null ? id.toLowerCase() : "");
    }
}
