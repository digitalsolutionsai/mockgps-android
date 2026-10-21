#!/usr/bin/env python3
"""Renders the 1024x500 Play feature graphic from a store screenshot.

Usage: tools/make-feature-graphic.py [screenshot] [output]
"""
import pathlib
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = pathlib.Path(__file__).resolve().parent.parent
GRAPHICS = ROOT / "docs/play-store/graphics"
SHOT = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else GRAPHICS / "screenshots/en-US/2-route.png"
OUT = pathlib.Path(sys.argv[2]) if len(sys.argv) > 2 else GRAPHICS / "feature-graphic.png"
FONTS = pathlib.Path("/usr/share/fonts/truetype/noto")
W, H, K = 1024, 500, 2  # draw at 2x, downscale at the end


def font(bold, size):
    return ImageFont.truetype(str(FONTS / ("NotoSans-Bold.ttf" if bold else "NotoSans-Regular.ttf")), size * K)


img = Image.new("RGB", (W * K, H * K))
d = ImageDraw.Draw(img)
top, bottom = (0x2B, 0x86, 0xF0), (0x13, 0x5C, 0xC9)
for y in range(H * K):
    t = y / (H * K)
    d.line([(0, y), (W * K, y)], fill=tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3)))

# Icon tile.
icon = Image.open(GRAPHICS / "icon-512.png").convert("RGBA").resize((88 * K, 88 * K), Image.LANCZOS)
mask = Image.new("L", icon.size, 0)
ImageDraw.Draw(mask).rounded_rectangle([0, 0, *icon.size], radius=22 * K, fill=255)
img.paste(icon, (64 * K, 70 * K), mask)

d.text((64 * K, 170 * K), "MockGPS", font=font(True, 64), fill="white")
d.text((66 * K, 258 * K), "Mock location tool for developers", font=font(False, 28), fill=(232, 240, 254))

# Feature chips: translucent fill needs its own RGBA layer.
chips = Image.new("RGBA", img.size, (0, 0, 0, 0))
cd = ImageDraw.Draw(chips)
x, y = 66 * K, 330 * K
labels = []
for label in ("GPS · Network · Fused", "Routes & joystick", "adb automation"):
    f = font(True, 19)
    w = d.textlength(label, font=f)
    if x + w + 36 * K > 600 * K:
        x, y = 66 * K, y + 52 * K
    cd.rounded_rectangle([x, y, x + w + 32 * K, y + 40 * K], radius=20 * K, fill=(255, 255, 255, 46), outline=(255, 255, 255, 200), width=2 * K)
    labels.append((x + 16 * K, y + 6 * K, label, f))
    x += w + 44 * K
img = Image.alpha_composite(img.convert("RGBA"), chips).convert("RGB")
d = ImageDraw.Draw(img)
for lx, ly, label, f in labels:
    d.text((lx, ly), label, font=f, fill="white")

# Phone frame with the screenshot, running off the bottom edge.
shot = Image.open(SHOT).convert("RGB")
pw = 250 * K
ph = int(pw * shot.height / shot.width)
screen = shot.resize((pw, ph), Image.LANCZOS)
bezel = 10 * K
fx, fy = 690 * K, 46 * K
frame = [fx - bezel, fy - bezel, fx + pw + bezel, fy + ph + bezel]
shadow = Image.new("L", img.size, 0)
ImageDraw.Draw(shadow).rounded_rectangle([frame[0] + 8 * K, frame[1] + 14 * K, frame[2] + 8 * K, frame[3] + 14 * K], radius=40 * K, fill=110)
img.paste((8, 30, 80), mask=shadow.filter(ImageFilter.GaussianBlur(18 * K)))
d.rounded_rectangle(frame, radius=40 * K, fill=(20, 22, 26))
smask = Image.new("L", screen.size, 0)
ImageDraw.Draw(smask).rounded_rectangle([0, 0, *screen.size], radius=30 * K, fill=255)
img.paste(screen, (fx, fy), smask)

img.resize((W, H), Image.LANCZOS).save(OUT)
print(f"wrote {OUT}")
