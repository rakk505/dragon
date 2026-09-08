#!/usr/bin/env python3
"""Generate Blockbench/Bedrock animation clips for the portable dragon rig."""

from __future__ import annotations

import json
import math
from pathlib import Path


HERE = Path(__file__).resolve().parent
OUT = HERE.parent / "animations" / "chinese_dragon_boss.animation.json"
FLIGHT_DURATION = 2.4
FLIGHT_SAMPLES = 12


def keyframes(*entries):
    return {f"{time:.2f}": [round(x, 3), round(y, 3), round(z, 3)] for time, (x, y, z) in entries}


def flight_rotation(index, cycle):
    """A travelling 3-D curvature wave, expressed as local joint rotations."""
    delayed = cycle - index * 0.62
    taper = index / 15.0
    pitch = math.cos(cycle - index * 0.49 + 0.55) * (4.2 + taper * 3.0)
    yaw = (
        math.sin(delayed) * (7.0 + taper * 4.0)
        + math.sin(cycle * 2.0 - index * 0.91) * (1.2 + taper * 0.8)
    )
    roll = math.sin(cycle - index * 0.40 + 1.1) * (1.8 + taper * 1.2)
    return pitch, yaw, roll


def flight_leg_bones():
    result = {}
    # Every limb has its own non-quadrature phase so the silhouette never reads
    # as mirrored pairs.  The three joints also lag one another like a relaxed,
    # swimming/flying stride rather than rotating as one rigid appendage.
    legs = (
        ("front_left", 1.0, 0.00),
        ("front_right", -1.0, 1.43),
        ("rear_left", 1.0, 3.21),
        ("rear_right", -1.0, 4.86),
    )
    for prefix, side, phase_offset in legs:
        upper_samples = []
        lower_samples = []
        paw_samples = []
        for frame in range(FLIGHT_SAMPLES + 1):
            time = FLIGHT_DURATION * frame / FLIGHT_SAMPLES
            cycle = math.tau * frame / FLIGHT_SAMPLES + phase_offset
            stride = math.sin(cycle)
            sweep = math.cos(cycle + phase_offset * 0.19)
            upper_samples.append((time, (10.0 + stride * 18.0, side * sweep * 5.5, side * (4.0 + stride * 4.5))))
            lower_samples.append((time, (22.0 - math.sin(cycle - 0.72) * 16.0, side * stride * 3.0, -side * sweep * 2.5)))
            paw_samples.append((time, (-14.0 + math.sin(cycle - 1.22) * 12.0, side * sweep * 2.0, side * stride * 3.5)))
        result[prefix + "_upper"] = {"rotation": keyframes(*upper_samples)}
        result[prefix + "_lower"] = {"rotation": keyframes(*lower_samples)}
        result[prefix + "_paw"] = {"rotation": keyframes(*paw_samples)}
    return result


def flight_bones():
    result = {}
    for index in range(16):
        samples = []
        for frame in range(FLIGHT_SAMPLES + 1):
            time = FLIGHT_DURATION * frame / FLIGHT_SAMPLES
            cycle = math.tau * frame / FLIGHT_SAMPLES
            samples.append((time, flight_rotation(index, cycle)))
        result[f"axial_{index:02d}"] = {"rotation": keyframes(*samples)}
    result.update(flight_leg_bones())
    result["jaw"] = {"rotation": keyframes((0.0, (2.5, 0, 0)), (1.2, (4.0, 0, 0)), (2.4, (2.5, 0, 0)))}
    return result


def attack_bones(kind):
    result = {}
    if kind == "rush":
        for index in range(16):
            wave = math.sin(index * 0.55) * 5.0
            result[f"axial_{index:02d}"] = {
                "rotation": keyframes((0.0, (0, wave, 0)), (1.0, (0, 0, 0)), (2.5, (0, 0, 0)), (3.0, (0, wave, 0)))
            }
        result["head"] = {"rotation": keyframes((0.0, (0, 0, 0)), (1.0, (-19, 0, 0)), (1.5, (12, 0, 0)), (3.0, (0, 0, 0)))}
        result["jaw"] = {"rotation": keyframes((0.0, (3, 0, 0)), (1.0, (13, 0, 0)), (1.5, (5, 0, 0)), (3.0, (3, 0, 0)))}
    elif kind == "coil":
        for index in range(16):
            tight = 19.2 + math.sin(index * 0.72)
            result[f"axial_{index:02d}"] = {
                "rotation": keyframes((0.0, (0, 0, 0)), (3.0, (math.sin(index * 0.52) * 2, tight, 0)),
                                     (3.5, (math.sin(index * 0.52) * 3, tight + 2, 0)), (4.7, (0, 0, 0)), (5.0, (0, 0, 0)))
            }
        result["jaw"] = {"rotation": keyframes((0.0, (3, 0, 0)), (3.0, (28, 0, 0)), (3.5, (52, 0, 0)), (4.7, (3, 0, 0)))}
    elif kind == "fireball":
        result["head"] = {"rotation": keyframes((0.0, (0, 0, 0)), (1.0, (-18, 0, 0)), (1.12, (13, 0, 0)),
                                               (1.6, (-18, 0, 0)), (1.72, (13, 0, 0)), (2.2, (-18, 0, 0)),
                                               (2.32, (13, 0, 0)), (3.25, (0, 0, 0)))}
        result["jaw"] = {"rotation": keyframes((0.0, (3, 0, 0)), (1.0, (34, 0, 0)), (2.4, (34, 0, 0)), (3.25, (3, 0, 0)))}
    elif kind == "beam":
        result["head"] = {"rotation": keyframes((0.0, (0, 0, 0)), (1.5, (-7, 0, 0)), (3.25, (-7, 0, 0)), (4.0, (0, 0, 0)))}
        result["jaw"] = {"rotation": keyframes((0.0, (3, 0, 0)), (1.5, (32, 0, 0)), (3.25, (32, 0, 0)), (4.0, (3, 0, 0)))}
    return result


payload = {
    "format_version": "1.8.0",
    "animations": {
        "animation.chinese_dragon_boss.flight": {"loop": True, "animation_length": FLIGHT_DURATION, "bones": flight_bones()},
        "animation.chinese_dragon_boss.rush": {"loop": False, "animation_length": 3.0, "bones": attack_bones("rush")},
        "animation.chinese_dragon_boss.coil": {"loop": False, "animation_length": 5.0, "bones": attack_bones("coil")},
        "animation.chinese_dragon_boss.fireball": {"loop": False, "animation_length": 3.25, "bones": attack_bones("fireball")},
        "animation.chinese_dragon_boss.beam": {"loop": False, "animation_length": 4.0, "bones": attack_bones("beam")},
    },
}

OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
print(f"wrote {OUT} with {len(payload['animations'])} clips")
