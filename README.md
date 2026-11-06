# Simple Void

M1 for Minecraft 26.1 to 26.1.2, Java 25, NeoForge. Mod id: `simplevoid`.

A void world preset with reusable, data-driven world setups. A setup places
several named structures at world creation or later through a command or API.
One placement is the spawn structure. No custom blocks, items, recipes, portals,
quests, resource progression, hazards or space backdrop are added. Hull and
boundary blocks belong to the Simple Machines ship blocks add-on.

## Create a world

Choose **Simple Void** as the world type. On a dedicated server, set
`level-type=simplevoid:void` before creating a new world.

The Overworld has no terrain, generated structures, lakes or decoration. Its
marker biome keeps plains weather, colours and mob spawning. The Nether and End
are vanilla. Existing worlds and other world types are not changed automatically.

The default `simplevoid:skyblock` setup creates:

- `start`: a vanilla grass/dirt island with an oak tree at `0 64 0`. Spawn is
  `4.5 67 2.5`. No starter chest or supplies are added.
- `distant_example`: a small vanilla stone-brick pad, 256 blocks east of spawn,
  with a 90-degree rotation. It is an example, not a resource-island progression.
- `later_example`: another example pad, disabled at creation and gated by a
  scoreboard condition for demonstrating later placement.

`simplevoid:ship` is a separate setup with a small empty vanilla-block cabin.
It demonstrates an interior spawn beneath a roof, not Homebound's final ship.
All bundled templates use vanilla blocks only.

## Select or define a setup

The highest-priority data pack resource
`data/simplevoid/simplevoid/world_setup.json` selects the setup:

```json
{"setup": "simplevoid:skyblock"}
```

The `setup` key in `config/simplevoid.properties` overrides that selector when
non-blank. For example, `setup=simplevoid:ship`. Blank uses the data pack selector.
The config file is generated on first Simple Void world creation. The optional
pack in `examples/ship-start` selects the demonstration ship instead.

A setup id such as `homebound:colony` resolves to
`data/homebound/simplevoid/world_setup/colony.json`. A local config file at
`config/simplevoid/world_setup/homebound/colony.json` can define the same setup
instead, with priority over its data pack resource. Both use the same format:

```json
{
  "spawn": {
    "placement": "ship",
    "offset": [4, 1, 5],
    "yaw": 180
  },
  "placements": [
    {
      "name": "ship",
      "structure": "homebound:colony_ship",
      "position": {"fixed": [0, 64, 0]},
      "rotation": "none",
      "at_creation": true
    },
    {
      "name": "distant_island",
      "structure": "simplevoid:outpost",
      "position": {"spawn_offset": [256, -1, 0]},
      "rotation": "clockwise_90",
      "at_creation": true
    },
    {
      "name": "later_island",
      "structure": "simplevoid:outpost",
      "position": {"fixed": [-256, 64, 0]},
      "rotation": "none",
      "at_creation": false,
      "condition": {
        "mod_loaded": "homebound",
        "score": {"objective": "repairs", "holder": "#world", "min": 1}
      }
    }
  ]
}
```

The ship template in that example is
`data/homebound/structure/colony_ship.nbt`, a normal compressed structure-block
NBT template supplied by the pack or another mod. Adjust its spawn offset to the
real ship's safe interior. Modded blocks can be used when their mods are installed.

### Fields

- `spawn.placement`: names exactly one placement as the spawn structure.
- `spawn.offset`: player-feet offset inside its unrotated template. The offset
  rotates with the template. Include air and headroom in the saved template.
- `spawn.yaw`: global facing angle, `-180` to `180`, default `0`.
- `placements`: 1 to 128 entries with unique, stable lowercase `name` values.
  Names may contain letters, digits, `_` and `-`, but not slashes or dots.
- `structure`: any namespaced structure-template id.
- `position`: exactly one of `fixed` or `spawn_offset`, each an integer `[x,y,z]`.
  These specify the placement origin. Rotation pivots around template-local
  `[0,0,0]`, so rotated bounds can extend into negative X or Z from the origin.
- `rotation`: `none`, `clockwise_90`, `180`, or `counterclockwise_90`.
  Default is `none`.
- `at_creation`: default `true`. False entries are available only to later
  command/API calls. They do not get a placement marker until actually placed.
- `condition`: optional object, all supplied checks must pass. `enabled` defaults
  to true; false disables the placement. `mod_loaded` checks one mod id. `score`
  checks a literal scoreboard `holder` in an `objective`, with optional inclusive
  `min` and `max`. Missing objectives or scores fail. No selectors are evaluated.
  Omitted conditions always pass. Failed conditions remain unplaced and can be
  retried later; they are not polled automatically.

The spawn placement must use `fixed` coordinates and `at_creation=true`. Its
condition must pass at creation. Other placements may be relative to the
configured spawn at creation or the current world spawn on later calls.

