"""Genera il manuale Word impaginato a partire dal Markdown.

Procedura completa (Windows con Word installato, Python con python-docx e Pillow):
  1. python copertina.py <logo strade_logo.png> <copertina.png>
  2. python manuale_docx.py <manuale.md> <copertina.png> <bozza.docx>
  3. esporta.ps1 -Src <bozza.docx> -Docx <manuale.docx> -Pdf <manuale.pdf>
     (Word aggiorna indice, intestazioni e numeri di pagina, salva il Word definitivo ed esporta il PDF).

Uso: python manuale_docx.py <manuale.md> <copertina.png> <uscita.docx>

Il Markdown usa un sottoinsieme: ## capitoli, ### sottotitoli, paragrafi, elenchi puntati e numerati,
tabelle, citazioni (> **Nota**, > **Attenzione**, > **Etichetta**) rese come riquadri, **grassetto**,
`codice`, [link](url) e paragrafi interamente in *corsivo* (note minori).
Il sommario e i campi si aggiornano aprendo il file con Word (script esporta.ps1).
"""
import re
import sys

from docx import Document
from docx.enum.style import WD_STYLE_TYPE
from docx.enum.table import WD_CELL_VERTICAL_ALIGNMENT, WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_LINE_SPACING, WD_TAB_ALIGNMENT, WD_TAB_LEADER
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn
from docx.enum.section import WD_SECTION
from docx.shared import Cm, Pt, RGBColor
from docx.text.run import Run

SRC, COVER, OUT = sys.argv[1:4]

FONT, FONT_SB, FONT_LIGHT, MONO = "Segoe UI", "Segoe UI Semibold", "Segoe UI Light", "Consolas"
INK, GRAPHITE, PLUM, WHITE = "1E1A1D", "5F585C", "3E2A37", "FFFFFF"
MAUVE_DEEP, MAUVE, MAUVE_SOFT, MIST, OUTLINE = "7F6074", "C7ADBF", "F1E6ED", "F5F0F3", "DCD2D8"
DANGER, DANGER_SOFT, MOSS, MOSS_SOFT = "B3261E", "FBEAE8", "3F6E57", "E3F0E8"

PAGE_W, PAGE_H = 21.0, 29.7
MARGIN_X, MARGIN_TOP, MARGIN_BOTTOM = 2.3, 2.6, 2.3
TEXT_W = PAGE_W - 2 * MARGIN_X
PT_PER_CM = 72 / 2.54

EDIZIONE = "9 novembre 2026"
VERSIONE_APP = "1.8.0"

PPR_ORDER = ("w:pStyle", "w:keepNext", "w:keepLines", "w:pageBreakBefore", "w:framePr", "w:widowControl",
             "w:numPr", "w:suppressLineNumbers", "w:pBdr", "w:shd", "w:tabs", "w:suppressAutoHyphens",
             "w:kinsoku", "w:wordWrap", "w:overflowPunct", "w:topLinePunct", "w:autoSpaceDE", "w:autoSpaceDN",
             "w:bidi", "w:adjustRightInd", "w:snapToGrid", "w:spacing", "w:ind", "w:contextualSpacing",
             "w:mirrorIndents", "w:suppressOverlap", "w:jc", "w:textDirection", "w:textAlignment",
             "w:textboxTightWrap", "w:outlineLvl", "w:divId", "w:cnfStyle", "w:rPr", "w:sectPr", "w:pPrChange")
RPR_ORDER = ("w:rStyle", "w:rFonts", "w:b", "w:bCs", "w:i", "w:iCs", "w:caps", "w:smallCaps", "w:strike",
             "w:dstrike", "w:outline", "w:shadow", "w:emboss", "w:imprint", "w:noProof", "w:snapToGrid",
             "w:vanish", "w:webHidden", "w:color", "w:spacing", "w:w", "w:kern", "w:position", "w:sz", "w:szCs",
             "w:highlight", "w:u", "w:effect", "w:bdr", "w:shd", "w:fitText", "w:vertAlign", "w:rtl", "w:cs",
             "w:em", "w:lang", "w:eastAsianLayout", "w:specVanish", "w:oMath")
TCPR_ORDER = ("w:cnfStyle", "w:tcW", "w:gridSpan", "w:hMerge", "w:vMerge", "w:tcBorders", "w:shd", "w:noWrap",
              "w:tcMar", "w:textDirection", "w:tcFitText", "w:vAlign", "w:hideMark")
