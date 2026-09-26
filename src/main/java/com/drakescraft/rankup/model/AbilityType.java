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
    DEVIL_FRUIT_GOMU("Fruta Gomu Gomu (Luffy)", "&cGear Second, Third, Fourth y Fifth: rebote elástico, velocidad y ráfaga colosal."),
    SANTORYU_ZORO("Estilo Tres Espadas (Santoryu)", "&2Maestría con espadas (+25% daño físico) y Corte Volador de viento."),
    ZENKAI_BOOST("Zenkai Saiyajin", "&cRegeneración II y Fuerza I al bajar de 3 corazones (CD: 90s)."),
    SSJ_2("Super Saiyajin 2", "&eKi Dorado Ascendente con arcos eléctricos permanentes, bio-electricidad y Fuerza II."),
    KAIOKEN("Kaio-ken (x3 / x10 / x20)", "&cMultiplicador divino con aura carmesí y propulsión aérea por 5 minutos."),
    SSJ_GOD("Super Saiyajin God", "&cKi Divino Carmesí: Carga de Ki con Shift activa regeneración y agilidad divina por 5m."),
    SSJ_BLUE("Super Saiyajin Blue", "&9Ki Divino Azul: Carga de Ki con Shift activa Fuerza I y Velocidad II con aura de plasma por 5m."),
    GOHAN_BEAST("Gohan Bestia (Beast)", "&fExplosión crítica colosal (+75%) y Makankosappo con rayos magentas por 5m."),
    BROLY_LSSJ("Broly Legendario (LSSJ)", "&aFuerza III e inmunidad total al empuje por 5 minutos."),
    VEGETTO_SPIRIT_SWORD("Espada de Haz de Luz (Vegetto)", "&eShift + Clic derecho con espada dispara un haz perforante dorado a 14 bloques."),
    KI_FLIGHT("Vuelo de Ki Supersónico", "&bPropulsión sónica ultra rápida con onda expansiva y 10s de vuelo aerodinámico."),
    SHADOW_EXTRACTION("Extracción de Sombras", "&812% al derrotar mobs de invocar un soldado de sombra temporal."),
    SERIOUS_PUNCH("Golpe Serio", "&612% en golpes cuerpo a cuerpo de infligir un empuje colosal."),
    ULTRA_INSTINCT("Doctrina Egoísta", "&f15% de esquivar cualquier golpe de combate (físico o flecha)."),
    MASTERED_ULTRA_INSTINCT("Ultra Instinto Dominado", "&fEsquiva total de ataques por 60s con agilidad milagrosa."),
    HAKAI_AURA("Aura de la Destrucción", "&5Inmunidad al fuego y daño de espinas de energía Hakai."),
    ULTRA_EGO("Mega Instinto (Ultra Ego)", "&5Poder de la Destrucción por 5m: A menor vida, mayor daño destructivo desatado."),
    KAMI_DIVINE("Presencia Divina", "&6Aura suprema: corona de luz, regeneración suave e intimidación hostil."),

    // ==========================================
    // SISTEMA ONE PIECE: FRUTAS & HAKI & ARMAS
    // ==========================================
    DEVIL_FRUIT_MERA("Fruta Mera Mera (Ace / Sabo)", "&6Inmunidad ígnea total, Puño de Fuego Hiken y esfera solar Dai Enkai: Entei."),
    DEVIL_FRUIT_OPE("Fruta Ope Ope (Trafalgar Law)", "&bDespliega ROOM para intercambiar posiciones (Shambles), levitar (Takt) y bisturí interno."),
    DEVIL_FRUIT_PIKA("Fruta Pika Pika (Almirante Kizaru)", "&eVelocidad fotónica: Salto Yata no Kagami y ráfaga de luz celestial Yasakani."),
    DEVIL_FRUIT_GURA("Fruta Gura Gura (Barbablanca)", "&fFisura espacial que sacude la tierra y genera ondas de choque sísmico masivo."),
    MAGNETIC_ATTRACTOR("Vórtice Magnético (Jiki Jiki)", "&bImán de magnetismo que atrae todos los ítems caídos en un radio de 200 bloques hacia ti."),
    DANCING_BLADES("Filos Danzantes (Aura Kill)", "&cEspadas y hachas que flotan y golpean solas como aura de exterminio alrededor del usuario."),
    BUSOSHOKU_HAKI("Haki de Armamento", "&8Endurecimiento corporal impenetrable: +45% daño melee y protección de obsidiana."),
    KENBUNSHOKU_HAKI("Haki de Observación", "&ePresciencia sensorial: 30% de evadir automáticamente ataques enemigos."),

    // ==========================================
    // NUEVAS HABILIDADES TIERS 51 - 100
    // ==========================================
    BOOGIE_WOOGIE("Boogie Woogie (Todo)", "&dIntercambio cinético de posición táctica con el adversario."),
    DISMANTLE_CLEAVE("Corte y Desmantelar (Sukuna)", "&4Cortes invisibles espaciales que rajan a enemigos cercanos."),
    MALEVOLENT_SHRINE("Relicario Maldito", "&4Expansión de dominio con ráfaga de tajos sangrientos."),
    HOLLOW_PURPLE("Púrpura Carmesí (Murasaki - Gojo)", "&5Esfera de masa imaginaria que distorsiona el vacío y oblitera materia."),
    BANKAI_ZANGETSU("Tensa Zangetsu (Ichigo)", "&8Bankai de velocidad extrema y ráfagas de Getsuga Tenshou oscuro."),
    KYOKA_SUIGETSU("Hipnosis Absoluta (Aizen)", "&bReflejo ilusorio que desvía ataques y confunde a los agresores."),
    ZANKA_NO_TACHI("Zanka no Tachi (Yamamoto)", "&6Armadura de 15 millones de grados que incinera todo lo que se aproxime."),
    CHAINSAW_DEVIL("Demonio Motosierra (Denji)", "&cRev motorizado: tajos continuos acelerados que restauran salud con sangre."),
    SHADOW_MONARCH_ARMY("Ejército de Sombras (Sung Jin-Woo)", "&8Invoca un destacamento permanente de soldados umbríos a tu servicio."),
    JAJANKEN_ROCK("Jajanken: Piedra (Gon Adulto)", "&eImpacto colosal de aura concentrada con retroceso sísmico brutal."),
    ZERO_HAND_GUANYIN("Mano Cero (Netero)", "&6Emisión final de energía condensada del Bodhisattva Guanyin."),
    HIRAISHIN_TELEPORT("Dios del Trueno Volador (Minato)", "&eSalto instantáneo a la velocidad del rayo amarillo de Konoha."),
    AMATERASU_TSUKUYOMI("Llamas Negras de Amaterasu (Itachi)", "&8Fuego infernal que quema sin extinguirse con daño continuo de alma."),
    TENGAI_SHINSEI("Tengai Shinsei (Madara)", "&4Invocación de meteorito orbital con impacto devastador en el terreno."),
    BARYON_MODE("Modo Barión (Naruto)", "&cEnergía nuclear de chakra concentrado que drena la vitalidad absoluta."),
    GEAR_FIVE_NIKA("Sun God Nika (Luffy Gear 5)", "&fGuerrero de la Liberación: rebote elástico total, tambores de júbilo y vuelo cartoon."),
    GURA_GURA_TREMOR("Fruta Gura Gura (Barbablanca)", "&bGolpe que agrieta el espacio generando ondas sísmicas de choque masivo."),
    KAMISARI_HAKI("Partida Divina (Kamisari - Shanks)", "&cTajo de Haki del Conquistador condensado que rompe toda resistencia."),
    SPHERE_OF_DESTRUCTION("Esfera de Destrucción (Bills)", "&5Orbe gigantesco de Hakai puro que vaporiza enemigos y terreno desprotegido."),
    STARDUST_BREAKER("Castigador de Almas (Gogeta Blue)", "&dPolvo estelar multicolor que purifica y desintegra energía negativa."),
    OMNI_CREATION_ERASE("Borrado y Creación Suprema (Omni-Zeno)", "&eAutoridad omnipotente capaz de restaurar o borrar existencias al instante.");

    private final String name;
    private final String description;

    AbilityType(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
