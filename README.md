# ⛩️ DrakesRankup

> **Sistema Oficial de Progresión por Rangos Anime/Shonen, Sumidero Económico de Late-Game y GUI Interactiva para DrakesCraft (Paper 1.21.11)**

---

## 🧭 Visión y Arquitectura

**DrakesRankup** resuelve la necesidad de dinamizar la economía del servidor proporcionando a los jugadores un camino de progresión de **50 Rangos Épicos** basados en series legendarias de combate y anime: **Dr. Stone, Hunter x Hunter, Naruto, Jujutsu Kaisen, One Piece, Bleach, Dragon Ball y Solo Leveling**.

### 🛡️ Convivencia con Rangos VIP de Dioses
Los rangos comprados en tienda (`HERCULES`, `HERMES`, `ZEUS`, `TITAN`, etc.) conservan la exclusividad de nombres divinos y su jerarquía en LuckPerms (pesos `100` a `200`). Los rangos de DrakesRankup operan en pesos `1` al `50` como grupos secundarios/sufijos, asegurando que un jugador VIP nunca pierda su prefijo principal en el TAB ni en el chat.

---

## 📜 Las 5 Divisiones (50 Rangos)

1. **División I: El Amanecer Shonen (1 - 10)**: Dr. Stone & Hunter x Hunter ($10k a $850k).
2. **División II: El Camino del Shinobi (11 - 20)**: Naruto & Jujutsu Kaisen ($1.2M a $12.9M).
3. **División III: La Gran Ruta Pirata (21 - 30)**: One Piece & Bleach ($15.9M a $85M).
4. **División IV: Guerreros Z y Despertar (31 - 40)**: Dragon Ball & Solo Leveling ($100.5M a $404.6M).
5. **División V: Trascendencia y Deidades (41 - 50)**: Shonen Endgame ($466M a $1.5B).

---

## 🎮 Comandos
* `/rankup` — Abre la interfaz gráfica o sube al siguiente rango.
* `/ranks` / `/rangos` — Abre el catálogo interactivo de los 50 rangos.
* `/rankup set <jugador> <tier>` — Establece el nivel de un jugador (Admin).
* `/rankup reload` — Recarga la configuración y los rangos (Admin).

---

## 📊 Placeholders de PlaceholderAPI
* `%drakesrankup_tier%`: Nivel actual (1 al 50).
* `%drakesrankup_rank%`: Nombre formateado del rango actual.
* `%drakesrankup_division%`: División actual.
* `%drakesrankup_next_rank%`: Nombre del siguiente rango.
* `%drakesrankup_next_cost%`: Costo restante para ascender.
* `%drakesrankup_progress%`: Porcentaje de completitud (0 a 100%).
* `%drakesrankup_tag%`: Tag para chat y TAB.