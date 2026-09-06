"""
Generates the launcher icon assets (legacy square/round PNGs for API<26 and
adaptive-icon foreground PNGs for API26+) plus a 512x512 Play Store listing
icon and a 1024x500 feature graphic. Run once; outputs are committed as
binary resources (no runtime dependency on PIL).

Usage:
    python scripts/gen_icons.py
"""
import os
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
STORE = os.path.join(ROOT, "store_assets")

BG_COLOR = (13, 17, 23, 255)       # near-black
FG_COLOR = (0, 230, 118, 255)      # bright green, matches in-app default

# legacy launcher icon: dp size -> px per density
LEGACY_DENSITIES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

# adaptive icon foreground layer: 108dp canvas -> px per density
ADAPTIVE_DENSITIES = {
    "mipmap-mdpi": 108,
    "mipmap-hdpi": 162,
    "mipmap-xhdpi": 216,
    "mipmap-xxhdpi": 324,
    "mipmap-xxxhdpi": 432,
}


def find_bold_font(size):
    candidates = [
        r"C:\Windows\Fonts\arialbd.ttf",
        r"C:\Windows\Fonts\segoeuib.ttf",
        r"C:\Windows\Fonts\calibrib.ttf",
    ]
    for path in candidates:
        if os.path.exists(path):
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def draw_fps_glyph(size, fg_only=False, safe_zone_ratio=1.0):
    """Draws a simple speedometer-needle + 'FPS' glyph centered in a
    `size`x`size` canvas. If fg_only, background is transparent (for
    adaptive-icon foreground layers, drawn inside the safe zone)."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0) if fg_only else BG_COLOR)
    draw = ImageDraw.Draw(img)

    content_size = int(size * safe_zone_ratio)
    offset = (size - content_size) // 2

    # Rounded background square for the legacy (non-adaptive) icon.
    if not fg_only:
        radius = int(size * 0.22)
        draw.rounded_rectangle([0, 0, size - 1, size - 1], radius=radius, fill=BG_COLOR)

    font = find_bold_font(int(content_size * 0.34))
    text = "FPS"
    bbox = draw.textbbox((0, 0), text, font=font)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    tx = offset + (content_size - tw) / 2 - bbox[0]
    ty = offset + (content_size - th) / 2 - bbox[1]
    draw.text((tx, ty), text, font=font, fill=FG_COLOR)

    # Small underline accent to suggest a speed/frame-rate readout.
    line_y = offset + int(content_size * 0.68)
    line_w = int(content_size * 0.44)
    line_x0 = offset + (content_size - line_w) // 2
    draw.rounded_rectangle(
        [line_x0, line_y, line_x0 + line_w, line_y + max(2, size // 24)],
        radius=size // 24,
        fill=FG_COLOR,
    )
    return img


def save_round(img, path):
    size = img.size[0]
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, size - 1, size - 1], fill=255)
    rounded = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    rounded.paste(img, (0, 0), mask)
    rounded.save(path)


def main():
    for folder, px in LEGACY_DENSITIES.items():
        d = os.path.join(RES, folder)
        os.makedirs(d, exist_ok=True)
        icon = draw_fps_glyph(px, fg_only=False, safe_zone_ratio=1.0)
        icon.save(os.path.join(d, "ic_launcher.png"))
        save_round(icon, os.path.join(d, "ic_launcher_round.png"))

    for folder, px in ADAPTIVE_DENSITIES.items():
        d = os.path.join(RES, folder)
        os.makedirs(d, exist_ok=True)
        fg = draw_fps_glyph(px, fg_only=True, safe_zone_ratio=0.60)
        fg.save(os.path.join(d, "ic_launcher_foreground.png"))

    os.makedirs(STORE, exist_ok=True)
    store_icon = draw_fps_glyph(512, fg_only=False, safe_zone_ratio=1.0)
    store_icon.save(os.path.join(STORE, "play_store_icon_512.png"))

    feature = Image.new("RGBA", (1024, 500), BG_COLOR)
    draw = ImageDraw.Draw(feature)
    font = find_bold_font(120)
    text = "FPS Overlay"
    bbox = draw.textbbox((0, 0), text, font=font)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    draw.text(((1024 - tw) / 2 - bbox[0], (500 - th) / 2 - bbox[1]), text, font=font, fill=FG_COLOR)
    feature.save(os.path.join(STORE, "play_store_feature_graphic_1024x500.png"))

    print("Icons generated.")


if __name__ == "__main__":
    main()
