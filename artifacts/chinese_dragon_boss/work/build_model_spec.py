#!/usr/bin/env python3
"""Author the deterministic 96-cuboid Chinese dragon model specification."""

from __future__ import annotations

import hashlib
import json
import math
from pathlib import Path

from PIL import Image


HERE = Path(__file__).resolve().parent
REFERENCE = HERE.parent / "references" / "chinese_dragon_concept.png"
OUTPUT = HERE / "model-spec.json"


def rounded(values):
    return [round(float(value), 4) for value in values]


bones: list[dict] = []
cubes: list[dict] = []
landmarks: list[dict] = []


def add_bone(name: str, parent: str | None, pivot):
    bones.append({"name": name, "parent": parent, "pivot": rounded(pivot)})


def add_cube(name: str, bone: str, center, size, role: str, material: str,
             origin=None, rotation=(0, 0, 0), faces=None):
    cubes.append({
        "name": name,
        "bone": bone,
        "center": rounded(center),
        "size": rounded(size),
        "rotation": rounded(rotation),
        "origin": rounded(origin if origin is not None else center),
        "role": role,
        "material": material,
        "faces": faces or {},
    })


def add_between(name: str, bone: str, start, end, thickness, role: str,
                material: str, origin=None):
    dx = end[0] - start[0]
    dy = end[1] - start[1]
    dz = end[2] - start[2]
    horizontal = math.hypot(dx, dz)
    length = math.sqrt(dx * dx + dy * dy + dz * dz)
    yaw = math.degrees(math.atan2(dx, dz))
    pitch = -math.degrees(math.atan2(dy, horizontal))
    # Blockbench rotates both the cube and its offset around ``origin``. Author
    # the unrotated box along local +Z; the Euler rotation then places its
    # center on the requested world-space segment without double-rotating it.
    center = [start[0], start[1], start[2] + length / 2]
    if isinstance(thickness, (tuple, list)):
        sx, sy = thickness
    else:
        sx = sy = thickness
    add_cube(name, bone, center, [sx, sy, length + 0.5], role, material,
             origin=start if origin is None else origin,
             rotation=[pitch, yaw, 0])


# Head/neck joint is the single rig root. The axial chain then runs continuously
# through sixteen overlapping sections to a strongly tapered tail.
axial_points = [
    (0.0, 18.0, 28.0), (0.0, 18.2, 22.0), (0.8, 18.5, 16.0),
    (1.8, 18.7, 10.0), (2.5, 18.3, 4.0), (2.2, 17.8, -2.0),
    (1.4, 17.3, -8.0), (0.2, 17.0, -14.0), (-1.0, 17.2, -20.0),
    (-1.8, 17.7, -26.0), (-2.0, 18.2, -32.0), (-1.6, 18.5, -38.0),
    (-0.8, 18.3, -44.0), (0.1, 17.9, -50.0), (0.9, 17.4, -56.0),
    (1.3, 17.0, -61.5), (1.5, 16.7, -66.5),
]
widths = [8.0, 8.5, 9.0, 9.2, 9.0, 8.8, 8.5, 8.2,
          7.8, 7.3, 6.8, 6.2, 5.5, 4.8, 4.0, 3.2]
heights = [7.0, 7.2, 7.5, 7.6, 7.5, 7.3, 7.0, 6.8,
           6.4, 6.0, 5.6, 5.1, 4.6, 4.0, 3.4, 2.8]

add_bone("root", None, axial_points[0])
add_bone("head", "root", axial_points[0])
add_bone("jaw", "head", (0, 16.0, 38.0))

for index in range(16):
    parent = "root" if index == 0 else f"axial_{index - 1:02d}"
    add_bone(f"axial_{index:02d}", parent, axial_points[index])

for index in range(16):
    start, end = axial_points[index], axial_points[index + 1]
    add_between(
        f"body_segment_{index:02d}", f"axial_{index:02d}", start, end,
        (widths[index], heights[index]),
        "neck segment" if index < 3 else ("tail segment" if index >= 11 else "serpentine body segment"),
        "jade_scales", origin=start,
    )
    cubes[-1]["faces"] = {"down": {"material": "gold_belly"}}

