# Simple Void

Version 1.0.0 for Minecraft 26.1.2, Java 25 and NeoForge. Mod id: `simplevoid`.

By dazsim / workshopcraft. All Rights Reserved.

Source: https://github.com/dazsim/simple-void
Issues: https://github.com/dazsim/simple-void/issues

A void world preset with reusable, data-driven world setups. A setup places
several named structures at world creation or later through a command or API.
One placement is the spawn structure. Named areas attached to placements have
persistent states, change events and optional command hooks. No custom blocks, items, recipes, portals,
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

## Named areas

Add `areas` to a placement in its existing world setup JSON. The data pack and
config paths are the same as above. For example, inside the `ship` placement:

```json
"areas": [
  {
    "name": "bridge",
    "bounds": {"min": [1, 1, 1], "max": [7, 3, 9]},
    "initial_state": "damaged",
    "on_state_change": {
      "enabled": true,
      "commands": {
        "repaired": [
          "place template homebound:colony_ship_repaired {x} {y} {z} {rotation}"
        ]
      }
    }
  }
]
```

- `areas` is optional, default empty, at most 128 per placement. Names are unique
  within that placement: 1 to 64 lowercase letters, digits, `_` or `-`.
- `bounds.min` and `bounds.max` are inclusive integer block coordinates in the
  unrotated template's local space. Both rotate around local `[0,0,0]` and translate
  by the actual saved placement origin. Bounds may surround the template, but
  must stay within world height/border. Local coordinates are within +/-4096,
  with at most 4096 blocks per axis. Reversed bounds are invalid.
- `initial_state` defaults to `damaged`. `repaired` is conventional, not special.
  Other states are supported without code: 1 to 64 lowercase letters, digits,
  `_`, `-` or `.`. No hazards or automatic block replacement are implied by a state.
- `on_state_change` is optional. Its `enabled` defaults to false, even when
  commands are supplied. `commands` maps the **new state** to an ordered list,
  at most 64 state entries and 32 nonempty single-line commands per state.

The canonical id is `<setup id>/<placement name>/<area name>`, such as
`homebound:colony/ship/bridge`. A bare area name works only when unique in the
command's dimension. Ambiguous names fail with the matching full ids. Dimension
scopes are independent; state is shared by all players there, not per player/team.

Areas are registered only after their structure is successfully placed. Deferred
and condition-failed placements have no areas yet. Definitions, transformed
bounds, origin, rotation, hook data and current state are snapshotted in
per-dimension saved data `simplevoid:areas`, separate from M1's unchanged
`simplevoid:start`. A restart does not reset states or run hooks. Keep ids stable.

On upgrade from M1, the current setup definition supplies areas for already-saved
placements, using their **saved** positions/rotations, without replacing any
blocks. Import runs at server start and before API/command queries. Missing setup
resources log a warning and can be retried after fixing the pack. Each placement
is imported once, including an empty area list. Later JSON edits do not silently
reset existing snapshots, add/remove areas or alter their hooks. Back up worlds
before upgrading or editing saved data. There is no destructive reset command.

### Area commands

All require operator level 2 or equivalent. They run in the source dimension:

```text
/void area list
/void area get bridge
/void area set homebound:colony/ship/bridge repaired
/execute in minecraft:overworld run void area set homebound:colony/ship/bridge repaired
```

`list` shows canonical ids and states; `get` also shows inclusive world bounds.
Unknown/ambiguous names or invalid states fail without mutation. Setting the
current state succeeds without firing another event or rerunning commands.

The bundled island, distant pad and deferred pad define `island`, `distant_pad`
and `deferred_pad`; the demonstration ship defines `cabin`. All start damaged,
with no hooks. These names are examples and do not add gameplay restrictions.

### Hooks and events

A real transition marks the new state for saving, fires change listeners
synchronously, then submits the commands for that new state to Minecraft's
command executor. When called inside another command/function, Minecraft may
queue hook execution until that command returns. Initialization/import does not count as a
change. Commands use a non-player server source at the saved structure origin,
in the area's dimension, at permission level **2**, with output suppressed.
`@s` has no entity. Selectors, functions and normal command permissions still
apply. Use `execute as` explicitly if a hook needs a player. Relative `~` positions
use the structure origin; they are world axes, not automatically rotated axes.

