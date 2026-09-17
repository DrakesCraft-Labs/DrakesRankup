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
    private String activeTransformation = null;
    private boolean kiFlightEnabled = true;

    public PlayerSettings(boolean particlesEnabled, boolean kineticPushEnabled, boolean abilitiesEnabled) {
        this.particlesEnabled = particlesEnabled;
        this.kineticPushEnabled = kineticPushEnabled;
        this.abilitiesEnabled = abilitiesEnabled;
        this.activeTransformation = null;
        this.kiFlightEnabled = true;
    }
}
