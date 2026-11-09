"""Grafica di sfondo della copertina (A4 a 200 dpi): fascia prugna con il logo STRADE in bianco."""
import sys

from PIL import Image, ImageDraw

LOGO, OUT = sys.argv[1], sys.argv[2]
DPI = 200
PX = DPI / 2.54  # pixel per centimetro
W, H = round(21 * PX), round(29.7 * PX)

PLUM = (0x3E, 0x2A, 0x37)
PLUM_LIGHT = (0x4C, 0x36, 0x45)
MAUVE = (0xC7, 0xAD, 0xBF)

BLOCK_H = 13.2  # cm


def cm(v):
    return round(v * PX)


img = Image.new("RGB", (W, H), "white")
d = ImageDraw.Draw(img)
d.rectangle([0, 0, W, cm(BLOCK_H)], fill=PLUM)

# Diagonale come quella del logo (dal basso a sinistra verso l'alto a destra), ritagliata nella fascia.
band = Image.new("RGB", (W, cm(BLOCK_H)), PLUM)
bd = ImageDraw.Draw(band)
half = 2.3  # metà larghezza della diagonale, misurata in orizzontale
x0, y0 = 11.0, BLOCK_H + 0.5  # punto basso della linea mediana
x1, y1 = x0 + 16, y0 - 16
bd.polygon([(cm(x0 - half), cm(y0)), (cm(x0 + half), cm(y0)), (cm(x1 + half), cm(y1)), (cm(x1 - half), cm(y1))], fill=PLUM_LIGHT)
img.paste(band, (0, 0))

# Filetto malva sotto la fascia.
d.rectangle([0, cm(BLOCK_H), W, cm(BLOCK_H + 0.22)], fill=MAUVE)

# Logo in bianco: il nero diventa bianco opaco, il bianco trasparente.
logo = Image.open(LOGO).convert("L")
w_cm = 4.6
lw = cm(w_cm)
lh = round(logo.height * lw / logo.width)
logo = logo.resize((lw, lh), Image.LANCZOS)
alpha = logo.point(lambda v: 255 - v)
white = Image.new("RGB", (lw, lh), "white")
img.paste(white, (cm(2.3), cm(2.2)), alpha)

img.save(OUT, optimize=True)
print("ok", OUT, img.size)