Supported substitutions: `{area}` (canonical id), `{state}`, `{previous}`, `{x}`,
`{y}`, `{z}` (structure origin), and `{rotation}` (vanilla rotation token). For a
rotated replacement template, pass all four position/rotation tokens as above.
The replacement template is supplied by the pack, not by this mod. Template
commands can overwrite player builds and entities, including outside the named
area. Only enable trusted pack commands, and design variants with matching bounds.

State remains committed if a listener or hook fails. Failures are logged, later
common listeners/commands continue, and hooks are not automatically retried or
rolled back. A crash before saving can lose a transition; command side effects
are not transactional or guaranteed exactly-once across crashes. Nested state
changes directly inside change listeners are rejected; read queries are allowed.
Hook commands can trigger further area transitions and follow vanilla command
ordering and chain/fork limits. Avoid cyclic state-changing hooks and never
raise those limits to work around a loop. A repeated same-state set is a harmless
no-op. A hook queued before another state change is not cancelled by that change.

### Public area API

Server thread only. Classes are under `com.workshopcraft.simplevoid.api`:

```java
VoidAreas.listAreas(level); // immutable, sorted List<VoidArea>
VoidAreas.getArea(level, "bridge"); // immutable snapshot
VoidAreas.getState(level, Identifier.parse("homebound:colony/ship/bridge"));
boolean changed = VoidAreas.setState(level, "bridge", "repaired");
var subscription = VoidAreaEvents.listen(change -> {
    // change.level(), change.before(), change.after()
});
subscription.close();
```

`getArea`, `getState` and `setState` accept a short/full string or an `Identifier`.
An `Identifier` is always a canonical id. Unknown/ambiguous names throw
`IllegalArgumentException`; wrong-thread/nested mutation throws
`IllegalStateException` for listener re-entry. State changes return false for a no-op, true for a
committed transition. `VoidArea` includes id, name, owning placement, origin,
rotation, world bounds, state and hook snapshot; `bounds().contains(blockPos)`
uses inclusive block bounds. Returned snapshots never mutate in place.

`VoidAreaEvents.StateChange` is loader-neutral, non-cancellable and contains the
level and immutable before/after snapshots. `listen` returns a closeable
subscription. Runtime listener failures are isolated and logged. On NeoForge,
every transition is also posted to `NeoForge.EVENT_BUS` as
`com.workshopcraft.simplevoid.neoforge.AreaStateChangedEvent`, with `getLevel()`,
`getBefore()` and `getAfter()`. NeoForge's bus controls its subscriber ordering
and exception handling. There is no Simple Books dependency or custom reward
registration required for commands.

### Simple Books quest rewards

Simple Books already accepts command rewards in a quest's `rewards`,
`choose_one`, or tier `rewards`. For a pack-defined bridge area:

```json
"rewards": [
  {
    "command": "execute in minecraft:overworld run void area set homebound:colony/ship/bridge repaired",
    "label": "Repair the bridge",
    "icon": "minecraft:iron_ingot"
  }
]
```

Commands execute on the server **as the claiming player**, with their dimension,
position and `@s`, but elevated to `command_reward_permission` in
`config/simplebooks.json` (default 2, allowed 0 to 4). The player need not be an
operator. Keep that setting at least 2 for `/void area`; use `execute in` to
address the right dimension. Output is hidden. Claims are per player and recorded
before rewards run; a command failure does not automatically unclaim or retry.
Simple Void's idempotent same-state behavior prevents later team members from
rerunning repair hooks on an already-repaired world area.

A quest detail page's claim button uses this reward flow. Merely opening an
ordinary guide page does not grant a reward or run a command. No Simple Books
engine change is needed for quest-command integration. Simple Books was inspected
read-only; its content and code were not changed. Exact commit and file/line
evidence are recorded in `tools/simplebooks-command-rewards.txt`.

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
  configuration, commands, areas, saved data, common events and spawn mixins.
