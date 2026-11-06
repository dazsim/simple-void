#!/usr/bin/env python3
import gzip
import io
import json
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "common/src/main/resources"


def read_nbt(path):
    stream = io.BytesIO(gzip.decompress(path.read_bytes()))

    def integer():
        return struct.unpack(">i", stream.read(4))[0]

    def string():
        return stream.read(struct.unpack(">H", stream.read(2))[0]).decode("utf-8")

    def payload(kind):
        if kind == 3:
            return integer()
        if kind == 8:
            return string()
        if kind == 9:
            element = stream.read(1)[0]
            return [payload(element) for _ in range(integer())]
        if kind == 10:
            result = {}
            while (tag := stream.read(1)[0]) != 0:
                name = string()
                assert name not in result
                result[name] = payload(tag)
            return result
        raise AssertionError(f"Unexpected NBT type {kind}")

    assert stream.read(1) == b"\x0a"
    assert string() == ""
    result = payload(10)
    assert stream.read() == b""
    return result


def verify():
    for path in ROOT.rglob("*.json"):
        if "build" not in path.parts and ".gradle" not in path.parts:
            json.loads(path.read_text())
    preset = json.loads((RESOURCES / "data/simplevoid/worldgen/world_preset/void.json").read_text())
    generator = preset["dimensions"]["minecraft:overworld"]["generator"]
    assert generator["type"] == "minecraft:flat"
    assert generator["settings"] == {"biome": "simplevoid:void", "layers": [], "features": False, "lakes": False, "structure_overrides": []}
    assert set(preset["dimensions"]) == {"minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"}
    biome = json.loads((RESOURCES / "data/simplevoid/worldgen/biome/void.json").read_text())
    assert biome["features"] == biome["carvers"] == []
    selector = json.loads((RESOURCES / "data/simplevoid/simplevoid/world_setup.json").read_text())
    assert selector == {"setup": "simplevoid:skyblock"}
    templates = {}
    for name in ("island", "ship", "end_portal_island", "outpost"):
        path = (ROOT / "tools/smoke-data/data/simplevoid_smoke/structure/outpost.nbt" if name == "outpost"
                else RESOURCES / f"data/simplevoid/structure/{name}.nbt")
        template = read_nbt(path)
        assert template["DataVersion"] == 4790 and not template["entities"]
        assert all(state["Name"].startswith("minecraft:") for state in template["palette"])
        blocks = {}
        for entry in template["blocks"]:
            pos = tuple(entry["pos"])
            assert pos not in blocks
            assert all(0 <= pos[i] < template["size"][i] for i in range(3))
            blocks[pos] = template["palette"][entry["state"]]["Name"]
        assert len(blocks) == template["size"][0] * template["size"][1] * template["size"][2]
        templates[name] = (template, blocks)
        print(f"{name}: {template['size']}, vanilla palette")
    assert not (RESOURCES / "data/simplevoid/structure/outpost.nbt").exists()
    template, blocks = templates["end_portal_island"]
    frames = {tuple(entry["pos"]): template["palette"][entry["state"]]["Properties"]
              for entry in template["blocks"]
              if template["palette"][entry["state"]]["Name"] == "minecraft:end_portal_frame"}
    expected = {}
    for value in range(3, 6):
        for x, z, facing in ((value, 2, "south"), (value, 6, "north"),
                             (2, value, "east"), (6, value, "west")):
            expected[x, 2, z] = {"facing": facing, "eye": "false"}
    assert len(frames) == 12 and frames == expected
    assert set(blocks.values()) == {"minecraft:air", "minecraft:end_stone", "minecraft:end_portal_frame"}
    assert all(blocks[x, y - 1, z] == "minecraft:end_stone" for x, y, z in frames)
    assert all(blocks[x, 2, z] == blocks[x, 3, z] == "minecraft:air" for x in range(3, 6) for z in range(3, 6))
    print("End portal island: 12 inward frames, no eyes, end-stone support, clear center")
    for name in ("skyblock", "skyblock_end_portal", "ship"):
        setup = json.loads((RESOURCES / f"data/simplevoid/simplevoid/world_setup/{name}.json").read_text())
        placements = {placement["name"]: placement for placement in setup["placements"]}
        assert len(placements) == len(setup["placements"])
        start = placements[setup["spawn"]["placement"]]
        assert "fixed" in start["position"] and start["at_creation"]
        template, blocks = templates[start["structure"].split(":")[1]]
        x, y, z = setup["spawn"]["offset"]
        assert blocks[x, y - 1, z] not in ("minecraft:air", "minecraft:water", "minecraft:lava")
        assert blocks[x, y, z] == blocks[x, y + 1, z] == "minecraft:air"
        if name == "ship":
            assert blocks[x, template["size"][1] - 1, z] != "minecraft:air"
        if name.startswith("skyblock"):
            assert set(placements) == {"start", "end_portal"}
            portal = placements["end_portal"]
            assert portal["structure"] == "simplevoid:end_portal_island"
            assert portal["position"] == {"spawn_offset": [1024, -3, 0]}
            assert portal["at_creation"] and portal["rotation"] == "none"
            assert portal["condition"]["enabled"] == (name == "skyblock_end_portal")
            eligible = [p["name"] for p in placements.values() if p["at_creation"] and p.get("condition", {}).get("enabled", True)]
            assert eligible == (["start"] if name == "skyblock" else ["start", "end_portal"])
        print(f"{name} setup: {len(placements)} placements, safe spawn {setup['spawn']['offset']}")
    smoke = json.loads((ROOT / "tools/smoke-data/data/simplevoid/simplevoid/world_setup/skyblock.json").read_text())
    examples = {p["name"]: p for p in smoke["placements"]}
    assert set(examples) == {"start", "distant_example", "later_example"}
    assert examples["distant_example"]["position"]["spawn_offset"] == [256, -3, 0]
    assert not examples["later_example"]["at_creation"]
    assert all(examples[name]["structure"] == "simplevoid_smoke:outpost" for name in ("distant_example", "later_example"))
    print("Distant and deferred examples retained only in smoke data")
    logo = RESOURCES / "simplevoid_logo.png"
    image = logo.read_bytes()
    assert image[:8] == b"\x89PNG\r\n\x1a\n" and struct.unpack(">II", image[16:24]) == (400, 400)
    fabric = json.loads((ROOT / "fabric/src/main/resources/fabric.mod.json").read_text())
    assert fabric["icon"] == logo.name
    assert f'logoFile = "{logo.name}"' in (ROOT / "neoforge/src/main/resources/META-INF/neoforge.mods.toml").read_text()
    print("Approved 400 x 400 logo and loader metadata paths verified")
    assert not list(RESOURCES.glob("assets/*/textures/**/"))
    print("Resource checks passed")


if __name__ == "__main__":
    verify()
