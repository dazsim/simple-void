#!/usr/bin/env python3
import json
import sys
from pathlib import Path

root = Path(__file__).resolve().parents[1]
out = Path(sys.argv[1])

def write(path, value):
    target = out / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(value, indent=2) + "\n")

write("pack.mcmeta", {"pack": {"description": "Simple Void M2 smoke fixtures only", "min_format": 84, "max_format": [101, 1]}})
setup = json.loads((root / "common/src/main/resources/data/simplevoid/simplevoid/world_setup/skyblock.json").read_text())
setup["placements"][0]["areas"][0]["on_state_change"] = {
    "enabled": True,
    "commands": {
        "repaired": ["scoreboard players add #repair sv_hooks 1", "setblock ~5 ~3 ~2 minecraft:emerald_block"],
        "damaged": ["setblock ~5 ~3 ~2 minecraft:red_concrete"],
    },
}
write("data/simplevoid/simplevoid/world_setup/skyblock.json", setup)
write("data/simplevoid/simplebooks/quests/smoke/chapters/repairs.json", {
    "title": "Simple Void smoke test", "icon": "minecraft:iron_ingot", "sort": 1,
    "layout": "story", "text": "Non-operator command reward integration.",
})
write("data/simplevoid/simplebooks/quests/smoke/quests/repair.json", {
    "chapter": "repairs", "title": "Repair the island", "subtitle": "Simple Void + Simple Books",
    "icon": "minecraft:emerald_block", "sort": 1,
    "text": "Claim as a non-operator. The reward repairs the island area and turns its red marker green.",
    "tasks": [{"id": "visit", "type": "dimension", "dimension": "minecraft:overworld"}],
    "rewards": [{"command": "void area set island repaired", "label": "Repair the island area", "icon": "minecraft:emerald_block"}],
})
print(out)
