#!/usr/bin/env python3
import gzip
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "common/src/main/resources/data/simplevoid/structure"


def string(value):
    data = value.encode("utf-8")
    return struct.pack(">H", len(data)) + data


def payload(kind, value):
    if kind == 3:
        return struct.pack(">i", value)
    if kind == 8:
        return string(value)
    if kind == 9:
        element, values = value
        return bytes([element]) + struct.pack(">i", len(values)) + b"".join(payload(element, item) for item in values)
    if kind == 10:
        return b"".join(bytes([tag]) + string(name) + payload(tag, data) for name, (tag, data) in value.items()) + b"\0"
    raise ValueError(kind)


def save(name, size, blocks):
    palette = []
    states = {}
    entries = []
    for y in range(size[1]):
        for z in range(size[2]):
            for x in range(size[0]):
                state = blocks.get((x, y, z), ("air", ()))
                if state not in states:
                    states[state] = len(palette)
                    entry = {"Name": (8, "minecraft:" + state[0])}
                    if state[1]:
                        entry["Properties"] = (10, {key: (8, value) for key, value in state[1]})
                    palette.append(entry)
                entries.append({"pos": (9, (3, [x, y, z])), "state": (3, states[state])})
    data = {"DataVersion": (3, 4790), "size": (9, (3, list(size))),
            "palette": (9, (10, palette)), "blocks": (9, (10, entries)), "entities": (9, (10, []))}
    ROOT.mkdir(parents=True, exist_ok=True)
    (ROOT / (name + ".nbt")).write_bytes(gzip.compress(b"\x0a\0\0" + payload(10, data), mtime=0))


def island():
    blocks = {}
    for y, radius in [(0, 1), (1, 2), (2, 3)]:
        for x in range(4 - radius, 5 + radius):
            for z in range(4 - radius, 5 + radius):
                if radius == 3 and abs(x - 4) == 3 and abs(z - 4) == 3:
                    continue
                blocks[x, y, z] = ("grass_block", (("snowy", "false"),)) if y == 2 else ("dirt", ())
    for y in range(3, 7):
        blocks[2, y, 6] = ("oak_log", (("axis", "y"),))
    for y, radius in [(5, 2), (6, 2), (7, 1), (8, 1)]:
        for x in range(2 - radius, 3 + radius):
            for z in range(6 - radius, 7 + radius):
                if abs(x - 2) == radius and abs(z - 6) == radius:
                    continue
                blocks.setdefault((x, y, z), ("oak_leaves", (("distance", str(min(7, abs(x - 2) + abs(z - 6) + max(0, y - 6)))), ("persistent", "false"), ("waterlogged", "false"))))
    save("island", (9, 9, 9), blocks)


def ship():
    blocks = {}
    for x in range(9):
        for z in range(13):
            if x in (0, 8) and z in (0, 12):
                continue
            blocks[x, 0, z] = ("iron_block", ())
            blocks[x, 5, z] = ("white_concrete", ())
            if x in (0, 8) or z in (0, 12):
                for y in range(1, 5):
                    window = y in (2, 3) and (x in (0, 8) and z in (3, 4, 8, 9) or z == 0 and 2 <= x <= 6)
                    blocks[x, y, z] = ("glass" if window else "gray_concrete", ())
    for x in (2, 6):
        for z in (3, 9):
            blocks[x, 5, z] = ("sea_lantern", ())
    save("ship", (9, 6, 13), blocks)


def outpost():
    blocks = {(x, 0, z): ("stone_bricks", ()) for x in range(5) for z in range(5)}
    save("outpost", (5, 3, 5), blocks)


if __name__ == "__main__":
    island()
    ship()
    outpost()
    print("Generated vanilla island and demonstration ship")
