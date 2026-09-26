package com.drakescraft.rankup.command;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.gui.TransformationMenu;
import com.drakescraft.rankup.model.PlayerSettings;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TransformationCommand implements TabExecutor {

    private final DrakesRankupPlugin plugin;

    public TransformationCommand(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSolo jugadores pueden abrir el menú de transformaciones.");
            return true;
        }

        if (!plugin.isWorldAllowed(player.getWorld())) {
            player.sendMessage(plugin.getWorldBlockedMessage());
            return true;
        }

        if (args.length >= 1) {
            String sub = args[0].toLowerCase();
            PlayerSettings s = plugin.getRankManager().getPlayerSettings(player.getUniqueId());

            if (sub.equals("off") || sub.equals("desactivar") || sub.equals("stop") || sub.equals("disable")) {
                s.setKiFlightEnabled(false);
                plugin.getRankManager().savePlayerData();
                player.setFlySpeed(0.10f);
                if (!plugin.hasExternalFlight(player) && player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                    player.setFlying(false);
                    player.setAllowFlight(false);
                }
                player.sendMessage("§b[Ki] §cHas desactivado el vuelo y propulsión supersónica de Ki. Velocidad de vuelo restablecida.");
                return true;
            }

            if (sub.equals("on") || sub.equals("activar") || sub.equals("enable")) {
                int tier = plugin.getRankManager().getPlayerTier(player.getUniqueId());
                if (tier < 31 && !player.hasPermission("drakesrankup.staff") && !plugin.getStaffManager().isAngel(player.getUniqueId())) {
                    player.sendMessage("§c[Ki] Necesitas alcanzar el Tier 31 (Saiyajin) para usar el Vuelo de Ki.");
                    return true;
                }
                s.setKiFlightEnabled(true);
                plugin.getRankManager().savePlayerData();
                player.sendMessage("§b[Ki] §aHas activado el vuelo de Ki supersónico (W + Doble Salto).");
                return true;
            }

            if (sub.equals("toggle")) {
                boolean newState = !s.isKiFlightEnabled();
                if (newState) {
                    int tier = plugin.getRankManager().getPlayerTier(player.getUniqueId());
                    if (tier < 31 && !player.hasPermission("drakesrankup.staff") && !plugin.getStaffManager().isAngel(player.getUniqueId())) {
                        player.sendMessage("§c[Ki] Necesitas alcanzar el Tier 31 (Saiyajin) para usar el Vuelo de Ki.");
                        return true;
                    }
                }
                s.setKiFlightEnabled(newState);
                plugin.getRankManager().savePlayerData();
                player.setFlySpeed(0.10f);
                if (!newState && !plugin.hasExternalFlight(player) && player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                    player.setFlying(false);
                    player.setAllowFlight(false);
                }
                player.sendMessage("§b[Ki] §7Vuelo de Ki supersónico: " + (newState ? "§aActivado" : "§cDesactivado (Velocidad normal restaurada)"));
                return true;
            }
        }

        new TransformationMenu(plugin, player).open();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.addAll(Arrays.asList("off", "on", "toggle", "menu"));
        }
        return list;
    }
}
