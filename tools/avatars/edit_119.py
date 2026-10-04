"""Portrait 119: long sleeves. Bare forearm skin is recoloured into the dress's cream linen, keeping the arm's
shading and line work. Writes pack/edited/119_1.png."""
import os

from PIL import Image, ImageDraw, ImageFilter

im = Image.open("pack/25_upper/119_1.png").convert("RGBA")
w, h = im.size

area = Image.new("L", (w, h), 0)
d = ImageDraw.Draw(area)
d.polygon([(200, 680), (300, 676), (302, 826), (200, 830)], fill=255)  # left forearm, sleeve cuff to bracelet
d.polygon([(490, 488), (548, 500), (552, 565), (492, 565)], fill=255)  # right wrist above the bracelet

px = im.load()
skin = Image.new("L", (w, h), 0)
sp = skin.load()
for y in range(h):
    for x in range(w):
        if area.getpixel((x, y)):
            r, g, b, a = px[x, y]
            if a > 0 and r > b + 20 and r > 80:
                sp[x, y] = 255
mask = skin.filter(ImageFilter.MaxFilter(5)).filter(ImageFilter.GaussianBlur(1.5))

ramp = [(0.0, (70, 60, 52)), (0.4, (198, 188, 170)), (0.75, (234, 227, 210)), (1.0, (250, 247, 238))]


def linen(lum):
    for (a, ca), (b, cb) in zip(ramp, ramp[1:]):
        if lum <= b:
            t = (lum - a) / (b - a)
            return tuple(round(ca[i] + (cb[i] - ca[i]) * t) for i in range(3))
    return ramp[-1][1]


cloth = im.copy()
cp = cloth.load()
mp = mask.load()
for y in range(h):
    for x in range(w):
        if mp[x, y]:
            r, g, b, a = px[x, y]
            lum = (0.3 * r + 0.59 * g + 0.11 * b) / 255
            cp[x, y] = linen(min(1.0, max(0.0, (lum - 0.2) / 0.6))) + (a,)
out = Image.composite(cloth, im, mask)
os.makedirs("pack/edited", exist_ok=True)
out.save("pack/edited/119_1.png")
print("ok")