TBLPR_ORDER = ("w:tblStyle", "w:tblpPr", "w:tblOverlap", "w:bidiVisual", "w:tblStyleRowBandSize",
               "w:tblStyleColBandSize", "w:tblW", "w:jc", "w:tblCellSpacing", "w:tblInd", "w:tblBorders", "w:shd",
               "w:tblLayout", "w:tblCellMar", "w:tblLook", "w:tblCaption", "w:tblDescription")
BORDER_ORDER = ("w:top", "w:left", "w:bottom", "w:right", "w:insideH", "w:insideV", "w:between", "w:bar")


def twips(cm):
    return str(round(cm * 1440 / 2.54))


def set_child(parent, tag, order, attrs=None):
    """Crea (sostituendo l'eventuale esistente) il figlio `tag` rispettando l'ordine dello schema."""
    for old in parent.findall(qn(tag)):
        parent.remove(old)
    el = OxmlElement(tag)
    for k, v in (attrs or {}).items():
        el.set(qn(k), str(v))
    later = {qn(t) for t in order[order.index(tag) + 1:]}
    for child in parent:
        if child.tag in later:
            child.addprevious(el)
            return el
    parent.append(el)
    return el


def set_fonts(rpr, name):
    rf = rpr.find(qn("w:rFonts"))
    if rf is None:
        rf = set_child(rpr, "w:rFonts", RPR_ORDER)
    for a in ("w:asciiTheme", "w:hAnsiTheme", "w:eastAsiaTheme", "w:cstheme"):
        rf.attrib.pop(qn(a), None)
    for a in ("w:ascii", "w:hAnsi", "w:eastAsia", "w:cs"):
        rf.set(qn(a), name)


def fmt_run(run, font=FONT, size=None, color=None, italic=False, caps=False, spacing=None, shade=None,
            underline=False):
    rpr = run._r.get_or_add_rPr()
    set_fonts(rpr, font)
    if italic:
        run.font.italic = True
    if caps:
        set_child(rpr, "w:caps", RPR_ORDER)
    if color:
        run.font.color.rgb = RGBColor.from_string(color)
    if spacing:
        set_child(rpr, "w:spacing", RPR_ORDER, {"w:val": round(spacing * 20)})
    if size:
        run.font.size = Pt(size)
    if underline:
        run.font.underline = True
    if shade:
        set_child(rpr, "w:shd", RPR_ORDER, {"w:val": "clear", "w:color": "auto", "w:fill": shade})
    return run


def fmt_para(p, before=None, after=None, line=None, exact=None, keep=None, align=None, left=None, right=None,
             page_break=None):
    pf = p.paragraph_format
    if before is not None:
        pf.space_before = Pt(before)
    if after is not None:
        pf.space_after = Pt(after)
    if exact is not None:
        pf.line_spacing = Pt(exact)
        pf.line_spacing_rule = WD_LINE_SPACING.EXACTLY
    elif line is not None:
        pf.line_spacing = line
    if keep is not None:
        pf.keep_with_next = keep
    if align is not None:
        pf.alignment = align
    if left is not None:
        pf.left_indent = Cm(left)
    if right is not None:
        pf.right_indent = Cm(right)
    if page_break is not None:
        pf.page_break_before = page_break
    return p


def para_border(ppr, side, sz, color, space=4):
    bdr = ppr.find(qn("w:pBdr"))
    if bdr is None:
        bdr = set_child(ppr, "w:pBdr", PPR_ORDER)
    set_child(bdr, f"w:{side}", BORDER_ORDER, {"w:val": "single", "w:sz": sz, "w:space": space, "w:color": color})


def add_field(p, instr, placeholder, **fmt):
    def fld(kind):
        run = fmt_run(p.add_run(), **fmt)
        el = OxmlElement("w:fldChar")
        el.set(qn("w:fldCharType"), kind)
        run._r.append(el)

    fld("begin")
    run = fmt_run(p.add_run(), **fmt)
    instr_el = OxmlElement("w:instrText")
    instr_el.set(qn("xml:space"), "preserve")
    instr_el.text = f" {instr} "
    run._r.append(instr_el)
    fld("separate")
    fmt_run(p.add_run(placeholder), **fmt)
    fld("end")


