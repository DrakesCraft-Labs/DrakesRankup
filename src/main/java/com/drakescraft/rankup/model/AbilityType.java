package com.drakescraft.rankup.model;

import lombok.Getter;

@Getter
public enum AbilityType {
    NONE("Ninguna", "&7Sin habilidad especial activa."),
    DOUBLE_DROP("Investigación Química", "&e15% de probabilidad de duplicar drops de minerales."),
    FEATHER_STEP("Paso de Acróbata", "&bInmunidad a daño de caída moderado y paso ligero."),
    ELECTRIC_SPEED("Transmutación de Rayo", "&eVelocidad pasiva aumentada al esprintar con chispas."),
    BLACK_FLASH("Destello Negro (Black Flash)", "&c15% de impacto crítico (x1.5 daño) con relámpago oscuro."),
    MUGEN_DEFENSE("Infinito (Mugen)", "&b15% de probabilidad de repeler proyectiles enemigos."),
    CURSED_FLAME("Fuego Maldito", "&4Ataques cuerpo a cuerpo infligen quemadura del alma."),
    WATER_GRACE("Paso Marino", "&9Respiración infinita y Gracia de Delfín bajo el agua."),
    HAKI_CONQUEROR("Haki del Conquistador", "&6Aturde y ralentiza mobs hostiles cercanos al recibir daño."),
    ZENKAI_BOOST("Zenkai Saiyajin", "&cRegeneración II y Fuerza I al bajar de 3 corazones (CD: 90s)."),
    SHADOW_EXTRACTION("Extracción de Sombras", "&810% al derrotar mobs de invocar un soldado de sombra temporal."),
    SERIOUS_PUNCH("Golpe Serio", "&612% en golpes cuerpo a cuerpo de infligir un empuje colosal."),
    ULTRA_INSTINCT("Doctrina Egoísta", "&f15% de esquivar completamente cualquier ataque físico o flecha."),
    HAKAI_AURA("Aura de la Destrucción", "&5Inmunidad al fuego y daño de espinas de energía Hakai."),
    KAMI_DIVINE("Presencia Divina", "&6Aura suprema: corona de luz, regeneración suave y respeto hostil.");

    private final String name;
    private final String description;

    AbilityType(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
