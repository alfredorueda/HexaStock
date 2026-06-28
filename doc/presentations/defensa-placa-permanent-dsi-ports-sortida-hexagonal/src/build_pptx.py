#!/usr/bin/env python3
from pathlib import Path

from PIL import Image
from pptx import Presentation
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_SHAPE
from pptx.enum.text import MSO_AUTO_SIZE, PP_ALIGN
from pptx.util import Inches, Pt


PRESENTATION_DIR = Path(__file__).resolve().parents[1]
ASSETS_DIR = PRESENTATION_DIR / "assets"
DIAGRAMS_DIR = PRESENTATION_DIR / "diagrams" / "rendered"
OUTPUT_DIR = PRESENTATION_DIR / "output"
OUTPUT_FILE = OUTPUT_DIR / "defensa-dsi-ports-sortida-hexagonal.pptx"
LOGO = ASSETS_DIR / "logotip-oficial-tecnocampus-upf-horitzontal-color.png"
HEXASTOCK_ARCHITECTURE_IMAGE = ASSETS_DIR / "hexastock-sellstocks-arquitectura-vpd.png"

TITLE_COLOR = RGBColor(25, 45, 62)
TEXT_COLOR = RGBColor(38, 38, 38)
MUTED = RGBColor(102, 112, 122)
BLUE = RGBColor(35, 96, 146)
LIGHT_BLUE = RGBColor(232, 241, 248)
LIGHT_GREEN = RGBColor(235, 246, 240)
LIGHT_RED = RGBColor(248, 234, 234)
LIGHT_GREY = RGBColor(247, 248, 250)
YELLOW = RGBColor(255, 243, 214)


def set_slide_size(prs: Presentation) -> None:
    prs.slide_width = Inches(13.333)
    prs.slide_height = Inches(7.5)


def blank_slide(prs: Presentation):
    return prs.slides.add_slide(prs.slide_layouts[6])


def add_text(slide, text, x, y, w, h, font_size=22, bold=False, color=TEXT_COLOR,
             align=PP_ALIGN.LEFT, font_name="Arial"):
    box = slide.shapes.add_textbox(x, y, w, h)
    frame = box.text_frame
    frame.clear()
    frame.word_wrap = True
    frame.auto_size = MSO_AUTO_SIZE.TEXT_TO_FIT_SHAPE
    p = frame.paragraphs[0]
    p.alignment = align
    run = p.add_run()
    run.text = text
    run.font.name = font_name
    run.font.size = Pt(font_size)
    run.font.bold = bold
    run.font.color.rgb = color
    return box


def add_bullets(slide, items, x, y, w, h, font_size=20, color=TEXT_COLOR):
    box = slide.shapes.add_textbox(x, y, w, h)
    frame = box.text_frame
    frame.clear()
    frame.word_wrap = True
    frame.auto_size = MSO_AUTO_SIZE.TEXT_TO_FIT_SHAPE
    for idx, item in enumerate(items):
        p = frame.paragraphs[0] if idx == 0 else frame.add_paragraph()
        p.text = item
        p.level = 0
        p.font.name = "Arial"
        p.font.size = Pt(font_size)
        p.font.color.rgb = color
        p.space_after = Pt(8)
    return box


def add_title(slide, title, subtitle=None):
    add_text(slide, title, Inches(0.65), Inches(0.35), Inches(9.8), Inches(0.55),
             font_size=28, bold=True, color=TITLE_COLOR)
    if subtitle:
        add_text(slide, subtitle, Inches(0.67), Inches(0.9), Inches(9.7), Inches(0.35),
                 font_size=13, color=MUTED)
    add_logo(slide, Inches(10.65), Inches(0.28), Inches(1.95), Inches(0.45))
    line = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.65), Inches(1.22), Inches(12), Inches(0.015))
    line.fill.solid()
    line.fill.fore_color.rgb = RGBColor(214, 219, 224)
    line.line.fill.background()


def add_footer(slide, number):
    add_text(slide, "Disseny de Sistemes d'Informació · Ports de sortida en arquitectura hexagonal",
             Inches(0.65), Inches(7.08), Inches(9.4), Inches(0.22), font_size=8, color=MUTED)
    add_text(slide, str(number), Inches(12.2), Inches(7.08), Inches(0.45), Inches(0.22),
             font_size=8, color=MUTED, align=PP_ALIGN.RIGHT)