Templates must fit 256 blocks per axis and 1,048,576 blocks in their bounding
volume, within build height and the world border. Spawn needs a solid floor and
two clear, dry blocks, inside the saved template. Missing templates, invalid data,
unsafe spawns or failed spawn conditions abort world creation with an explanation.
Fix the data and create a fresh test world after a failed placement. Placements
are preflighted for missing templates and bounds, but are not transactional if a
block/entity handler fails partway through a template.

## Later placement

Operator level 2 or equivalent permission is required:

```text
/void place simplevoid:skyblock
/void place simplevoid:skyblock distant_example
/void place distant_example
```

A namespaced argument selects a whole setup; an optional second argument selects
one placement in it. A bare placement name uses the setup saved when the world
was created, falling back to the configured selection on other worlds. Commands
run in the command source's dimension and do not change world spawn. They can
overwrite existing blocks, so use them carefully in built-up areas.

For the deferred default example:

```text
/scoreboard objectives add void_example dummy
/scoreboard players set #world void_example 1
/void place later_example
```

Public API, called on the server thread:

```java
VoidPlacements.placeSetup(level, Identifier.parse("homebound:colony"));
VoidPlacements.placePlacement(level, Identifier.parse("homebound:colony"), "later_island");
```

Import `com.workshopcraft.simplevoid.api.VoidPlacements`. Both methods return a
`Result` with `placed`, `alreadyPlaced` and `conditionFailed` name lists, and throw
on invalid data or failed placement. They obey conditions and do not reset spawn.
They work in any server dimension, not only the automatic void Overworld.

Each successful placement is recorded independently in namespaced, per-dimension
saved data as `<setup id>/<placement name>`, with its structure, position and
rotation. Commands, API calls, reloads and normal world restarts skip that key.
Player edits are preserved. Keep names stable: renaming a setup or placement
creates a new identity and can place another copy. There is no force/replacement
command. As with other world data, durability depends on saving the world.

## Spawn behaviour

Automatic placement happens before vanilla's initial spawn search, once at world
creation. The selected setup and spawn are saved. Changing config or data later
does not automatically add or replace structures; use the command/API explicitly.
Later calls load the current setup definition, but already-placed keys still skip.

World spawn is set to the configured point and `minecraft:respawn_radius` is set
to zero on creation. A narrow spawn hook keeps first joins and fallback respawns
at that point, including under ship roofs. Beds and respawn anchors work normally.
Changing world spawn or setting a nonzero radius restores vanilla spawn searching.
Destroying or blocking the saved spawn also falls back to vanilla; the mod does
not repair structures or create rescue platforms. Bonus chests are skipped: pack
supplies belong in templates. This is not a per-player island system.

## Layout and builds

This is a real Stonecutter 0.9.8 branched build, active version `26.1.2`, on Git
branch `master`. There are no other Minecraft versions configured.

- `stonecutter.gradle`, `settings.gradle`: controller and version/branch graph.
- `common/src`: shared preset, biome, setup data, templates, placement API,
  configuration, commands, saved data and spawn mixins.
- `common/versions/26.1.2`: common build node.
- `neoforge/src`, `neoforge/versions/26.1.2`: loader entrypoint and release node.
- `fabric`: retained source/metadata scaffold, not an enabled Stonecutter node
  and not built or shipped in this alpha.
- `build-logic`, `gradle`: sibling build conventions and wrapper, adapted for
  Stonecutter. Dependency versions remain in `gradle.properties` for this one node.
- `tools`: deterministic vanilla-template generation and headless validation.
- `examples/ship-start`: optional data pack selector.

```sh
export JAVA_HOME=/home/box/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2
export PATH=$JAVA_HOME/bin:$PATH
./gradlew -q build -x test -x javadoc
python3 tools/verify_resources.py
./gradlew -q :common:26.1.2:validateResources -x test -x javadoc
./gradlew -q build -x test -x javadoc -Pversion=0.1.0-alpha.1
```

The committed version is `0.1.0`. The NeoForge jar is under
`neoforge/versions/26.1.2/build/libs`. No Fabric build is included.
`tools/generate_structures.py` recreates the vanilla templates. Headless checks
load registries, codecs, templates, conditions and saved data only; they do not
construct worlds or start a Minecraft server/client.

## Validation still needed

Compile and headless checks are not an in-game test. No shared-server tests or
screenshots have been run. ModDev's lock is required before either game process.

When a slot is available: verify default spawn and distant pad; travel into empty
terrain; test a normal world; edit blocks and restart to check persistence; join
as a second player and test death/bed respawns; test the ship's interior spawn;
place a named entry and a whole setup twice; unlock the deferred scoreboard
condition; reload data and confirm no automatic placement; try fixed/relative
rotations, custom templates and invalid/unsafe setups.

Homebound's production ship, named areas, repairs, hazards and animated sky are
later work. Simple Books and skyblock resource progression are untouched.
