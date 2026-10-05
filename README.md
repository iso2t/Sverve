# Sverve

Sverve makes Minecraft 26.3 survival harder through heat, cold, wetness, dry air, and thirst.
It targets Fabric and NeoForge, uses Java 25 and Lombok, and manages configuration with Easy Config.

The current foundation includes independent survival simulations, bounded immutable player state,
an Easy Config schema, and persistent per-player thirst, body temperature, and wetness on both loaders. Thirst drains
during active
Survival/Adventure play, and finishing a plain water bottle restores hydration. Plain water bottles stack to 16.
Thirst survives reconnects,
server restarts, and dimension changes, and resets on death respawn. Creative and Spectator players
are excluded from gameplay updates while retaining their saved thirst.

The server synchronizes each player's thirst to a ten-icon bar above hunger. It currently reuses
Sverve's custom full/half water sprites and moves above air/mount rows when needed. Creative,
Spectator, F1, and the server's thirst enable switch hide it. At zero thirst, dehydration deals
one heart every four seconds, bypasses armor, and can kill. Warm/Hot body exposure accelerates thirst loss.

Body temperature gradually follows the current biome's declared base temperature, including
datapack and modded biomes. Sampling runs once per second of eligible active play. Temperature
survives reconnects, restarts, and dimension changes, and resets to comfortable on death.
Its mapping and response rate are configurable. The player's skin face appears between health and hunger,
with a server-selected Freezing, Cold, Warm, or Hot overlay and a small transition buffer to prevent flicker.
Normal shows the face alone. The four 16x16 overlays can be replaced independently. Heat increases vanilla hunger
exhaustion and thirst loss; cold slows food-based natural healing. Freezing/Hot add lethal periodic damage, reduced by
stacking armor
enchantments: Insulation, Heat Protection, and the weaker universal Thermal Protection, each I-IV.

Water contact soaks players and exposed rain wets them gradually. Shelter and leaving water
allow drying; hot biomes speed it up. Saved wetness cools body temperature and resets on death.
Nearby lit campfires, placed torches, and lava warm players and speed up drying. Campfires reach four blocks;
torches provide mild warmth within two blocks, and source/flowing lava supplies stronger warmth within six.
Warmth fades with distance, walls block it, and only
the strongest source applies. Soul variants count too. The `heatsources` config controls this behavior,
and block tags allow datapacks to include compatible modded sources. Fluids in Minecraft's lava tag also count.
Moisture and temperature settings pause independently. A tiny droplet overlaps the upper-right edge of the face and
fills
as wetness increases and disappears when dry. Its independent placeholder textures can be replaced.
See [the wetness guide](docs/moisture.md).

See [the architecture guide](docs/architecture.md) for package responsibilities, feature interactions,
coding conventions, and the implementation sequence. Balance values are provisional.

## Build and verify

Use a Java 25 JDK to run Gradle:

```powershell
.\gradlew.bat :common:test :fabric:build :neoforge:build
.\gradlew.bat :fabric:runGametest
.\gradlew.bat :fabric:runClientGametest
.\gradlew.bat :neoforge:runHudCheck
```

Loader jars are written to `fabric/build/libs` and `neoforge/build/libs`.
Install the matching Easy Config mod on both sides, plus Fabric API on Fabric.
Lombok is a compile-time dependency and is not bundled into the mod.
Common compiles against Easy Config's standalone API; the loader artifacts supply it at runtime.
The Fabric GameTest run exercises real server players, bottle consumption, and the completion
mixin, biome temperature sampling, and native temperature saving/loading. The client GameTest verifies real packet
delivery, drinking updates, visibility, and captures
HUD screenshots under `fabric/build/client-gametest/screenshots`. The test mod stays out of release jars.
The NeoForge HUD check copies that generated world into `neoforge/build/hud-check`, tests real
drinking, packets, HUD rows, lethal dehydration, death respawn, and Nether transfer, and writes `result.txt` plus
screenshots there. It also checks native temperature updates, death reset, dimension preservation,
stacked dedicated/universal armor protection, natural healing, hunger/thirst drain, lethal overheating,
and native water/rain soaking, shelter, drying, wet cooling, and wetness HUD snapshots.
Run the Fabric client GameTest first to generate its fixture. Both test mods
are excluded from ordinary development runs and release jars.

## Configuration

Initialization creates `config/sverve-survival-server.toml`, with `temperature`, `moisture`, and
`thirst` sections. Easy Config writes comments and numeric bounds; serialized field names are lowercase.
Each feature has its own enable switch. Server configuration determines gameplay; the client HUD
displays received server snapshots. `Side.SERVER` identifies the file's purpose and suffix;
this foundation does not provide remote server editing or full config synchronization.

Thirst defaults to a full-to-empty time of 20 minutes at rest or 10 minutes while continuously
sprinting, measured in game ticks. A plain water bottle restores 30%, capped at full hydration.
The `thirst` settings `baseloss`, `sprintloss`, and `waterbottlehydration` control these values;
`enabled` pauses both drain and restoration and hides the bar on connected clients. Existing
configuration files retain their saved values.

The `thirst` settings `dehydrationdamage` (health points; 2 is one heart) and
`dehydrationintervalseconds` configure zero-thirst damage. Disabling thirst or setting damage to
zero pauses its timer. Drinking resets the timer when hydration rises above zero. Damage can
kill on any difficulty; Creative, Spectator, and dead players are exempt.

The `temperature` section's `comfortablebiometemperature` defaults to 0.8 (plains),
`coldbiomerange` to 0.8, and `hotbiomerange` to 1.2. These map snowy plains (0.0) to full cold
exposure and deserts (2.0) to full heat exposure. `responserate` controls gradual change;
`enabled` freezes saved temperature and hides its icon. See [the temperature guide](docs/temperature.md)
for display thresholds and custom texture paths.

Temperature damage defaults to one heart every four seconds before protection. Equipped levels
add across helmet, chestplate, leggings, and boots: 16 dedicated levels prevent the corresponding
damage; 16 Thermal Protection levels halve both kinds. The three are mutually exclusive per piece
and compatible with vanilla Protection. They use vanilla enchanting, books, trades, and random loot.
Protection applies to extreme damage; it does not alter body exposure or metabolic penalties.
Warm/Hot multiply baseline plus sprinting thirst loss by 1.5/2 and add 0.1/0.2 vanilla food exhaustion
per second. Cold/Freezing make natural healing intervals 1.5/2 times longer, preserving healing
amounts and hunger costs. These values are configurable in the `temperature` section.

## Development

Open this directory as a Gradle project and set both the project SDK and Gradle JVM to Java 25.

```powershell
.\gradlew.bat :fabric:runClient
.\gradlew.bat :neoforge:runClient
```

Shared gameplay belongs in `common`; native loader hooks belong in `fabric` and `neoforge`.
Feature state, settings, and calculations live together under `survival/<feature>`.
`SverveRuntime` exposes thirst, temperature, and moisture through their feature services.
Each feature owns its player access, gameplay coordinator, and synchronizer under `player/<feature>`.
Use short names that read in the context of their class: `consume`, `drain`, `sample`, `refresh`.
Tests name the case, such as `armorChanges` or `persistence`; assertions explain the expected result.
The project EditorConfig keeps Java tabs, aligned fields, and spacing consistent. Source comments
and JavaDoc are omitted.
The original build layout comes from the MultiLoader Template.

## License

[LGPL v3](LICENSE.md).