def add_logo(slide, x, y, max_w, max_h):
    if not LOGO.exists():
        add_text(slide, "LOGOTIP OFICIAL TECNOCAMPUS / UPF", x, y, max_w, max_h,
                 font_size=8, color=MUTED, align=PP_ALIGN.RIGHT)
        return
    add_picture_fit(slide, LOGO, x, y, max_w, max_h)


def add_band(slide, x, y, w, h, fill, line=RGBColor(220, 224, 228)):
    shape = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, x, y, w, h)
    shape.fill.solid()
    shape.fill.fore_color.rgb = fill
    shape.line.color.rgb = line
    shape.line.width = Pt(0.75)
    return shape


def add_rect_band(slide, x, y, w, h, fill, line=RGBColor(220, 224, 228)):
    shape = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, x, y, w, h)
    shape.fill.solid()
    shape.fill.fore_color.rgb = fill
    shape.line.color.rgb = line
    shape.line.width = Pt(0.75)
    return shape


def add_picture_fit(slide, path, x, y, max_w, max_h):
    path = Path(path)
    if not path.exists():
        placeholder = add_band(slide, x, y, max_w, max_h, LIGHT_GREY)
        add_text(slide, f"Pendent: {path.name}", x + Inches(0.15), y + Inches(0.15),
                 max_w - Inches(0.3), max_h - Inches(0.3), font_size=14, color=MUTED,
                 align=PP_ALIGN.CENTER)
        return placeholder

    with Image.open(path) as img:
        iw, ih = img.size
    ratio = min(max_w / iw, max_h / ih)
    width = int(iw * ratio)
    height = int(ih * ratio)
    left = x + int((max_w - width) / 2)
    top = y + int((max_h - height) / 2)
    return slide.shapes.add_picture(str(path), left, top, width=width, height=height)


def add_code(slide, code, x, y, w, h):
    shape = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, x, y, w, h)
    shape.fill.solid()
    shape.fill.fore_color.rgb = RGBColor(31, 36, 41)
    shape.line.color.rgb = RGBColor(31, 36, 41)
    box = slide.shapes.add_textbox(x + Inches(0.22), y + Inches(0.18), w - Inches(0.44), h - Inches(0.36))
    frame = box.text_frame
    frame.clear()
    frame.word_wrap = True
    p = frame.paragraphs[0]
    run = p.add_run()
    run.text = code
    run.font.name = "Consolas"
    run.font.size = Pt(15)
    run.font.color.rgb = RGBColor(245, 246, 247)
    return shape


