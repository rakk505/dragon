#!/usr/bin/env python3
"""Render GIF/contact-sheet reviews of the animation-ready axial rig."""

from __future__ import annotations

import json
import math
from pathlib import Path

from PIL import Image, ImageDraw

from render_audit_views import render


HERE = Path(__file__).resolve().parent
SPEC = json.loads((HERE / "model-spec.json").read_text(encoding="utf-8"))
OUT = HERE.parent / "renders"


def flight_rotation(index, cycle):
    delayed = cycle - index * 0.62
    taper = index / 15.0
    return [
        math.cos(cycle - index * 0.49 + 0.55) * (4.2 + taper * 3.0),
        math.sin(delayed) * (7.0 + taper * 4.0)
        + math.sin(cycle * 2.0 - index * 0.91) * (1.2 + taper * 0.8),
        math.sin(cycle - index * 0.40 + 1.1) * (1.8 + taper * 1.2),
    ]


def pose_flight(phase):
    result = {"head": [math.sin(phase) * 2.0, math.sin(phase) * 2.0, 0.0]}
    for index in range(16):
        result[f"axial_{index:02d}"] = flight_rotation(index, phase)

    legs = (
        ("front_left", 1.0, 0.00),
        ("front_right", -1.0, 1.43),
        ("rear_left", 1.0, 3.21),
        ("rear_right", -1.0, 4.86),
    )
    for prefix, side, phase_offset in legs:
        cycle = phase + phase_offset
        stride = math.sin(cycle)
        sweep = math.cos(cycle + phase_offset * 0.19)
        result[prefix + "_upper"] = [10.0 + stride * 18.0, side * sweep * 5.5, side * (4.0 + stride * 4.5)]
        result[prefix + "_lower"] = [22.0 - math.sin(cycle - 0.72) * 16.0, side * stride * 3.0, -side * sweep * 2.5]
        result[prefix + "_paw"] = [-14.0 + math.sin(cycle - 1.22) * 12.0, side * sweep * 2.0, side * stride * 3.5]
    return result


def pose_coil(progress):
    # Wind to a little over one turn, then snap back during the final quarter.
    if progress <= 0.72:
        amount = progress / 0.72
    else:
        amount = max(0.0, 1.0 - (progress - 0.72) / 0.28)
    amount = amount * amount * (3.0 - 2.0 * amount)
    result = {"head": [-12.0 * amount, -8.0 * amount, 0.0]}
    for index in range(16):
        result[f"axial_{index:02d}"] = [
            math.sin(index * 0.52) * 2.0 * amount,
            (19.2 + math.sin(index * 0.72)) * amount,
            0.0,
        ]
    return result


def make_animation(name, poser, frame_count, view=(1.0, 0.58, 1.0)):
    frames = []
    for frame_index in range(frame_count):
        progress = frame_index / frame_count
        image = render(
            SPEC,
            "unused.png",
            view,
            target=(0.0, 18.0, -14.0),
            span=132.0,
            bone_rotations=poser(progress),
            save=False,
        ).resize((512, 512), Image.Resampling.NEAREST)
        frames.append(image)
    frames[0].save(
        OUT / f"{name}.gif",
        save_all=True,
        append_images=frames[1:],
        duration=85,
        loop=0,
        optimize=False,
    )
    samples = [frames[index] for index in (0, frame_count // 4, frame_count // 2, 3 * frame_count // 4)]
    sheet = Image.new("RGB", (1024, 1024), "#d9e5df")
    for index, sample in enumerate(samples):
        sheet.paste(sample, ((index % 2) * 512, (index // 2) * 512))
    ImageDraw.Draw(sheet).text((18, 18), name.replace("_", " ").upper(), fill="#191714")
    sheet.save(OUT / f"{name}_contact_sheet.png")


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    make_animation("flight_wave", lambda progress: pose_flight(progress * math.tau), 24)
    make_animation("flight_wave_top", lambda progress: pose_flight(progress * math.tau), 24, (0.0, 1.0, 0.0))
    make_animation("flight_wave_side", lambda progress: pose_flight(progress * math.tau), 24, (1.0, 0.0, 0.0))
    make_animation("coil_attack", pose_coil, 32)
    print("rendered three-view flight and coil animation previews")


if __name__ == "__main__":
    main()