- `common/versions/26.1.2`: common build node.
- `neoforge/src`, `neoforge/versions/26.1.2`: loader entrypoint and release node.
- `fabric`: retained source/metadata scaffold, not an enabled Stonecutter node
  and not built or shipped in this release.
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
./gradlew -q :neoforge:26.1.2:validateAreaEvents -x test -x javadoc
./gradlew -q :neoforge:26.1.2:build -x test -x javadoc -Pversion=1.0.0
```

The committed version is `1.0.0`. The NeoForge jar is under
`neoforge/versions/26.1.2/build/libs`. No Fabric build is included.
`tools/generate_structures.py` recreates the vanilla templates. Headless checks
load registries, codecs, templates, conditions and saved data only; they do not
construct worlds or start a Minecraft server/client.

## Validation

NeoForge 26.1.2 smoke passed on 6 Nov 2026 with the `0.1.0-alpha.1` dedicated
server jar and matching development client. All 17 in-game checks passed: fresh
void setup, exact island spawn, two entirely empty terrain chunks, distant pad
position/rotation, deferred condition gating, command placement, namespaced setup
commands and duplicate suppression with player edits preserved. The saved file
contained all three placement markers after shutdown. The 63 headless checks
also passed.

Real client screenshots and `SMOKE-RESULTS.txt` are in
`/workspace/mods/simplevoid-dist/0.1.0-alpha.1/`. The first smoke caught a command
parser bug with namespaced ids; `IdentifierArgument` replaced the restricted word
argument, and both the headless parser regressions and in-game retest passed.

The current repeatable harness is `tools/smoke-simplevoid.sh`, installed as
`/workspace/mods/smoke/simplevoid-m2.sh`, with `tools/smoke-client.gradle` supplying
client launch arguments. The historical M1-only script remains at
`/workspace/mods/smoke/simplevoid-m1.sh`. It sources `sm-b-lib.sh`, uses isolated fresh run
folders, takes read-only X11 client captures, and traps exit/failure to stop only
its own server/client. ModDev's shared test lock is required before every run.
The lock file is read-only for this script.

Version 1.0.0 passed 48 in-game smoke checks on 6 Nov 2026, including all 17 M1
checks, area list/get/set, rotated area bounds, common/NeoForge change events,
visible opt-in hooks, same-state no-ops, a full server/client restart with state
and hook counts preserved, non-operator command refusal, and a non-operator Simple
Books command reward. The saved area file was checked after shutdown. Exactly one
server/client pair ran at a time, and both were stopped afterward.

Simple Books 1.2.0 was installed from its existing NeoForge 26.1.2 distribution,
read-only. The test-only probe invokes the installed Claim packet handler for the
real non-op player, without bypassing completion, claim or reward checks. The
actual quest screen was captured before and after, showing Rewards claimed. This
covers the server claim path, not a synthetic mouse click.

`tools/create-smoke-pack.py` creates only test data: an island repair hook changes
a vanilla red-concrete marker to emerald and increments a scoreboard counter;
a quest reward runs `void area set island repaired`. Shipped defaults are not
changed. `tools/smoke-probe.gradle` builds the separate probe from
`tools/smoke-probe`; it is not included in the release jar. Set
`SIMPLEVOID_BOOKS_JAR` to the distributed Books jar when building the probe:

```sh
export SIMPLEVOID_BOOKS_JAR=/workspace/mods/simplebooks-261/dist/1.2.0/simplebooks-neoforge-26.1.2-1.2.0.jar
./gradlew -q -I tools/smoke-probe.gradle :neoforge:26.1.2:smokeProbeJar -x test -x javadoc
# Only after ModDev grants the shared server/client lock:
bash tools/smoke-simplevoid.sh
```

The fixture pack must load after `mod_data` before the initial area snapshots are
created. Full evidence and screenshots are under
`/workspace/mods/simplevoid-dist/1.0.0/`.

All 221 headless checks also passed for 1.0.0: 63 existing resource checks, 155
area checks, and 3 checks of the actual NeoForge event-bus bridge. Further coverage
is still needed for the production ship, ship interior spawns, bed/anchor respawns,
a normal world, an in-game M1-to-1.0.0 upgrade and other loaders.

M3 area hazards and M4 animated sky are planned for 1.1.0. Simple Books and
skyblock resource progression are untouched. No custom artwork or icon is bundled.
