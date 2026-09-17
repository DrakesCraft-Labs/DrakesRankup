package com.drakescraft.rankup;

import com.drakescraft.rankup.command.RankupCommand;
import com.drakescraft.rankup.gui.RankupMenu;
import com.drakescraft.rankup.manager.RankManager;
import com.drakescraft.rankup.papi.DrakesRankupExpansion;
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

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        setupEconomy();

        rankManager = new RankManager(this);

        RankupCommand cmd = new RankupCommand(this);
        if (getCommand("rankup") != null) {
            getCommand("rankup").setExecutor(cmd);
        }
        Bukkit.getPluginManager().registerEvents(new RankupMenu(this, null, 0), this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new DrakesRankupExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion %drakesrankup_*% registrada.");
        }

        getLogger().info("DrakesRankup v" + getPluginMeta().getVersion() + " habilitado con exito. 50 Rangos Anime activos.");
    }

    @Override
    public void onDisable() {
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
