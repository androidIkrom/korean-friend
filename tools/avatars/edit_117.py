"""Portrait 117: closes the open shoulders and neckline. Everything inside the shoulder/chest area is recoloured
into the dress's purple silk, keeping the original shading and line work, gold jewellery is kept, and a gold
trim marks the new high neckline. Writes pack/edited/117_1.png."""
import os

from PIL import Image, ImageDraw, ImageFilter

im = Image.open("pack/25_upper/117_1.png").convert("RGBA")
w, h = im.size

area = Image.new("L", (w, h), 0)


def curve(pts, steps=12):
    """Catmull-Rom through pts, so the neckline is a smooth line, not a polygon."""
    out = []
    ext = [pts[0]] + pts + [pts[-1]]
    for i in range(1, len(ext) - 2):
        p0, p1, p2, p3 = ext[i - 1], ext[i], ext[i + 1], ext[i + 2]
        for k in range(steps):
            t = k / steps
            out.append(tuple(0.5 * (2 * p1[j] + (-p0[j] + p2[j]) * t + (2 * p0[j] - 5 * p1[j] + 4 * p2[j] - p3[j]) * t * t
                                    + (-p0[j] + 3 * p1[j] - 3 * p2[j] + p3[j]) * t ** 3) for j in range(2)))
    out.append(pts[-1])
    return out


neck = curve([(262, 470), (305, 444), (385, 438), (440, 455), (500, 470), (560, 482), (606, 498), (624, 522)])
ImageDraw.Draw(area).polygon(
    neck + [(600, 550), (545, 550), (500, 542), (440, 552), (405, 566), (402, 642), (352, 642), (345, 562), (300, 547), (262, 542)],
    fill=255,
)
mask = area.filter(ImageFilter.GaussianBlur(2))

ramp = [(0.0, (16, 10, 28)), (0.45, (52, 38, 84)), (0.8, (96, 76, 140)), (1.0, (150, 128, 190))]


def silk(lum):
    for (a, ca), (b, cb) in zip(ramp, ramp[1:]):
        if lum <= b:
            t = (lum - a) / (b - a)
            return tuple(round(ca[i] + (cb[i] - ca[i]) * t) for i in range(3))
    return ramp[-1][1]


px = im.load()
soft = im.filter(ImageFilter.GaussianBlur(3)).load()
cloth = im.copy()
cp = cloth.load()
for y in range(420, 640):
    for x in range(260, 640):
        if not area.getpixel((x, y)):
            continue
        r, g, b, a = px[x, y]
        if r > 150 and r - b > 80 and (430 <= x <= 520 and 440 <= y <= 490 or 520 <= x <= 590 and 500 <= y <= 540 or 380 <= x <= 470 and 480 <= y <= 540):  # brooch and flowers stay
            continue
        line = (0.3 * r + 0.59 * g + 0.11 * b) / 255
        sr, sg, sb, _ = soft[x, y]
        lum = (0.3 * sr + 0.59 * sg + 0.11 * sb) / 255
        # Smooth silk from the blurred shading; the drawing's dark line work stays on top.
        lum = min(lum, line + 0.15) if line < 0.25 else lum
        cp[x, y] = silk(min(1.0, max(0.0, (lum - 0.1) / 0.85))) + (a,)
out = Image.composite(cloth, im, mask)

trim = ImageDraw.Draw(out)
trim.line(neck, fill=(28, 18, 14, 255), width=8, joint="curve")
trim.line(neck, fill=(206, 164, 86, 255), width=4, joint="curve")
trim.line([(x, y - 1) for x, y in neck], fill=(245, 214, 140, 255), width=1, joint="curve")

os.makedirs("pack/edited", exist_ok=True)
out.save("pack/edited/117_1.png")
print("ok")
