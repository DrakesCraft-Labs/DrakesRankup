package com.drakescraft.rankup.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerSettings {
    private boolean particlesEnabled = true;
    private boolean kineticPushEnabled = true;
    private boolean abilitiesEnabled = true;
}
