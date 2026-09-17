package com.drakescraft.rankup.command;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.gui.RankupMenu;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RankupCommand implements CommandExecutor {

    private final DrakesRankupPlugin plugin;

    public RankupCommand(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("drakesrankup.admin")) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.no-permission")));
                    return true;
                }
                plugin.reloadConfig();
                plugin.getRankManager().loadRanks();
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.reload-success")));
                return true;
            }

            if (args[0].equalsIgnoreCase("set")) {
                if (!sender.hasPermission("drakesrankup.admin")) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.no-permission")));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Uso: /rankup set <jugador> <tier (0-50)>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
                    return true;
                }
                try {
                    int tier = Integer.parseInt(args[2]);
                    plugin.getRankManager().setPlayerTier(target.getUniqueId(), tier);
                    sender.sendMessage(ChatColor.GREEN + "Rango de " + target.getName() + " establecido en nivel " + tier + ".");
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "El nivel debe ser un número entero.");
                }
                return true;
            }
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser ejecutado por jugadores.");
            return true;
        }

        new RankupMenu(plugin, player, 0).open();
        return true;
    }
}
