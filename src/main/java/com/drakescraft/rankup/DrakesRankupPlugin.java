package com.drakescraft.rankup;

import com.drakescraft.rankup.ability.DragonBallListener;
import com.drakescraft.rankup.ability.KineticPushListener;
import com.drakescraft.rankup.ability.RankAbilityListener;
import com.drakescraft.rankup.command.AngelCommand;
import com.drakescraft.rankup.command.RankupCommand;
import com.drakescraft.rankup.gui.RankupGuiListener;
import com.drakescraft.rankup.manager.RankManager;
import com.drakescraft.rankup.manager.StaffManager;
import com.drakescraft.rankup.papi.DrakesRankupExpansion;
import com.drakescraft.rankup.task.AuraTask;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

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
    private StaffManager staffManager;

    private AuraTask auraTask;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        setupEconomy();

        rankManager = new RankManager(this);
        staffManager = new StaffManager(this);

        RankupCommand cmd = new RankupCommand(this);
        if (getCommand("rankup") != null) {
            getCommand("rankup").setExecutor(cmd);
            getCommand("rankup").setTabCompleter(cmd);
        }

        AngelCommand angelCmd = new AngelCommand(this);
        if (getCommand("angel") != null) {
            getCommand("angel").setExecutor(angelCmd);
        }

        // Register event listeners
        kineticPushListener = new KineticPushListener(this);
        dragonBallListener = new DragonBallListener(this);

        Bukkit.getPluginManager().registerEvents(new RankupGuiListener(), this);
        Bukkit.getPluginManager().registerEvents(kineticPushListener, this);
        Bukkit.getPluginManager().registerEvents(new RankAbilityListener(this), this);
        Bukkit.getPluginManager().registerEvents(dragonBallListener, this);
        Bukkit.getPluginManager().registerEvents(staffManager, this);

        // Start aura particle task every second (20 ticks)
        auraTask = new AuraTask(this);
        auraTask.runTaskTimerAsynchronously(this, 20L, 20L);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new DrakesRankupExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion %drakesrankup_*% registrada.");
        }

        getLogger().info("DrakesRankup v" + getPluginMeta().getVersion() + " habilitado exitosamente. Motor de 50 Rangos Anime, Modo Ángel Staff, Ki y Empujes activo.");
    }

    @Override
    public void onDisable() {
        if (auraTask != null) {
            auraTask.cancel();
        }
        if (rankManager != null) {
            rankManager.savePlayerData();
        }
        getLogger().info("DrakesRankup deshabilitado limpiamente.");
        instance = null;
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
}
