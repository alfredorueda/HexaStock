# Defensa DSI: ports de sortida en arquitectura hexagonal

Carpeta de treball per a una classe magistral reduïda de 20 minuts dins l'assignatura Disseny de Sistemes d'Informació.

Títol:
**Disseny de ports de sortida en arquitectura hexagonal: desacoblament entre casos d'ús, domini i APIs externes**

Subtítol:
**Una microlliçó de Disseny de Sistemes d'Informació a partir d'un cas real de consultoria i del projecte HexaStock**

## Estat del material

- `outline.md`: estructura narrativa de 20 minuts.
- `slide-by-slide.md`: contingut, visual i notes del presentador per diapositiva.
- `guio-oral-20-minuts.md`: guió oral amb temps aproximat.
- `checklist.md`: verificació final docent, tècnica i institucional.
- `sources-and-evidence.md`: fonts, evidències i punts pendents de verificar.
- `diagrams/*.puml`: diagrames PlantUML.
- `diagrams/rendered/*`: diagrames renderitzats en SVG i PNG.
- `src/render_diagrams.sh`: renderitzat amb Docker i PlantUML.
- `src/build_pptx.py`: generació d'una primera plantilla PowerPoint editable.
- `assets/evidencies/informe-acreditatiu-agaur-arquitectura-hexagonal.pdf`: informe AGAUR incorporat com a evidència documental.
- `assets/evidencies/pla-docent-dsi-103322-2025-26.pdf`: guia docent oficial de Disseny de Sistemes d'Informació 2025/26.
- `assets/logotip-oficial-tecnocampus-upf-horitzontal-color.png`: logotip descarregat de la pàgina oficial d'imatge corporativa del TecnoCampus.
- `assets/hexastock-sellstocks-arquitectura-vpd.png`: diagrama del tutorial HexaStock Sell Stocks incorporat com a visual principal de la diapositiva HexaStock.

## Com regenerar els diagrames

Des de l'arrel del repositori:

```bash
doc/presentations/defensa-placa-permanent-dsi-ports-sortida-hexagonal/src/render_diagrams.sh
```

L'script utilitza Docker amb la imatge `plantuml/plantuml:latest` i genera SVG i PNG.

## Com generar el PowerPoint

El generador utilitza `python-pptx`. Si no està instal·lat al sistema, es pot instal·lar en una carpeta temporal:

```bash
python3 -m pip install --target /tmp/python-pptx-deps python-pptx
PYTHONPATH=/tmp/python-pptx-deps python3 doc/presentations/defensa-placa-permanent-dsi-ports-sortida-hexagonal/src/build_pptx.py
```

Sortida prevista:

```text
doc/presentations/defensa-placa-permanent-dsi-ports-sortida-hexagonal/output/defensa-dsi-ports-sortida-hexagonal.pptx
```

## Criteri docent

La sessió no intenta explicar tota l'arquitectura hexagonal. Se centra en una decisió concreta i transferible: dissenyar un port de sortida quan un cas d'ús necessita informació d'un sistema extern.

Idea força:

> Quan canvia el proveïdor extern, volem canviar l'adaptador, no el cas d'ús ni el model de domini.