RT_HYPERLINK = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink"
INLINE = re.compile(r"\*\*(.+?)\*\*|`([^`]+)`|\[([^\]]+)\]\(([^)]+)\)")


def add_hyperlink(p, text, url, size=None):
    link = OxmlElement("w:hyperlink")
    link.set(qn("r:id"), p.part.relate_to(url, RT_HYPERLINK, is_external=True))
    r = OxmlElement("w:r")
    link.append(r)
    p._p.append(link)
    run = Run(r, p)
    run.text = text
    fmt_run(run, size=size, color=MAUVE_DEEP, underline=True)


def add_inline(p, text, size=None, color=None, font=FONT, strong_color=PLUM):
    pos = 0
    for m in INLINE.finditer(text):
        if m.start() > pos:
            fmt_run(p.add_run(text[pos:m.start()]), font=font, size=size, color=color)
        if m.group(1) is not None:
            fmt_run(p.add_run(m.group(1)), font=FONT_SB, size=size, color=strong_color or color)
        elif m.group(2) is not None:
            fmt_run(p.add_run(m.group(2)), font=MONO, size=(size or 10) - 1, color=PLUM, shade=MIST)
        else:
            add_hyperlink(p, m.group(3), m.group(4), size)
        pos = m.end()
    if pos < len(text):
        fmt_run(p.add_run(text[pos:]), font=font, size=size, color=color)


# ---------------------------------------------------------------------------------------------------------------
# Documento, stili e numerazioni

doc = Document()
st = doc.styles

sec = doc.sections[0]
sec.page_width, sec.page_height = Cm(PAGE_W), Cm(PAGE_H)
sec.left_margin = sec.right_margin = Cm(MARGIN_X)
sec.top_margin, sec.bottom_margin = Cm(MARGIN_TOP), Cm(MARGIN_BOTTOM)
sec.header_distance, sec.footer_distance = Cm(1.2), Cm(1.1)

defaults = st.element.find(qn("w:docDefaults"))
rpr_default = defaults.find(qn("w:rPrDefault")).find(qn("w:rPr"))
set_fonts(rpr_default, FONT)
lang = rpr_default.find(qn("w:lang"))
if lang is None:
    lang = set_child(rpr_default, "w:lang", RPR_ORDER)
lang.set(qn("w:val"), "it-IT")
lang.set(qn("w:eastAsia"), "it-IT")

normal = st["Normal"]
set_fonts(normal.element.get_or_add_rPr(), FONT)
normal.font.size = Pt(10)
normal.font.color.rgb = RGBColor.from_string(INK)
npf = normal.paragraph_format
npf.space_before, npf.space_after, npf.line_spacing = Pt(0), Pt(6), 1.1
npf.widow_control = True

for name, font, size, color, before, after in (
        ("Heading 1", FONT_SB, 18, PLUM, 26, 12),
        ("Heading 2", FONT_SB, 11.5, MAUVE_DEEP, 14, 4),
):
    hs = st[name]
    set_fonts(hs.element.get_or_add_rPr(), font)
    hs.font.size = Pt(size)
    hs.font.bold = False
    hs.font.italic = False
    hs.font.color.rgb = RGBColor.from_string(color)
    pf = hs.paragraph_format
    pf.space_before, pf.space_after, pf.line_spacing = Pt(before), Pt(after), 1.0
    pf.keep_with_next = True
    pf.keep_together = True

h1_ppr = st["Heading 1"].element.get_or_add_pPr()
para_border(h1_ppr, "bottom", 6, OUTLINE, 6)

for name in ("Header", "Footer"):
    hs = st[name]
    ppr = hs.element.get_or_add_pPr()
    for tabs in ppr.findall(qn("w:tabs")):
        ppr.remove(tabs)
    hs.paragraph_format.tab_stops.add_tab_stop(Cm(TEXT_W), WD_TAB_ALIGNMENT.RIGHT)
    hs.font.size = Pt(8)
    hs.font.color.rgb = RGBColor.from_string(GRAPHITE)

toc1 = st.add_style("toc 1", WD_STYLE_TYPE.PARAGRAPH, builtin=True)
toc1.element.set(qn("w:styleId"), "TOC1")
toc1.base_style = normal
toc1.next_paragraph_style = normal
toc1.font.size = Pt(10.5)
tpf = toc1.paragraph_format
tpf.space_before, tpf.space_after, tpf.line_spacing = Pt(5), Pt(5), 1.0
tpf.left_indent, tpf.first_line_indent = Cm(1.1), Cm(-1.1)
tpf.tab_stops.add_tab_stop(Cm(1.1), WD_TAB_ALIGNMENT.LEFT)
tpf.tab_stops.add_tab_stop(Cm(TEXT_W), WD_TAB_ALIGNMENT.RIGHT, WD_TAB_LEADER.DOTS)

