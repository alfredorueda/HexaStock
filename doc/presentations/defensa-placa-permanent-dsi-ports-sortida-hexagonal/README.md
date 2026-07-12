# Defensa DSI: ports de sortida en arquitectura hexagonal

Carpeta de treball per a una classe magistral reduïda de 20 minuts dins l'assignatura Disseny de Sistemes d'Informació.

Títol:
**Disseny de ports de sortida en arquitectura hexagonal: desacoblament entre casos d'ús, domini i serveis externs**

Subtítol:
**Una microlliçó de Disseny de Sistemes d'Informació a partir d'un cas real de consultoria i del projecte HexaStock**

## Estat del material

- Versió actual del PowerPoint: 12 diapositives simplificades, pensades com a suport visual per a una explicació oral de 20 minuts.
- `outline.md`: estructura narrativa de 20 minuts.
- `slide-by-slide.md`: contingut, visual i notes del presentador per diapositiva.
- `output/IMPORTANTE_MANUAL_defensa-dsi-ports-sortida-hexagonal.pptx`: presentació canònica.
- `output/defensa-dsi-ports-sortida-hexagonal_Alfredo_Rueda.pdf`: export PDF de la presentació canònica.
- `output/materials-orals-canonics-20-minuts.md`: guió oral, notes del presentador, esquema de memorització i pla d'entrenament.
- `output/materials-orals-canonics-20-minuts.docx`: exportació editable del material oral canònic.
- `output/REMARKABLE_materials-orals-canonics-20-minuts.docx`: exportació editable amb mida gran per preparar la versió reMarkable.
- `output/REMARKABLE_materials-orals-canonics-20-minuts.pdf`: versió de lectura amb mida gran per a reMarkable.
- `cas-funcional-agaur-beca.md`: dossier de suport sobre el cas funcional AGAUR, beca, renda, patrimoni, valors cadastrals, PICA i fonts públiques.
- `checklist.md`: verificació final docent, tècnica i institucional.
- `sources-and-evidence.md`: fonts, evidències i punts pendents de verificar.
- `diagrams/*.puml`: diagrames PlantUML.
- `diagrams/rendered/*`: diagrames renderitzats en SVG i PNG.
- `src/render_diagrams.sh`: renderitzat amb Docker i PlantUML.
- `assets/evidencies/informe-acreditatiu-agaur-arquitectura-hexagonal.pdf`: informe AGAUR incorporat com a evidència documental.
- `assets/evidencies/pla-docent-dsi-103322-2025-26.pdf`: guia docent oficial de Disseny de Sistemes d'Informació 2025/26.
- `assets/logotip-oficial-tecnocampus-upf-horitzontal-color.png`: logotip descarregat de la pàgina oficial d'imatge corporativa del TecnoCampus.
- `assets/hexastock-hexagonal-code-dependencies.png`: diagrama de dependències de codi del tutorial HexaStock Sell Stocks incorporat com a visual principal de la diapositiva HexaStock.

## Com regenerar els diagrames

Des de l'arrel del repositori:

```bash
doc/presentations/defensa-placa-permanent-dsi-ports-sortida-hexagonal/src/render_diagrams.sh
```

L'script utilitza Docker amb la imatge `plantuml/plantuml:latest` i genera SVG i PNG.

## Com regenerar els materials derivats

Des de l'arrel del repositori:

```bash
doc/presentations/defensa-placa-permanent-dsi-ports-sortida-hexagonal/src/generate_derived_materials.sh
```

Aquest script regenera els DOCX i PDF derivats del guió oral canònic, exporta el PDF de la presentació canònica i aplica una mida mínima de 18 pt al DOCX per a reMarkable abans de convertir-lo a PDF.

Si cal augmentar temporalment la mida mínima del reMarkable:

```bash
REMARKABLE_MIN_FONT_PT=20 doc/presentations/defensa-placa-permanent-dsi-ports-sortida-hexagonal/src/generate_derived_materials.sh
```

Requereix `pandoc`, `python3` i LibreOffice (`soffice`).

## Criteri d'edició del PowerPoint

La presentació canònica s'edita directament sobre el fitxer PowerPoint:

```text
output/IMPORTANTE_MANUAL_defensa-dsi-ports-sortida-hexagonal.pptx
```

El generador Python antic s'ha eliminat perquè havia quedat descoordinat respecte de la versió manualment curada de la presentació. En aquesta fase final, els canvis han de ser petits i s'han de fer directament sobre el PowerPoint canònic, amb l'export PDF actualitzat quan calgui.

## Criteri docent

La sessió no intenta explicar tota l'arquitectura hexagonal. Se centra en una decisió concreta i transferible: dissenyar un port de sortida quan un cas d'ús necessita informació d'un sistema extern.

La versió actual redueix deliberadament el text visible. El rigor s'ha de sostenir amb el guió oral, els diagrames i una demo controlada amb HexaStock.

Idea força:

> Quan canvia el proveïdor extern, volem canviar l'adaptador, no el cas d'ús ni el model de domini.
