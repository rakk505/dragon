#!/usr/bin/env python3
"""Render the original pixel-cuboid concept used by img2blockbench.

The low-resolution construction is intentional: every contour is made from
hard-edged polygons and square pixels so the reference remains Minecraft-like.
"""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw


SCALE = 4
SIZE = (384, 256)
OUT = Path(__file__).resolve().parents[1] / "references" / "chinese_dragon_concept.png"

INK = "#191714"
JADE = "#355b48"
JADE_DARK = "#20372f"
JADE_LIGHT = "#4f8065"
GOLD = "#b99750"
GOLD_LIGHT = "#dbc37e"
VERMILION = "#9e392d"
VERMILION_DARK = "#642a27"
BONE = "#d5c6a0"
GLOW = "#76ddd4"


def segment_polygon(a: tuple[float, float], b: tuple[float, float], ra: float, rb: float):
    dx, dy = b[0] - a[0], b[1] - a[1]
    length = max(1.0, math.hypot(dx, dy))
    nx, ny = -dy / length, dx / length
    return [
        (a[0] + nx * ra, a[1] + ny * ra),
        (b[0] + nx * rb, b[1] + ny * rb),
        (b[0] - nx * rb, b[1] - ny * rb),
        (a[0] - nx * ra, a[1] - ny * ra),
    ]


