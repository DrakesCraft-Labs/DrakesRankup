package com.drakescraft.rankup.command;

import com.drakescraft.rankup.DrakesRankupPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AngelCommand implements CommandExecutor {

    private final DrakesRankupPlugin plugin;

    public AngelCommand(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSolo jugadores en el servidor pueden usar el modo Ángel.");
            return true;
        }

        if (!player.hasPermission("drakesrankup.staff") && !player.hasPermission("drakesrankup.admin")) {
            player.sendMessage("§cNo tienes autorización celestial para activar el modo Ángel.");
            return true;
        }

        if (!plugin.isWorldAllowed(player.getWorld())) {
            player.sendMessage(plugin.getWorldBlockedMessage());
            return true;
        }

        plugin.getStaffManager().toggleAngel(player);
        return true;
    }
}
