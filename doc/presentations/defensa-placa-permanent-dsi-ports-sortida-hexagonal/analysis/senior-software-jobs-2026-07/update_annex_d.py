#!/usr/bin/env python3
"""Replace only the text nodes of slide 10 (Annex D) in the canonical PPTX."""

from __future__ import annotations

import os
import sys
import tempfile
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET


NS = {
    "a": "http://schemas.openxmlformats.org/drawingml/2006/main",
    "p": "http://schemas.openxmlformats.org/presentationml/2006/main",
}

REPLACEMENTS = {
    "Annex D. Demanda professional de competències comunicatives": [
        "Annex D. Demanda professional de competències socioemocionals"
    ],
    "Un cas actual d'Enginyeria del Software sènior en un entorn d'IA": [
        "Mostra internacional actual · resultats amb controls de concentració per empresa"
    ],
    "14 BLOCS COMPETENCIALS EXPLÍCITS": [
        "222 OFERTES · 102 EMPRESES · 6 FONTS"
    ],
    "Principalment\ntècniques\n28,6 %": [
        "≥1 competència", "84,7 %", "188 de 222"
    ],
    "Híbrides\n28,6 %": [
        "≥2 competències", "60,8 %"
    ],
    "Principalment\nsocioemocionals\n42,9 %": [
        "PES IGUAL", "PER EMPRESA", "79,1 %"
    ],
    "Component socioemocional estimat: 57,1 %": [
        "La hipòtesi es confirma en la mostra observada"
    ],
    "(6 + 4 × 0,5) / 14 × 100 = 57,1 %\nproporció de blocs amb component comunicatiu, relacional o de lideratge": [
        "IC95%: ≥1 79,4–88,8 · ≥2 54,3–67,0",
        "Pes igual per empresa: IC95% bootstrap 71,1–86,4",
    ],
    "Què demana el perfil sènior": [
        "Què apareix explícitament"
    ],
    "comunicació tècnica i de negoci\nalineament entre stakeholders\nfeedback · mentoria · coaching\ncol·laboració i treball multicultural\nlideratge i capacitat d'influència\nadaptabilitat i aprenentatge continu": [
        "col·laboració 64,0 %",
        "lideratge / influència 39,2 %",
        "feedback / revisió 36,9 %",
        "comunicació explícita 30,2 %",
        "mentoria / coaching 28,8 %",
        "aprenentatge / adaptabilitat 15,8 %",
    ],
    "Base imprescindible: Java · arquitectura · microserveis · cloud · dades · qualitat · IA": [
        "Lead · Staff · Principal: 93,5 % amb ≥1 competència"
    ],
    "En un entorn assistit per intel·ligència artificial, augmenta el valor diferencial de comunicar, col·laborar, decidir, influir i acompanyar altres professionals.": [
        "La base tècnica és imprescindible; l'excel·lència sènior també exigeix coordinar, decidir, comunicar i acompanyar."
    ],
    "La competència tècnica continua sent imprescindible, però ja no és suficient per definir l'excel·lència professional en Enginyeria del Software.": [
        "Resultat robust: amb màxim 3 ofertes per empresa, 83,3 % en demanen ≥1 i 70,3 % en demanen ≥2."
    ],
    "La formació universitària ha de preparar també per comunicar, col·laborar, donar feedback, gestionar desacords i treballar amb stakeholders.": [
        "Criteri conservador · deduplicació · auditoria manual de 60 ofertes · feedback/revisió exclòs de l'indicador principal"
    ],
    "Anàlisi pròpia d'una oferta actual per a un perfil sènior d'Enginyeria del Software. Cas il·lustratiu; no és una estimació estadística del conjunt del mercat laboral.": [
        "Estudi observacional multifuente, 17/04–16/07/2026. Fonts: The Muse, Himalayas, Arbeitnow, Jobicy, Remotive i Remote OK. No és una mostra probabilística de tot el mercat."
    ],
}


def shape_text(shape: ET.Element) -> str:
    paragraphs = []
    for paragraph in shape.findall(".//a:p", NS):
        paragraphs.append("".join(node.text or "" for node in paragraph.findall(".//a:t", NS)))
    return "\n".join(paragraphs)


def update_slide(xml: bytes) -> bytes:
    ET.register_namespace("a", NS["a"])
    ET.register_namespace("p", NS["p"])
    root = ET.fromstring(xml)
    found = set()
    for shape in root.findall(".//p:sp", NS):
        original = shape_text(shape)
        replacement = REPLACEMENTS.get(original)
        if replacement is None:
            continue
        text_nodes = shape.findall(".//a:t", NS)
        if len(text_nodes) != len(replacement):
            raise RuntimeError(
                f"Unexpected text-node count for {original!r}: {len(text_nodes)} != {len(replacement)}"
            )
        for node, value in zip(text_nodes, replacement):
            node.text = value
        found.add(original)
    missing = set(REPLACEMENTS) - found
    if missing:
        raise RuntimeError("Expected slide text not found: " + "; ".join(sorted(missing)))
    return ET.tostring(root, encoding="utf-8", xml_declaration=True)


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit(f"Usage: {Path(sys.argv[0]).name} canonical.pptx")
    pptx = Path(sys.argv[1]).resolve()
    with zipfile.ZipFile(pptx, "r") as source:
        slide_xml = update_slide(source.read("ppt/slides/slide10.xml"))
        fd, temporary_name = tempfile.mkstemp(prefix=pptx.stem + "-", suffix=".pptx", dir=pptx.parent)
        os.close(fd)
        temporary = Path(temporary_name)
        try:
            with zipfile.ZipFile(temporary, "w") as target:
                for info in source.infolist():
                    data = slide_xml if info.filename == "ppt/slides/slide10.xml" else source.read(info.filename)
                    target.writestr(info, data)
            os.replace(temporary, pptx)
        finally:
            if temporary.exists():
                temporary.unlink()


if __name__ == "__main__":
    main()
