package com.drakescraft.rankup;

import com.drakescraft.rankup.ability.SeriousPunchHandler;

import com.drakescraft.rankup.ability.DragonBallListener;
import com.drakescraft.rankup.ability.KineticPushListener;
import com.drakescraft.rankup.ability.OnePieceListener;
import com.drakescraft.rankup.ability.RankAbilityListener;
import com.drakescraft.rankup.ability.SpecialAbilitiesListener;
import com.drakescraft.rankup.command.AngelCommand;
import com.drakescraft.rankup.command.RankupCommand;
import com.drakescraft.rankup.command.TransformationCommand;
import com.drakescraft.rankup.gui.RankupGuiListener;
import com.drakescraft.rankup.gui.TransformationGuiListener;
import com.drakescraft.rankup.listener.RankDecayListener;
import com.drakescraft.rankup.manager.RankManager;
import com.drakescraft.rankup.manager.StaffManager;
import com.drakescraft.rankup.manager.KitManager;
import com.drakescraft.rankup.ability.CustomKitItemListener;
import com.drakescraft.rankup.papi.DrakesRankupExpansion;
import com.drakescraft.rankup.task.AuraTask;
import com.drakescraft.rankup.task.RankDecayTask;
import com.drakescraft.rankup.command.RebirthCommand;
import com.drakescraft.rankup.protection.ProtectionGate;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class DrakesRankupPlugin extends JavaPlugin {

    @Getter
    private static DrakesRankupPlugin instance;
    @Getter
    private RankManager rankManager;
    @Getter
    private Economy economy;
    @Getter
    private KineticPushListener kineticPushListener;
    @Getter
    private DragonBallListener dragonBallListener;
    @Getter
    private OnePieceListener onePieceListener;
    @Getter
    private SpecialAbilitiesListener specialAbilitiesListener;
    @Getter
    private SeriousPunchHandler seriousPunchHandler;

    @Getter
    private StaffManager staffManager;
    @Getter
    private KitManager kitManager;
    @Getter
    private ProtectionGate protectionGate;

    private AuraTask auraTask;
    private RankDecayTask rankDecayTask;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        setupEconomy();

        rankManager = new RankManager(this);
        staffManager = new StaffManager(this);
        kitManager = new KitManager(this);
        protectionGate = new ProtectionGate(this);

        RankupCommand cmd = new RankupCommand(this);
        if (getCommand("rankup") != null) {
            getCommand("rankup").setExecutor(cmd);
            getCommand("rankup").setTabCompleter(cmd);
        }

        RebirthCommand rebirthCmd = new RebirthCommand(this);
        if (getCommand("rebirth") != null) {
            getCommand("rebirth").setExecutor(rebirthCmd);
            getCommand("rebirth").setTabCompleter(rebirthCmd);
        }

        AngelCommand angelCmd = new AngelCommand(this);
        if (getCommand("angel") != null) {
            getCommand("angel").setExecutor(angelCmd);
        }

        TransformationCommand transCmd = new TransformationCommand(this);
        if (getCommand("transform") != null) {
            getCommand("transform").setExecutor(transCmd);
            getCommand("transform").setTabCompleter(transCmd);
        }

        // Register event listeners
        kineticPushListener = new KineticPushListener(this);
        dragonBallListener = new DragonBallListener(this);
        onePieceListener = new OnePieceListener(this);
        specialAbilitiesListener = new SpecialAbilitiesListener(this);
        seriousPunchHandler = new SeriousPunchHandler(this);
        Bukkit.getPluginManager().registerEvents(seriousPunchHandler, this);


        Bukkit.getPluginManager().registerEvents(new RankupGuiListener(), this);
        Bukkit.getPluginManager().registerEvents(new TransformationGuiListener(), this);
        Bukkit.getPluginManager().registerEvents(kineticPushListener, this);
        Bukkit.getPluginManager().registerEvents(new RankAbilityListener(this), this);
        Bukkit.getPluginManager().registerEvents(dragonBallListener, this);
        Bukkit.getPluginManager().registerEvents(onePieceListener, this);
        Bukkit.getPluginManager().registerEvents(specialAbilitiesListener, this);
        Bukkit.getPluginManager().registerEvents(staffManager, this);
        Bukkit.getPluginManager().registerEvents(new CustomKitItemListener(this), this);
        Bukkit.getPluginManager().registerEvents(new RankDecayListener(this), this);

        // Start aura particle task every second (20 ticks)
        auraTask = new AuraTask(this);
        auraTask.runTaskTimerAsynchronously(this, 20L, 20L);

        // Start periodic rank decay check task
        if (getConfig().getBoolean("settings.maintenance.enabled", true)) {
            rankDecayTask = new RankDecayTask(this);
            long intervalMinutes = getConfig().getLong("settings.maintenance.check-interval-minutes", 30L);
            long intervalTicks = Math.max(1200L, intervalMinutes * 60L * 20L);
            rankDecayTask.runTaskTimer(this, 200L, intervalTicks);
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new DrakesRankupExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion %drakesrankup_*% registrada.");
        }

        getLogger().info("DrakesRankup v" + getPluginMeta().getVersion() + " habilitado exitosamente. Motor de 50 Rangos Anime, Sistema de Mantención/Decay, GUI de Transformaciones, Ki y Staff Celestial activo.");
    }

    @Override
    public void onDisable() {
        if (auraTask != null) {
            auraTask.cancel();
        }
        if (rankDecayTask != null) {
            rankDecayTask.cancel();
        }
        if (seriousPunchHandler != null) {
            seriousPunchHandler.restoreAllPending();
        }
        if (rankManager != null) {
            rankManager.savePlayerData();
        }
        getLogger().info("DrakesRankup deshabilitado limpiamente.");
        instance = null;
    }

    public boolean isWorldAllowed(World world) {
        if (world == null) return true;
        if (!getConfig().getBoolean("settings.world-filter.enabled", true)) {
            return true;
        }
        String mode = getConfig().getString("settings.world-filter.mode", "BLACKLIST").toUpperCase();
        String worldName = world.getName().toLowerCase();

        if ("WHITELIST".equals(mode)) {
            List<String> enabledWorlds = getConfig().getStringList("settings.world-filter.enabled-worlds");
            for (String w : enabledWorlds) {
                if (w.equalsIgnoreCase(worldName)) return true;
            }
            return false;
        } else {
            // Default: BLACKLIST
            List<String> disabledWorlds = getConfig().getStringList("settings.world-filter.disabled-worlds");
            for (String w : disabledWorlds) {
                if (w.equalsIgnoreCase(worldName)) return false;
            }
            return true;
        }
    }

    public String getWorldBlockedMessage() {
        return ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("settings.world-filter.blocked-message",
                        "&c[DrakesCraft] El sistema de rangos y transformaciones es exclusivo de las modalidades custom (Survival SF, OneBlock y SkyBlock). ¡En Survival Clásico no está disponible!"));
    }

    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault no encontrado. El cobro por subida de rango estara desactivado.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
            getLogger().info("Vault Economy enganchado exitosamente (" + economy.getName() + ").");
        }
    }

    /**
     * True si el jugador tiene un vuelo LEGITIMO ajeno a las habilidades de impulso:
     * staff/angel, vuelo ya activo, o fly de Essentials (el que se otorga a rangos).
     *
     * Es el punto unico que decide "este doble-salto es para VOLAR, no para lanzar un
     * impulso". Antes solo lo consultaba KineticPush; DragonBall (vuelo de Ki, tier 31+)
     * no lo miraba, asi que secuestraba el vuelo de rango aunque el jugador desactivara
     * el empuje cinetico -- el "desactivo uno y queda otro" que reporto Mr_Em1lio.
     *
     * Se comprueba tambien el permiso essentials.fly ademas del estado isFlyModeEnabled:
     * el estado solo es true DESPUES de /fly, pero quien tiene el permiso por su rango
     * tiene derecho a volar y su doble-salto no debe convertirse en un dash.
     */
    public boolean hasExternalFlight(org.bukkit.entity.Player player) {
        if (player == null || !player.isOnline()) return false;
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) return true;
        if (player.hasPermission("drakesrankup.admin") || player.hasPermission("essentials.fly")
                || player.hasPermission("cmi.command.fly") || player.hasPermission("bentobox.island.fly")
                || player.hasPermission("bskyblock.island.fly") || player.hasPermission("tempfly.fly")) {
            return true;
        }
        if (getStaffManager() != null && getStaffManager().isAngel(player.getUniqueId())) return true;

        // BentoBox / Isla check
        try {
            org.bukkit.plugin.Plugin bb = getServer().getPluginManager().getPlugin("BentoBox");
            if (bb != null && bb.isEnabled()) {
                if (player.hasPermission("bskyblock.island.fly") || player.hasPermission("bentobox.island.fly")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}

        // Essentials check
        try {
            org.bukkit.plugin.Plugin ess = getServer().getPluginManager().getPlugin("Essentials");
            if (ess != null && ess.isEnabled()) {
                Object user = ess.getClass().getMethod("getUser", org.bukkit.entity.Player.class).invoke(ess, player);
                if (user != null && Boolean.TRUE.equals(user.getClass().getMethod("isFlyModeEnabled").invoke(user))) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}

        // Slimefun / Custom items check en el inventario (Infinity Matrix, Flight Gem, etc.)
        try {
            String pUuidStr = player.getUniqueId().toString();
            for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.hasItemMeta()) {
                    if (item.getItemMeta().hasDisplayName()) {
                        String name = org.bukkit.ChatColor.stripColor(item.getItemMeta().getDisplayName()).toUpperCase();
                        if (name.contains("FLIGHT") || name.contains("VUELO") || name.contains("MATRIX")) {
                            return true;
                        }
                    }
                    if (item.getItemMeta().hasLore()) {
                        java.util.List<String> lore = item.getItemMeta().getLore();
                        if (lore != null) {
                            for (String line : lore) {
                                String stripped = org.bukkit.ChatColor.stripColor(line).trim().toUpperCase();
                                if (stripped.startsWith("UUID:") && stripped.substring(5).trim().equalsIgnoreCase(pUuidStr)) {
                                    return true;
                                }
                                if (stripped.contains("FLIGHT") || stripped.contains("VUELO") || stripped.contains("INFINITY FLIGHT")) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        return false;
    }

}
