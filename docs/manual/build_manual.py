#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Generates the FX-7000G Owner's Manual as a PDF.

Styled after an original CASIO scientific-calculator manual: a cover with a
line-art drawing of the calculator, a preface, a table of contents, worked
examples with "Operation / Display" columns and key-sequence graphics, an
appendix, specifications and an alphabetical index.

The document is built entirely with fpdf2 and the DejaVu font family (which
carries every calculator glyph: pi, sqrt, arrows, superscripts, degree marks).
All facts, key labels and examples are taken directly from the app's source
(CalculatorButtons.kt, CalculatorState.kt, Evaluator.kt, NumberFormatter.kt).
"""

import os
from fpdf import FPDF
from fpdf.enums import XPos, YPos

# --------------------------------------------------------------------------- #
#  Paths / fonts
# --------------------------------------------------------------------------- #
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.abspath(os.path.join(HERE, "..", "FX-7000G_Manual.pdf"))
FONT_DIR = "/usr/share/fonts/truetype/dejavu"

F_SANS = os.path.join(FONT_DIR, "DejaVuSans.ttf")
F_SANS_B = os.path.join(FONT_DIR, "DejaVuSans-Bold.ttf")
F_SANS_O = os.path.join(FONT_DIR, "DejaVuSans-Oblique.ttf")
F_MONO = os.path.join(FONT_DIR, "DejaVuSansMono.ttf")
F_MONO_B = os.path.join(FONT_DIR, "DejaVuSansMono-Bold.ttf")

# --------------------------------------------------------------------------- #
#  Palette  (muted, print-manual look)
# --------------------------------------------------------------------------- #
INK = (28, 32, 36)          # near-black body text
ACCENT = (16, 78, 108)      # deep teal headings
ACCENT_LT = (218, 232, 238) # heading rule / table header wash
RULE = (170, 182, 188)      # thin rules
SUB = (90, 100, 108)        # secondary text
BODY_DARK = (48, 54, 60)    # calculator body
BODY_EDGE = (30, 34, 38)
KEY_FILL = (74, 82, 90)
KEY_NUM = (96, 104, 112)
KEY_TXT = (238, 240, 242)
SHIFT_C = (222, 150, 44)    # orange shift legend
ALPHA_C = (196, 86, 74)     # red alpha legend
LCD_BG = (150, 176, 120)    # green LCD
LCD_DK = (34, 46, 30)       # LCD pixels/text
EXE_C = (44, 110, 96)

# Glyph shorthands (exact display glyphs the app inserts)
INV = "\u207B\u00B9"        # superscript -1
XROOT = "\u02E3\u221A"      # x-th root
SQRT = "\u221A"
PI = "\u03C0"
MUL = "\u00D7"
DIV = "\u00F7"
MINUS = "\u2212"
SUP2 = "\u00B2"
DEG = "\u00B0"
MIN = "\u2032"
SEC = "\u2033"
ARR = "\u2192"
LARR = "\u25C4"
RARR = "\u25BA"
SUPX = "\u02E3"


# --------------------------------------------------------------------------- #
#  PDF subclass
# --------------------------------------------------------------------------- #
class Manual(FPDF):
    def __init__(self):
        super().__init__(orientation="P", unit="mm", format="A4")
        self.set_margins(20, 22, 20)
        self.set_auto_page_break(True, margin=20)
        self.add_font("DJ", "", F_SANS)
        self.add_font("DJ", "B", F_SANS_B)
        self.add_font("DJ", "I", F_SANS_O)
        self.add_font("MONO", "", F_MONO)
        self.add_font("MONO", "B", F_MONO_B)
        self.chapter = ""          # running header text
        self.show_chrome = False   # header/footer on?
        self.index = {}            # term -> sorted set of page numbers

    # -- running header / footer ------------------------------------------- #
    def header(self):
        if not self.show_chrome:
            return
        self.set_font("DJ", "", 8)
        self.set_text_color(*SUB)
        self.set_y(11)
        self.cell(0, 5, "CASIO  fx-7000G", align="L")
        self.cell(0, 5, self.chapter, align="R",
                  new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.set_draw_color(*RULE)
        self.set_line_width(0.2)
        self.line(20, 18, 190, 18)

    def footer(self):
        if not self.show_chrome:
            return
        self.set_y(-15)
        self.set_draw_color(*RULE)
        self.set_line_width(0.2)
        self.line(20, self.get_y(), 190, self.get_y())
        self.set_font("DJ", "", 8)
        self.set_text_color(*SUB)
        self.set_y(-13)
        self.cell(0, 5, str(self.page_no()), align="C")

    # -- index helper ------------------------------------------------------ #
    def idx(self, *terms):
        for t in terms:
            self.index.setdefault(t, set()).add(self.page_no())

    # -- typographic helpers ---------------------------------------------- #
    def h1(self, title, number=None, new_page=True):
        if new_page:
            self.add_page()
        self.set_text_color(*ACCENT)
        if number is not None:
            self.set_font("DJ", "B", 10)
            self.set_text_color(*SUB)
            self.cell(0, 6, "CHAPTER %s" % number,
                      new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.set_text_color(*ACCENT)
        self.set_font("DJ", "B", 21)
        self.multi_cell(0, 9, title, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        y = self.get_y() + 1.5
        self.set_draw_color(*ACCENT)
        self.set_line_width(0.7)
        self.line(20, y, 190, y)
        self.ln(6)
        self.set_text_color(*INK)

    def h2(self, title):
        self.ln(2)
        if self.get_y() > 250:
            self.add_page()
        self.set_font("DJ", "B", 13)
        self.set_text_color(*ACCENT)
        self.multi_cell(0, 7, title, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.set_text_color(*INK)
        self.ln(1.5)

    def h3(self, title):
        self.ln(1.5)
        if self.get_y() > 255:
            self.add_page()
        self.set_font("DJ", "B", 10.5)
        self.set_text_color(*BODY_EDGE)
        self.multi_cell(0, 6, title, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.set_text_color(*INK)
        self.ln(0.5)

    def body(self, text):
        self.set_font("DJ", "", 10)
        self.set_text_color(*INK)
        self.multi_cell(0, 5.4, text, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.ln(1.2)

    def bullet(self, text, indent=6):
        self.set_font("DJ", "", 10)
        self.set_text_color(*INK)
        x0 = self.l_margin + indent
        self.set_x(x0)
        self.cell(4, 5.2, "\u2022")
        self.multi_cell(190 - x0 - 4, 5.2, text,
                        new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.ln(0.6)

    def note(self, text, label="Note"):
        self.ln(1)
        y0 = self.get_y()
        self.set_font("DJ", "", 9.2)
        # measure height
        self.set_xy(28, y0 + 2)
        lines = self.multi_cell(154, 4.8, text, dry_run=True, output="LINES")
        h = 4 + len(lines) * 4.8
        self.set_fill_color(244, 240, 228)
        self.set_draw_color(214, 196, 150)
        self.set_line_width(0.3)
        self.rect(20, y0, 170, h, style="DF")
        self.set_fill_color(214, 196, 150)
        self.rect(20, y0, 2.2, h, style="F")
        self.set_xy(28, y0 + 2)
        self.set_font("DJ", "B", 9.2)
        self.set_text_color(*BODY_EDGE)
        self.cell(0, 4.8, label + ": ", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.set_xy(28 + self.get_string_width(label + ": ") , y0 + 2)
        self.set_font("DJ", "", 9.2)
        self.set_text_color(*INK)
        self.multi_cell(154 - self.get_string_width(label + ": "), 4.8, text,
                        new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.set_y(y0 + h)
        self.ln(3)
        self.set_text_color(*INK)

    def need(self, mm):
        """Page-break if less than *mm* space remains."""
        if self.get_y() + mm > self.h - self.b_margin:
            self.add_page()

    # ----------------------------------------------------------------- #
    #  Key-cap chip drawn inline in text lines
    # ----------------------------------------------------------------- #
    def keycap(self, x, y, label, w=None, fill=KEY_FILL, txt=KEY_TXT, fs=8):
        self.set_font("DJ", "B", fs)
        if w is None:
            w = self.get_string_width(label) + 4.5
        h = 5.2
        self.set_fill_color(*fill)
        self.set_draw_color(*BODY_EDGE)
        self.set_line_width(0.25)
        self.rect(x, y, w, h, style="DF", round_corners=True, corner_radius=1.1)
        self.set_text_color(*txt)
        self.set_xy(x, y + 0.2)
        self.cell(w, h - 0.4, label, align="C")
        return w

    def key_sequence(self, keys, gap=2.4):
        """Render a row of key-caps with -> arrows between (a key sequence)."""
        self.need(12)
        x = self.l_margin
        y = self.get_y() + 1
        for i, k in enumerate(keys):
            if i:
                self.set_font("DJ", "", 9)
                self.set_text_color(*SUB)
                self.set_xy(x, y - 0.2)
                self.cell(4, 5.6, ARR, align="C")
                x += 4.6
            label, kind = (k if isinstance(k, tuple) else (k, "fn"))
            if x > 175:  # wrap
                x = self.l_margin
                y += 8
            fill = {"num": KEY_NUM, "fn": KEY_FILL, "shift": SHIFT_C,
                    "op": KEY_FILL, "exe": EXE_C, "ac": (150, 60, 52)}.get(kind, KEY_FILL)
            w = self.keycap(x, y, label, fill=fill)
            x += w + gap
        self.set_y(y + 8)
        self.set_text_color(*INK)

    # ----------------------------------------------------------------- #
    #  Operation / Display example table (classic CASIO layout)
    # ----------------------------------------------------------------- #
    def example(self, title, rows, note=None):
        """rows: list of (operation, display).  Header 'Example ...'."""
        self.need(18 + 6 * len(rows))
        self.ln(1)
        # caption bar
        self.set_font("DJ", "B", 9.8)
        self.set_fill_color(*ACCENT)
        self.set_text_color(255, 255, 255)
        self.cell(170, 6.6, "  " + title, fill=True,
                  new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        # column heads
        opw, dsw = 108, 62
        self.set_font("DJ", "B", 8.6)
        self.set_text_color(*BODY_EDGE)
        self.set_fill_color(*ACCENT_LT)
        self.set_draw_color(*RULE)
        self.set_line_width(0.2)
        self.cell(opw, 5.6, "  Operation", border="LR", fill=True)
        self.cell(dsw, 5.6, "  Display", border="R", fill=True,
                  new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        # rows
        for op, disp in rows:
            hop = self._mc_height(op, opw - 4, 9.4, "MONO")
            hds = self._mc_height(disp, dsw - 4, 9.4, "MONO", bold=True)
            rh = max(hop, hds, 5.6)
            x0, y0 = self.get_x(), self.get_y()
            self.need_row(rh)
            x0, y0 = self.get_x(), self.get_y()
            self.set_draw_color(*RULE)
            self.rect(x0, y0, opw, rh)
            self.rect(x0 + opw, y0, dsw, rh)
            self.set_font("MONO", "", 9.4)
            self.set_text_color(*INK)
            self.set_xy(x0 + 2, y0 + (rh - hop) / 2)
            self.multi_cell(opw - 4, 4.7, op)
            self.set_font("MONO", "B", 9.4)
            self.set_text_color(*BODY_EDGE)
            self.set_xy(x0 + opw + 2, y0 + (rh - hds) / 2)
            self.multi_cell(dsw - 4, 4.7, disp, align="R")
            self.set_xy(x0, y0 + rh)
        if note:
            self.set_font("DJ", "I", 8.4)
            self.set_text_color(*SUB)
            self.multi_cell(170, 4.4, note,
                            new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.set_text_color(*INK)
        self.ln(3)

    def need_row(self, rh):
        if self.get_y() + rh > self.h - self.b_margin:
            self.add_page()

    def _mc_height(self, text, w, fs, family, bold=False):
        self.set_font(family, "B" if bold else "", fs)
        lines = self.multi_cell(w, 4.7, text, dry_run=True, output="LINES")
        return max(1, len(lines)) * 4.7 + 1.2

    # ----------------------------------------------------------------- #
    #  Small green LCD illustration
    # ----------------------------------------------------------------- #
    def lcd(self, x, y, w, status, entry_lines, result, cap=None):
        h = w * 64 / 96.0
        # bezel
        self.set_fill_color(*BODY_DARK)
        self.set_draw_color(*BODY_EDGE)
        self.set_line_width(0.4)
        self.rect(x - 2, y - 2, w + 4, h + 4, style="DF",
                  round_corners=True, corner_radius=1.5)
        # screen
        self.set_fill_color(*LCD_BG)
        self.rect(x, y, w, h, style="F")
        # 16 columns x 8 rows character grid
        cw = w / 16.0
        ch = h / 8.0
        self.set_text_color(*LCD_DK)
        mono = "MONO"
        fs = ch * 2.05
        # status row (row 0)
        self.set_font(mono, "", fs)
        self.set_xy(x + 0.4, y + 0.2)
        self.cell(w / 2, ch, status[0])
        self.set_xy(x + w / 2 - 0.4, y + 0.2)
        self.cell(w / 2, ch, status[1], align="R")
        # entry lines (rows 2..3)
        for i, ln in enumerate(entry_lines):
            self.set_xy(x + 0.4, y + ch * (2 + i))
            self.cell(w, ch, ln)
        # result (row 6, right aligned)
        if result is not None:
            self.set_font(mono, "B", fs)
            self.set_xy(x - 0.4, y + ch * 6)
            self.cell(w, ch, result, align="R")
        if cap:
            self.set_font("DJ", "I", 8)
            self.set_text_color(*SUB)
            self.set_xy(x - 2, y + h + 3)
            self.cell(w + 4, 4, cap, align="C")
        self.set_text_color(*INK)
        return h

    # ----------------------------------------------------------------- #
    #  Full calculator line-art (cover + general guide)
    # ----------------------------------------------------------------- #
    def calculator(self, x, y, w):
        # Layout mirrors CalculatorButtons.kt.
        rows = [
            ("f", [("SHIFT", "sh"), ("ALPHA", "al"), ("hyp", None),
                   ("MODE", "SET"), ("DEL", None), ("AC", "ac")]),
            ("f", [("sin", "L", "sin"+INV), ("cos", "N", "cos"+INV),
                   ("tan", "O", "tan"+INV), ("(", "P", None), (")", "Q", ",")]),
            ("f", [("log", "R", "10"+SUPX), ("ln", "S", "e"+SUPX),
                   ("x"+SUP2, "T", XROOT), ("x"+SUPX, "U", "x"+INV),
                   (SQRT, "V", "Rng")]),
            ("f", [("x!", "W", None), ("Abs", "Y", SEC), ("Int", None, DEG),
                   ("Frac", None, MIN), ("X", None, ARR)]),
            ("f", [("DEC", None, "and"), ("HEX", None, "or"),
                   ("BIN", None, "xor"), ("OCT", None, "Not"),
                   ("Graph", None, "Bltin")]),
            ("n", [("7", "A", "nPr"), ("8", "B", "nCr"), ("9", "C", "Ran#"),
                   (DIV, None, None), (MUL, None, None)]),
            ("n", [("4", "D", "Pol"), ("5", "E", "Rec"), ("6", "F", "%"),
                   (MINUS, None, None), ("+", None, None)]),
            ("n", [("1", "G", None), ("2", "H", None), ("3", "I", None),
                   (PI, "J", "e"), ("EXP", "K", None)]),
            ("n", [("0", "Z", None), (".", None, None), ("(-)", None, None),
                   ("Ans", None, None), ("M+", "M", "M")]),
            ("e", [(LARR, None, None), (RARR, None, None), ("EXE", None, None)]),
        ]
        h = w * 1.62
        # body
        self.set_fill_color(*BODY_DARK)
        self.set_draw_color(*BODY_EDGE)
        self.set_line_width(0.6)
        self.rect(x, y, w, h, style="DF", round_corners=True, corner_radius=4)
        # brand
        self.set_font("DJ", "B", w * 0.052)
        self.set_text_color(235, 237, 239)
        self.set_xy(x + 5, y + 3.5)
        self.cell(0, 5, "CASIO")
        self.set_font("DJ", "B", w * 0.045)
        self.set_text_color(*SHIFT_C)
        self.set_xy(x + w - 42, y + 3.6)
        self.cell(37, 5, "fx-7000G", align="R")
        self.set_font("DJ", "", w * 0.026)
        self.set_text_color(200, 204, 208)
        self.set_xy(x + w - 42, y + 8.2)
        self.cell(37, 3, "SCIENTIFIC  \u00b7  GRAPH", align="R")
        # screen
        pad = 5
        sw = w - 2 * pad
        sy = y + 12
        self.lcd(x + pad, sy, sw, ("DEG", "M"),
                 ["2+3" + MUL + "4"], "14")
        sh = sw * 64 / 96.0
        # keypad
        ky = sy + sh + 6
        kpad = 4
        kw = w - 2 * kpad
        gap = 1.6
        total_units = 0.5 * 5 + 0.85 * 4 + 0.72
        avail = h - (ky - y) - 5
        unit = (avail - gap * (len(rows) - 1)) / total_units
        cy = ky
        for kind, keys in rows:
            rh = unit * (0.5 if kind == "f" else (0.72 if kind == "e" else 0.85))
            if kind == "e":
                # arrows small + big EXE
                n = 5  # arrow, arrow, EXE(x3)
                kwid = (kw - gap * 2) / 5.0
                specs = [(keys[0][0], 1, KEY_FILL), (keys[1][0], 1, KEY_FILL),
                         (keys[2][0], 3, EXE_C)]
                cx = x + kpad
                for lbl, span, fill in specs:
                    bw = kwid * span + gap * (span - 1)
                    self._draw_key(cx, cy, bw, rh, lbl, None, None, fill,
                                   kind, unit)
                    cx += bw + gap
            else:
                n = len(keys)
                kwid = (kw - gap * (n - 1)) / n
                cx = x + kpad
                for spec in keys:
                    lbl = spec[0]
                    al = spec[1] if len(spec) > 1 else None
                    sf = spec[2] if len(spec) > 2 else None
                    fill = KEY_NUM if (kind == "n" and lbl.isdigit()) else KEY_FILL
                    if lbl in ("SHIFT",):
                        fill = SHIFT_C
                    if lbl in ("ALPHA",):
                        fill = ALPHA_C
                    if lbl == "AC":
                        fill = (150, 60, 52)
                    self._draw_key(cx, cy, kwid, rh, lbl, al, sf, fill,
                                   kind, unit)
                    cx += kwid + gap
            cy += rh + gap
        self.set_text_color(*INK)
        return h

    def _draw_key(self, x, y, w, h, label, alpha, shift, fill, kind, unit):
        self.set_fill_color(*fill)
        self.set_draw_color(*BODY_EDGE)
        self.set_line_width(0.2)
        self.rect(x, y, w, h, style="DF", round_corners=True, corner_radius=0.8)
        # shift legend (above)
        if shift:
            self.set_font("DJ", "", max(2.6, unit * 0.30))
            self.set_text_color(*SHIFT_C)
            self.set_xy(x, y + 0.3)
            self.cell(w, unit * 0.34, shift, align="C")
        # main label
        self.set_font("DJ", "B", max(3.4, unit * (0.40 if kind != "f" else 0.42)))
        self.set_text_color(*KEY_TXT)
        yoff = y + (h * (0.30 if shift or alpha else 0.5)) - unit * 0.22
        self.set_xy(x, max(y, yoff))
        self.cell(w, unit * 0.42, label, align="C")
        # alpha legend (below)
        if alpha:
            self.set_font("DJ", "", max(2.6, unit * 0.30))
            self.set_text_color(*ALPHA_C)
            self.set_xy(x, y + h - unit * 0.34)
            self.cell(w, unit * 0.32, alpha, align="C")


# --------------------------------------------------------------------------- #
#  Build
# --------------------------------------------------------------------------- #
pdf = Manual()
pdf.set_title("CASIO fx-7000G Owner's Manual")
pdf.set_author("fx-7000G project")
pdf.set_creator("build_manual.py")

# ============================== COVER ====================================== #
pdf.show_chrome = False
pdf.add_page()
pdf.set_fill_color(*ACCENT)
pdf.rect(0, 0, 210, 46, style="F")
pdf.set_fill_color(*SHIFT_C)
pdf.rect(0, 46, 210, 2.4, style="F")
pdf.set_xy(20, 12)
pdf.set_font("DJ", "B", 30)
pdf.set_text_color(255, 255, 255)
pdf.cell(0, 12, "CASIO", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
pdf.set_xy(20, 26)
pdf.set_font("DJ", "", 15)
pdf.cell(0, 8, "fx-7000G  Scientific & Graphing Calculator")

pdf.set_xy(20, 60)
pdf.set_font("DJ", "B", 25)
pdf.set_text_color(*ACCENT)
pdf.cell(0, 12, "Owner's Manual", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
pdf.set_x(20)
pdf.set_font("DJ", "", 11)
pdf.set_text_color(*SUB)
pdf.cell(0, 7, "Operation guide \u00b7 Worked examples \u00b7 Key reference")

# calculator drawing
pdf.calculator(63, 84, 84)

pdf.set_xy(20, 272)
pdf.set_font("DJ", "", 8.5)
pdf.set_text_color(*SUB)
pdf.cell(0, 4, "A faithful recreation of the 1985 CASIO fx-7000G \u2014 Kotlin + Jetpack Compose edition.",
         new_x=XPos.LMARGIN, new_y=YPos.NEXT)
pdf.set_x(20)
pdf.cell(0, 4, "Dot-matrix 96 \u00d7 64 LCD \u00b7 DEG/RAD/GRA \u00b7 BASE-n \u00b7 function graphing.")

# ============================== PREFACE ==================================== #
pdf.show_chrome = True
pdf.chapter = "Preface"
pdf.add_page()
pdf.set_font("DJ", "B", 21)
pdf.set_text_color(*ACCENT)
pdf.cell(0, 10, "Preface", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
yy = pdf.get_y() + 1
pdf.set_draw_color(*ACCENT); pdf.set_line_width(0.7); pdf.line(20, yy, 190, yy)
pdf.ln(7)
pdf.body(
    "Congratulations on your fx-7000G. This calculator revives the spirit of the "
    "machine that, in 1985, became the world's first graphing scientific calculator "
    "\u2014 now rebuilt as a modern Android application with a genuine dot-matrix "
    "display, a full scientific engine and function graphing.")
pdf.body(
    "This manual is your complete guide. It explains every key, every operating mode "
    "and every function, and it is filled with worked examples set out in the classic "
    "\u201cOperation / Display\u201d style: read the key sequence on the left, and see "
    "exactly what appears on the LCD on the right. Follow along on your own calculator "
    "and you will master it in an afternoon.")
pdf.body(
    "The examples in this book are not decorative \u2014 each one is a real calculation "
    "the engine performs, with the exact result the display produces. Where a key has "
    "more than one meaning, the manual shows how the SHIFT, ALPHA and hyp prefixes "
    "unlock its second and third functions.")
pdf.h3("How to use this manual")
pdf.bullet("If you have never used the calculator before, read Chapter 1 (General Guide) first \u2014 it explains the display and the prefix keys that everything else depends on.")
pdf.bullet("For everyday arithmetic and editing, see Chapter 2.")
pdf.bullet("Use the Table of Contents (overleaf) to jump to a topic, and the alphabetical Index at the back to look up a specific function or key.")
pdf.bullet("Throughout, a key on the keypad is drawn as a chip, e.g. the EXE key, and multi-key operations are shown as a sequence.")
pdf.note(
    "In this manual the display glyphs are printed exactly as they appear on the "
    "calculator: \u00d7 and \u00f7 for multiply and divide, " + MINUS +
    " for subtract/negative, " + SQRT + " for square root, " + PI +
    " for pi and " + ARR + " for the variable-store arrow.", label="Reading the symbols")

# ============================== CONTENTS =================================== #
def render_toc(p: FPDF, outline):
    p.set_xy(p.l_margin, 24)
    p.set_font("DJ", "B", 21)
    p.set_text_color(*ACCENT)
    p.cell(0, 10, "Contents", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    y = p.get_y() + 1
    p.set_draw_color(*ACCENT); p.set_line_width(0.7); p.line(20, y, 190, y)
    p.ln(7)
    for e in outline:
        if e.level == 0:
            p.ln(1.2)
            p.set_font("DJ", "B", 10.6)
            p.set_text_color(*ACCENT)
            indent = 0
        else:
            p.set_font("DJ", "", 9.6)
            p.set_text_color(*INK)
            indent = 8
        label = e.name
        pg = str(e.page_number)
        p.set_x(20 + indent)
        avail = 170 - indent - 12
        # dotted leader
        tw = p.get_string_width(label)
        p.cell(tw + 1.5, 6, label)
        dots_w = avail - tw
        if dots_w > 4:
            p.set_text_color(*RULE)
            p.set_font("DJ", "", 9)
            ndots = int(dots_w / p.get_string_width("."))
            p.cell(dots_w, 6, "." * max(0, ndots))
        p.set_text_color(*SUB if e.level else ACCENT)
        p.set_font("DJ", "B" if e.level == 0 else "", 9.6)
        p.cell(12, 6, pg, align="R", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    p.set_text_color(*INK)

pdf.chapter = "Contents"
pdf.add_page()
pdf.chapter = "General Guide"
pdf.insert_toc_placeholder(render_toc, allow_extra_pages=True)


# --------------------------------------------------------------------------- #
#  CHAPTER 1 - GENERAL GUIDE
# --------------------------------------------------------------------------- #
pdf.chapter = "General Guide"
pdf.h1("General Guide", number="1", new_page=False)
pdf.start_section("1  General Guide", level=0)

pdf.body(
    "This chapter introduces the parts of the calculator you will use in every "
    "calculation: the display, the keyboard, and the three prefix keys that give "
    "each button more than one function.")

pdf.start_section("1-1  The keyboard", level=1)
pdf.h2("1-1  The keyboard")
pdf.body(
    "The keyboard is arranged in ten rows. The upper five rows are the slim "
    "function keys (scientific functions, modes and base conversion); the lower "
    "rows carry the numbers, the four arithmetic operators and the large EXE key "
    "that evaluates an entry. Most keys print a small legend above and below the "
    "main label: the upper (orange) legend is reached with SHIFT, the lower (red) "
    "legend with ALPHA.")
pdf.idx("keyboard", "keys")

# keypad figure
pdf.need(150)
figx = 55
pdf.calculator(figx, pdf.get_y(), 100)
pdf.set_font("DJ", "I", 8)
pdf.set_text_color(*SUB)
pdf.ln(2)
pdf.cell(0, 4, "The fx-7000G keyboard and display.", align="C",
         new_x=XPos.LMARGIN, new_y=YPos.NEXT)
pdf.set_text_color(*INK)
pdf.ln(3)

pdf.start_section("1-2  The prefix keys: SHIFT, ALPHA, hyp", level=1)
pdf.h2("1-2  The prefix keys: SHIFT, ALPHA and hyp")
pdf.idx("SHIFT key", "ALPHA key", "hyp key", "prefix keys")
pdf.body(
    "Three keys do not enter anything by themselves. Instead they change the meaning "
    "of the next key you press. They are one-shot: after the next key they switch off "
    "automatically. While a prefix is active a small indicator (S, A or h) appears at "
    "the right of the status line.")
pdf.bullet("SHIFT selects the function printed in orange above a key. Example: SHIFT then the sin key gives sin" + INV + " (arc-sine).")
pdf.bullet("ALPHA selects the red letter printed below a key, so you can enter the variables A through Z.")
pdf.bullet("hyp turns the sin / cos / tan keys into the hyperbolic functions sinh, cosh and tanh. Press SHIFT before hyp to reach the inverse hyperbolics sinh" + INV + ", cosh" + INV + " and tanh" + INV + ".")
pdf.note("SHIFT and ALPHA are mutually exclusive \u2014 turning one on turns the other off. Pressing a prefix a second time cancels it.")

pdf.start_section("1-3  The display", level=1)
pdf.h2("1-3  The display")
pdf.idx("display", "LCD")
pdf.body(
    "The screen is a dot-matrix liquid-crystal display of 96 \u00d7 64 pixels. Text "
    "is drawn on a 5 \u00d7 7 pixel font inside 6 \u00d7 8 cells, giving 16 characters "
    "across and up to 8 rows. In calculation the layout is:")
pdf.bullet("Status line (top): the current mode at the left (DEG, RAD, GRA, or the base name Hex/Oct/Bin/Dec), and at the right the S / A / h prefix flags and the M memory flag.")
pdf.bullet("Entry line: the expression you are typing, over two rows if needed (up to 32 characters), with the cursor kept in view.")
pdf.bullet("Result line: the answer, right-aligned, shown after you press EXE.")

pdf.need(52)
yy = pdf.get_y() + 2
pdf.lcd(28, yy, 64, ("DEG", "  h  M"), ["sin" + INV + "(0.5"], "30",
        cap="Status flags, an entry line and a right-aligned result.")
pdf.lcd(120, yy, 64, ("Hex", "M"), ["1A" + MUL + "FF"], "19E6",
        cap="BASE-n mode: the active base replaces the angle unit.")
pdf.set_y(yy + 64 * 64 / 96.0 + 10)

pdf.start_section("1-4  Turning entries into answers", level=1)
pdf.h2("1-4  Entering and evaluating")
pdf.body(
    "Type an expression using the number and function keys, then press EXE to "
    "evaluate it. The result appears on the result line and is remembered as Ans "
    "(see Chapter 2). To correct a mistake use DEL to erase the character before the "
    "cursor, or AC to clear the whole entry.")
pdf.key_sequence([("2", "num"), ("+", "op"), ("3", "num"),
                  (MUL, "op"), ("4", "num"), ("EXE", "exe")])
pdf.body("The sequence above computes 2 + 3 " + MUL + " 4. Because " + MUL +
         " binds tighter than +, the answer is 14 (not 20).")


# --------------------------------------------------------------------------- #
#  CHAPTER 2 - BASIC CALCULATIONS
# --------------------------------------------------------------------------- #
pdf.chapter = "Basic Calculations"
pdf.h1("Basic Calculations", number="2")
pdf.start_section("2  Basic Calculations", level=0)

pdf.start_section("2-1  Arithmetic and precedence", level=1)
pdf.h2("2-1  Arithmetic and precedence")
pdf.idx("arithmetic", "operator precedence", "precedence")
pdf.body(
    "The four operators +, " + MINUS + ", " + MUL + " and " + DIV +
    " work as you expect. Multiplication and division are evaluated before addition "
    "and subtraction; use parentheses to override the order. The closing parenthesis "
    "may be omitted at the end of an expression \u2014 the calculator supplies it "
    "automatically when you press EXE.")
pdf.example("Example 1 \u2014 order of operations", [
    ("2+3" + MUL + "4  EXE", "14"),
    ("(2+3)" + MUL + "4  EXE", "20"),
    ("2" + MUL + "(3+4  EXE", "14"),
], note="The last line shows the optional closing parenthesis: 2" + MUL + "(3+4 is read as 2" + MUL + "(3+4).")

pdf.start_section("2-2  Negative numbers", level=1)
pdf.h2("2-2  Negative numbers")
pdf.idx("negative numbers", "(-) key")
pdf.body(
    "Use the (-) key to enter a negative value, and the " + MINUS +
    " key for subtraction. Both print the same minus glyph on the display; the "
    "engine understands a leading minus as negation.")
pdf.example("Example 2 \u2014 signed values", [
    ("(-)5+8  EXE", "3"),
    ("8" + MINUS + "(-)5  EXE", "13"),
])

pdf.start_section("2-3  Implicit multiplication", level=1)
pdf.h2("2-3  Implicit multiplication")
pdf.idx("implicit multiplication")
pdf.body(
    "You may leave out the " + MUL + " sign in front of a constant, a variable, an "
    "opening parenthesis or a function. The calculator inserts the multiplication for "
    "you. This makes expressions read like ordinary mathematics.")
pdf.example("Example 3 \u2014 omitting the multiply sign", [
    ("2" + PI + "  EXE", "6.283185307"),
    ("2(3+2)  EXE", "10"),
    ("(2)(3)  EXE", "6"),
])

pdf.start_section("2-4  Answer memory (Ans)", level=1)
pdf.h2("2-4  Answer memory (Ans)")
pdf.idx("Ans", "answer memory")
pdf.body(
    "The result of every calculation is stored in Ans. Press the Ans key to insert it "
    "into a new expression. As a shortcut, if you press an operator immediately after "
    "EXE, the calculator begins the new entry with Ans automatically \u2014 so you can "
    "chain calculations.")
pdf.example("Example 4 \u2014 chaining with Ans", [
    ("150" + MUL + "1.05  EXE", "157.5"),
    ("+  (auto Ans)", "Ans+"),
    ("10  EXE", "167.5"),
    ("Ans" + DIV + "2  EXE", "83.75"),
])

pdf.start_section("2-5  Editing: replay, DEL and AC", level=1)
pdf.h2("2-5  Editing an entry")
pdf.idx("DEL key", "AC key", "replay", "cursor")
pdf.body(
    "Move the cursor with the " + LARR + " and " + RARR + " keys. Pressing an arrow "
    "just after EXE replays the previous expression so you can edit and re-run it. "
    "DEL deletes the item to the left of the cursor \u2014 a whole function name such "
    "as sin( or Ans is removed in a single press. AC clears everything and also "
    "erases any graph.")
pdf.bullet(LARR + " / " + RARR + "  move the cursor (or, right after EXE, recall the last entry for editing).")
pdf.bullet("DEL  erase one character or one whole token to the left.")
pdf.bullet("AC  clear the entry, the result and any graph curves.")


# --------------------------------------------------------------------------- #
#  CHAPTER 3 - MODES & DISPLAY FORMATS
# --------------------------------------------------------------------------- #
pdf.chapter = "Modes & Formats"
pdf.h1("Modes and Display Formats", number="3")
pdf.start_section("3  Modes and Display Formats", level=0)

pdf.start_section("3-1  Angle unit: DEG / RAD / GRA", level=1)
pdf.h2("3-1  Angle unit \u2014 DEG / RAD / GRA")
pdf.idx("angle mode", "DEG", "RAD", "GRA", "MODE key")
pdf.body(
    "The MODE key cycles the angle unit used by the trigonometric functions: "
    "DEG " + ARR + " RAD " + ARR + " GRA " + ARR + " DEG. The active unit is always "
    "shown at the left of the status line. In degrees a right angle is 90, in radians "
    + PI + "/2, and in grades 100.")
pdf.example("Example 5 \u2014 the same angle in three units", [
    ("[DEG]  sin30  EXE", "0.5"),
    ("[RAD]  sin(" + PI + DIV + "2  EXE", "1"),
    ("[GRA]  sin100  EXE", "1"),
])

pdf.start_section("3-2  Display format: Norm / Fix / Sci", level=1)
pdf.h2("3-2  Display format \u2014 Norm / Fix / Sci")
pdf.idx("display format", "Norm", "Fix", "Sci", "SET menu")
pdf.body(
    "Press SHIFT then MODE (the SET function) to open the setup menu. From here you "
    "choose the angle unit or the number-display format. The menu offers:")
pdf.bullet("1 Deg   2 Rad   3 Gra \u2014 set the angle unit directly.")
pdf.bullet("4 Fix \u2014 then a digit 0\u20139 fixes that many decimal places.")
pdf.bullet("5 Sci \u2014 then a digit 1\u20139 sets the number of significant figures (0 means 10).")
pdf.bullet("6 Norm \u2014 return to the standard 10-significant-digit display.")
pdf.body(
    "In Norm the calculator shows up to 10 significant digits, switching to "
    "exponential notation (mantissa E exponent) for magnitudes of 10\u00b9\u2070 or "
    "greater, or smaller than 10" + INV + "\u2079. Fix rounds to a fixed number of "
    "decimals; Sci always uses exponential form.")
pdf.example("Example 6 \u2014 the same value in three formats", [
    ("[Norm]  2" + DIV + "3  EXE", "0.6666666667"),
    ("[Fix 4]  2" + DIV + "3  EXE", "0.6667"),
    ("[Sci 3]  2" + DIV + "3  EXE", "6.67E-1"),
], note="Set the format from SHIFT " + ARR + " MODE (SET) before evaluating.")
pdf.note(
    "SET is reached with SHIFT " + ARR + " MODE. After choosing 4 Fix or 5 Sci the "
    "display prompts for the digit count \u2014 just press a number key.",
    label="Opening SET")


# --------------------------------------------------------------------------- #
#  CHAPTER 4 - SCIENTIFIC FUNCTIONS
# --------------------------------------------------------------------------- #
pdf.chapter = "Scientific Functions"
pdf.h1("Scientific Function Calculations", number="4")
pdf.start_section("4  Scientific Function Calculations", level=0)

pdf.start_section("4-1  Trigonometric functions", level=1)
pdf.h2("4-1  Trigonometric and inverse functions")
pdf.idx("sin", "cos", "tan", "inverse trigonometric functions", "trigonometric functions")
pdf.body(
    "The sin, cos and tan keys open a function with a left parenthesis, e.g. sin(. "
    "They use the current angle unit. Their SHIFT functions sin" + INV + ", cos" +
    INV + " and tan" + INV + " are the inverse (arc) functions.")
pdf.example("Example 7 \u2014 trig and arc-trig (DEG)", [
    ("cos60  EXE", "0.5"),
    ("tan45  EXE", "1"),
    ("sin" + INV + "(0.5  EXE", "30"),
])

pdf.start_section("4-2  Hyperbolic functions", level=1)
pdf.h2("4-2  Hyperbolic functions (hyp)")
pdf.idx("hyperbolic functions", "sinh", "cosh", "tanh")
pdf.body(
    "Press hyp before sin, cos or tan to get sinh, cosh and tanh. Press SHIFT then "
    "hyp then the key for the inverse hyperbolic functions.")
pdf.key_sequence([("hyp", "fn"), ("sin", "fn"), ("1", "num"), ("EXE", "exe")])
pdf.example("Example 8 \u2014 hyperbolic", [
    ("hyp sin1  EXE", "1.175201194"),
    ("hyp cos0  EXE", "1"),
    ("SHIFT hyp tan(0.5  EXE", "0.5493061443"),
])

pdf.start_section("4-3  Logarithms and exponentials", level=1)
pdf.h2("4-3  Logarithms and exponentials")
pdf.idx("logarithm", "log", "ln", "exponential", "10^x", "e^x")
pdf.body(
    "log is the common (base-10) logarithm and ln is the natural logarithm. Their "
    "SHIFT functions are the antilogarithms: 10" + SUPX + " and e" + SUPX + ", which "
    "begin the power expressions 10^( and e^(.")
pdf.example("Example 9 \u2014 logs and powers of the base", [
    ("log100  EXE", "2"),
    ("ln(e  EXE", "1"),
    ("10" + SUPX + "3  EXE", "1000"),
    ("e" + SUPX + "1  EXE", "2.718281828"),
])

pdf.start_section("4-4  Powers, roots and reciprocals", level=1)
pdf.h2("4-4  Powers, roots and reciprocals")
pdf.idx("power", "square", "square root", "cube root", "reciprocal", "x^y")
pdf.body(
    "The calculator offers x" + SUP2 + " (square), x" + SUPX + " (general power, the "
    "^ key), " + SQRT + " (square root), " + XROOT + " (the index-th root, SHIFT of "
    "x" + SUP2 + "), and x" + INV + " (reciprocal, SHIFT of the power key). Powers are "
    "evaluated right-to-left, so 2^3^2 is 2^(3^2) = 512.")
pdf.example("Example 10 \u2014 powers and roots", [
    ("5" + SUP2 + "  EXE", "25"),
    ("2^10  EXE", "1024"),
    ("2^3^2  EXE", "512"),
    (SQRT + "(2  EXE", "1.414213562"),
    ("3" + XROOT + "8  EXE", "2"),
    ("4" + INV + "  EXE", "0.25"),
], note="3" + XROOT + "8 is the cube root of 8; the index is entered before the " + XROOT + " symbol.")

pdf.start_section("4-5  Factorial, percent and combinatorics", level=1)
pdf.h2("4-5  Factorial, percent, nPr and nCr")
pdf.idx("factorial", "percent", "nPr", "nCr", "permutation", "combination")
pdf.body(
    "x! gives the factorial of a whole number (0 to 69). % divides by 100. nPr and "
    "nCr (SHIFT of 7 and 8) are the permutation and combination counts; they bind "
    "more tightly than + and " + MINUS + " but looser than " + MUL + " and " + DIV + ".")
pdf.example("Example 11 \u2014 counting", [
    ("5!  EXE", "120"),
    ("50%  EXE", "0.5"),
    ("5nPr2  EXE", "20"),
    ("5nCr2  EXE", "10"),
])

pdf.start_section("4-6  Number functions: Abs, Int, Frac", level=1)
pdf.h2("4-6  Abs, Int and Frac")
pdf.idx("Abs", "Int", "Frac", "integer part", "fractional part")
pdf.body(
    "Abs( returns the absolute value. Int( truncates toward zero to the integer part, "
    "and Frac( returns the fractional part (the value minus its integer part).")
pdf.example("Example 12 \u2014 parts of a number", [
    ("Abs((-)7  EXE", "7"),
    ("Int(3.7  EXE", "3"),
    ("Frac(3.7  EXE", "0.7"),
    ("Int((-)3.7  EXE", "-3"),
])

pdf.start_section("4-7  Coordinate conversion: Pol and Rec", level=1)
pdf.h2("4-7  Coordinate conversion \u2014 Pol and Rec")
pdf.idx("Pol", "Rec", "coordinate conversion", "polar", "rectangular")
pdf.body(
    "Pol(x, y) converts rectangular coordinates to polar: it returns the radius r and "
    "stores the angle " + PI.upper() + " in variable J (with r also placed in I). "
    "Rec(r, " + "\u03b8" + ") does the reverse, returning x and storing y in J. The "
    "comma is the SHIFT of the ) key. The angle uses the current unit.")
pdf.example("Example 13 \u2014 rectangular " + ARR + " polar (DEG)", [
    ("Pol(3,4  EXE", "5"),
    ("J  EXE", "53.13010235"),
    ("Rec(5,53.13  EXE", "3"),
], note="After Pol, recall the angle by pressing ALPHA then J. Results I and J are ordinary variables.")

pdf.start_section("4-8  Degrees, minutes, seconds", level=1)
pdf.h2("4-8  Sexagesimal (degrees " + DEG + " minutes " + MIN + " seconds " + SEC + ")")
pdf.idx("degrees minutes seconds", "sexagesimal", "DMS")
pdf.body(
    "Enter an angle in degrees-minutes-seconds using the " + DEG + " (SHIFT Int), " +
    MIN + " (SHIFT Frac) and " + SEC + " (SHIFT Abs) marks. The calculator converts "
    "the entry to decimal degrees.")
pdf.example("Example 14 \u2014 DMS to decimal degrees", [
    ("30" + DEG + "30" + MIN + "  EXE", "30.5"),
    ("1" + DEG + "30" + MIN + "36" + SEC + "  EXE", "1.51"),
])


# --------------------------------------------------------------------------- #
#  CHAPTER 5 - MEMORY & VARIABLES
# --------------------------------------------------------------------------- #
pdf.chapter = "Memory & Variables"
pdf.h1("Memory and Variables", number="5")
pdf.start_section("5  Memory and Variables", level=0)

pdf.start_section("5-1  Variables A\u2013Z", level=1)
pdf.h2("5-1  Variables A through Z")
pdf.idx("variables", "store", "arrow key", "A-Z")
pdf.body(
    "The calculator has 26 lettered memories, A to Z. Enter a letter with the ALPHA "
    "prefix. Store a value by following it with the " + ARR + " arrow (SHIFT of the X "
    "key) and a letter: value " + ARR + " A. Recall a variable simply by using its "
    "letter in an expression; an unset variable reads as 0.")
pdf.example("Example 15 \u2014 storing and recalling", [
    ("5" + ARR + "A  EXE", "5"),
    ("A" + MUL + "2  EXE", "10"),
    ("A" + SUP2 + "+1  EXE", "26"),
], note="Type A with ALPHA " + ARR + " (the 7 key's red legend), and " + ARR + " with SHIFT " + ARR + " X.")

pdf.start_section("5-2  Independent memory (M+ and M)", level=1)
pdf.h2("5-2  Independent memory \u2014 M+ and M")
pdf.idx("independent memory", "M+", "memory", "M flag")
pdf.body(
    "The M+ key adds the current value to the independent memory M. Recall M with "
    "SHIFT M+ (the M legend). Whenever M holds a non-zero value the M flag is shown "
    "on the status line. Clear it by storing 0 into M (0 " + ARR + " M).")
pdf.example("Example 16 \u2014 accumulating a running total", [
    ("250  M+", "250"),
    ("120  M+", "120"),
    ("M  EXE", "370"),
], note="Each M+ adds to M; recall the total with the M legend (SHIFT M+).")


# --------------------------------------------------------------------------- #
#  CHAPTER 6 - BASE-N
# --------------------------------------------------------------------------- #
pdf.chapter = "BASE-n"
pdf.h1("BASE-n Calculations", number="6")
pdf.start_section("6  BASE-n Calculations", level=0)

pdf.start_section("6-1  Choosing a base", level=1)
pdf.h2("6-1  Decimal, hexadecimal, binary and octal")
pdf.idx("BASE-n", "hexadecimal", "binary", "octal", "DEC", "HEX", "BIN", "OCT")
pdf.body(
    "The DEC, HEX, BIN and OCT keys switch the calculator into integer base-n mode "
    "and convert the current value to that base. The active base (Dec, Hex, Bin or "
    "Oct) is shown on the status line in place of the angle unit. In a non-decimal "
    "base the keypad accepts only the digits valid for that base \u2014 hexadecimal "
    "digits A\u2013F are the ALPHA letters. Values are whole numbers only.")
pdf.example("Example 17 \u2014 converting between bases", [
    ("[DEC] 255 HEX", "FF"),
    ("[same value] BIN", "11111111"),
    ("[same value] OCT", "377"),
    ("[same value] DEC", "255"),
], note="Press a base key to convert the shown value; press DEC to return to ordinary decimal.")

pdf.start_section("6-2  Bitwise logic", level=1)
pdf.h2("6-2  Bitwise logic \u2014 and, or, xor, Not")
pdf.idx("bitwise", "and", "or", "xor", "Not", "logical operators")
pdf.body(
    "In base-n mode the SHIFT legends of the base keys give the logical operators "
    "and, or, xor and Not, which act bit by bit on the integer operands. Their "
    "precedence, from loosest to tightest, is: or, then xor, then and, then + and " +
    MINUS + ", then " + MUL + " and " + DIV + ", then the unary Not. These words also "
    "work in decimal, where they force integer arithmetic.")
pdf.example("Example 18 \u2014 logic in hexadecimal / decimal", [
    ("[HEX] 1A and F  EXE", "A"),
    ("[HEX] 1C or 3  EXE", "1F"),
    ("[DEC] 12 and 8  EXE", "8"),
    ("[DEC] Not 0  EXE", "-1"),
])


# --------------------------------------------------------------------------- #
#  CHAPTER 7 - GRAPHING
# --------------------------------------------------------------------------- #
pdf.chapter = "Graphing"
pdf.h1("Graphing Functions", number="7")
pdf.start_section("7  Graphing Functions", level=0)

pdf.start_section("7-1  Drawing a graph", level=1)
pdf.h2("7-1  Drawing Y = f(X)")
pdf.idx("graph", "Graph key", "X variable")
pdf.body(
    "Enter an expression that uses the variable X (the X key), then press Graph. The "
    "calculator plots Y = f(X) across the display, drawing the axes with tick marks. "
    "Graph again with a new expression to overlay it on the previous curves \u2014 up "
    "to six curves are kept. Pressing any ordinary key returns you to the "
    "calculation screen.")
pdf.key_sequence([("sin", "fn"), ("X", "fn"), ("Graph", "fn")])
pdf.need(52)
yy = pdf.get_y() + 2
pdf.lcd(72, yy, 76, ("DEG", ""), [], None, cap="A plot of Y = sin X with axes and tick marks.")
# draw a sine curve inside that lcd
def _sine_overlay(x, y, w):
    import math
    h = w * 64 / 96.0
    pdf.set_draw_color(*LCD_DK); pdf.set_line_width(0.25)
    # axis
    pdf.line(x, y + h / 2, x + w, y + h / 2)
    pdf.line(x + w / 2, y, x + w / 2, y + h)
    px = None; py = None
    for i in range(0, 97):
        ax = -180 + 360 * i / 96.0
        ay = math.sin(math.radians(ax))
        cx = x + w * i / 96.0
        cy = y + h / 2 - ay * (h / 2 - 2)
        if px is not None:
            pdf.line(px, py, cx, cy)
        px, py = cx, cy
_sine_overlay(72, yy, 76)
pdf.set_y(yy + 76 * 64 / 96.0 + 10)

pdf.start_section("7-2  The graph window (Range)", level=1)
pdf.h2("7-2  Setting the window \u2014 Range")
pdf.idx("Range", "graph window", "XMIN", "XMAX", "XSCL")
pdf.body(
    "Press SHIFT then " + SQRT + " (the Rng function) to open the Range editor. It "
    "lists six values that define the viewing window and the spacing of the tick "
    "marks. Move between fields with EXE (or the arrows), type a new value, and press "
    "Graph to redraw. The defaults are XMIN=-4.7, XMAX=4.7, XSCL=1, YMIN=-3.1, "
    "YMAX=3.1, YSCL=1.")
pdf.bullet("XMIN / XMAX \u2014 the left and right edges of the window.")
pdf.bullet("XSCL \u2014 the distance between tick marks along the x-axis.")
pdf.bullet("YMIN / YMAX \u2014 the bottom and top edges.")
pdf.bullet("YSCL \u2014 the tick spacing along the y-axis.")
pdf.note("If XMAX is not greater than XMIN (or YMAX not greater than YMIN) the graph cannot be drawn and Ma ERROR is shown.")

pdf.start_section("7-3  Built-in graphs (Bltin)", level=1)
pdf.h2("7-3  Built-in graphs \u2014 Bltin")
pdf.idx("built-in graphs", "Bltin", "preset graphs")
pdf.body(
    "Press SHIFT then Graph (the Bltin function) to choose a ready-made graph. Pick a "
    "number from the menu and the calculator plots it immediately:")
pdf.bullet("1 sinX   2 cosX   3 tanX   4 X" + SUP2)
pdf.bullet("5 X^3   6 " + SQRT + "X   7 1/X   8 lnX")

pdf.start_section("7-4  Tracing a curve", level=1)
pdf.h2("7-4  Tracing")
pdf.idx("trace", "graph trace")
pdf.body(
    "While a graph is shown, press " + LARR + " or " + RARR + " to switch on the "
    "trace cursor. A pointer moves along the most recent curve, and the X and Y "
    "coordinates of the cursor are read out at the foot of the screen. Keep pressing "
    "the arrows to move the pointer one dot-column at a time.")


# --------------------------------------------------------------------------- #
#  CHAPTER 8 - ERRORS
# --------------------------------------------------------------------------- #
pdf.chapter = "Error Messages"
pdf.h1("Error Messages", number="8")
pdf.start_section("8  Error Messages", level=0)
pdf.idx("error", "Ma ERROR")
pdf.body(
    "When a calculation cannot be completed the display shows Ma ERROR. Press AC, "
    "DEL or any editing key to clear the message and correct the entry. The common "
    "causes are listed below.")

# simple two-column table
def simple_table(headers, rows, widths):
    pdf.set_font("DJ", "B", 9)
    pdf.set_fill_color(*ACCENT_LT); pdf.set_draw_color(*RULE); pdf.set_line_width(0.2)
    pdf.set_text_color(*BODY_EDGE)
    for h, w in zip(headers, widths):
        pdf.cell(w, 6.4, "  " + h, border=1, fill=True)
    pdf.ln()
    pdf.set_font("DJ", "", 9)
    pdf.set_text_color(*INK)
    for row in rows:
        # height
        hs = []
        for txt, w in zip(row, widths):
            n = len(pdf.multi_cell(w - 3, 4.7, txt, dry_run=True, output="LINES"))
            hs.append(max(1, n) * 4.7 + 1.4)
        rh = max(hs)
        if pdf.get_y() + rh > pdf.h - pdf.b_margin:
            pdf.add_page()
        x0, y0 = pdf.get_x(), pdf.get_y()
        cx = x0
        for txt, w in zip(row, widths):
            pdf.rect(cx, y0, w, rh)
            pdf.set_xy(cx + 1.6, y0 + (rh - (len(pdf.multi_cell(w-3,4.7,txt,dry_run=True,output="LINES"))*4.7))/2 + 0.4)
            pdf.multi_cell(w - 3, 4.7, txt)
            cx += w
        pdf.set_xy(x0, y0 + rh)

pdf.ln(1)
simple_table(
    ["Cause", "Example"],
    [
        ["Division by zero", "5" + DIV + "0,  or  x" + INV + " of 0"],
        ["Argument outside a function's domain (e.g. log or " + SQRT +
         " of a non-positive number, sin" + INV + " outside -1\u20131)",
         "log0,  " + SQRT + "((-)4,  sin" + INV + "(2"],
        ["Result too large to display (overflow, |x| \u2265 10\u00b9\u2070\u2070; "
         "base-n beyond about 9.2\u00d710\u00b9\u2078)", "10" + SUPX + "200"],
        ["Factorial of a non-integer, a negative number, or a value above 69",
         "3.5!,  70!"],
        ["nPr / nCr with invalid arguments (negative, non-integer, or r > n)",
         "3nPr5"],
        ["Incomplete or malformed expression", "2+" + MUL],
    ],
    [78, 92],
)


# --------------------------------------------------------------------------- #
#  APPENDIX A - KEY REFERENCE
# --------------------------------------------------------------------------- #
pdf.chapter = "Appendix"
pdf.h1("Appendix A \u2014 Key Reference")
pdf.start_section("Appendix A  Key Reference", level=0)
pdf.idx("key reference")
pdf.body(
    "Every key with its primary function and its SHIFT (orange) and ALPHA (red) "
    "legends. Prefix keys (SHIFT, ALPHA, hyp) are one-shot.")

key_rows = [
    ("SHIFT", "select orange legend", "\u2014"),
    ("ALPHA", "select red letter", "\u2014"),
    ("hyp", "sinh/cosh/tanh prefix", "\u2014"),
    ("MODE", "cycle DEG/RAD/GRA", "SET (Norm/Fix/Sci setup)"),
    ("DEL / AC", "delete token / clear all", "\u2014"),
    ("sin cos tan", "trig functions", "sin" + INV + " cos" + INV + " tan" + INV + "  \u00b7  ALPHA L N O"),
    ("(  )", "parentheses", ")\u2192 , (comma)  \u00b7  ALPHA P Q"),
    ("log  ln", "base-10 / natural log", "10" + SUPX + "  e" + SUPX + "  \u00b7  ALPHA R S"),
    ("x" + SUP2 + "  x" + SUPX, "square / power", XROOT + "  x" + INV + "  \u00b7  ALPHA T U"),
    (SQRT, "square root", "Rng (Range editor)  \u00b7  ALPHA V"),
    ("x!", "factorial", "ALPHA W"),
    ("Abs Int Frac", "abs / integer / fraction", SEC + "  " + DEG + "  " + MIN + "  \u00b7  ALPHA Y"),
    ("X", "graph variable X", ARR + " (store)"),
    ("DEC HEX BIN OCT", "select number base", "and  or  xor  Not"),
    ("Graph", "plot Y = f(X)", "Bltin (built-in graphs)"),
    ("7 8 9", "digits", "nPr  nCr  Ran#  \u00b7  ALPHA A B C"),
    ("4 5 6", "digits", "Pol  Rec  %  \u00b7  ALPHA D E F"),
    ("1 2 3", "digits", "ALPHA G H I"),
    (PI + "  EXP", "pi / exponent (E)", "e  \u00b7  ALPHA J K"),
    ("0 . (-)", "digit / point / sign", "ALPHA Z"),
    ("Ans", "last answer", "\u2014"),
    ("M+", "add to memory M", "M (recall)  \u00b7  ALPHA M"),
    (LARR + " " + RARR + "  EXE", "cursor / evaluate", "\u2014"),
]
pdf.ln(1)
simple_table(["Key", "Primary function", "SHIFT / ALPHA"], key_rows, [40, 62, 68])


# --------------------------------------------------------------------------- #
#  APPENDIX B - SPECIFICATIONS
# --------------------------------------------------------------------------- #
pdf.chapter = "Appendix"
pdf.h1("Appendix B \u2014 Specifications")
pdf.start_section("Appendix B  Specifications", level=0)
pdf.idx("specifications")
pdf.ln(1)
simple_table(["Item", "Specification"], [
    ["Display", "Dot-matrix LCD, 96 \u00d7 64 pixels; 5\u00d77 font, 16 characters \u00d7 8 rows"],
    ["Entry line", "Up to 32 characters over two rows, with a visible cursor"],
    ["Number display", "Norm (10 significant digits), Fix (0\u20139 decimals), Sci (1\u201310 sig. figures)"],
    ["Overflow", "Ma ERROR at |x| \u2265 10\u00b9\u2070\u2070 (about 9.2\u00d710\u00b9\u2078 in base-n)"],
    ["Angle units", "Degree, Radian, Gradient"],
    ["Number bases", "Decimal, Hexadecimal, Binary, Octal with and / or / xor / Not"],
    ["Memories", "Independent memory M; 26 variables A\u2013Z; Ans; graph variable X"],
    ["Functions", "Trig & inverse, hyperbolic & inverse, log/ln, 10" + SUPX + "/e" + SUPX +
     ", powers & roots, x!, %, nPr, nCr, Abs, Int, Frac, Pol, Rec, DMS"],
    ["Graphing", "Y = f(X) plots, up to 6 overlaid curves, 8 built-in graphs, Range editor, trace"],
    ["Constants", PI + ", e, Ran# (random), Ans"],
], [46, 124])


# --------------------------------------------------------------------------- #
#  INDEX
# --------------------------------------------------------------------------- #
pdf.chapter = "Index"
pdf.h1("Index")

# two-column alphabetical index
entries = sorted(pdf.index.items(), key=lambda kv: kv[0].lower())
col_w = 82
col_x = [pdf.l_margin, pdf.l_margin + col_w + 6]
col = 0
top = pdf.get_y()
pdf.set_xy(col_x[0], top)
pdf.set_font("DJ", "", 9)
line_h = 5.0
bottom = pdf.h - pdf.b_margin
cur_letter = ""
y = top
for term, pages in entries:
    letter = term[0].upper()
    block = line_h + (5.5 if letter != cur_letter else 0)
    if y + block > bottom:
        col += 1
        if col > 1:
            pdf.add_page()
            col = 0
            top = pdf.get_y()
        y = top
        cur_letter = ""
    if letter != cur_letter:
        cur_letter = letter
        pdf.set_xy(col_x[col], y)
        pdf.set_font("DJ", "B", 10)
        pdf.set_text_color(*ACCENT)
        pdf.cell(col_w, 5.5, letter, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        y += 5.5
    pdf.set_xy(col_x[col], y)
    pdf.set_font("DJ", "", 9)
    pdf.set_text_color(*INK)
    pg = ", ".join(str(p) for p in sorted(pages))
    label = term
    # leader dots
    avail = col_w
    tw = pdf.get_string_width(label + "  ")
    pdf.cell(tw, line_h, label + "  ")
    pw = pdf.get_string_width(pg)
    dots_w = avail - tw - pw
    if dots_w > 3:
        pdf.set_text_color(*RULE)
        nd = int(dots_w / pdf.get_string_width("."))
        pdf.cell(dots_w, line_h, "." * max(0, nd))
    pdf.set_text_color(*SUB)
    pdf.cell(pw, line_h, pg)
    y += line_h
    pdf.set_text_color(*INK)

pdf.output(OUT)
print("Wrote", OUT)