numbering = doc.part.numbering_part.element


def add_abstract(xml):
    el = parse_xml(xml)
    nums = numbering.findall(qn("w:num"))
    if nums:
        nums[0].addprevious(el)
    else:
        numbering.append(el)


def add_num(abstract_id, restart=False):
    num = numbering.add_num(abstract_id)
    if restart:
        num.add_lvlOverride(ilvl=0).add_startOverride(1)
    return num.numId


add_abstract(
    f'<w:abstractNum {nsdecls("w")} w:abstractNumId="90"><w:multiLevelType w:val="singleLevel"/>'
    '<w:lvl w:ilvl="0"><w:start w:val="1"/><w:numFmt w:val="decimal"/><w:pStyle w:val="Heading1"/>'
    '<w:suff w:val="tab"/><w:lvlText w:val="%1"/><w:lvlJc w:val="left"/>'
    f'<w:pPr><w:tabs><w:tab w:val="num" w:pos="{twips(1.1)}"/></w:tabs><w:ind w:left="0" w:firstLine="0"/></w:pPr>'
    f'<w:rPr><w:rFonts w:ascii="{FONT_LIGHT}" w:hAnsi="{FONT_LIGHT}" w:cs="{FONT_LIGHT}"/>'
    f'<w:color w:val="{MAUVE_DEEP}"/></w:rPr></w:lvl></w:abstractNum>')
add_abstract(
    f'<w:abstractNum {nsdecls("w")} w:abstractNumId="91"><w:multiLevelType w:val="singleLevel"/>'
    '<w:lvl w:ilvl="0"><w:start w:val="1"/><w:numFmt w:val="bullet"/><w:lvlText w:val="▪"/>'
    f'<w:lvlJc w:val="left"/><w:pPr><w:ind w:left="{twips(0.65)}" w:hanging="{twips(0.45)}"/></w:pPr>'
    f'<w:rPr><w:rFonts w:ascii="{FONT}" w:hAnsi="{FONT}" w:cs="{FONT}"/><w:color w:val="{MAUVE_DEEP}"/>'
    '</w:rPr></w:lvl></w:abstractNum>')
add_abstract(
    f'<w:abstractNum {nsdecls("w")} w:abstractNumId="92"><w:multiLevelType w:val="singleLevel"/>'
    '<w:lvl w:ilvl="0"><w:start w:val="1"/><w:numFmt w:val="decimal"/><w:lvlText w:val="%1."/>'
    f'<w:lvlJc w:val="left"/><w:pPr><w:ind w:left="{twips(0.65)}" w:hanging="{twips(0.65)}"/></w:pPr>'
    f'<w:rPr><w:rFonts w:ascii="{FONT_SB}" w:hAnsi="{FONT_SB}" w:cs="{FONT_SB}"/><w:color w:val="{MAUVE_DEEP}"/>'
    '</w:rPr></w:lvl></w:abstractNum>')

chapter_num = add_num(90)
bullet_num = add_num(91)
h1_numpr = set_child(h1_ppr, "w:numPr", PPR_ORDER)
num_id_el = OxmlElement("w:numId")
num_id_el.set(qn("w:val"), str(chapter_num))
h1_numpr.append(num_id_el)


# ---------------------------------------------------------------------------------------------------------------
# Blocchi

def spacer(pt):
    return fmt_para(doc.add_paragraph(), before=0, after=0, exact=pt)


