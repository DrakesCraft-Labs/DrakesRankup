package com.drakescraft.rankup.model;

import lombok.Getter;

@Getter
public enum AbilityType {
    NONE("Ninguna", "&7Sin habilidad especial activa."),
    DOUBLE_DROP("Investigación Química", "&e18% de probabilidad de duplicar drops de minerales."),
    FEATHER_STEP("Paso de Acróbata", "&bInmunidad al 50% de daño de caída."),
    ELECTRIC_SPEED("Transmutación de Rayo", "&eVelocidad pasiva aumentada al esprintar con chispas."),
    BLACK_FLASH("Destello Negro (Black Flash)", "&c15% de impacto crítico (x1.5 daño) con relámpago oscuro."),
    MUGEN_DEFENSE("Infinito (Mugen)", "&b25% de probabilidad de repeler proyectiles enemigos."),
    CURSED_FLAME("Fuego Maldito", "&4Ataques cuerpo a cuerpo infligen quemadura del alma."),
    WATER_GRACE("Paso Marino", "&9Respiración infinita y Gracia de Delfín bajo el agua."),
    HAKI_CONQUEROR("Haki del Conquistador", "&6Aturde y ralentiza mobs hostiles cercanos al recibir daño."),
    DEVIL_FRUIT_GOMU("Fruta Gomu Gomu (Luffy)", "&cGear Second: velocidad y ráfaga de golpes Gatling. ¡Debilidad mortal en el agua!"),
    SANTORYU_ZORO("Estilo Tres Espadas (Santoryu)", "&2Maestría con espadas (+25% daño físico) y Corte Volador de viento."),
    ZENKAI_BOOST("Zenkai Saiyajin", "&cRegeneración II y Fuerza I al bajar de 3 corazones (CD: 90s)."),
    SSJ_GOD("Super Saiyajin God", "&cKi Divino Carmesí: Carga de Ki con Shift activa regeneración y agilidad divina."),
    SSJ_BLUE("Super Saiyajin Blue", "&9Ki Divino Azul: Carga de Ki con Shift activa Fuerza I y Velocidad II con aura de plasma."),
    GOHAN_BEAST("Gohan Bestia (Beast)", "&fExplosión crítica colosal (+75%) y Makankosappo con rayos magentas. Consumo alto de hambre."),
    BROLY_LSSJ("Broly Legendario (LSSJ)", "&aFuerza III e inmunidad total al empuje. Cólera descontrolada y fatiga posterior."),
    VEGETTO_SPIRIT_SWORD("Espada de Haz de Luz (Vegetto)", "&eShift + Clic derecho con espada dispara un haz perforante dorado a 14 bloques."),
    KI_FLIGHT("Vuelo de Ki Supersónico", "&bPropulsión sónica ultra rápida por 4 segundos con onda expansiva. Cooldown de 30s."),
    SHADOW_EXTRACTION("Extracción de Sombras", "&812% al derrotar mobs de invocar un soldado de sombra temporal."),
    SERIOUS_PUNCH("Golpe Serio", "&612% en golpes cuerpo a cuerpo de infligir un empuje colosal."),
    ULTRA_INSTINCT("Doctrina Egoísta", "&f15% de esquivar completamente cualquier ataque físico o flecha."),
    MASTERED_ULTRA_INSTINCT("Ultra Instinto Dominado", "&fEvasión visual 100% de ataques por 5s. ¡El abuso pasa factura corporal!"),
    HAKAI_AURA("Aura de la Destrucción", "&5Inmunidad al fuego y daño de espinas de energía Hakai."),
    ULTRA_EGO("Mega Instinto (Ultra Ego)", "&5Poder de la Destrucción: A menor vida, mayor daño destructivo desatado."),
    KAMI_DIVINE("Presencia Divina", "&6Aura suprema: corona de luz, regeneración suave e intimidación hostil.");

    private final String name;
    private final String description;

    AbilityType(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