def create_deck():
    prs = Presentation()
    set_slide_size(prs)

    # 1
    slide = blank_slide(prs)
    add_logo(slide, Inches(9.2), Inches(0.45), Inches(3.25), Inches(0.8))
    add_text(slide,
             "Disseny de ports de sortida en arquitectura hexagonal",
             Inches(0.72), Inches(1.35), Inches(10.8), Inches(0.9),
             font_size=32, bold=True, color=TITLE_COLOR)
    add_text(slide,
             "desacoblament entre casos d'ús, domini i APIs externes",
             Inches(0.75), Inches(2.18), Inches(10.2), Inches(0.45),
             font_size=21, color=BLUE)
    add_text(slide,
             "Una microlliçó de Disseny de Sistemes d'Informació a partir d'un cas real de consultoria i del projecte HexaStock",
             Inches(0.75), Inches(3.05), Inches(10.5), Inches(0.8),
             font_size=18, color=TEXT_COLOR)
    add_text(slide,
             "Alfredo Rueda Unsain\nDefensa de plaça de professorat permanent\nTecnoCampus · classe magistral reduïda, 20 minuts",
             Inches(0.75), Inches(5.1), Inches(7.5), Inches(0.85),
             font_size=15, color=MUTED)
    add_footer(slide, 1)

    # 2
    slide = blank_slide(prs)
    add_title(slide, "On som dins l'assignatura", "Disseny de Sistemes d'Informació")
    add_text(slide,
             "Guia docent oficial 2025/26: assignatura 103322, tercer curs, segon trimestre, 6 ECTS. Professorat: Josep Roure Alcobé i Alfredo Rueda Unsain.",
             Inches(0.8), Inches(1.48), Inches(11.6), Inches(0.48), font_size=16, bold=True, color=TITLE_COLOR)
    add_text(slide,
             "No explicarem tota l'arquitectura hexagonal. Ens centrarem en una decisió concreta: consumir APIs externes sense acoblar casos d'ús i domini a la infraestructura.",
             Inches(0.8), Inches(2.02), Inches(11.6), Inches(0.68), font_size=18)
    sequence = [
        "1. Arquitectura per capes i limitacions",
        "2. Introducció a Clean / Hexagonal Architecture",
        "3. Ports i adaptadors",
        "4. Ports de sortida per a integracions externes",
        "5. Testing, mocks i substitució d'infraestructura",
        "6. Microserveis i DDD",
    ]
    for i, item in enumerate(sequence):
        y = Inches(2.95 + i * 0.5)
        fill = LIGHT_BLUE if i == 3 else LIGHT_GREY
        add_band(slide, Inches(1.05), y, Inches(10.8), Inches(0.38), fill)
        add_text(slide, item, Inches(1.28), y + Inches(0.055), Inches(10.1), Inches(0.24),
                 font_size=14, bold=(i == 3), color=TITLE_COLOR if i == 3 else TEXT_COLOR)
    add_text(slide,
             "Blocs oficials del temari: clean/hexagonal, ports i adaptadors, mapping, mòduls, microserveis i DDD.",
             Inches(1.05), Inches(6.25), Inches(10.8), Inches(0.32), font_size=12, color=MUTED,
             align=PP_ALIGN.CENTER)
    add_footer(slide, 2)

    # 3
    slide = blank_slide(prs)
    add_title(slide, "Objectiu d'aprenentatge")
    add_text(slide,
             "Dissenyar un port de sortida orientat al llenguatge de l'aplicació, implementar adaptadors intercanviables i justificar la reducció d'acoblament.",
             Inches(0.8), Inches(1.65), Inches(11.3), Inches(0.8), font_size=23, color=TITLE_COLOR)
    objectives = [
        "Identificar el risc d'acoblament amb APIs externes.",
        "Definir el port en termes de capacitat requerida.",
        "Substituir l'adaptador sense modificar domini ni cas d'ús.",
    ]
    for i, item in enumerate(objectives):
        x = Inches(0.9 + i * 4.08)
        add_band(slide, x, Inches(3.0), Inches(3.55), Inches(1.7), [LIGHT_RED, YELLOW, LIGHT_GREEN][i])
        add_text(slide, item, x + Inches(0.22), Inches(3.28), Inches(3.1), Inches(1.05),
                 font_size=18, bold=True, color=TITLE_COLOR, align=PP_ALIGN.CENTER)
    add_footer(slide, 3)

    # 4
    slide = blank_slide(prs)
    add_title(slide, "El problema en sistemes reals", "AGAUR, processos corporatius i informació externa")
    add_picture_fit(slide, DIAGRAMS_DIR / "agaur-before-coupled.png",
                    Inches(0.55), Inches(1.35), Inches(12.25), Inches(4.0))
    add_text(slide,
             "Lectura funcional: una sol·licitud de beca genera un expedient, s'avaluen requisits i es prepara una proposta de resolució. El risc arquitectònic apareix quan aquesta avaluació incorpora directament SOAP/XML, DTOs, mapping i detalls de PICA.",
             Inches(0.85), Inches(5.55), Inches(7.65), Inches(0.9), font_size=14, color=TITLE_COLOR)
    add_band(slide, Inches(8.9), Inches(5.55), Inches(3.6), Inches(0.9), LIGHT_GREY)
    add_text(slide,
             "Evidència documental\nInforme AGAUR: formació i consultoria especialitzada, 60 h, maig-juliol 2025, serveis REST i arquitectura hexagonal.",
             Inches(9.1), Inches(5.68), Inches(3.2), Inches(0.58), font_size=9, color=MUTED,
             align=PP_ALIGN.CENTER)
    add_footer(slide, 4)

    # 5
    slide = blank_slide(prs)
    add_title(slide, "Quan l'acoblament es fa risc d'evolució",
              "Lectura institucional prudent: PICA, web services i orientació CTTI cap a APIs")
    add_text(slide,
             "La qüestió no és si SOAP desapareix: és que cap tecnologia d'integració ha d'arrossegar el procediment.",
             Inches(0.9), Inches(1.42), Inches(11.4), Inches(0.35), font_size=16, bold=True, color=TITLE_COLOR,
             align=PP_ALIGN.CENTER)

    add_rect_band(slide, Inches(0.85), Inches(1.95), Inches(3.55), Inches(3.25), LIGHT_RED)
    add_text(slide, "Situació verificable",
             Inches(1.1), Inches(2.2), Inches(3.05), Inches(0.32), font_size=18, bold=True, color=TITLE_COLOR,
             align=PP_ALIGN.CENTER)
    add_text(slide,
             "PICA continua essent una plataforma corporativa d'interoperabilitat.",
             Inches(1.12), Inches(2.72), Inches(3.05), Inches(0.7), font_size=16, bold=True, color=TITLE_COLOR,
             align=PP_ALIGN.CENTER)
    add_text(slide,
             "La documentació pública mostra integració amb web services i antecedents SOAP.",
             Inches(1.12), Inches(3.68), Inches(3.05), Inches(0.7), font_size=14, color=TEXT_COLOR,
             align=PP_ALIGN.CENTER)
    add_text(slide,
             "PICA / web services",
             Inches(1.12), Inches(4.5), Inches(3.05), Inches(0.35), font_size=17, bold=True, color=BLUE,
             align=PP_ALIGN.CENTER)

    add_rect_band(slide, Inches(4.85), Inches(1.95), Inches(3.55), Inches(3.25), YELLOW)
    add_text(slide, "Risc arquitectònic",
             Inches(5.1), Inches(2.2), Inches(3.05), Inches(0.32), font_size=18, bold=True, color=TITLE_COLOR,
             align=PP_ALIGN.CENTER)
    add_bullets(slide, [
        "El flux de l'expedient coneix PICA, SOAP/XML, DTOs, mapping i errors tècnics.",
        "La lògica administrativa depèn d'una tecnologia d'integració concreta.",
        "Qualsevol evolució cap a REST, API Manager o EventHub impacta massa capes.",
    ], Inches(5.12), Inches(2.75), Inches(3.05), Inches(1.9), font_size=12)

    add_rect_band(slide, Inches(8.85), Inches(1.95), Inches(3.55), Inches(3.25), LIGHT_BLUE)
    add_text(slide, "Criteri docent",
             Inches(9.1), Inches(2.2), Inches(3.05), Inches(0.32), font_size=18, bold=True, color=TITLE_COLOR,
             align=PP_ALIGN.CENTER)
    add_text(slide,
             "Separar criteris del procediment, casos d'ús i infraestructura.",
             Inches(9.12), Inches(2.78), Inches(3.05), Inches(0.75), font_size=16, bold=True, color=TITLE_COLOR,
             align=PP_ALIGN.CENTER)
    add_text(slide,
             "Port de sortida\n+\nadaptadors substituïbles",
             Inches(9.12), Inches(3.8), Inches(3.05), Inches(0.85), font_size=18, bold=True, color=BLUE,
             align=PP_ALIGN.CENTER)

    add_text(slide,
             "El motiu arquitectònic no és només \"fer REST\": és evitar que l'evolució tecnològica arrossegui els casos d'ús.",
             Inches(1.15), Inches(5.55), Inches(7.6), Inches(0.62), font_size=17, bold=True, color=TITLE_COLOR)
    add_text(slide,
             "Evidència: l'informe AGAUR acredita refactorització SOAP cap a REST i reducció d'acoblament.",
             Inches(9.0), Inches(5.58), Inches(3.25), Inches(0.55), font_size=10, color=MUTED,
             align=PP_ALIGN.CENTER)
    add_footer(slide, 5)

    # 6
    slide = blank_slide(prs)
    add_title(slide, "Solució AGAUR: port de sortida i adaptador")
    add_picture_fit(slide, DIAGRAMS_DIR / "agaur-after-hexagonal.png",
                    Inches(0.75), Inches(1.45), Inches(8.0), Inches(5.3))
    add_text(slide,
             "El port defineix què necessita l'aplicació.\n\nL'adaptador resol com obtenir-ho amb una tecnologia concreta.",
             Inches(9.0), Inches(2.0), Inches(3.25), Inches(2.1), font_size=21, bold=True, color=TITLE_COLOR)
    add_text(slide,
             "El cas d'ús necessita una capacitat externa, no una tecnologia externa concreta.",
             Inches(9.0), Inches(4.55), Inches(3.25), Inches(0.9), font_size=17, color=BLUE)
    add_footer(slide, 6)

    # 7
    slide = blank_slide(prs)
    add_title(slide, "De la interface al port", "Una frontera arquitectònica, no només un contracte Java")
    add_picture_fit(slide, DIAGRAMS_DIR / "output-port-pattern.png",
                    Inches(0.75), Inches(1.45), Inches(7.4), Inches(5.25))
    add_band(slide, Inches(8.45), Inches(1.75), Inches(3.65), Inches(3.8), LIGHT_GREY)
    add_text(slide,
             "Una interface pot desacoblar dues classes.\n\nUn port de sortida defineix una frontera arquitectònica.",
             Inches(8.75), Inches(2.2), Inches(3.05), Inches(2.4), font_size=22, bold=True, color=TITLE_COLOR,
             align=PP_ALIGN.CENTER)
    add_footer(slide, 7)

    # 8
    slide = blank_slide(prs)
    add_title(slide, "Transferència a HexaStock", "Venda d'accions amb proveïdor de preus substituïble")
    add_picture_fit(slide, HEXASTOCK_ARCHITECTURE_IMAGE,
                    Inches(0.55), Inches(1.35), Inches(8.55), Inches(5.6))
    add_text(slide,
             "El cas d'ús necessita el preu actual d'un ticker, però no ha de dependre de Finnhub, Alpha Vantage, REST, JSON, tokens, endpoints ni clients HTTP.",
             Inches(9.35), Inches(1.75), Inches(3.0), Inches(2.2), font_size=17, bold=True, color=TITLE_COLOR)
    add_bullets(slide, [
        "Port: StockPriceProviderPort",
        "Adaptadors: Finnhub, Alpha Vantage, mock",
        "Domini: Portfolio, Holding, Price, Ticker, ShareQuantity",
    ], Inches(9.35), Inches(4.3), Inches(3.0), Inches(1.55), font_size=13)
    add_text(slide,
             "Diagrama del tutorial HexaStock Sell Stocks.",
             Inches(9.35), Inches(6.2), Inches(3.0), Inches(0.25), font_size=10, color=MUTED,
             align=PP_ALIGN.CENTER)
    add_footer(slide, 8)

    # 9
    slide = blank_slide(prs)
    add_title(slide, "Flux i codi essencial", "El servei coordina. El domini decideix. La infraestructura adapta.")
    add_picture_fit(slide, DIAGRAMS_DIR / "sell-stock-sequence.png",
                    Inches(0.55), Inches(1.35), Inches(8.0), Inches(4.95))
    code = (
        "StockPrice stockPrice =\n"
        "    stockPriceProviderPort.fetchStockPrice(ticker);\n\n"
        "Price price = stockPrice.price();\n\n"
        "SellResult sellResult =\n"
        "    portfolio.sell(ticker, quantity, price);"
    )
    add_code(slide, code, Inches(8.85), Inches(1.8), Inches(3.85), Inches(2.65))
    add_text(slide,
             "El servei obté informació externa a través d'un port i delega la decisió al domini ric.",
             Inches(8.9), Inches(4.85), Inches(3.75), Inches(0.9), font_size=17, color=TITLE_COLOR)
    add_footer(slide, 9)

    # 10
    slide = blank_slide(prs)
    add_title(slide, "Conclusió", "L'arquitectura no elimina el canvi. El localitza.")
    add_picture_fit(slide, DIAGRAMS_DIR / "before-after-comparison.png",
                    Inches(0.75), Inches(1.45), Inches(7.75), Inches(5.05))
    add_text(slide,
             "Quan el proveïdor canvia, volem canviar l'adaptador, no el cas d'ús ni el model de domini.",
             Inches(8.95), Inches(2.05), Inches(3.25), Inches(1.9), font_size=24, bold=True, color=TITLE_COLOR,
             align=PP_ALIGN.CENTER)
    add_text(slide,
             "L'arquitectura fa explícita, substituïble i localitzada la dependència del món exterior.",
             Inches(8.95), Inches(4.55), Inches(3.25), Inches(0.95), font_size=17, color=BLUE,
             align=PP_ALIGN.CENTER)
    add_footer(slide, 10)

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    prs.save(OUTPUT_FILE)
    print(f"PowerPoint generat: {OUTPUT_FILE}")


if __name__ == "__main__":
    create_deck()