def new_table(rows, widths, borders=None, margins=(85, 115, 85, 115)):
    t = doc.add_table(rows=rows, cols=len(widths))
    t.alignment = WD_TABLE_ALIGNMENT.LEFT
    t.autofit = False
    tbl_pr = t._tbl.tblPr
    set_child(tbl_pr, "w:tblW", TBLPR_ORDER, {"w:w": twips(sum(widths)), "w:type": "dxa"})
    set_child(tbl_pr, "w:tblInd", TBLPR_ORDER, {"w:w": "0", "w:type": "dxa"})
    bdr = set_child(tbl_pr, "w:tblBorders", TBLPR_ORDER)
    for side in ("top", "left", "bottom", "right", "insideH", "insideV"):
        el = OxmlElement(f"w:{side}")
        spec = (borders or {}).get(side)
        if spec:
            el.set(qn("w:val"), "single")
            el.set(qn("w:sz"), str(spec[0]))
            el.set(qn("w:space"), "0")
            el.set(qn("w:color"), spec[1])
        else:
            el.set(qn("w:val"), "nil")
        bdr.append(el)
    set_child(tbl_pr, "w:tblLayout", TBLPR_ORDER, {"w:type": "fixed"})
    mar = set_child(tbl_pr, "w:tblCellMar", TBLPR_ORDER)
    for side, val in zip(("top", "left", "bottom", "right"), margins):
        el = OxmlElement(f"w:{side}")
        el.set(qn("w:w"), str(val))
        el.set(qn("w:type"), "dxa")
        mar.append(el)
    for i, w in enumerate(widths):
        t.columns[i].width = Cm(w)
    for row in t.rows:
        for i, cell in enumerate(row.cells):
            cell.width = Cm(widths[i])
    return t


def row_flags(row, header=False):
    tr_pr = row._tr.get_or_add_trPr()
    tr_pr.append(OxmlElement("w:cantSplit"))
    if header:
        tr_pr.append(OxmlElement("w:tblHeader"))


def cell_shade(cell, fill):
    set_child(cell._tc.get_or_add_tcPr(), "w:shd", TCPR_ORDER, {"w:val": "clear", "w:color": "auto", "w:fill": fill})


def cell_border(cell, side, sz, color):
    tc_pr = cell._tc.get_or_add_tcPr()
    bdr = tc_pr.find(qn("w:tcBorders"))
    if bdr is None:
        bdr = set_child(tc_pr, "w:tcBorders", TCPR_ORDER)
    set_child(bdr, f"w:{side}", BORDER_ORDER, {"w:val": "single", "w:sz": sz, "w:space": 0, "w:color": color})


WIDTHS = {2: (4.2, TEXT_W - 4.2), 3: (3.4, TEXT_W - 7.2, 3.8), 5: (3.6, 2.1, 3.3, TEXT_W - 11.4, 2.4)}


def data_table(rows):
    cols = len(rows[0])
    t = new_table(len(rows), WIDTHS[cols], borders={"bottom": (8, MAUVE), "insideH": (4, OUTLINE)})
    for r, cells in enumerate(rows):
        row_flags(t.rows[r], header=r == 0)
        for c in range(cols):
            cell = t.cell(r, c)
            p = fmt_para(cell.paragraphs[0], before=0, after=0, line=1.0)
            text = cells[c] if c < len(cells) else ""
            if r == 0:
                cell_shade(cell, PLUM)
                cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
                fmt_para(p, keep=True)
                fmt_run(p.add_run(text), font=FONT_SB, size=7.5, color=WHITE, caps=True, spacing=0.6)
            elif c == 0:
                add_inline(p, text, size=9, color=PLUM, font=FONT_SB)
            else:
                add_inline(p, text, size=9)
    spacer(10)


def callout(label, paras):
    kind = label.lower()
    if kind.startswith("attenzione"):
        fill, edge = DANGER_SOFT, DANGER
    elif kind.startswith("suggerimento"):
        fill, edge = MOSS_SOFT, MOSS
    else:
        fill, edge = MAUVE_SOFT, MAUVE_DEEP
    t = new_table(1, [TEXT_W], margins=(110, 230, 120, 200))
    row_flags(t.rows[0])
    cell = t.cell(0, 0)
    cell_shade(cell, fill)
    cell_border(cell, "left", 24, edge)
    p = fmt_para(cell.paragraphs[0], before=0, after=2, line=1.0)
    fmt_run(p.add_run(label), font=FONT_SB, size=7.5, color=edge, caps=True, spacing=1)
    for k, text in enumerate(paras):
        p = fmt_para(cell.add_paragraph(), before=0, after=0 if k == len(paras) - 1 else 4, line=1.05)
        add_inline(p, text, size=9.5)
    spacer(10)


def bullets(items):
    for k, text in enumerate(items):
        p = fmt_para(doc.add_paragraph(), after=8 if k == len(items) - 1 else 3)
        num_pr = p._p.get_or_add_pPr().get_or_add_numPr()
        num_pr.get_or_add_ilvl().val = 0
        num_pr.get_or_add_numId().val = bullet_num
        add_inline(p, text)


