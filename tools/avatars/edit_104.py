"""Closes the side slit of portrait 104's dress: skin pixels in the slit area are replaced by dress fabric
cloned from just beside it, with a soft mask edge. Writes pack/edited/104_1.png."""
import os

from PIL import Image, ImageChops, ImageDraw, ImageFilter

src = Image.open("pack/25_upper/104_1.png").convert("RGBA")
w, h = src.size

# Slit area (left of the skirt front), found on the 50px grid of the crop.
area = Image.new("L", (w, h), 0)
ImageDraw.Draw(area).polygon([(228, 640), (318, 640), (312, h), (222, h)], fill=255)

# Skin: warm pixels (red clearly above blue) inside the area.
px = src.load()
skin = Image.new("L", (w, h), 0)
sp = skin.load()
ap = area.load()
for y in range(620, h):
    for x in range(200, 340):
        if not ap[x, y]:
            continue
        r, g, b, a = px[x, y]
        if a > 0 and r > b + 25 and r > 90:
            sp[x, y] = 255
# Grow a little so the outline of the leg goes too, then soften the edge.
mask = skin.filter(ImageFilter.MaxFilter(9)).filter(ImageFilter.GaussianBlur(2.5))
mask = ImageChops.multiply(mask, area.filter(ImageFilter.GaussianBlur(3)))

# Fabric: the same rows of the skirt, taken 95px to the right.
fabric = src.transform(src.size, Image.AFFINE, (1, 0, 95, 0, 1, 0))
out = Image.composite(fabric, src, mask)
os.makedirs("pack/edited", exist_ok=True)
out.save("pack/edited/104_1.png")

print("ok")
