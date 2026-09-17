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
}
