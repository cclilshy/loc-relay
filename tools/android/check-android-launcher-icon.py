#!/usr/bin/env python3
from pathlib import Path
from sys import exit

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "android" / "app" / "src" / "main" / "res"

DENSITIES = {
    "mdpi": 1,
    "hdpi": 1.5,
    "xhdpi": 2,
    "xxhdpi": 3,
    "xxxhdpi": 4,
}


def main():
    failures = []
    for density, scale in DENSITIES.items():
        path = RES / f"mipmap-{density}" / "ic_launcher_foreground.png"
        image = Image.open(path).convert("RGBA")
        expected_size = int(108 * scale)
        if image.size != (expected_size, expected_size):
            failures.append(f"{path}: expected {expected_size}x{expected_size}, got {image.size}")
            continue

        pixels = image.load()
        center = (expected_size - 1) / 2
        safe_radius = 33 * scale
        has_alpha = False
        outside_safe_zone = 0
        meaningful_pixels = 0

        for y in range(expected_size):
            for x in range(expected_size):
                r, g, b, a = pixels[x, y]
                if a < 255:
                    has_alpha = True
                if a > 32 and max(r, g, b) > 32:
                    meaningful_pixels += 1
                    if ((x - center) ** 2 + (y - center) ** 2) ** 0.5 > safe_radius:
                        outside_safe_zone += 1

        if not has_alpha:
            failures.append(f"{path}: foreground must include transparent padding")
        if meaningful_pixels == 0:
            failures.append(f"{path}: foreground has no meaningful visible pixels")
        if outside_safe_zone:
            failures.append(
                f"{path}: {outside_safe_zone} meaningful pixels exceed the 66dp adaptive-icon safe circle"
            )

    if failures:
        print("\n".join(failures))
        exit(1)

    print("Android launcher foreground assets fit the adaptive-icon safe circle.")


if __name__ == "__main__":
    main()
