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
    private final Map<UUID, Long> maintenanceExpiries = new HashMap<>();
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

                boolean defaultPermanent = (tier <= 10 || tier % 10 == 0 || tier == 50);
                boolean permanent = sec.getBoolean(key + ".permanent", defaultPermanent);

                double costPercentage = plugin.getConfig().getDouble("settings.maintenance.cost-percentage", 0.05);
                double minCost = plugin.getConfig().getDouble("settings.maintenance.min-cost", 1000.0);
                double defaultMCost = Math.max(minCost, cost * costPercentage);
                double maintenanceCost = sec.getDouble(key + ".maintenance-cost", defaultMCost);

                Rank rank = new Rank(tier, key, displayName, division, cost, mat, perms, perks, rewardCommands,
                        hasPush, pushMult, pushCd, abilityType, particleType, permanent, maintenanceCost);
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
        maintenanceExpiries.clear();

        for (String uuidStr : playersConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                if (playersConfig.isConfigurationSection(uuidStr)) {
                    int tier = playersConfig.getInt(uuidStr + ".tier", 0);
                    boolean particles = playersConfig.getBoolean(uuidStr + ".particles", true);
                    boolean push = playersConfig.getBoolean(uuidStr + ".kinetic-push", true);
                    boolean abilities = playersConfig.getBoolean(uuidStr + ".abilities", true);
                    long expiry = playersConfig.getLong(uuidStr + ".maintenance-expiry", 0L);

                    playerTiers.put(uuid, tier);
                    playerSettings.put(uuid, new PlayerSettings(particles, push, abilities));
                    if (expiry > 0) {
                        maintenanceExpiries.put(uuid, expiry);
                    }
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
            Long expiry = maintenanceExpiries.get(entry.getKey());
            if (expiry != null && expiry > 0) {
                playersConfig.set(path + ".maintenance-expiry", expiry);
            } else {
                playersConfig.set(path + ".maintenance-expiry", null);
            }
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

    public Set<UUID> getAllRegisteredPlayerUuids() {
        return new HashSet<>(playerTiers.keySet());
    }

    // ==========================================
    // MANTENCIÓN Y DESGASTE (RANK DECAY & UPKEEP)
    // ==========================================

    public boolean isRankPermanent(int tier) {
        Rank r = ranksByTier.get(tier);
        if (r == null || tier <= 0) return true;
        return r.isPermanent();
    }

    public long getMaintenanceExpiry(UUID uuid) {
        int tier = getPlayerTier(uuid);
        if (tier <= 0 || isRankPermanent(tier)) {
            return -1L;
        }
        Long exp = maintenanceExpiries.get(uuid);
        if (exp == null || exp <= 0) {
            long periodDays = plugin.getConfig().getLong("settings.maintenance.period-days", 14L);
            long fresh = System.currentTimeMillis() + (periodDays * 86400000L);
            maintenanceExpiries.put(uuid, fresh);
            savePlayerData();
            return fresh;
        }
        return exp;
    }

    public long getRemainingMaintenanceMs(UUID uuid) {
        long exp = getMaintenanceExpiry(uuid);
        if (exp <= 0) return -1L;
        return Math.max(0L, exp - System.currentTimeMillis());
    }

    public void resetMaintenance(UUID uuid) {
        int tier = getPlayerTier(uuid);
        if (tier <= 0 || isRankPermanent(tier)) {
            maintenanceExpiries.remove(uuid);
            savePlayerData();
            return;
        }
        long periodDays = plugin.getConfig().getLong("settings.maintenance.period-days", 14L);
        long fresh = System.currentTimeMillis() + (periodDays * 86400000L);
        maintenanceExpiries.put(uuid, fresh);
        savePlayerData();
    }

    public boolean processMaintenance(Player player) {
        if (player == null || !player.isOnline()) return false;
        UUID uuid = player.getUniqueId();
        Rank current = getPlayerRank(uuid);
        if (current == null || current.getTier() <= 0) {
            player.sendMessage("§cNo tienes un rango activo que requiera mantención.");
            return false;
        }
        if (current.isPermanent()) {
            player.sendMessage("§aTu rango §b" + current.getDisplayName() + " §aes permanente (Ancla de División) y no requiere mantención.");
            return false;
        }

        double fee = current.getMaintenanceCost();
        Economy eco = plugin.getEconomy();
        if (eco != null) {
            double balance = eco.getBalance(player);
            if (balance < fee) {
                double missing = fee - balance;
                player.sendMessage("§cNo tienes fondos suficientes para alimentar tu Núcleo de Resonancia. Te faltan §e$" + MONEY_FORMAT.format(missing) + " Dragmas§c.");
                return false;
            }

            EconomyResponse resp = eco.withdrawPlayer(player, fee);
            if (!resp.transactionSuccess()) {
                player.sendMessage("§cError procesando el pago en Vault: " + resp.errorMessage);
                return false;
            }
        }

        long now = System.currentTimeMillis();
        long currentExp = getMaintenanceExpiry(uuid);
        long periodDays = plugin.getConfig().getLong("settings.maintenance.period-days", 14L);
        long periodMs = periodDays * 86400000L;
        long maxDays = plugin.getConfig().getLong("settings.maintenance.max-accumulated-days", 30L);
        long maxMs = maxDays * 86400000L;

        long base = (currentExp > now) ? currentExp : now;
        long newExp = Math.min(now + maxMs, base + periodMs);
        maintenanceExpiries.put(uuid, newExp);
        savePlayerData();

        long remainingMs = newExp - now;
        long days = remainingMs / 86400000L;
        long hours = (remainingMs % 86400000L) / 3600000L;

        player.sendMessage("§8§m--------------------------------------------------");
        player.sendMessage(" §6&l🏺 NÚCLEO DE RESONANCIA ALIMENTADO");
        player.sendMessage(" §7Has renovado la mantención de tu rango §b" + current.getDisplayName() + "§7.");
        player.sendMessage(" §7Tiempo de estabilidad restante: §a" + days + " días y " + hours + " horas§7.");
        player.sendMessage(" §7Costo abonado: §6$" + MONEY_FORMAT.format(fee) + " Dragmas§7.");
        player.sendMessage("§8§m--------------------------------------------------");

        try {
            player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 1.2f);
        } catch (Exception ignored) {}
        return true;
    }

    public int getLastCheckpointTier(int currentTier) {
        if (currentTier <= 10) return Math.min(currentTier, 10);
        if (currentTier <= 20) return 10;
        if (currentTier <= 30) return 20;
        if (currentTier <= 40) return 30;
        return 40;
    }

    public boolean checkDecay(UUID uuid, boolean notifyIfOnline) {
        if (!plugin.getConfig().getBoolean("settings.maintenance.enabled", true)) {
            return false;
        }
        int tier = getPlayerTier(uuid);
        if (tier <= 0 || isRankPermanent(tier)) {
            return false;
        }

        long exp = getMaintenanceExpiry(uuid);
        if (exp <= 0 || System.currentTimeMillis() <= exp) {
            return false;
        }

        Rank oldRank = ranksByTier.get(tier);
        String mode = plugin.getConfig().getString("settings.maintenance.decay-mode", "CHECKPOINT").toUpperCase();
        int targetTier;
        if ("RESET".equals(mode)) {
            targetTier = 0;
        } else if ("SINGLE_TIER".equals(mode)) {
            targetTier = Math.max(0, tier - 1);
        } else {
            targetTier = getLastCheckpointTier(tier);
        }

        setPlayerTier(uuid, targetTier);
        Rank newRank = ranksByTier.get(targetTier);

        if (targetTier <= 0 || isRankPermanent(targetTier)) {
            maintenanceExpiries.remove(uuid);
        } else {
            long periodDays = plugin.getConfig().getLong("settings.maintenance.period-days", 14L);
            maintenanceExpiries.put(uuid, System.currentTimeMillis() + (periodDays * 86400000L));
        }
        savePlayerData();

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            downgradeLuckPermsRank(player, oldRank, newRank);
            if (notifyIfOnline) {
                try {
                    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.6f);
                } catch (Exception ignored) {}
                player.sendTitle("§c§l¡COLAPSO DE RANGO!", "§7Tu rango ha decaído por falta de mantención.", 10, 60, 20);
                player.sendMessage("§c§m--------------------------------------------------");
                player.sendMessage(" §c§l⚠️ COLAPSO DE RANGO POR FALTA DE MANTENCIÓN");
                player.sendMessage(" §7Han pasado 14 días sin alimentar tu Núcleo de Resonancia.");
                player.sendMessage(" §7Tu rango ha descendido de §c" + (oldRank != null ? oldRank.getDisplayName() : "Tier " + tier)
                        + " §7a §b" + (newRank != null ? newRank.getDisplayName() : "§7Sin Rango") + "§7.");
                player.sendMessage(" §eAlimenta tu Núcleo en §f/rankup §epara mantener la estabilidad cósmica.");
                player.sendMessage("§c§m--------------------------------------------------");
            }
        }

        plugin.getLogger().warning("[DrakesRankup] Rank decay aplicado a UUID " + uuid + ": Tier " + tier + " -> Tier " + targetTier);
        return true;
    }

    public void checkWarning(Player player) {
        if (player == null || !player.isOnline()) return;
        UUID uuid = player.getUniqueId();
        int tier = getPlayerTier(uuid);
        if (tier <= 0 || isRankPermanent(tier)) return;

        long remainingMs = getRemainingMaintenanceMs(uuid);
        if (remainingMs <= 0) return;

        long hoursRemaining = remainingMs / 3600000L;
        long warningThreshold = plugin.getConfig().getLong("settings.maintenance.warning-threshold-hours", 72L);

        if (hoursRemaining <= warningThreshold) {
            long days = hoursRemaining / 24L;
            long h = hoursRemaining % 24L;
            Rank rank = getPlayerRank(uuid);
            String rankName = rank != null ? rank.getDisplayName() : "Rango";
            player.sendMessage("§e§l[DRAKES RANKUP] ⚠️ §7Tu rango " + rankName + " §7está perdiendo estabilidad (§c" + days + "d " + h + "h restantes§7).");
            player.sendMessage("§eUsa §f/rankup §eo §f/rankup mantener §epara recargar tu Núcleo de Resonancia antes del colapso.");
            try {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.2f);
            } catch (Exception ignored) {}
        }
    }

    private void downgradeLuckPermsRank(Player player, Rank oldRank, Rank newRank) {
        if (!Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) return;
        try {
            LuckPerms lp = LuckPermsProvider.get();
            User user = lp.getUserManager().getUser(player.getUniqueId());
            if (user == null) return;

            String groupPrefix = plugin.getConfig().getString("settings.luckperms.group-prefix", "rankup_");

            if (oldRank != null) {
                user.data().remove(InheritanceNode.builder(groupPrefix + oldRank.getId()).build());
                for (String perm : oldRank.getPermissions()) {
                    user.data().remove(PermissionNode.builder(perm).build());
                }
            }

            if (newRank != null) {
                if (plugin.getConfig().getBoolean("settings.luckperms.apply-group", true)) {
                    user.data().add(InheritanceNode.builder(groupPrefix + newRank.getId()).build());
                }
                for (String perm : newRank.getPermissions()) {
                    user.data().add(PermissionNode.builder(perm).build());
                }
            }

            lp.getUserManager().saveUser(user);
        } catch (Throwable t) {
            plugin.getLogger().warning("No se pudo sincronizar LuckPerms en downgrade: " + t.getMessage());
        }
    }

    // ==========================================
    // PROCESO DE ASCENSO (RANKUP)
    // ==========================================

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
        resetMaintenance(uuid); // Renovación automática a 14 días al subir de rango

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
            if (next != null) {
                double missing = next.getCost() - balance;
                player.sendMessage("§cNo tienes suficiente dinero para ascender al siguiente rango (" + next.getDisplayName() + "§c). Te faltan §e$" + MONEY_FORMAT.format(missing) + "§c.");
            }
            return 0;
        }

        if (eco != null && totalCost > 0) {
            EconomyResponse r = eco.withdrawPlayer(player, totalCost);
            if (!r.transactionSuccess()) {
                player.sendMessage(ChatColor.RED + "Error procesando el cobro en Vault: " + r.errorMessage);
                return 0;
            }
        }

        int previousTier = currentTier;
        setPlayerTier(uuid, targetTier);
        resetMaintenance(uuid); // Renovación automática a 14 días al subir de rango

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
