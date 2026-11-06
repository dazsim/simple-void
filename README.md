# Simple Void

Minecraft 26.1 to 26.1.2, Java 25, NeoForge and Fabric. Mod id: `simplevoid`.

M1 provides a void Overworld preset and one starting structure per world. It does
not add blocks, items, recipes, portals, quests, hazards or a space backdrop.
Hull and boundary blocks belong to the Simple Machines ship blocks add-on.

## Create a world

Choose **Simple Void** as the world type. On a dedicated server, set
`level-type=simplevoid:void` in `server.properties` before creating a new world.
Fabric requires Fabric Loader and Fabric API.

The Overworld has no terrain, generated structures, lakes or decoration. Its
marker biome keeps ordinary plains weather, colours and mob spawning. The Nether
and End are vanilla. Existing worlds and other world types are not changed.

The default start is a small grass and dirt island with an oak tree, at origin
`0 64 0`. Players start at `4.5 67 2.5`. There is no chest or resource progression
added. The optional `simplevoid:ship` is a small, empty vanilla-block demonstration
cabin, not Homebound's final ship. Its spawn is inside, beneath the roof.

## Choose a start

The highest-priority data pack resource
`data/simplevoid/simplevoid/start.json` selects the start definition:

```json
{"start": "simplevoid:island"}
```

Use `simplevoid:ship` for the bundled demonstration ship. The ready-to-use pack in
`examples/ship-start` switches to it. Enable that pack before world creation.

Alternatively, set `start=simplevoid:ship` in `config/simplevoid.properties`.
The `start` key selects a **definition id**, not an NBT structure id. A non-blank
config value overrides the data pack selector. Blank, the generated default,
uses the selector. The file is created on the first Simple Void world creation.
Selection is read only when creating a world, not on `/reload` or reconnect.

## Supply a Homebound ship

For a definition id `homebound:ship`, provide
`data/homebound/simplevoid/start/ship.json` in a data pack:

```json
{
  "structure": "homebound:colony_ship",
  "origin": [0, 64, 0],
  "spawn_offset": [4, 1, 5],
  "yaw": 180
}
```

The matching structure file is
`data/homebound/structure/colony_ship.nbt`, a normal compressed structure template
saved with a structure block. The structure can come from another mod or any
enabled data pack. Templates may contain modded blocks when their mod is loaded.
Use a relative template offset for the player's feet, not world coordinates.
The example offset must be adjusted to the real ship's safe interior position.

Select it with `{"start":"homebound:ship"}` in the selector, or
`start=homebound:ship` in the config. `origin` defaults to `[0,64,0]`; `yaw`
defaults to `0` and accepts `-180` through `180`. `structure` and `spawn_offset`
are required. Template placement has no rotation or mirroring.

The template must have a solid floor at the spawn and two clear, dry blocks for
the player, entirely inside its saved bounds. Include the spawn air/headroom in
the structure-block selection. Limits are 256 blocks on each axis and 1,048,576
blocks in the bounding volume, inside the world's build height and border.
Invalid configuration, missing structures or unsafe spawns abort world creation
with an explanation rather than dropping players into the void. Fix the data and
create a fresh test world after a failed placement.

## Placement and respawning

The structure is placed before vanilla's initial spawn search, once at world
creation. The chosen definition is saved as namespaced world saved data. Normal
world loads, joins, deaths, data reloads and config changes never place it again.
Player edits to the island or ship are preserved. This is not a per-player island
system.

World spawn is set to the configured point and `minecraft:respawn_radius` is set
to zero on creation. A narrow spawn hook keeps new players and fallback respawns
at that point, including under ship roofs, instead of the surface heightmap.
Beds and respawn anchors keep their normal behaviour. Changing world spawn or
setting a nonzero respawn radius restores vanilla spawn searching. Destroying or
blocking the saved spawn also falls back to vanilla; the mod does not repair the
structure or manufacture a rescue platform. Bonus-chest generation is skipped in
this preset: any supplies belong in the pack's template.

## Layout and builds

- `common`: preset, biome, start data, templates, configuration, placement and
  spawn mixins shared by both loaders.
- `neoforge` and `fabric`: small loader entrypoints and metadata.
- `build-logic`, `gradle`: build conventions and wrapper copied from More Lighting.
- `tools`: deterministic template generator, resource checks and a headless Java
  codec/configuration verifier.
- `examples/ship-start`: optional start-selection data pack.

Versions and dependencies are centralised in `gradle.properties`; source and
resources are separate from the loader builds so a Stonecutter controller can add
version projects later. Only 26.1.2 is configured now. No ports or duplicate
version trees are included. The committed version is `0.1.0`.

```sh
export JAVA_HOME=/home/box/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2
export PATH=$JAVA_HOME/bin:$PATH
# Iterate only NeoForge
./gradlew -q build -Ploaders=neoforge -x test -x javadoc
python3 tools/verify_resources.py
./gradlew -q :common:validateResources -Ploaders=neoforge -x test -x javadoc
# Both loaders for the alpha
./gradlew -q build -x test -x javadoc -Pversion=0.1.0-alpha.1
```

Loader jars are written under each loader's `build/libs`. The `loaders` property
also avoids configuring Fabric while iterating. `tools/generate_structures.py`
recreates the bundled vanilla templates without an art generator or game launch.
The Java verifier loads registries, codecs, templates and saved data only; it does
not construct a world or start a Minecraft server/client.

## Validation status

Compile and headless checks are not an in-game test. Shared-server testing and
screenshots require ModDev's lock and have not been run for this alpha.

When that slot is available, test on both loaders:

1. Create Simple Void with the default island; verify the island and exact spawn.
2. Travel away and verify empty terrain. Check a normal world remains normal.
3. Edit blocks, stop, reload and reconnect; verify there is no second placement.
4. Join as another player, die without a bed, then test a bed respawn.
5. Select the ship through config and through the example pack in fresh worlds;
   verify spawning inside rather than on its roof. Check config precedence.
6. Try a custom namespaced ship template, invalid id and obstructed spawn.
7. Reload data and change selection after creation; verify the start is unchanged.

Homebound's production ship, named areas, repairs, hazards and animated sky are
later work. Simple Books and skyblock resource progression are untouched.
