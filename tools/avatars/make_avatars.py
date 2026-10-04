"""Builds the 24 avatar slots (theme x hero x rank) from the Cogabushi upper-body portraits.

Each portrait is cropped head-to-waist (5:6), scaled to leave room for an aura, and set over a soft glow in
the theme's colour whose strength grows with the rank. Output: out/<theme>_<hero>_<rank>.webp, 480x576.
Usage: python make_avatars.py [out_dir]; ART_SRC names the folder that holds pack/ (default: this folder).
"""
import os
import sys

from PIL import Image, ImageFilter

ROOT = os.environ.get("ART_SRC", os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "pack", "25_upper")
# Hand-fixed portraits (edit_*.py) win over the originals.
EDITED = os.path.join(ROOT, "pack", "edited")
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "out")
W, H = 480, 576
RANKS = ["e", "d", "c", "b", "a", "s"]
MAPPING = {
    "boy": ["113", "118", "123", "105", "112", "125"],
    "girl": ["119", "111", "104", "117", "101", "124"],
}
# Inner and outer aura colours per theme (System: violet core, blue edge; Neon: pink core, cyan edge).
THEMES = {"system": ((169, 139, 255), (92, 200, 255)), "neon": ((255, 61, 154), (46, 230, 255))}
STRENGTH = {"e": 0.3, "d": 0.42, "c": 0.55, "b": 0.7, "a": 0.85, "s": 1.0}



def portrait(pid):
    edited = os.path.join(EDITED, f"{pid}_1.png")
    im = Image.open(edited if os.path.exists(edited) else os.path.join(SRC, f"{pid}_1.png")).convert("RGBA")
    crop = im.crop((0, 0, im.width, min(im.height, round(im.width * H / W))))
    scale = 0.86
    fitted = crop.resize((round(W * scale), round(H * scale)), Image.LANCZOS)
    canvas = Image.new("RGBA", (W, H))
    canvas.paste(fitted, ((W - fitted.width) // 2, H - fitted.height), fitted)
    return canvas


def glow(alpha, grow, blur, color, strength):
    mask = alpha.filter(ImageFilter.MaxFilter(grow)).filter(ImageFilter.GaussianBlur(blur))
    mask = mask.point(lambda v: round(v * strength))
    layer = Image.new("RGBA", (W, H), color + (0,))
    layer.putalpha(mask)
    return layer


def avatar(pid, theme, rank):
    char = portrait(pid)
    alpha = char.getchannel("A")
    inner, outer = THEMES[theme]
    k = STRENGTH[rank]
    out = Image.new("RGBA", (W, H))
    out = Image.alpha_composite(out, glow(alpha, 31, 26 + 18 * k, outer, 0.55 * k))
    out = Image.alpha_composite(out, glow(alpha, 15, 10 + 6 * k, inner, 0.9 * k))
    return Image.alpha_composite(out, char)


def main():
    os.makedirs(OUT, exist_ok=True)
    for hero, ids in MAPPING.items():
        for rank, pid in zip(RANKS, ids):
            for theme in THEMES:
                avatar(pid, theme, rank).save(os.path.join(OUT, f"{theme}_{hero}_{rank}.webp"), "WEBP", quality=88, method=6)
    print("done", len(os.listdir(OUT)))


if __name__ == "__main__":
    main()
