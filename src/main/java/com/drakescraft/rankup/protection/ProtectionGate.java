package com.drakescraft.rankup.protection;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Compuerta de seguridad Fail-Closed para daño y modificación del terreno.
 * Protege de forma estricta claims de ProtectionStones y regiones de WorldGuard.
 * Si una explosión o habilidad impacta dentro o cerca de una protección, el daño a bloques
 * se anula al 100%, realizando únicamente daño a entidades y efectos visuales.
 */
public final class ProtectionGate {

    private final Plugin plugin;
    private final Method protectionStoneLookup;
    private boolean warned;

    public ProtectionGate(Plugin plugin) {
        this.plugin = plugin;
        this.protectionStoneLookup = findProtectionStoneLookup();
    }

    /**
     * Evalúa si una habilidad puede alterar o destruir bloques en el radio indicado.
     * Si cualquiera de los puntos muestreados toca un claim ajeno o protegido, retorna false.
     */
    public boolean allowTerrainDamage(Player actor, Location center, double radius) {
        if (center == null || center.getWorld() == null) return false;
        
        // Muestrear centro y perímetro
        for (Location sample : samples(center, radius)) {
            if (isProtected(sample, actor)) {
                if (actor != null && actor.isOnline()) {
                    actor.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&6[Rankup] &c¡Zona protegida! &7La destrucción del terreno se anula dentro o cerca de claims."));
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Ejecuta una detonación controlada: si la zona es virgen / desprotegida, destruye bloques;
     * si está protegida o hay duda, la explosión no rompe ningún bloque (breakBlocks = false).
     */
    public boolean applyTerrainExplosion(Player actor, Location loc, float power, boolean setFire) {
        if (loc == null || loc.getWorld() == null) return false;
        boolean allowed = allowTerrainDamage(actor, loc, power);
        if (allowed) {
            loc.getWorld().createExplosion(actor, loc, power, setFire, true);
        } else {
            loc.getWorld().createExplosion(actor, loc, power, false, false);
        }
        return allowed;
    }

    private List<Location> samples(Location center, double radius) {
        List<Location> result = new ArrayList<>();
        result.add(center);
        int points = 12;
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 * i) / points;
            result.add(center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius));
            result.add(center.clone().add(Math.cos(angle) * (radius * 0.5), 0, Math.sin(angle) * (radius * 0.5)));
        }
        return result;
    }

    private boolean isProtected(Location location, Player actor) {
        try {
            // 1. Verificación en ProtectionStones
            if (protectionStoneLookup != null) {
                Object psRegion = protectionStoneLookup.invoke(null, location);
                if (psRegion != null) {
                    if (actor != null) {
                        try {
                            Object region = psRegion.getClass().getMethod("getRegion").invoke(psRegion);
                            Object owners = region.getClass().getMethod("getOwners").invoke(region);
                            Object members = region.getClass().getMethod("getMembers").invoke(region);
                            UUID uuid = actor.getUniqueId();
                            boolean isOwner = (boolean) owners.getClass().getMethod("contains", UUID.class).invoke(owners, uuid);
                            boolean isMember = (boolean) members.getClass().getMethod("contains", UUID.class).invoke(members, uuid);
                            if (isOwner || isMember) {
                                // Aún siendo dueño, para evitar auto-griefing accidental en construcciones grandes
                                // retornamos true (protegido) si la config lo exige, o permitimos si es dueño.
                                return false; // El dueño puede detonar en su propio terreno
                            }
                        } catch (Throwable ignored) {}
                    }
                    return true; // Claim ajeno: bloqueado
                }
            }

            // 2. Verificación en WorldGuard
            if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null) {
                Class<?> worldGuardType = Class.forName("com.sk89q.worldguard.WorldGuard");
                Object worldGuard = worldGuardType.getMethod("getInstance").invoke(null);
                Object platform = worldGuard.getClass().getMethod("getPlatform").invoke(worldGuard);
                Object container = platform.getClass().getMethod("getRegionContainer").invoke(platform);
                Object query = container.getClass().getMethod("createQuery").invoke(container);
                Class<?> adapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
                Object adapted = adapter.getMethod("adapt", Location.class).invoke(null, location);
                Class<?> worldEditLocation = Class.forName("com.sk89q.worldedit.util.Location");
                Object regions = query.getClass().getMethod("getApplicableRegions", worldEditLocation).invoke(query, adapted);
                int size = ((Number) regions.getClass().getMethod("size").invoke(regions)).intValue();
                if (size > 0) {
                    return true;
                }
            }
            return false;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            if (!warned) {
                warned = true;
                plugin.getLogger().log(Level.WARNING, "[Rankup] Falló consulta de protecciones; compuerta en modo Fail-Closed.", error);
            }
            return true; // Fail-closed: ante cualquier duda o error, proteger el terreno
        }
    }

    private Method findProtectionStoneLookup() {
        try {
            return Class.forName("dev.espi.protectionstones.PSRegion").getMethod("fromLocation", Location.class);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }
}