def numbered(items):
    num_id = add_num(92, restart=True)
    for k, text in enumerate(items):
        p = fmt_para(doc.add_paragraph(), after=8 if k == len(items) - 1 else 3)
        num_pr = p._p.get_or_add_pPr().get_or_add_numPr()
        num_pr.get_or_add_ilvl().val = 0
        num_pr.get_or_add_numId().val = num_id
        add_inline(p, text)


def process(items):
    """Passi in sequenza: riquadri numerati collegati da frecce."""
    n = len(items)
    arrow_w = 0.6
    step_w = (TEXT_W - arrow_w * (n - 1)) / n
    widths = []
    for k in range(n):
        widths.append(step_w)
        if k < n - 1:
            widths.append(arrow_w)
    t = new_table(1, widths, margins=(120, 140, 140, 140))
    row_flags(t.rows[0])
    for k, text in enumerate(items):
        cell = t.cell(0, 2 * k)
        cell_shade(cell, MAUVE_SOFT)
        cell_border(cell, "top", 18, MAUVE_DEEP)
        p = fmt_para(cell.paragraphs[0], before=0, after=2, exact=26)
        fmt_run(p.add_run(str(k + 1)), font=FONT_LIGHT, size=20, color=MAUVE_DEEP)
        p = fmt_para(cell.add_paragraph(), before=0, after=0, line=1.05)
        add_inline(p, text, size=9)
        if k < n - 1:
            arrow = t.cell(0, 2 * k + 1)
            arrow.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
            ap = fmt_para(arrow.paragraphs[0], before=0, after=0, line=1.0, align=WD_ALIGN_PARAGRAPH.CENTER)
            fmt_run(ap.add_run("›"), font=FONT, size=20, color=MAUVE_DEEP)
    spacer(10)


# ---------------------------------------------------------------------------------------------------------------
# Copertina

def add_cover_art(p, path):
    run = p.add_run()
    run.add_picture(path, width=Cm(PAGE_W), height=Cm(PAGE_H))
    drawing = run._r.find(qn("w:drawing"))
    inline = drawing[0]
    extent = inline.find(qn("wp:extent"))
    anchor = parse_xml(
        f'<wp:anchor {nsdecls("wp", "a", "pic", "r")} distT="0" distB="0" distL="0" distR="0" simplePos="0" '
        'relativeHeight="0" behindDoc="1" locked="1" layoutInCell="1" allowOverlap="1">'
        '<wp:simplePos x="0" y="0"/>'
        '<wp:positionH relativeFrom="page"><wp:posOffset>0</wp:posOffset></wp:positionH>'
        '<wp:positionV relativeFrom="page"><wp:posOffset>0</wp:posOffset></wp:positionV>'
        f'<wp:extent cx="{extent.get("cx")}" cy="{extent.get("cy")}"/>'
        '<wp:effectExtent l="0" t="0" r="0" b="0"/><wp:wrapNone/></wp:anchor>')
    anchor.append(inline.find(qn("wp:docPr")))
    anchor.append(OxmlElement("wp:cNvGraphicFramePr"))
    anchor.append(inline.find(qn("a:graphic")))
    drawing.replace(inline, anchor)


cursor = MARGIN_TOP  # posizione verticale corrente sulla copertina, in cm


def cover_para(top_cm, line_pt, lines=1, **kw):
    global cursor
    p = fmt_para(doc.add_paragraph(), before=(top_cm - cursor) * PT_PER_CM, after=0, exact=line_pt, **kw)
    cursor = top_cm + lines * line_pt / PT_PER_CM
    return p


art = fmt_para(doc.add_paragraph(), before=0, after=0, exact=1)
add_cover_art(art, COVER)
cursor += 1 / PT_PER_CM

p = cover_para(8.0, 14)
fmt_run(p.add_run("Manuale d'uso"), font=FONT_SB, size=10, color=MAUVE, caps=True, spacing=3)
p = cover_para(8.75, 46)
fmt_run(p.add_run("STRADE su Misura"), font=FONT_SB, size=36, color=WHITE)
p = cover_para(10.5, 24)
fmt_run(p.add_run(f"Versione per Android · {VERSIONE_APP}"), font=FONT_LIGHT, size=16, color=MAUVE_SOFT)

