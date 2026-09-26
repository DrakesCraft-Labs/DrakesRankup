package com.drakescraft.rankup.command;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.PlayerSettings;
import com.drakescraft.rankup.model.Rank;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RebirthCommand implements CommandExecutor, TabCompleter {

    private final DrakesRankupPlugin plugin;

    public RebirthCommand(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cEste comando solo puede ser ejecutado por un jugador.");
            return true;
        }

        if (!plugin.isWorldAllowed(player.getWorld())) {
            player.sendMessage(plugin.getWorldBlockedMessage());
            return true;
        }

        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(player.getUniqueId());
        int currentRebirth = settings.getRebirthCount();
        Rank currentRank = plugin.getRankManager().getPlayerRank(player.getUniqueId());
        int currentTier = currentRank != null ? currentRank.getTier() : 0;
        int maxTier = 100;

        if (args.length > 0 && args[0].equalsIgnoreCase("confirm")) {
            plugin.getRankManager().processRebirth(player);
            return true;
        }

        // Mostrar menú informativo de Rebirth
        player.sendMessage("§8§m--------------------------------------------------");
        player.sendMessage("  §6§lSISTEMA DE RENACIMIENTO (REBIRTH) · DRAKESRANKUP");
        player.sendMessage("§8§m--------------------------------------------------");
        player.sendMessage("§7Nivel de Rebirth actual: §e" + currentRebirth + " §8/ §650");
        player.sendMessage("§7Tu Rango actual: " + (currentRank != null ? currentRank.getDisplayName() : "§7Sin rango") + " §8(Tier " + currentTier + "/" + maxTier + ")");
        player.sendMessage("");
        player.sendMessage("§e§nBeneficios permanentes por Rebirth:§r");
        player.sendMessage(" §8▪ §fDaño físico y mágico: §a+" + (currentRebirth * 3) + "% §7(+3% por cada Rebirth)");
        player.sendMessage(" §8▪ §fReducción de enfriamiento: §b-" + Math.min(50, currentRebirth) + "% §7(-1% CDR por Rebirth)");
        player.sendMessage(" §8▪ §fDistintivo visual en chat, TAB y auras celestiales exclusivas.");
        player.sendMessage("");

        if (currentRebirth >= 50) {
            player.sendMessage("§6§l¡HAS ALCANZADO LA GLORIA MÁXIMA DE 50 REBIRTHS!");
        } else if (currentTier < maxTier) {
            player.sendMessage("§c✖ No cumples con los requisitos: Debes alcanzar el §6Tier " + maxTier + " §cpara renacer.");
            player.sendMessage("§7Al renacer, tu progreso de rangos se reiniciará a §aTier 1 (Senku)§7, pero tus");
            player.sendMessage("§7multiplicadores de daño y auras de Rebirth quedarán guardados para siempre.");
        } else {
            player.sendMessage("§a✔ ¡CUMPLES TODOS LOS REQUISITOS PARA RENACER!");
            player.sendMessage("§eEscribe §6/rebirth confirm §epara ejecutar el renacimiento supremo.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
        }
        player.sendMessage("§8§m--------------------------------------------------");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> sub = new ArrayList<>();
            if ("confirm".startsWith(args[0].toLowerCase())) sub.add("confirm");
            return sub;
        }
        return Collections.emptyList();
    }
}
