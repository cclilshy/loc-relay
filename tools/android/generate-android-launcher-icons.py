#!/usr/bin/env python3
from math import hypot
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "static" / "icon.png"
RES = ROOT / "android" / "app" / "src" / "main" / "res"

DENSITIES = {
    "mdpi": 1,
    "hdpi": 1.5,
    "xhdpi": 2,
    "xxhdpi": 3,
    "xxxhdpi": 4,
}

VISIBLE_THRESHOLD = 32
ALPHA_TRANSPARENT_THRESHOLD = 4
ALPHA_OPAQUE_THRESHOLD = 40
SAFE_RADIUS_DP = 33
SAFE_RADIUS_FILL = 0.95


def visible_points(image):
    pixels = image.load()
    width, height = image.size
    points = []
    for y in range(height):
        for x in range(width):
            if max(pixels[x, y]) > VISIBLE_THRESHOLD:
                points.append((x, y))
    if not points:
        raise RuntimeError(f"{SOURCE} does not contain visible icon pixels")
    return points


def alpha_from_black(image):
    result = image.convert("RGBA")
    pixels = result.load()
    width, height = result.size
    for y in range(height):
        for x in range(width):
            r, g, b, _ = pixels[x, y]
            strongest = max(r, g, b)
            if strongest <= ALPHA_TRANSPARENT_THRESHOLD:
                alpha = 0
            elif strongest >= ALPHA_OPAQUE_THRESHOLD:
                alpha = 255
            else:
                alpha = round(
                    255
                    * (strongest - ALPHA_TRANSPARENT_THRESHOLD)
                    / (ALPHA_OPAQUE_THRESHOLD - ALPHA_TRANSPARENT_THRESHOLD)
                )
            pixels[x, y] = (r, g, b, alpha)
    return result


def save_foreground(source, center, radius):
    for density, density_scale in DENSITIES.items():
        layer_px = int(108 * density_scale)
        safe_radius_px = SAFE_RADIUS_DP * density_scale
        source_scale = safe_radius_px * SAFE_RADIUS_FILL / radius
        source_size = (
            max(1, round(source.width * source_scale)),
            max(1, round(source.height * source_scale)),
        )
        resized = source.resize(source_size, Image.Resampling.LANCZOS)
        foreground = alpha_from_black(resized)
        canvas = Image.new("RGBA", (layer_px, layer_px), (0, 0, 0, 0))
        offset = (
            round(layer_px / 2 - center[0] * source_scale),
            round(layer_px / 2 - center[1] * source_scale),
        )
        canvas.alpha_composite(foreground, offset)
        output = RES / f"mipmap-{density}" / "ic_launcher_foreground.png"
        canvas.save(output)
        print(f"wrote {output.relative_to(ROOT)}")


def save_legacy(source):
    for density, density_scale in DENSITIES.items():
        size = int(48 * density_scale)
        output = RES / f"mipmap-{density}" / "ic_launcher.png"
        source.resize((size, size), Image.Resampling.LANCZOS).convert("RGB").save(output)
        print(f"wrote {output.relative_to(ROOT)}")


def main():
    source = Image.open(SOURCE).convert("RGB")
    points = visible_points(source)
    min_x = min(x for x, _ in points)
    max_x = max(x for x, _ in points)
    min_y = min(y for _, y in points)
    max_y = max(y for _, y in points)
    center = ((min_x + max_x + 1) / 2, (min_y + max_y + 1) / 2)
    radius = max(hypot(x + 0.5 - center[0], y + 0.5 - center[1]) for x, y in points)

    save_foreground(source, center, radius)
    save_legacy(source)


if __name__ == "__main__":
    main()
