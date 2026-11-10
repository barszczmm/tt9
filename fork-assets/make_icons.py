"""Generate the TT9 fork launcher icons from a flat logo on a light background.

usage (from the repository root, requires: pip install numpy scipy pillow):
	python3 fork-assets/make_icons.py fork-assets/logo.jpg . <preview dir>

The old launcher vectors (ic_launcher_color, ic_launcher_mono, ic_launcher_background) are left
in place, unused, to avoid conflicts with upstream.
"""
import sys
import os
import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage

SRC, REPO, PREVIEW = sys.argv[1], sys.argv[2], sys.argv[3]
RES = os.path.join(REPO, 'app/src/main/res')

src = np.asarray(Image.open(SRC).convert('RGB')).astype(np.float32)
H, W, _ = src.shape

# --- 1. background colour: median of a ring inside the rounded square, away from the hand
ring = np.concatenate([src[60:140, 300:1700].reshape(-1, 3), src[1880:1940, 300:1700].reshape(-1, 3)])
BG = np.median(ring, axis=0)
print('background', BG.round())

# --- 2. key out the background
dist = np.abs(src - BG).max(axis=2)
LO, HI = 10.0, 45.0
alpha = np.clip((dist - LO) / (HI - LO), 0, 1)

# Pixels close to the background colour that are NOT connected to the outside are enclosed
# by the hand (the white "T9" letters). They belong to the foreground.
bg_like = alpha < 0.5
labels, _ = ndimage.label(bg_like)
outside = set(np.unique(np.concatenate([labels[0], labels[-1], labels[:, 0], labels[:, -1]]))) - {0}
# the white corners outside the rounded square also count as "outside"
enclosed = bg_like & ~np.isin(labels, list(outside))
alpha[enclosed] = 1.0
# the white corners of the source must be fully transparent
alpha[(src.min(axis=2) > 250) & ~enclosed & (dist > HI)] = 0

# also make the pixels next to the enclosed letters fully opaque (anti-aliasing towards white)
grown = ndimage.binary_dilation(enclosed, iterations=6)
alpha[grown] = 1.0

# --- 3. colour decontamination: remove the background colour mixed into the edge pixels
a = alpha[..., None]
fg = np.where(a > 0.02, (src - (1 - a) * BG) / np.maximum(a, 1e-3), 0)
fg = np.clip(fg, 0, 255)
fg_rgba = np.dstack([fg, alpha * 255]).astype(np.uint8)
FG = Image.fromarray(fg_rgba, 'RGBA')

# bounding box and "radius" of the opaque part
ys, xs = np.nonzero(alpha > 0.5)
x0, x1, y0, y1 = xs.min(), xs.max(), ys.min(), ys.max()
cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
radius = np.sqrt((xs - cx) ** 2 + (ys - cy) ** 2).max()
print('bbox', x0, y0, x1, y1, 'centre', cx, cy, 'max radius', radius)
FG_CROP = FG.crop((x0, y0, x1 + 1, y1 + 1))

# Centre the icon horizontally on the four fingers (and the palm below them), not on the whole
# bounding box, which is shifted to the left by the thumb. Only the fingers reach above the thumb.
thumb_top = np.nonzero((alpha > 0.5)[:, :int(x0 + (x1 - x0) * 0.25)].any(axis=1))[0].min()
finger_xs = np.nonzero((alpha[:thumb_top - 20] > 0.5).any(axis=0))[0]
fx0, fx1 = finger_xs.min(), finger_xs.max()
# width of one finger: the first continuous run of opaque columns
finger_width = int(np.argmax(np.diff(finger_xs) > 1)) + 1
# Offset of the visual centre from the centre of the cropped image, in source pixels. Exactly
# centring on the fingers looks too far to the left, so we move half a finger back to the right.
CENTRE_OFFSET_X = (fx0 + fx1) / 2 - cx - finger_width / 2
new_radius = np.sqrt((xs - (cx + CENTRE_OFFSET_X)) ** 2 + (ys - cy) ** 2).max()
print('fingers', fx0, fx1, 'finger width', finger_width, 'centre offset', CENTRE_OFFSET_X, 'max radius from the new centre', new_radius)

# monochrome: the hand silhouette with the letters cut out
mono_alpha = alpha.copy()
is_letter = (src.min(axis=2) > 200) & (np.ptp(src, axis=2) < 30)
mono_alpha[is_letter & (ndimage.binary_dilation(enclosed, iterations=8))] = 0
mono = np.zeros((H, W, 4), np.uint8)
mono[..., :3] = 255
mono[..., 3] = (mono_alpha * 255).astype(np.uint8)
MONO_CROP = Image.fromarray(mono, 'RGBA').crop((x0, y0, x1 + 1, y1 + 1))

bg_hex = '#FF%02X%02X%02X' % tuple(int(round(c)) for c in BG)


def place(img, size, content_radius_frac, canvas_bg=None):
	"""Scale the cropped hand so that its farthest pixel from the centre of its bounding box would be
	at content_radius_frac * size, and place it on a size x size canvas, centred on the fingers."""
	scale = content_radius_frac * size / radius
	w, h = round(img.width * scale), round(img.height * scale)
	resized = img.resize((w, h), Image.LANCZOS)
	canvas = Image.new('RGBA', (size, size), canvas_bg or (0, 0, 0, 0))
	canvas.alpha_composite(resized, (round(size / 2 - w / 2 - CENTRE_OFFSET_X * scale), round(size / 2 - h / 2)))
	return canvas