# Twelve characteristic head volumes.
head_parts = [
    ("cranium", "head", (0, 20.2, 33.0), (10.0, 8.5, 10.0), "broad ox-like head", "jade_scales"),
    ("upper_snout", "head", (0, 18.2, 40.5), (8.0, 5.0, 7.5), "upper muzzle", "jade_light"),
    ("lower_jaw", "jaw", (0, 15.3, 40.2), (8.0, 2.5, 7.5), "articulated lower jaw", "gold_belly"),
    ("eye_left", "head", (4.7, 21.8, 35.2), (3.0, 3.2, 3.0), "bulging left eye", "eye_gold"),
    ("eye_right", "head", (-4.7, 21.8, 35.2), (3.0, 3.2, 3.0), "bulging right eye", "eye_gold"),
    ("brow_left", "head", (4.4, 23.7, 35.0), (2.2, 2.0, 5.5), "left heavy brow", "vermilion_mane"),
    ("brow_right", "head", (-4.4, 23.7, 35.0), (2.2, 2.0, 5.5), "right heavy brow", "vermilion_mane"),
    ("ear_left", "head", (5.5, 21.4, 29.7), (2.0, 4.5, 3.0), "left pointed ear", "deep_jade"),
    ("ear_right", "head", (-5.5, 21.4, 29.7), (2.0, 4.5, 3.0), "right pointed ear", "deep_jade"),
    ("chin_beard", "jaw", (0, 12.8, 36.8), (3.5, 4.0, 5.0), "catfish beard", "vermilion_mane"),
    ("nose_bridge", "head", (0, 20.0, 38.5), (4.5, 3.5, 5.0), "nose bridge", "jade_light"),
    ("nose_pad", "head", (0, 18.6, 44.4), (7.0, 4.0, 2.5), "wide nostril pad", "deep_jade"),
]
for item in head_parts:
    name, bone_name, center, size, role, material = item
    add_cube(name, bone_name, center, size, role, material,
             origin=(0, 16.0, 38.0) if bone_name == "jaw" else axial_points[0])

# Ten mane tufts. These are silhouette volumes rather than color-only splits.
mane_data = [
    ("mane_crown_left", (3.4, 26.2, 32.5), (2.8, 5.5, 3.5), (0, 0, -25)),
    ("mane_crown_right", (-3.4, 26.2, 32.5), (2.8, 5.5, 3.5), (0, 0, 25)),
    ("mane_cheek_left", (6.7, 20.2, 32.0), (3.0, 5.0, 3.5), (0, 0, -35)),
    ("mane_cheek_right", (-6.7, 20.2, 32.0), (3.0, 5.0, 3.5), (0, 0, 35)),
    ("mane_jowl_left", (5.4, 15.7, 33.0), (3.0, 4.5, 3.5), (0, 0, 35)),
    ("mane_jowl_right", (-5.4, 15.7, 33.0), (3.0, 4.5, 3.5), (0, 0, -35)),
    ("mane_neck_left", (5.0, 20.8, 27.5), (3.0, 5.5, 4.0), (15, 0, -25)),
    ("mane_neck_right", (-5.0, 20.8, 27.5), (3.0, 5.5, 4.0), (15, 0, 25)),
    ("mane_neck_top", (0, 24.0, 27.0), (3.0, 5.0, 4.0), (20, 0, 0)),
    ("mane_throat", (0, 14.2, 28.2), (3.0, 5.0, 4.0), (-20, 0, 0)),
]
for name, center, size, rotation in mane_data:
    add_cube(name, "head", center, size, "lion mane tuft", "vermilion_mane",
             origin=axial_points[0], rotation=rotation)

# Two symmetric branching antlers, five cuboids and five bones per side.
for side_name, side in (("left", 1), ("right", -1)):
    horn_paths = [
        ((side * 3.0, 24.5, 32.0), (side * 3.4, 29.5, 29.8), 1.5, "base"),
        ((side * 3.4, 29.5, 29.8), (side * 4.2, 34.2, 26.2), 1.3, "shaft"),
        ((side * 4.2, 34.2, 26.2), (side * 4.8, 38.0, 22.0), 1.0, "tip"),
        ((side * 3.5, 29.8, 29.4), (side * 7.0, 32.5, 29.0), 1.0, "front_branch"),
        ((side * 4.1, 34.0, 26.4), (side * 7.2, 36.2, 23.8), 0.9, "rear_branch"),
    ]
    previous = "head"
    for index, (start, end, thickness, role_suffix) in enumerate(horn_paths):
        bone_name = f"horn_{side_name}_{index}"
        parent = previous if index < 3 else (f"horn_{side_name}_{0}" if index == 3 else f"horn_{side_name}_{1}")
        add_bone(bone_name, parent, start)
        add_between(f"horn_{side_name}_{role_suffix}", bone_name, start, end,
                    thickness, f"{side_name} deer antler {role_suffix}", "bone", origin=start)
        if index < 3:
            previous = bone_name

