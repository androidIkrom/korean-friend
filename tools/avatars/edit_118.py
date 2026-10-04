"""Portrait 118: dresses the bare torso and arms. Skin under the shoulder armour is recoloured into a dark navy
tunic like his sleeves, keeping the body's shading and line work. Writes pack/edited/118_1.png."""
import os

from PIL import Image, ImageDraw, ImageFilter

im = Image.open("pack/25_upper/118_1.png").convert("RGBA")
w, h = im.size

area = Image.new("L", (w, h), 0)
d = ImageDraw.Draw(area)
d.polygon([(215, 655), (330, 640), (470, 645), (565, 650), (570, h), (205, h)], fill=255)  # chest and belly
d.polygon([(40, 470), (238, 470), (244, h), (30, h)], fill=255)  # left arm
d.polygon([(555, 680), (740, 680), (753, h), (560, h)], fill=255)  # right arm

px = im.load()
ap = area.load()
skin = Image.new("L", (w, h), 0)
sp = skin.load()
for y in range(460, h):
    for x in range(w):
        if ap[x, y]:
            r, g, b, a = px[x, y]
            if a > 0 and r > b + 25 and r > 70 and g > b:
                sp[x, y] = 255
mask = skin.filter(ImageFilter.MaxFilter(5)).filter(ImageFilter.GaussianBlur(1.5))

ramp = [(0.0, (10, 12, 22)), (0.45, (34, 40, 66)), (0.8, (62, 72, 108)), (1.0, (110, 120, 160))]


def tunic(lum):
    for (a, ca), (b, cb) in zip(ramp, ramp[1:]):
        if lum <= b:
            t = (lum - a) / (b - a)
            return tuple(round(ca[i] + (cb[i] - ca[i]) * t) for i in range(3))
    return ramp[-1][1]


cloth = im.copy()
cp = cloth.load()
mp = mask.load()
for y in range(460, h):
    for x in range(w):
        if mp[x, y]:
            r, g, b, a = px[x, y]
            lum = (0.3 * r + 0.59 * g + 0.11 * b) / 255
            cp[x, y] = tunic(min(1.0, max(0.0, (lum - 0.1) / 0.6))) + (a,)
out = Image.composite(cloth, im, mask)
os.makedirs("pack/edited", exist_ok=True)
out.save("pack/edited/118_1.png")
print("ok")