def rounded_mask(size, radius_frac, inset=0):
	m = Image.new('L', (size * 4, size * 4), 0)
	ImageDraw.Draw(m).rounded_rectangle(
		(inset * 4, inset * 4, size * 4 - 1 - inset * 4, size * 4 - 1 - inset * 4),
		radius=round(size * 4 * radius_frac), fill=255)
	return m.resize((size, size), Image.LANCZOS)


def circle_mask(size, inset=0):
	m = Image.new('L', (size * 4, size * 4), 0)
	ImageDraw.Draw(m).ellipse((inset * 4, inset * 4, size * 4 - 1 - inset * 4, size * 4 - 1 - inset * 4), fill=255)
	return m.resize((size, size), Image.LANCZOS)


bg_rgba = tuple(int(round(c)) for c in BG) + (255,)
DENSITIES = {'mdpi': 1, 'hdpi': 1.5, 'xhdpi': 2, 'xxhdpi': 3, 'xxxhdpi': 4}

# --- 4. adaptive icon layers (108dp). The safe zone is a circle of 66dp diameter (radius 33dp).
# The hand is allowed slightly past it (35dp), because its farthest points are rounded corners.
ADAPTIVE_RADIUS = 35 / 108
for d, f in DENSITIES.items():
	size = round(108 * f)
	out = os.path.join(RES, f'drawable-{d}')
	os.makedirs(out, exist_ok=True)
	place(FG_CROP, size, ADAPTIVE_RADIUS).save(os.path.join(out, 'ic_launcher_fork_foreground.png'), optimize=True)
	place(MONO_CROP, size, ADAPTIVE_RADIUS).save(os.path.join(out, 'ic_launcher_fork_monochrome.png'), optimize=True)

with open(os.path.join(RES, 'drawable/ic_launcher_fork_background.xml'), 'w') as fh:
	fh.write(f'''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
	<solid android:color="{bg_hex}" />
</shape>
''')

# --- 5. legacy icons (Android 7 and older): 48dp, 1dp transparent margin like the stock icons
LEGACY_RADIUS = 17.5 / 48
for d, f in DENSITIES.items():
	size = round(48 * f)
	inset = round(1 * f)
	sq = place(FG_CROP, size, 0.40, bg_rgba)
	sq.putalpha(rounded_mask(size, 0.18, inset))
	sq.save(os.path.join(RES, f'mipmap-{d}/ic_launcher.webp'), lossless=True)
	rd = place(FG_CROP, size, 0.37, bg_rgba)
	rd.putalpha(circle_mask(size, inset))
	rd.save(os.path.join(RES, f'mipmap-{d}/ic_launcher_round.webp'), lossless=True)

# --- 6. store listing icon (F-Droid metadata)
store = place(FG_CROP, 512, 0.36, bg_rgba)
store.putalpha(rounded_mask(512, 0.18))
store.save(os.path.join(REPO, 'fastlane/metadata/android/en-US/images/icon.png'), optimize=True)

# --- 7. preview: how launchers would mask the adaptive icon, plus themed and legacy icons
P = 216
fg_l = place(FG_CROP, P, ADAPTIVE_RADIUS)
mono_l = place(MONO_CROP, P, ADAPTIVE_RADIUS)


# the system crops the 108dp layers to the central 72dp, then applies the launcher mask
def visible(img):
	m = round(P * 18 / 108)
	return img.crop((m, m, P - m, P - m)).resize((P, P), Image.LANCZOS)


def adaptive(mask):
	c = Image.new('RGBA', (P, P), bg_rgba)
	c.alpha_composite(fg_l)
	c = visible(c)
	c.putalpha(mask)
	return c


def themed(mask):
	tint = Image.new('RGBA', (P, P), (196, 222, 255, 255))
	tint.putalpha(mono_l.getchannel('A'))
	c = Image.new('RGBA', (P, P), (28, 49, 77, 255))
	c.alpha_composite(tint)
	c = visible(c)
	c.putalpha(mask)
	return c


tiles = [
	('circle', adaptive(circle_mask(P))),
	('squircle', adaptive(rounded_mask(P, 0.3))),
	('rounded sq.', adaptive(rounded_mask(P, 0.12))),
	('themed (A13+)', themed(circle_mask(P))),
	('legacy', Image.open(os.path.join(RES, 'mipmap-xxxhdpi/ic_launcher.webp')).resize((P, P), Image.LANCZOS)),
	('legacy round', Image.open(os.path.join(RES, 'mipmap-xxxhdpi/ic_launcher_round.webp')).resize((P, P), Image.LANCZOS)),
]
pad = 24
for name, bgc in [('light', (245, 245, 245, 255)), ('dark', (24, 24, 28, 255))]:
	sheet = Image.new('RGBA', (len(tiles) * (P + pad) + pad, P + 2 * pad), bgc)
	for i, (_, t) in enumerate(tiles):
		sheet.alpha_composite(t.convert('RGBA'), (pad + i * (P + pad), pad))
	sheet.convert('RGB').save(os.path.join(PREVIEW, f'icon-preview-{name}.png'))

# safe-zone check: draw the 66dp circle over the foreground
chk = Image.new('RGBA', (P, P), bg_rgba)
chk.alpha_composite(fg_l)
dr = ImageDraw.Draw(chk)
r = P * 33 / 108
dr.ellipse((P / 2 - r, P / 2 - r, P / 2 + r, P / 2 + r), outline=(255, 0, 0, 255), width=2)
m = P * 18 / 108
dr.rectangle((m, m, P - m, P - m), outline=(0, 160, 0, 255), width=2)
chk.convert('RGB').save(os.path.join(PREVIEW, 'icon-safe-zone.png'))
print('farthest point of the adaptive icon: %.1fdp from the centre (safe zone: 33dp, visible: 36dp)' % (new_radius / radius * ADAPTIVE_RADIUS * 108))
print('done')