def line_blocks(draw: ImageDraw.ImageDraw, points, fill, width=3):
    for a, b in zip(points, points[1:]):
        draw.line([a, b], fill=INK, width=width + 2)
        draw.line([a, b], fill=fill, width=width)
        draw.rectangle((b[0] - width // 2, b[1] - width // 2,
                        b[0] + width // 2, b[1] + width // 2), fill=fill)


def draw_leg(draw: ImageDraw.ImageDraw, hip, side, far=False):
    tint = JADE_DARK if far else JADE
    x, y = hip
    knee = (x + side * 10, y + 18)
    ankle = (x + side * 16, y + 34)
    paw = (x + side * 18, y + 39)
    for a, b, half in [(hip, knee, 5), (knee, ankle, 4), (ankle, paw, 4)]:
        draw.polygon(segment_polygon(a, b, half, half - 1), fill=tint, outline=INK)
    draw.rectangle((paw[0] - 7, paw[1] - 3, paw[0] + 8, paw[1] + 4), fill=GOLD, outline=INK)
    for offset in (-5, 0, 5):
        draw.polygon([(paw[0] + offset - 1, paw[1] + 3),
                      (paw[0] + offset + 2, paw[1] + 3),
                      (paw[0] + offset + 5, paw[1] + 10),
                      (paw[0] + offset + 1, paw[1] + 8)], fill=BONE, outline=INK)


def render() -> None:
    image = Image.new("RGB", SIZE, "#d9e5df")
    draw = ImageDraw.Draw(image)

    # Pixel-cloud backdrop keeps the silhouette readable without hiding anatomy.
    for x, y, w in [(18, 51, 52), (83, 34, 68), (238, 36, 56), (302, 55, 58)]:
        draw.rectangle((x, y, x + w, y + 9), fill="#edf4ef")
        draw.rectangle((x + 9, y - 7, x + w - 10, y + 15), fill="#edf4ef")

    spine = [
        (31, 167, 3), (52, 176, 5), (76, 174, 7), (100, 157, 10),
        (125, 136, 13), (153, 123, 15), (184, 124, 16), (215, 137, 16),
        (244, 142, 15), (270, 132, 13), (292, 111, 11),
    ]

    # Far limbs first, then the segmented trunk.
    draw_leg(draw, (126, 141), -1, True)
    draw_leg(draw, (231, 145), -1, True)
    for index, (a, b) in enumerate(zip(spine, spine[1:])):
        poly = segment_polygon(a[:2], b[:2], a[2], b[2])
        draw.polygon(poly, fill=JADE_DARK if index % 2 else JADE, outline=INK)
        midx, midy = int((a[0] + b[0]) / 2), int((a[1] + b[1]) / 2)
        draw.rectangle((midx - 2, midy - 2, midx + 1, midy + 1), fill=JADE_LIGHT)
        draw.rectangle((midx + 3, midy + 2, midx + 6, midy + 5), fill=GOLD)

    # Belly plates and dorsal ridge are texture/shape cues, not dense scales.
    for x, y, _ in spine[2:-1]:
        draw.rectangle((x - 3, y + 8, x + 4, y + 11), fill=GOLD, outline=JADE_DARK)
        draw.polygon([(x - 3, y - 10), (x, y - 18), (x + 4, y - 9)], fill=VERMILION, outline=INK)

    draw_leg(draw, (143, 129), 1, False)
    draw_leg(draw, (249, 139), 1, False)

    # Broad ox-like head, articulated jaw, eyes, brows, ears, and blocky mane.
    draw.rectangle((284, 84, 328, 119), fill=JADE, outline=INK, width=2)
    draw.rectangle((318, 99, 360, 122), fill=JADE_LIGHT, outline=INK, width=2)
    draw.rectangle((315, 122, 354, 132), fill=GOLD, outline=INK, width=2)
    draw.rectangle((340, 105, 363, 118), fill=JADE, outline=INK)
    draw.rectangle((357, 108, 362, 113), fill=INK)
    draw.rectangle((290, 91, 302, 103), fill=GOLD_LIGHT, outline=INK)
    draw.rectangle((295, 94, 300, 100), fill=INK)
    draw.rectangle((286, 84, 304, 90), fill=VERMILION_DARK, outline=INK)
    draw.polygon([(282, 91), (270, 84), (283, 105)], fill=JADE_DARK, outline=INK)

    mane = [(281, 82), (288, 72), (300, 71), (313, 74), (326, 81),
            (282, 107), (286, 119), (298, 126), (312, 126), (326, 119)]
    for i, (x, y) in enumerate(mane):
        dx = -7 if i in (0, 5, 6) else (7 if i in (4, 9) else 0)
        dy = -8 if i < 5 else 8
        draw.polygon([(x - 4, y - 3), (x + dx, y + dy), (x + 5, y + 3)],
                     fill=VERMILION if i % 2 == 0 else VERMILION_DARK, outline=INK)

    # Branching deer antlers.
    line_blocks(draw, [(294, 84), (287, 63), (275, 48), (269, 29)], BONE, 4)
    line_blocks(draw, [(286, 62), (273, 59), (264, 48)], BONE, 3)
    line_blocks(draw, [(308, 84), (311, 61), (323, 45), (326, 26)], BONE, 4)
    line_blocks(draw, [(312, 60), (326, 57), (337, 45)], BONE, 3)

    # Paired segmented whiskers and chin beard.
    line_blocks(draw, [(340, 116), (347, 132), (341, 146), (351, 157), (370, 157)], GOLD_LIGHT, 2)
    line_blocks(draw, [(336, 118), (329, 137), (313, 147), (298, 146), (288, 155)], GOLD_LIGHT, 2)
    draw.polygon([(310, 130), (318, 149), (306, 143), (300, 156), (301, 133)], fill=VERMILION, outline=INK)

    # Flame-shaped tail fin and attack pearl motif.
    draw.polygon([(31, 167), (15, 154), (19, 169), (8, 179), (27, 179)], fill=VERMILION, outline=INK)
    draw.ellipse((335, 61, 357, 83), fill=GLOW, outline=INK, width=2)
    draw.rectangle((341, 66, 349, 73), fill="#c9fff7")

    # Nearest-neighbor enlargement preserves the square-pixel aesthetic.
    OUT.parent.mkdir(parents=True, exist_ok=True)
    image.resize((SIZE[0] * SCALE, SIZE[1] * SCALE), Image.Resampling.NEAREST).save(OUT)


if __name__ == "__main__":
    render()
