"""Portrait 117: closes the open shoulder, upper arm and chest so the sleeve and bodice look like one dress.

Skin inside the bare area is replaced by the bodice's own purple silk (its hatching, cloned with mirrored
tiling), shaded by the body underneath so the cloth follows the shoulder and chest. The drawing's dark line
work and the blonde hair (less warm than skin) stay. Writes pack/edited/117_1.png."""
import os

from PIL import Image, ImageDraw, ImageFilter

im = Image.open("pack/25_upper/117_1.png").convert("RGBA")
w, h = im.size

area = Image.new("L", (w, h), 0)
ImageDraw.Draw(area).polygon(
    [(262, 452), (300, 436), (345, 440), (392, 452), (424, 470), (416, 522), (404, 545), (400, 655),
     (356, 655), (350, 548), (300, 536), (256, 530)],
    fill=255,
)

px = im.load()
ap = area.load()
skin = Image.new("L", (w, h), 0)
sp = skin.load()
for y in range(420, 670):
    for x in range(240, 440):
        if ap[x, y]:
            r, g, b, a = px[x, y]
            if a > 0 and r > 120 and r - b >= 45:
                sp[x, y] = 255
# The shoulder's white and pale highlights are skin too when skin surrounds them.
near = skin.filter(ImageFilter.MaxFilter(11)).load()
for y in range(420, 670):
    for x in range(240, 440):
        if ap[x, y] and near[x, y] and not sp[x, y]:
            r, g, b, a = px[x, y]
            if a > 0 and r > 235 and g > 225:
                sp[x, y] = 255
# Close the tiny gaps between skin pixels (posterised colours), keep the edge soft.
mask = skin.filter(ImageFilter.MaxFilter(3)).filter(ImageFilter.MinFilter(3)).filter(ImageFilter.GaussianBlur(1.2))

# Silk swatch from inside the bodice, mirrored so tiles meet without seams.
sx, sy, tw, th = 445, 572, 110, 110
swatch = im.crop((sx, sy, sx + tw, sy + th)).convert("RGB")
tile = Image.new("RGB", (tw * 2, th * 2))
tile.paste(swatch, (0, 0))
tile.paste(swatch.transpose(Image.FLIP_LEFT_RIGHT), (tw, 0))
tile.paste(swatch.transpose(Image.FLIP_TOP_BOTTOM), (0, th))
tile.paste(swatch.transpose(Image.ROTATE_180), (tw, th))
tp = tile.load()

soft = im.filter(ImageFilter.GaussianBlur(4)).load()
lums = [0.3 * soft[x, y][0] + 0.59 * soft[x, y][1] + 0.11 * soft[x, y][2] for y in range(420, 670) for x in range(240, 440) if sp[x, y]]
mean = sum(lums) / len(lums)

cloth = im.copy()
cp = cloth.load()
mp = mask.load()
for y in range(420, 670):
    for x in range(240, 440):
        if not mp[x, y]:
            continue
        r, g, b, a = px[x, y]
        line = (0.3 * r + 0.59 * g + 0.11 * b) / 255
        if line < 0.28:  # ink lines of the drawing stay
            continue
        sr, sg, sb, _ = soft[x, y]
        shade = max(0.55, min(1.35, (0.3 * sr + 0.59 * sg + 0.11 * sb) / mean))
        tr, tg, tb = tp[x % (tw * 2), y % (th * 2)]
        cp[x, y] = (min(255, round(tr * shade * 1.15)), min(255, round(tg * shade * 1.15)), min(255, round(tb * shade * 1.15)), a)
out = Image.composite(cloth, im, mask)

os.makedirs("pack/edited", exist_ok=True)
out.save("pack/edited/117_1.png")
print("ok")
