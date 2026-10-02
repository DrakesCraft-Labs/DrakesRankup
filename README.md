<div align= center>

![DrakesRankup](banner.svg)

# ⛩️ DrakesRankup

**Sistema de progresión por rangos anime/shōnen, auras, poderes y sumidero económico de late-game para DrakesCraft.**

Paper 1.21.11 · Java 21 · LuckPerms · Essentials · Slimefun

</div>

---

## Qué hace

DrakesRankup es la escalera de progresión de largo plazo de DrakesCraft: **100 tiers (10 Divisiones)**
de rango que el jugador sube pagando desde su economía, cada uno con **aura
visual, habilidades y transformaciones** de temática anime. Está pensado como
**sumidero económico**: da a los jugadores ricos algo en qué gastar sin romper el
balance del servidor.

Sólo opera en las modalidades personalizadas (**Slimefun, OneBlock, SkyBlock**);
el **Clásico** queda excluido a propósito.

---

## Características

### Progresión (100 Tiers / 10 Divisiones)
- **100 tiers** de rango con coste creciente y sincronización en tiempo real con LuckPerms.
- **10 Divisiones (I a X)** navegables en GUI interactiva (/ranks o /rangos) con botones de cambio rápido entre Divisiones I-V y VI-X.
- **Auto-reparación de rango al entrar**: si LuckPerms y el tier se desalinean
  (incluso offline), se reconcilian solos.
- **Bolsa de ascenso** sin tope para acumular pagos de los rangos que superan el
  tope de dinero en mano.

### Habilidades y Combate Épico
- **Golpe Serio de Saitama (Buff True Damage)**:
  - Onda sónica cinética devastadora (DamageType.SONIC_BOOM) que perfora armaduras infinitas (Infinity Armor), Protección vanilla y resistencias extremas.
  - Rompeescudos integrado: inutiliza escudos rivales durante 8 segundos.
  - Escalado de daño cinético con umbral mínimo de verdadero daño (True Damage Floor) basado en rebirths y vida máxima.
  - Knockback masivo con partículas Warden Sonic Boom y efectos de aturdimiento (Slowness III y Blindness).
- **Empuje cinético** (Shunpo): impulso direccional con doble salto y cooldown.
- **Vuelo de Ki** (tier 31+): dash sónico con inmunidad a caída.
- **Convivencia con el vuelo de rango**: si el jugador tiene fly legítimo
  (Essentials, FlyingBubble, AngelGem o staff), las habilidades de impulso **no** secuestran su
  vuelo. El doble salto vuela; no se convierte en dash.

### Auras y efectos visuales
- **Cobertura continua tier 1–100**, distribuida armónicamente en 10 divisiones. La densidad de partículas escala con el tier dentro de cada franja.
- **Auras de transformación** para los poderes anime activos (MUI, Ultra Ego,
  Gohan Beast, Broly, SSJ Blue/God…).
- **Tres niveles de intensidad**: completo → reducido → apagado. El reducido
  aligera a la mitad la densidad y el tráfico.

### Staff
- **Modo Ángel** (/angel): invulnerabilidad y aura de halo doble para
  administración.

---

## Comandos

| Comando | Alias | Descripción |
|---|---|---|
| /rankup | 
anks, 
angos, subirrango | Abre la GUI con /ranks o /rangos, o gestiona ascensos. |
| /rankup max [jugador] | — | Sube al nivel máximo posible según el saldo actual. |
| /rankup particles | — | Cicla la intensidad del aura: completo → reducido → apagado. |
| /rankup admin set <jugador> <tier\|max> | — | Fija el tier exacto (1–100) o máximo de un jugador *(admin)*. |
| /rankup admin max <jugador> | — | Lleva a un jugador directamente al Tier 100 *(admin)*. |
| /transform | 	ransformaciones, ki, poderes | GUI de transformaciones y poderes anime. |
| /angel | zenosama, zeno, daishinkan | Modo staff de deidad *(permiso \drakesrankup.staff\)*. |

---

## Permisos

| Permiso | Para qué |
|---|---|
| drakesrankup.staff | Comandos de staff (/angel, gestión). |
| drakesrankup.admin | Bypass total; gestión de tiers administrativos. |
| ssentials.fly | Tratado como vuelo legítimo: las habilidades no lo secuestran. |

---

## Build

\\\ash
mvn clean package
\\\

El artefacto sale en \	arget/DrakesRankup-v1.0.0.jar\. Autor: **JackStar6677-1**.

---

## 📄 License & Intellectual Property

Copyright © 2026 [**JackStar6677-1**](https://github.com/JackStar6677-1) · [**DrakesCraft Labs**](https://github.com/DrakesCraft-Labs). All Rights Reserved.

This software is **Source-Available** for public inspection and technical audit. Redistribution, commercial repackaging, or unauthorized derivative distribution without explicit written permission from the author is strictly prohibited.