p = cover_para(15.3, 22, lines=3, right=3.2)
fmt_run(p.add_run("Guida all'installazione e all'uso dell'app per terapisti occupazionali: archivio delle "
                  "persone, strumenti di valutazione e report PDF."), font=FONT_LIGHT, size=14, color=PLUM)

rule = cover_para(24.4, 2, right=TEXT_W - 2.4)
para_border(rule._p.get_or_add_pPr(), "bottom", 12, MAUVE_DEEP, 1)
for k, (label, value) in enumerate((
        ("Edizione", EDIZIONE),
        ("App", f"STRADE su Misura {VERSIONE_APP} per Android 8.0 o successivo"),
        ("Autore", "Angelo Bottini"),
)):
    p = cover_para(24.85 + k * 0.55, 14)
    p.paragraph_format.tab_stops.add_tab_stop(Cm(2.2))
    fmt_run(p.add_run(label), font=FONT_SB, size=8.5, color=PLUM, caps=True, spacing=0.6)
    fmt_run(p.add_run("\t" + value), size=9, color=GRAPHITE)

# ---------------------------------------------------------------------------------------------------------------
# Indice e convenzioni

p = fmt_para(doc.add_paragraph(), before=0, after=14, line=1.0, page_break=True, keep=True)
fmt_run(p.add_run("Indice"), font=FONT_SB, size=18, color=PLUM)
para_border(p._p.get_or_add_pPr(), "bottom", 6, OUTLINE, 6)
p = doc.add_paragraph(style="toc 1")
add_field(p, 'TOC \\o "1-1" \\h \\z', "Aggiornare il sommario con Word (tasto F9).")

p = doc.add_paragraph(style="Heading 2")
fmt_para(p, before=34, after=8)
p.add_run("Come leggere questo manuale")
conv = [
    ("bold", "Salva valutazione", "In grassetto i nomi di pulsanti, sezioni e campi come compaiono sullo schermo."),
    ("text", "«Guide per strumento»",
     "Tra virgolette caporali i rimandi ad altre parti del manuale e i messaggi mostrati dall'app."),
    ("chip", "Nota", "Riquadro con informazioni utili e rimandi."),
    ("chip", "Attenzione", "Riquadro per le operazioni che cancellano dati in modo definitivo."),
]
t = new_table(len(conv), (4.2, TEXT_W - 4.2), borders={"insideH": (4, OUTLINE), "top": (4, OUTLINE),
                                                        "bottom": (4, OUTLINE)})
for r, (kind, sample, desc) in enumerate(conv):
    row_flags(t.rows[r])
    left, right = t.cell(r, 0), t.cell(r, 1)
    for c in (left, right):
        c.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
    lp = fmt_para(left.paragraphs[0], before=0, after=0, line=1.0)
    if kind == "chip":
        danger = sample == "Attenzione"
        cell_shade(left, DANGER_SOFT if danger else MAUVE_SOFT)
        cell_border(left, "left", 24, DANGER if danger else MAUVE_DEEP)
        fmt_run(lp.add_run(sample), font=FONT_SB, size=7.5, color=DANGER if danger else MAUVE_DEEP, caps=True,
                spacing=1)
    elif kind == "bold":
        fmt_run(lp.add_run(sample), font=FONT_SB, size=9.5, color=PLUM)
    else:
        fmt_run(lp.add_run(sample), size=9.5)
    add_inline(fmt_para(right.paragraphs[0], before=0, after=0, line=1.05), desc, size=9, color=GRAPHITE)

doc.add_section(WD_SECTION.NEW_PAGE)

# ---------------------------------------------------------------------------------------------------------------
# Corpo del manuale

