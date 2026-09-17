package com.drakescraft.rankup.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Material;

import java.util.List;

@Getter
@AllArgsConstructor
public class Rank {
    private final int tier;
    private final String id;
    private final String displayName;
    private final String division;
    private final double cost;
    private final Material icon;
    private final List<String> permissions;
    private final List<String> perks;
    private final List<String> rewardCommands;
    private final boolean hasKineticPush;
    private final double pushMultiplier;
    private final int pushCooldownSeconds;
    private final AbilityType abilityType;
    private final String particleType;
    private final boolean permanent;
    private final double maintenanceCost;

    public Rank(int tier, String id, String displayName, String division, double cost, Material icon,
                List<String> permissions, List<String> perks, List<String> rewardCommands,
                boolean hasKineticPush, double pushMultiplier, int pushCooldownSeconds,
                AbilityType abilityType, String particleType) {
        this(tier, id, displayName, division, cost, icon, permissions, perks, rewardCommands,
                hasKineticPush, pushMultiplier, pushCooldownSeconds, abilityType, particleType,
                (tier <= 10 || tier % 10 == 0 || tier == 50), Math.max(1000.0, cost * 0.05));
    }
}