# Long segmented whiskers, five cubes/bones on each side.
for side_name, side in (("left", 1), ("right", -1)):
    whisker_points = [
        (side * 3.5, 17.2, 43.0), (side * 7.5, 16.5, 46.0),
        (side * 11.5, 15.0, 47.0), (side * 15.0, 13.5, 45.5),
        (side * 17.5, 14.5, 42.5), (side * 18.0, 16.5, 39.5),
    ]
    previous = "head"
    for index in range(5):
        bone_name = f"whisker_{side_name}_{index}"
        add_bone(bone_name, previous, whisker_points[index])
        add_between(f"whisker_{side_name}_segment_{index}", bone_name,
                    whisker_points[index], whisker_points[index + 1], 0.8,
                    f"{side_name} catfish whisker segment", "gold_belly",
                    origin=whisker_points[index])
        previous = bone_name

# Four articulated legs: upper, lower, paw, plus three separate eagle talons.
leg_specs = [
    ("front_left", 1, 3), ("front_right", -1, 3),
    ("rear_left", 1, 10), ("rear_right", -1, 10),
]
for name, side, axial_index in leg_specs:
    anchor = axial_points[axial_index]
    start = (anchor[0] + side * widths[axial_index] * 0.43, anchor[1] - 0.5, anchor[2])
    knee = (start[0] + side * 4.2, start[1] - 6.5, start[2] + 0.8)
    ankle = (knee[0] + side * 1.8, knee[1] - 6.0, knee[2] + 2.2)
    toe = (ankle[0], ankle[1] - 1.0, ankle[2] + 4.0)
    upper_bone = f"{name}_upper"
    lower_bone = f"{name}_lower"
    paw_bone = f"{name}_paw"
    add_bone(upper_bone, f"axial_{axial_index:02d}", start)
    add_bone(lower_bone, upper_bone, knee)
    add_bone(paw_bone, lower_bone, ankle)
    add_between(f"{name}_upper_leg", upper_bone, start, knee, 3.2,
                f"{name} muscular upper leg", "jade_scales", origin=start)
    add_between(f"{name}_lower_leg", lower_bone, knee, ankle, 2.5,
                f"{name} lower leg", "deep_jade", origin=knee)
    add_between(f"{name}_paw", paw_bone, ankle, toe, (4.8, 2.2),
                f"{name} gold clawed paw", "gold_belly", origin=ankle)
    for claw_index, x_offset in enumerate((-1.4, 0.0, 1.4)):
        claw_start = (toe[0] + side * x_offset, toe[1] - 0.5, toe[2] + 1.2)
        claw_end = (claw_start[0] + side * x_offset * 0.35,
                    claw_start[1] - 0.6, claw_start[2] + 3.0)
        add_between(f"{name}_claw_{claw_index}", paw_bone, claw_start, claw_end,
                    0.7, f"{name} eagle talon", "bone", origin=ankle)

# Eleven dorsal ridge blocks accent the serpentine silhouette.
for ridge_index, axial_index in enumerate(range(1, 12)):
    point = axial_points[axial_index]
    height = max(2.5, 4.5 - ridge_index * 0.13)
    add_cube(f"dorsal_spine_{ridge_index:02d}", f"axial_{axial_index:02d}",
             (point[0], point[1] + heights[axial_index] * 0.52 + height * 0.45, point[2]),
             (1.6, height, 2.8), "dorsal flame ridge", "vermilion_mane",
             origin=point, rotation=(0, 0, -8 if ridge_index % 2 else 8))

# Three tail flame fins fan from the terminal joint.
tail_base = axial_points[-1]
tail_fin_ends = [
    (tail_base[0], tail_base[1] + 5.0, tail_base[2] - 4.5),
    (tail_base[0] + 4.5, tail_base[1], tail_base[2] - 4.8),
    (tail_base[0] - 4.5, tail_base[1], tail_base[2] - 4.8),
]
for index, end in enumerate(tail_fin_ends):
    add_between(f"tail_flame_{index}", "axial_15", tail_base, end,
                (1.6, 2.8), "flame-shaped tail fin", "vermilion_mane",
                origin=tail_base)

