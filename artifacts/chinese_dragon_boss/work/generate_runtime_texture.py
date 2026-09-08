#!/usr/bin/env python3
"""Generate the Java-model texture atlas with a fixed, auditable palette."""

from pathlib import Path
from PIL import Image, ImageDraw


SIZE = 256
OUT = (
    Path(__file__).resolve().parents[3]
    / "src/main/resources/assets/dragon/textures/entity/chinese_dragon_boss.png"
)

INK = "#111815"
JADE_DARK = "#173a31"
JADE = "#315e4b"
JADE_LIGHT = "#5e9173"
JADE_GLOW = "#8bb69a"
GOLD_DARK = "#755b25"
GOLD = "#b49342"
GOLD_LIGHT = "#ddc36e"
CRIMSON_DARK = "#4e171a"
CRIMSON = "#8f302c"
CRIMSON_LIGHT = "#c8543d"
IVORY_DARK = "#9f8d62"
IVORY = "#d8cba2"
IVORY_LIGHT = "#f2e7bf"
CYAN = "#70e0d3"
CYAN_LIGHT = "#c6fff5"


def patterned_region(draw, box, base, dark, light, period=8):
    x0, y0, x1, y1 = box
    draw.rectangle(box, fill=base)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            value = (x * 17 + y * 31 + (x // period) * 7) % 37
            if value in (0, 1, 2):
                draw.point((x, y), fill=light)
            elif value in (18, 19):
                draw.point((x, y), fill=dark)


def scale_marks(draw, box, color, spacing=9):
    x0, y0, x1, y1 = box
    for row, y in enumerate(range(y0 + 4, y1 - 2, spacing)):
        shift = (spacing // 2) if row % 2 else 0
        for x in range(x0 + 3 + shift, x1 - 4, spacing):
            draw.line((x - 2, y, x, y + 2, x + 2, y), fill=color, width=1)


def bands(draw, box, dark, light, spacing=8):
    x0, y0, x1, y1 = box
    for x in range(x0 + spacing, x1, spacing):
        draw.line((x, y0, x, y1), fill=dark, width=1)
        if x + 1 <= x1:
            draw.line((x + 1, y0, x + 1, y1), fill=light, width=1)


def main():
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)

    # (0, 0): axial body, tail, and general jade hide.
    patterned_region(draw, (0, 0, 95, 47), JADE, JADE_DARK, JADE_LIGHT)
    scale_marks(draw, (0, 0, 95, 47), JADE_GLOW)
    draw.line((0, 0, 95, 0), fill=INK)

    # (96, 0): dorsal mane, beard, ears, and tail fan.
    patterned_region(draw, (96, 0, 159, 47), CRIMSON, CRIMSON_DARK, CRIMSON_LIGHT, 6)
    for y in range(5, 48, 9):
        draw.line((96, y, 159, y - 3), fill=CRIMSON_DARK)

    # (0, 52): belly scutes and gold paw armor.
    patterned_region(draw, (0, 52, 95, 103), GOLD, GOLD_DARK, GOLD_LIGHT, 7)
    for y in range(57, 104, 7):
        draw.line((0, y, 95, y), fill=GOLD_DARK)
        draw.line((0, min(y + 1, 103), 95, min(y + 1, 103)), fill=GOLD_LIGHT)

    # (96, 52): darker articulated limbs.
    patterned_region(draw, (96, 52, 159, 103), JADE_DARK, INK, JADE, 7)
    scale_marks(draw, (96, 52, 159, 103), JADE_LIGHT, 8)

    # (160, 52): antlers, teeth, and claws.
    patterned_region(draw, (160, 52, 223, 103), IVORY, IVORY_DARK, IVORY_LIGHT, 6)
    bands(draw, (160, 52, 223, 103), IVORY_DARK, IVORY_LIGHT, 9)

    # (0, 112): head and muzzle hide. Denser scales keep the face distinct.
    patterned_region(draw, (0, 112, 95, 175), JADE, JADE_DARK, JADE_LIGHT, 6)
    scale_marks(draw, (0, 112, 95, 175), JADE_GLOW, 7)
    for y in (126, 148, 167):
        draw.line((0, y, 95, y), fill=JADE_DARK)

    # (96, 112): nose and mouth interior.
    patterned_region(draw, (96, 112, 159, 175), CRIMSON_DARK, INK, CRIMSON, 5)
    draw.rectangle((102, 119, 153, 124), fill=INK)
    draw.rectangle((104, 120, 151, 121), fill=CRIMSON_LIGHT)

    # (160, 112): raised eye plates with a supernatural turquoise glint.
    draw.rectangle((160, 112, 191, 175), fill=GOLD_DARK)
    for y in range(112, 176, 8):
        draw.line((160, y, 191, y), fill=GOLD_LIGHT)
    for x in range(164, 192, 8):
        draw.rectangle((x, 120, min(x + 3, 191), 127), fill=CYAN)
        draw.point((min(x + 1, 191), 121), fill=CYAN_LIGHT)
        draw.rectangle((x, 142, min(x + 3, 191), 149), fill=INK)
        draw.point((min(x + 1, 191), 143), fill=GOLD_LIGHT)

    # (192, 112): long whiskers, warm ivory with gold binding rings.
    patterned_region(draw, (192, 112, 239, 175), IVORY, IVORY_DARK, IVORY_LIGHT, 8)
    bands(draw, (192, 112, 239, 175), GOLD_DARK, GOLD_LIGHT, 7)

    # A tiny palette key in unused space helps keep future revisions consistent.
    palette = [INK, JADE_DARK, JADE, JADE_LIGHT, GOLD_DARK, GOLD, GOLD_LIGHT,
               CRIMSON_DARK, CRIMSON, CRIMSON_LIGHT, IVORY_DARK, IVORY,
               IVORY_LIGHT, CYAN, CYAN_LIGHT]
    for i, color in enumerate(palette):
        x = 4 + (i % 8) * 6
        y = 224 + (i // 8) * 6
        draw.rectangle((x, y, x + 4, y + 4), fill=color)

    OUT.parent.mkdir(parents=True, exist_ok=True)
    image.save(OUT, format="PNG", optimize=False, compress_level=9)
    print(OUT)


if __name__ == "__main__":
    main()
