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

    /**
     * Intensidad de las auras: 2 = completa, 1 = reducida (menos densa y menos
     * frecuente), 0 = apagada. Convive con particlesEnabled por retrocompatibilidad:
     * el boolean sigue siendo la puerta on/off y el nivel afina la intensidad.
     */
    private int particleLevel = 2;

    /**
     * Contador de Renacimientos (Rebirths): máximo 50.
     * Otorga +3% de daño permanente y -1% de enfriamiento en habilidades por nivel.
     */
    private int rebirthCount = 0;

    public PlayerSettings(boolean particlesEnabled, boolean kineticPushEnabled, boolean abilitiesEnabled) {
        this.particlesEnabled = particlesEnabled;
        this.kineticPushEnabled = kineticPushEnabled;
        this.abilitiesEnabled = abilitiesEnabled;
        this.activeTransformation = null;
        this.kiFlightEnabled = true;
        this.rebirthCount = 0;
    }
}