# Pixel landmarks are reserved for genuinely flat facial details.
landmarks.extend([
    {"name": "left_pupil", "cube": "eye_left", "face": "east",
     "center_uv": [0.5, 0.5], "size": [2, 2], "color": "#191714", "center_color": "#76ddd4"},
    {"name": "right_pupil", "cube": "eye_right", "face": "west",
     "center_uv": [0.5, 0.5], "size": [2, 2], "color": "#191714", "center_color": "#76ddd4"},
    {"name": "left_nostril", "cube": "nose_pad", "face": "south",
     "center_uv": [0.3, 0.52], "size": [2, 2], "color": "#191714", "center_color": "#191714"},
    {"name": "right_nostril", "cube": "nose_pad", "face": "south",
     "center_uv": [0.7, 0.52], "size": [2, 2], "color": "#191714", "center_color": "#191714"},
    {"name": "mouth_line", "cube": "lower_jaw", "face": "south",
     "center_uv": [0.5, 0.3], "size": [5, 1], "color": "#191714", "center_color": "#642a27"},
])

with Image.open(REFERENCE) as image:
    width, height = image.size

spec = {
    "schema_version": 1,
    "id": "chinese_dragon_boss",
    "reference": {
        "image": "../references/chinese_dragon_concept.png",
        "sha256": hashlib.sha256(REFERENCE.read_bytes()).hexdigest(),
        "width": width,
        "height": height,
    },
    "subject": {
        "type": "boss_mob",
        "description": (
            "An original Minecraft-native wingless Chinese dragon boss in a neutral flying pose, "
            "with sixteen continuous serpentine segments, four eagle-clawed legs, branching deer "
            "antlers, lion mane, catfish beard, long whiskers, and a tapered flame tail."
        ),
        "symmetry": "bilateral",
        "uncertainties": [
            "The direct concept is a side-dominant view; hidden-side geometry is mirrored conservatively.",
            "Dynamic coil curvature and the attack pearl are animation/effect states, not static geometry."
        ],
    },
    "quality_contract": {
        "complexity": "complex",
        "target_cuboids": [90, 96],
        "identity_features": [
            "sixteen-part wingless serpent body", "branching deer antlers", "lion mane",
            "bulging turquoise-pupiled eyes", "paired segmented whiskers", "four eagle-clawed legs",
            "gold belly plates", "vermilion dorsal ridge", "three-pronged flame tail",
        ],
        "required_views": [
            "front", "back", "left", "right", "isometric",
            "left_head_closeup", "right_head_closeup", "connected_joint_closeup",
        ],
        "review_targets": ["silhouette", "face", "joints", "texture", "rig_hierarchy", "animation_clearance"],
    },
    "texture": {"density": 2, "palette_size": 32, "gutter": 2, "atlas_size": 512},
    "materials": {
        "jade_scales": {"base": "#355b48", "shade": "#20372f", "highlight": "#4f8065", "pattern": "spots", "pattern_scale": 3},
        "jade_light": {"base": "#4f8065", "shade": "#355b48", "highlight": "#70a887", "pattern": "dither", "pattern_scale": 3},
        "deep_jade": {"base": "#20372f", "shade": "#14231f", "highlight": "#355b48", "pattern": "gradient", "pattern_scale": 4},
        "gold_belly": {"base": "#b99750", "shade": "#765f32", "highlight": "#dbc37e", "pattern": "stripes", "pattern_scale": 3},
        "vermilion_mane": {"base": "#9e392d", "shade": "#642a27", "highlight": "#cf6047", "pattern": "dither", "pattern_scale": 3},
        "bone": {"base": "#d5c6a0", "shade": "#927e58", "highlight": "#eee1bd", "pattern": "gradient", "pattern_scale": 4},
        "eye_gold": {"base": "#dbc37e", "shade": "#765f32", "highlight": "#fff1ad", "pattern": "solid", "pattern_scale": 2},
    },
    "bones": bones,
    "cubes": cubes,
    "landmarks": landmarks,
    "collision": {"width": 3.0, "height": 3.2, "eye_height": 2.55},
    "generation": {
        "lane": "direct-cuboid",
        "authoring": "Original deterministic cuboid reconstruction from project concept art",
        "source_references": [
            {"title": "Eleven Dragons", "institution": "Smithsonian National Museum of Asian Art", "license": "CC0", "url": "https://asia-archive.si.edu/object/F1919.173/"},
            {"title": "Mirror with a Coiling Dragon", "institution": "Cleveland Museum of Art", "license": "CC0", "url": "https://www.clevelandart.org/art/1995.367"},
            {"title": "Dragon", "institution": "The Metropolitan Museum of Art", "license": "Public Domain / Open Access", "url": "https://www.metmuseum.org/art/collection/search/36431"},
        ],
    },
}

assert len(cubes) == 96, f"expected 96 cubes, got {len(cubes)}"
OUTPUT.write_text(json.dumps(spec, indent=2) + "\n", encoding="utf-8")
print(f"wrote {OUTPUT} with {len(bones)} bones and {len(cubes)} cubes")
