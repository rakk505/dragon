#!/usr/bin/env python3
"""Render deterministic orthographic audit views from the final cuboid spec."""

from __future__ import annotations

import json
import math
from pathlib import Path

from PIL import Image, ImageDraw


HERE = Path(__file__).resolve().parent
SPEC = HERE / "model-spec.json"
OUT = HERE.parent / "renders"
CANVAS = 1024


def add(a, b):
    return tuple(a[i] + b[i] for i in range(3))


def sub(a, b):
    return tuple(a[i] - b[i] for i in range(3))


def mul(v, s):
    return tuple(component * s for component in v)


def dot(a, b):
    return sum(a[i] * b[i] for i in range(3))


def cross(a, b):
    return (a[1] * b[2] - a[2] * b[1],
            a[2] * b[0] - a[0] * b[2],
            a[0] * b[1] - a[1] * b[0])


def norm(v):
    length = math.sqrt(dot(v, v))
    return tuple(component / length for component in v)


def rotate(point, origin, degrees):
    x, y, z = sub(point, origin)
    rx, ry, rz = [math.radians(value) for value in degrees]
    cx, sx = math.cos(rx), math.sin(rx)
    cy, sy = math.cos(ry), math.sin(ry)
    cz, sz = math.cos(rz), math.sin(rz)
    y, z = y * cx - z * sx, y * sx + z * cx
    x, z = x * cy + z * sy, -x * sy + z * cy
    x, y = x * cz - y * sz, x * sz + y * cz
    return add((x, y, z), origin)


def shade(hex_color, factor):
    values = [int(hex_color[i:i + 2], 16) for i in (1, 3, 5)]
    values = [max(0, min(255, round(value * factor))) for value in values]
    return "#" + "".join(f"{value:02x}" for value in values)


FACE_INDICES = [
    (0, 1, 3, 2), (4, 6, 7, 5), (0, 4, 5, 1),
    (2, 3, 7, 6), (0, 2, 6, 4), (1, 5, 7, 3),
]


def apply_bone_pose(point, bone_name, bones_by_name, bone_rotations):
    current = bone_name
    transformed = point
    while current is not None:
        bone = bones_by_name[current]
        rotation = bone_rotations.get(current)
        if rotation is not None:
            transformed = rotate(transformed, bone["pivot"], rotation)
        current = bone["parent"]
    return transformed


def cube_geometry(cube, bones_by_name, bone_rotations):
    center, size = cube["center"], cube["size"]
    x0, y0, z0 = [center[i] - size[i] / 2 for i in range(3)]
    x1, y1, z1 = [center[i] + size[i] / 2 for i in range(3)]
    corners = [
        (x0, y0, z0), (x0, y0, z1), (x0, y1, z0), (x0, y1, z1),
        (x1, y0, z0), (x1, y0, z1), (x1, y1, z0), (x1, y1, z1),
    ]
    static_corners = [rotate(point, cube["origin"], cube["rotation"]) for point in corners]
    return [apply_bone_pose(point, cube["bone"], bones_by_name, bone_rotations) for point in static_corners]


def render(spec, filename, view, target=None, span=None, bone_rotations=None, save=True):
    view = norm(view)
    world_up = (0.0, 1.0, 0.0)
    if abs(dot(view, world_up)) > 0.98:
        world_up = (0.0, 0.0, 1.0)
    right = norm(cross(world_up, view))
    up = norm(cross(view, right))
    projected = []
    all_uv = []
    light = norm((0.4, 1.0, 0.7))
    materials = spec["materials"]
    bones_by_name = {bone["name"]: bone for bone in spec["bones"]}
    bone_rotations = bone_rotations or {}

    for cube in spec["cubes"]:
        corners = cube_geometry(cube, bones_by_name, bone_rotations)
        base = materials[cube["material"]]["base"]
        for indices in FACE_INDICES:
            verts = [corners[index] for index in indices]
            edge_a, edge_b = sub(verts[1], verts[0]), sub(verts[2], verts[0])
            normal = norm(cross(edge_a, edge_b))
            if dot(normal, view) <= 0.015:
                continue
            uv = [(dot(point, right), dot(point, up)) for point in verts]
            depth = sum(dot(point, view) for point in verts) / 4
            brightness = 0.62 + 0.38 * max(0.0, dot(normal, light))
            projected.append((depth, uv, shade(base, brightness)))
            all_uv.extend(uv)

    if target is None:
        min_u = min(point[0] for point in all_uv)
        max_u = max(point[0] for point in all_uv)
        min_v = min(point[1] for point in all_uv)
        max_v = max(point[1] for point in all_uv)
        center_u, center_v = (min_u + max_u) / 2, (min_v + max_v) / 2
        extent = max(max_u - min_u, max_v - min_v)
    else:
        center_u, center_v = dot(target, right), dot(target, up)
        extent = span
    scale = CANVAS * 0.82 / extent

    image = Image.new("RGB", (CANVAS, CANVAS), "#d9e5df")
    draw = ImageDraw.Draw(image)
    for _, uv, color in sorted(projected, key=lambda entry: entry[0]):
        poly = [
            (round(CANVAS / 2 + (u - center_u) * scale),
             round(CANVAS / 2 - (v - center_v) * scale))
            for u, v in uv
        ]
        draw.polygon(poly, fill=color, outline="#151a17", width=2)

    if save:
        OUT.mkdir(parents=True, exist_ok=True)
        image.save(OUT / filename)
    return image


def main():
    spec = json.loads(SPEC.read_text(encoding="utf-8"))
    views = [
        ("front.png", (0, 0.12, 1), None, None),
        ("back.png", (0, 0.12, -1), None, None),
        ("left.png", (1, 0.12, 0), None, None),
        ("right.png", (-1, 0.12, 0), None, None),
        ("isometric.png", (1, 0.65, 1), None, None),
        ("head_left_closeup.png", (1, 0.25, 1), (0, 21, 35), 42),
        ("head_right_closeup.png", (-1, 0.25, 1), (0, 21, 35), 42),
        ("connected_joints_closeup.png", (1, 0.55, 1), (0, 17.5, -15), 42),
    ]
    for filename, view, target, span in views:
        render(spec, filename, view, target, span)
    print(f"rendered {len(views)} audit views to {OUT}")


if __name__ == "__main__":
    main()
