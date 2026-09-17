package com.drakescraft.rankup.command;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.gui.RankupMenu;
import com.drakescraft.rankup.model.PlayerSettings;
import com.drakescraft.rankup.model.Rank;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RankupCommand implements CommandExecutor, TabCompleter {

    private final DrakesRankupPlugin plugin;
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###,###,###.##");

    public RankupCommand(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cSolo los jugadores pueden ejecutar /rankup directamente.");
                return true;
            }
            plugin.getRankManager().processRankup(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("gui") || sub.equals("menu") || sub.equals("ranks") || sub.equals("ver")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cSolo los jugadores pueden abrir el menú visual.");
                return true;
            }
            int page = 0;
            if (args.length >= 2) {
                try {
                    page = Integer.parseInt(args[1]) - 1;
                } catch (NumberFormatException ignored) {}
            }
            new RankupMenu(plugin, player, page).open();
            return true;
        }

        if (sub.equals("kit") || sub.equals("reclamarkit")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cSolo jugadores en el servidor pueden reclamar kits.");
                return true;
            }
            plugin.getKitManager().claimKit(player);
            return true;
        }

        if (sub.equals("max")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cSolo los jugadores pueden ejecutar /rankup max.");
                return true;
            }
            plugin.getRankManager().processRankupMax(player);
            return true;
        }

        if (sub.equals("toggle")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cSolo jugadores.");
                return true;
            }
            PlayerSettings s = plugin.getRankManager().getPlayerSettings(player.getUniqueId());
            String target = args.length >= 2 ? args[1].toLowerCase() : "empuje";

            if (target.contains("part")) {
                s.setParticlesEnabled(!s.isParticlesEnabled());
                player.sendMessage("§b[Rankup] §7Partículas y Auras: " + (s.isParticlesEnabled() ? "§aActivadas" : "§cDesactivadas"));
            } else if (target.contains("hab") || target.contains("abil")) {
                s.setAbilitiesEnabled(!s.isAbilitiesEnabled());
                player.sendMessage("§b[Rankup] §7Habilidades Pasivas: " + (s.isAbilitiesEnabled() ? "§aActivadas" : "§cDesactivadas"));
            } else {
                s.setKineticPushEnabled(!s.isKineticPushEnabled());
                player.sendMessage("§b[Rankup] §7Empuje Cinético: " + (s.isKineticPushEnabled() ? "§aActivado" : "§cDesactivado"));
                if (plugin.getKineticPushListener() != null) {
                    plugin.getKineticPushListener().updatePushEligibility(player);
                }
            }
            plugin.getRankManager().savePlayerData();
            return true;
        }

        if (sub.equals("test")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cSolo jugadores en el servidor pueden usar el modo de pruebas.");
                return true;
            }
            if (!player.hasPermission("drakesrankup.staff") && !player.hasPermission("drakesrankup.admin")) {
                sender.sendMessage("§cNo tienes permiso de staff para el modo de pruebas de rangos.");
                return true;
            }

            if (args.length >= 2 && args[1].equalsIgnoreCase("reset")) {
                plugin.getStaffManager().resetTestTier(player);
                return true;
            }

            if (args.length >= 2) {
                int targetTier = -1;
                try {
                    targetTier = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    Rank r = plugin.getRankManager().getRankById(args[1]);
                    if (r != null) targetTier = r.getTier();
                }

                if (targetTier >= 1 && targetTier <= 50) {
                    plugin.getStaffManager().setTestTier(player, targetTier);
                    return true;
                }
            }

            player.sendMessage("§cUso: /rankup test <1-50 | id_rango> §7o §e/rankup test reset");
            return true;
        }

        if (sub.equals("info")) {
            Player targetPlayer = (sender instanceof Player p) ? p : null;
            if (args.length >= 2) {
                Player specified = Bukkit.getPlayer(args[1]);
                if (specified != null) targetPlayer = specified;
            }
            if (targetPlayer == null) {
                sender.sendMessage("§cJugador no encontrado.");
                return true;
            }
            int tier = plugin.getRankManager().getPlayerTier(targetPlayer.getUniqueId());
            Rank rank = plugin.getRankManager().getPlayerRank(targetPlayer.getUniqueId());
            Rank next = plugin.getRankManager().getNextRank(targetPlayer.getUniqueId());

            sender.sendMessage("§8§m--------------------------------------------------");
            sender.sendMessage(" §e§lFICHA TECNICA · RANKUP ANIME");
            sender.sendMessage(" §7Jugador: §f" + targetPlayer.getName());
            sender.sendMessage(" §7Nivel actual: §a" + tier + " §7/ §e50");
            sender.sendMessage(" §7Rango: §b" + (rank != null ? rank.getDisplayName() : "§7Sin Rango"));
            sender.sendMessage(" §7División: §8" + (rank != null ? rank.getDivision() : "§7Ninguna"));
            sender.sendMessage(" §7Habilidad: §e" + (rank != null ? rank.getAbilityType().getName() : "§7Ninguna"));
            sender.sendMessage(" §7Empuje: §f" + (rank != null && rank.isHasKineticPush() ? "§aSí (x" + rank.getPushMultiplier() + ")" : "§cNo"));
            if (next != null) {
                sender.sendMessage(" §7Siguiente: §6" + next.getDisplayName() + " §7(Costo: §e$" + MONEY_FORMAT.format(next.getCost()) + "§7)");
            } else {
                sender.sendMessage(" §a¡Has alcanzado el rango máximo!");
            }
            sender.sendMessage("§8§m--------------------------------------------------");
            return true;
        }

        if (sub.equals("admin")) {
            if (!sender.hasPermission("drakesrankup.admin")) {
                sender.sendMessage("§cNo tienes permiso para comandos administrativos.");
                return true;
            }
            if (args.length >= 2 && args[1].equalsIgnoreCase("reload")) {
                plugin.reloadConfig();
                plugin.getRankManager().loadRanks();
                sender.sendMessage("§a[Rankup] Configuración y rangos recargados en caliente.");
                return true;
            }
            if (args.length >= 4 && args[1].equalsIgnoreCase("set")) {
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage("§cJugador no encontrado.");
                    return true;
                }
                try {
                    int newTier = Integer.parseInt(args[3]);
                    plugin.getRankManager().setPlayerTier(target.getUniqueId(), Math.max(0, Math.min(50, newTier)));
                    sender.sendMessage("§a[Rankup] Nivel de " + target.getName() + " establecido en " + newTier + ".");
                    target.sendMessage("§a[Rankup] Tu nivel de rango ha sido actualizado a " + newTier + " por un administrador.");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cEl nivel debe ser un número entre 0 y 50.");
                }
                return true;
            }
            if (args.length >= 3 && args[1].equalsIgnoreCase("reset")) {
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage("§cJugador no encontrado.");
                    return true;
                }
                plugin.getRankManager().setPlayerTier(target.getUniqueId(), 0);
                sender.sendMessage("§a[Rankup] Progreso de " + target.getName() + " reiniciado a 0.");
                target.sendMessage("§c[Rankup] Tu progreso de rangos ha sido reiniciado.");
                return true;
            }
            sender.sendMessage("§cUso: /rankup admin <set <jugador> <nivel> | reset <jugador> | reload>");
            return true;
        }

        sender.sendMessage("§eComandos de DrakesRankup:");
        sender.sendMessage(" §6/rankup §7- Asciende al siguiente rango");
        sender.sendMessage(" §6/rankup kit §7- Reclama el kit diario de tu división anime con objetos custom");
        sender.sendMessage(" §6/rankup max §7- Sube al rango máximo que puedas pagar");
        sender.sendMessage(" §6/rankup gui §7- Abre el menú visual de las 5 divisiones");
        sender.sendMessage(" §6/rankup info §7- Consulta tus habilidades y progreso");
        sender.sendMessage(" §6/rankup toggle [particulas|empuje|habilidades] §7- Ajustes personales");
        if (sender.hasPermission("drakesrankup.staff")) {
            sender.sendMessage(" §b/rankup test <1-50> §7- Modo Staff de pruebas de rango instantáneo");
            sender.sendMessage(" §b/angel §7(o /zenosama) - Modo Ángel invulnerable con Ultra Instinto perpetuo");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.addAll(Arrays.asList("gui", "kit", "max", "info", "toggle", "admin"));
            if (sender.hasPermission("drakesrankup.staff")) {
                list.add("test");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) {
            list.addAll(Arrays.asList("empuje", "particulas", "habilidades"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("test")) {
            list.addAll(Arrays.asList("reset", "1", "10", "20", "30", "35", "36", "46", "47", "50"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            list.addAll(Arrays.asList("set", "reset", "reload"));
        }
        return list;
    }
}
