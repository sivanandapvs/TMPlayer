#!/usr/bin/env python3
"""Reproduce original Veylora vectors and Android PNGs. Requires Pillow and fonttools."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
import json
import os

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "art/veylora"
RES = ROOT / "app/src/main/res"
LEFT = [(170, 220), (332, 220), (519, 645), (445, 820)]
RIGHT = [(710, 220), (872, 220), (570, 820), (445, 820)]
PLAY = [(470, 325), (679, 448), (470, 571)]
PATHS = ["M170 220H332L519 645L445 820Z", "M710 220H872L570 820H445Z", "M470 325L679 448L470 571Z"]
FONT = Path(os.environ.get("VEYLORA_WORDMARK_FONT", "C:/Windows/Fonts/segoeuib.ttf"))


def wordmark():
    font = TTFont(FONT)
    glyphs, cmap = font.getGlyphSet(), font.getBestCmap()
    scale = 0.13
    x, parts = 0, []
    for char in "VEYLORA":
        glyph = glyphs[cmap[ord(char)]]
        pen = SVGPathPen(glyphs)
        glyph.draw(pen)
        parts.append(f'<path transform="translate({x:.2f} 0)" d="{pen.getCommands()}"/>')
        x += glyph.width + 100
    return f'<g transform="translate(1020 657) scale({scale} {-scale})">' + "".join(parts) + "</g>"


def svg(background=None, word=False, monochrome=False):
    width = 2650 if word else 1024
    bg = f'<rect width="{width}" height="1024" rx="180" fill="{background}"/>' if background else ""
    defs = '<defs><linearGradient id="v" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#C5A3FF"/><stop offset=".48" stop-color="#963EFF"/><stop offset="1" stop-color="#32106B"/></linearGradient><linearGradient id="r" x2="1" y2="1"><stop stop-color="#557BFF"/><stop offset="1" stop-color="#963EFF"/></linearGradient></defs>'
    fills = ["url(#v)", "url(#r)", "#C5A3FF"] if not monochrome else ["#FFFFFF"] * 3
    if background == "#FFFFFF":
        fills = ["#32106B", "#963EFF", "#557BFF"]
    shapes = "".join(f'<path d="{path}" fill="{fill}"/>' for path, fill in zip(PATHS, fills))
    ink = "#32106B" if background == "#FFFFFF" else "#FFFFFF"
    mark = f'<g fill="{ink}">{wordmark()}</g>' if word else ""
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="1024" viewBox="0 0 {width} 1024"><title>Veylora</title>{defs}{bg}{shapes}{mark}</svg>\n'


def png(size, background=True, foreground=False, mono=False, light=False):
    n = size * 4
    canvas = Image.new("RGBA", (n, n), "#080713" if background else (0, 0, 0, 0))
    # Adaptive icons reserve the outer 18dp of the 108dp viewport for launcher masking.
    scale = n / 1024 * (0.72 if foreground else 1)
    inset = n * .14 if foreground else 0
    colors = [(197, 163, 255), (85, 123, 255), (197, 163, 255)]
    ends = [(96, 28, 190), (150, 62, 255), (197, 163, 255)]
    if light:
        colors = ends = [(50, 16, 107), (150, 62, 255), (85, 123, 255)]
    for polygon, start, end in zip([LEFT, RIGHT, PLAY], colors, ends):
        mask = Image.new("L", (n, n))
        ImageDraw.Draw(mask).polygon([(x * scale + inset, y * scale + inset) for x, y in polygon], fill=255)
        layer = Image.new("RGBA", (n, n))
        draw = ImageDraw.Draw(layer)
        for y in range(n):
            t = y / max(1, n - 1)
            rgb = (255, 255, 255) if mono else tuple(round(a + (b-a)*t) for a, b in zip(start, end))
            draw.line((0, y, n, y), fill=rgb + (255,))
        canvas.paste(layer, mask=mask)
    return canvas.resize((size, size), Image.Resampling.LANCZOS)


def vector(foreground=False, mono=False, background=False):
    fills = ["#963EFF", "#557BFF", "#C5A3FF"] if not mono else ["#FFFFFF"] * 3
    paths = "".join(f'<path android:fillColor="{color}" android:pathData="{path}"/>' for path, color in zip(PATHS, fills))
    if foreground:
        paths = '<group android:scaleX="0.72" android:scaleY="0.72" android:translateX="143.36" android:translateY="143.36">' + paths + '</group>'
    if background:
        paths = '<path android:fillColor="#080713" android:pathData="M0 0H1024V1024H0Z"/>' + paths
    return '<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="1024" android:viewportHeight="1024">' + paths + '</vector>\n'


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, bg, word, mono in [
        ("primary", None, True, False), ("symbol", None, False, False),
        ("logo-dark", "#080713", True, False), ("logo-light", "#FFFFFF", True, False),
        ("launcher", "#080713", False, False), ("adaptive-foreground", None, False, False),
        ("monochrome", None, False, True), ("splash", None, False, False),
    ]:
        (OUT / f"{name}.svg").write_text(svg(bg, word, mono), encoding="utf-8")
    (OUT / "adaptive-background.svg").write_text('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108"><path fill="#080713" d="M0 0H108V108H0Z"/></svg>\n', encoding="utf-8")
    for name, args in [("symbol", (False, False, False)), ("launcher", (True, False, False)),
                       ("adaptive-foreground", (False, True, False)), ("monochrome", (False, True, True)),
                       ("splash", (False, False, False))]:
        png(1024, *args).save(OUT / f"{name}.png")
    Image.new("RGB", (1080, 1080), "#080713").save(OUT / "adaptive-background.png")
    for name, bg in [("primary", None), ("logo-dark", "#080713"), ("logo-light", "#FFFFFF")]:
        canvas = Image.new("RGBA", (2650, 1024), bg or (0, 0, 0, 0))
        symbol = png(1024, False, light=bg == "#FFFFFF")
        canvas.paste(symbol, (0, 0), symbol)
        draw = ImageDraw.Draw(canvas)
        font = ImageFont.truetype(str(FONT), 266)
        x = 1020
        for char in "VEYLORA":
            draw.text((x, 657), char, font=font, fill="#32106B" if bg == "#FFFFFF" else "#FFFFFF", anchor="ls")
            x += font.getlength(char) + 13
        canvas.save(OUT / f"{name}.png")
    for density, size in {"mdpi":48, "hdpi":72, "xhdpi":96, "xxhdpi":144, "xxxhdpi":192}.items():
        folder = RES / f"mipmap-{density}"
        folder.mkdir(exist_ok=True)
        icon = png(size)
        icon.save(folder / "ic_launcher.png")
        icon.save(folder / "ic_launcher_round.png")
    for name, args in [("ic_logo", (False, False, False)), ("ic_launcher_foreground", (True, False, False)),
                       ("ic_launcher_monochrome", (True, True, False))]:
        (RES / "drawable" / f"{name}.xml").write_text(vector(*args), encoding="utf-8")
    (RES / "drawable/ic_launcher_background.xml").write_text('<?xml version="1.0" encoding="utf-8"?><shape xmlns:android="http://schemas.android.com/apk/res/android"><solid android:color="#080713"/></shape>\n', encoding="utf-8")
    banner = Image.new("RGB", (640, 360), "#080713")
    symbol = png(290, False)
    banner.paste(symbol, (12, 35), symbol)
    ImageDraw.Draw(banner).text((290, 148), "VEYLORA", fill="#FFFFFF", font=ImageFont.truetype(str(FONT), 49))
    # A raster banner carries a real wordmark at Android TV's required 320x180 size.
    banner.resize((320, 180), Image.Resampling.LANCZOS).save(RES / "drawable-nodpi/tv_banner.png")
    desktop = ROOT / "desktop/packaging/icons"
    icon = png(1024)
    icon.save(desktop / "tmplayer.png")
    icon.save(desktop / "tmplayer.ico", sizes=[(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])
    icon.save(desktop / "tmplayer.icns")
    (desktop / "tmplayer.svg").write_text(svg("#080713"), encoding="utf-8")
    for path in (desktop / "hicolor").glob("*.png"):
        side = int(path.stem.split("x")[0])
        icon.resize((side, side), Image.Resampling.LANCZOS).save(path)


if __name__ == "__main__":
    main()
