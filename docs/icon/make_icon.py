"""Icône du mod : un coffre ouvert d'où sort « EVERY », en pixel art 32×32 agrandi sans lissage.

Lancer avec : python docs/icon/make_icon.py  (écrit icon.png en 512 et 128 pixels, bibliothèque standard seulement)
"""
import struct
import sys
import zlib
from pathlib import Path

SIZE = 32
OUT = Path(__file__).resolve().parents[2]

OUTLINE = (42, 26, 10, 255)
WOOD = (176, 122, 50, 255)
WOOD_LIGHT = (205, 152, 72, 255)
WOOD_DARK = (128, 84, 32, 255)
INSIDE = (54, 34, 14, 255)
LID_INSIDE = (96, 62, 24, 255)
IRON = (220, 220, 220, 255)
IRON_DARK = (130, 130, 130, 255)
GOLD = (255, 214, 64, 255)
TEXT_OUTLINE = (92, 52, 0, 255)
GLOW = (255, 226, 120, 110)
SPARK = (255, 250, 210, 255)

FONT = {
    'E': ["####", "#...", "###.", "#...", "#...", "####"],
    'V': ["#..#", "#..#", "#..#", "#..#", ".##.", ".##."],
    'R': ["###.", "#..#", "###.", "#.#.", "#..#", "#..#"],
    'Y': ["#..#", "#..#", ".##.", ".##.", ".##.", ".##."],
}
GOLD_DARK = (214, 150, 20, 255)

pixels = [[(0, 0, 0, 0)] * SIZE for _ in range(SIZE)]


def put(x, y, color):
    if 0 <= x < SIZE and 0 <= y < SIZE:
        if color[3] == 255:
            pixels[y][x] = color
        elif pixels[y][x][3] == 0:  # lueur sur fond transparent : la couleur telle quelle
            pixels[y][x] = color
        else:  # lueur sur un pixel déjà peint : mélange
            r, g, b, a = pixels[y][x]
            t = color[3] / 255
            pixels[y][x] = (round(r * (1 - t) + color[0] * t), round(g * (1 - t) + color[1] * t),
                            round(b * (1 - t) + color[2] * t), max(a, color[3]))


def rect(x0, y0, x1, y1, color):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(x, y, color)


def box(x0, y0, x1, y1, fill, outline=OUTLINE):
    rect(x0, y0, x1, y1, outline)
    rect(x0 + 1, y0 + 1, x1 - 1, y1 - 1, fill)


# Couvercle ouvert, basculé vers l'arrière : on voit sa face intérieure, plus étroite en haut (perspective).
for i, y in enumerate(range(9, 15)):
    left, right = 8 - i // 2, 23 + i // 2
    put(left, y, OUTLINE)
    put(right, y, OUTLINE)
    for x in range(left + 1, right):
        put(x, y, OUTLINE if y == 9 else (WOOD_DARK if y == 10 else LID_INSIDE))

# Lueur qui monte du coffre vers le texte.
for y in range(9, 15):
    spread = (14 - y) // 2
    for x in range(11 - spread, 21 + spread):
        put(x, y, GLOW)

# Ouverture du coffre (intérieur) puis corps.
box(5, 14, 26, 17, INSIDE)
box(5, 17, 26, 29, WOOD)
rect(6, 18, 25, 18, WOOD_LIGHT)
rect(6, 22, 25, 22, WOOD_DARK)
rect(6, 28, 25, 28, WOOD_DARK)
for x in (11, 20):  # joints des planches
    rect(x, 23, x, 27, WOOD_DARK)
# Loquet.
box(14, 19, 17, 23, IRON, IRON_DARK)
rect(15, 22, 16, 22, IRON_DARK)

# « EVERY » : lettres dorées sur une plaque sombre, qui sortent du coffre.
word = "EVERY"
x0 = (SIZE - (len(word) * 6 - 2)) // 2
y0 = 2
mask = set()
for i, letter in enumerate(word):
    for dy, row in enumerate(FONT[letter]):
        for dx, c in enumerate(row):
            if c == '#':
                mask.add((x0 + i * 6 + dx, y0 + dy))
# Plaque sombre aux coins arrondis derrière les lettres, pour qu'elles restent lisibles sur tout fond.
px0, px1, py0, py1 = x0 - 1, x0 + len(word) * 6 - 2, y0 - 1, y0 + 6
for y in range(py0, py1 + 1):
    for x in range(px0, px1 + 1):
        if (x, y) not in ((px0, py0), (px1, py0), (px0, py1), (px1, py1)):
            put(x, y, TEXT_OUTLINE)
for (x, y) in mask:
    put(x, y, GOLD_DARK if y == y0 + 5 else GOLD)

# Étincelles.
for (cx, cy) in [(2, 12), (29, 11)]:  # étoiles en croix
    put(cx, cy, SPARK)
    for (x, y) in [(cx, cy - 1), (cx, cy + 1), (cx - 1, cy), (cx + 1, cy)]:
        put(x, y, GOLD)
for (x, y) in [(5, 10), (26, 13)]:
    put(x, y, SPARK)


def write_png(path, scale):
    size = SIZE * scale
    raw = bytearray()
    for y in range(size):
        raw.append(0)
        for x in range(size):
            raw.extend(pixels[y // scale][x // scale])

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xFFFFFFFF)

    png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', size, size, 8, 6, 0, 0, 0))
    png += chunk(b'IDAT', zlib.compress(bytes(raw), 9)) + chunk(b'IEND', b'')
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)
    print(path)


write_png(OUT / 'docs' / 'icon' / 'icon_512.png', 16)
write_png(OUT / 'common' / 'src' / 'main' / 'resources' / 'icon.png', 4)
sys.exit(0)
