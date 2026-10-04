"""Second pass on portrait 104: long sleeves. Bare forearm skin is recoloured into the dress's blue silk, keeping
the arm's own shading so it reads as a fitted sleeve, and a gold cuff closes the sleeve at the right wrist.
Reads and rewrites pack/edited/104_1.png (run edit_104.py first)."""
from PIL import Image, ImageDraw, ImageFilter

im = Image.open("pack/edited/104_1.png").convert("RGBA")
w, h = im.size

# Forearms only; the fist and the face stay as they are.
area = Image.new("L", (w, h), 0)
d = ImageDraw.Draw(area)
d.polygon([(505, 398), (540, 420), (600, 445), (660, 480), (660, 575), (560, 575), (520, 500), (498, 455)], fill=255)  # right forearm
d.polygon([(20, 628), (110, 625), (125, 690), (95, 829), (0, 829), (0, 700)], fill=255)  # left forearm
d.polygon([(482, 640), (500, 640), (545, 760), (545, 829), (484, 829)], fill=255)  # right leg in the skirt's side slit
d.polygon([(195, 780), (245, 780), (245, 829), (195, 829)], fill=255)  # bottom tip of the left slit

px = im.load()
ap = area.load()
skin = Image.new("L", (w, h), 0)
sp = skin.load()
for y in range(h):
    for x in range(w):
        if ap[x, y]:
            r, g, b, a = px[x, y]
            if a > 0 and r > b + 20 and r > 80:
                sp[x, y] = 255
mask = skin.filter(ImageFilter.MaxFilter(5)).filter(ImageFilter.GaussianBlur(1.5))

# Silk ramp from the dress: deep shadow, body blue, sheen.
ramp = [(0.0, (12, 16, 40)), (0.45, (32, 48, 104)), (0.8, (58, 82, 148)), (1.0, (105, 130, 190))]


def silk(lum):
    for (a, ca), (b, cb) in zip(ramp, ramp[1:]):
        if lum <= b:
            t = (lum - a) / (b - a)
            return tuple(round(ca[i] + (cb[i] - ca[i]) * t) for i in range(3))
    return ramp[-1][1]


sleeve = im.copy()
sl = sleeve.load()
for y in range(h):
    for x in range(w):
        if sp[x, y] or mask.getpixel((x, y)) > 0:
            r, g, b, a = px[x, y]
            lum = (0.3 * r + 0.59 * g + 0.11 * b) / 255
            sl[x, y] = silk(min(1.0, max(0.0, (lum - 0.35) / 0.6))) + (a,)
out = Image.composite(sleeve, im, mask)

# Gold cuff at the right wrist, under the fist.
cuff = ImageDraw.Draw(out)
cuff.line([(497, 452), (508, 400)], fill=(28, 20, 16, 255), width=13)
cuff.line([(497, 452), (508, 400)], fill=(206, 164, 86, 255), width=9)
cuff.line([(499, 446), (507, 408)], fill=(245, 214, 140, 255), width=2)
out.save("pack/edited/104_1.png")
print("ok")
