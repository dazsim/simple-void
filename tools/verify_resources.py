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
    for name in ("island", "ship", "outpost"):
        template = read_nbt(RESOURCES / f"data/simplevoid/structure/{name}.nbt")
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
    for name in ("skyblock", "ship"):
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
        if name == "skyblock":
            assert placements["distant_example"]["position"]["spawn_offset"] == [256, -3, 0]
            assert not placements["later_example"]["at_creation"]
        print(f"{name} setup: {len(placements)} placements, safe spawn {setup['spawn']['offset']}")
    assert not list(RESOURCES.glob("assets/*/textures/**/"))
    print("Resource checks passed")


if __name__ == "__main__":
    verify()