lines = open(SRC, encoding="utf-8").read().splitlines()
i = 0
last_h3 = None
first_chapter = True
while i < len(lines):
    s = lines[i].strip()
    if not s or s.startswith("# ") or s.startswith("Versione del manuale:"):
        i += 1
        continue
    if s.startswith("## "):
        p = doc.add_paragraph(style="Heading 1")
        p.add_run(s[3:])
        if first_chapter:
            fmt_para(p, before=0)
            first_chapter = False
        last_h3 = None
        i += 1
        continue
    if s.startswith("### "):
        p = doc.add_paragraph(style="Heading 2")
        p.add_run(s[4:])
        last_h3 = s[4:]
        i += 1
        continue
    if s.startswith(">"):
        block = []
        while i < len(lines) and lines[i].strip().startswith(">"):
            block.append(lines[i].strip()[1:].strip())
            i += 1
        paras, cur = [], []
        for b in block + [""]:
            if b:
                cur.append(b)
            elif cur:
                paras.append(" ".join(cur))
                cur = []
        label = "Nota"
        m = re.fullmatch(r"\*\*(.+?)\*\*", paras[0])
        if m:
            label, paras = m.group(1), paras[1:]
        callout(label, paras)
        continue
    if s.startswith("|"):
        rows = []
        while i < len(lines) and lines[i].strip().startswith("|"):
            cells = [c.strip() for c in lines[i].strip().strip("|").split("|")]
            if not all(re.fullmatch(r":?-{3,}:?", c) for c in cells):
                rows.append(cells)
            i += 1
        data_table(rows)
        continue
    if re.match(r"^\d+\.\s", s):
        items = []
        while i < len(lines) and re.match(r"^\d+\.\s", lines[i].strip()):
            items.append(re.sub(r"^\d+\.\s", "", lines[i].strip()))
            i += 1
        if last_h3 == "Flusso di lavoro tipico":
            process(items)
        else:
            numbered(items)
        continue
    if s.startswith("- "):
        items = []
        while i < len(lines) and lines[i].strip().startswith("- "):
            items.append(lines[i].strip()[2:])
            i += 1
        bullets(items)
        continue
    buf = [s]
    i += 1
    while i < len(lines) and lines[i].strip() and not re.match(r"^(#|\||- |>|\d+\.\s)", lines[i].strip()):
        buf.append(lines[i].strip())
        i += 1
    text = " ".join(buf)
    p = doc.add_paragraph()
    if re.fullmatch(r"\*[^*].*[^*]\*", text):
        fmt_para(p, before=8)
        add_inline(p, text[1:-1], size=8.5, color=GRAPHITE, strong_color=GRAPHITE)
        for r in p.runs:
            r.font.italic = True
    else:
        add_inline(p, text)

# ---------------------------------------------------------------------------------------------------------------
# Intestazioni e piè di pagina

def build_header(header, right_text=None, chapter=False):
    header.is_linked_to_previous = False
    p = header.paragraphs[0]
    p.style = st["Header"]
    fmt_para(p, after=0)
    fmt_run(p.add_run("STRADE su Misura"), font=FONT_SB, size=8, color=MAUVE_DEEP)
    fmt_run(p.add_run("  ·  Manuale d'uso per Android"), size=8, color=GRAPHITE)
    fmt_run(p.add_run("\t"), size=8)
    if chapter:
        add_field(p, "STYLEREF 1 \\* MERGEFORMAT", "Capitolo", size=8, color=GRAPHITE)
    else:
        fmt_run(p.add_run(right_text), size=8, color=GRAPHITE)
    para_border(p._p.get_or_add_pPr(), "bottom", 4, OUTLINE, 5)


def build_footer(footer):
    footer.is_linked_to_previous = False
    p = footer.paragraphs[0]
    p.style = st["Footer"]
    fmt_para(p, after=0)
    fmt_run(p.add_run(f"Edizione del {EDIZIONE}"), size=7.5, color=GRAPHITE)
    fmt_run(p.add_run("\tPagina "), size=8, color=GRAPHITE)
    add_field(p, "PAGE", "1", font=FONT_SB, size=8, color=PLUM)
    fmt_run(p.add_run(" di "), size=8, color=GRAPHITE)
    add_field(p, "NUMPAGES", "1", size=8, color=GRAPHITE)


front, body = doc.sections[0], doc.sections[1]
front.different_first_page_header_footer = True
front.first_page_header.is_linked_to_previous = False
front.first_page_footer.is_linked_to_previous = False
build_header(front.header, right_text="Indice")
build_footer(front.footer)
body.different_first_page_header_footer = False
build_header(body.header, chapter=True)
build_footer(body.footer)

cp = doc.core_properties
cp.title = "STRADE su Misura – Manuale d'uso (Android)"
cp.subject = "Manuale d'uso della versione Android " + VERSIONE_APP
cp.author = "Angelo Bottini"
cp.keywords = "STRADE su Misura; terapia occupazionale; valutazione; Android; manuale"
cp.language = "it-IT"
doc.save(OUT)
print("ok", OUT)
