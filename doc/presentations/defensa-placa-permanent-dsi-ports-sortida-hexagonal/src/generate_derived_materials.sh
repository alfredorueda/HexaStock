#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PRESENTATION_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
OUTPUT_DIR="${PRESENTATION_DIR}/output"

PPTX="${OUTPUT_DIR}/IMPORTANTE_MANUAL_defensa-dsi-ports-sortida-hexagonal.pptx"
PRESENTATION_PDF="${OUTPUT_DIR}/defensa-dsi-ports-sortida-hexagonal_Alfredo_Rueda.pdf"

ORAL_MD="${OUTPUT_DIR}/materials-orals-canonics-20-minuts.md"
ORAL_DOCX="${OUTPUT_DIR}/materials-orals-canonics-20-minuts.docx"
ORAL_PDF="${OUTPUT_DIR}/guio-oral-complet-imprimible-professor.pdf"

REMARKABLE_DOCX="${OUTPUT_DIR}/REMARKABLE_materials-orals-canonics-20-minuts.docx"
REMARKABLE_PDF="${OUTPUT_DIR}/REMARKABLE_materials-orals-canonics-20-minuts.pdf"
REMARKABLE_MIN_FONT_PT="${REMARKABLE_MIN_FONT_PT:-18}"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Falta la comanda requerida: $1" >&2
    exit 1
  fi
}

require_file() {
  if [[ ! -f "$1" ]]; then
    echo "No existeix el fitxer requerit: $1" >&2
    exit 1
  fi
}

convert_to_pdf() {
  local input_file="$1"
  local output_file="$2"
  local generated_pdf

  soffice --headless --convert-to pdf --outdir "${CONVERT_DIR}" "${input_file}" >/dev/null

  generated_pdf="${CONVERT_DIR}/$(basename "${input_file}")"
  generated_pdf="${generated_pdf%.*}.pdf"

  if [[ ! -f "${generated_pdf}" ]]; then
    echo "LibreOffice no ha generat el PDF esperat: ${generated_pdf}" >&2
    exit 1
  fi

  mv "${generated_pdf}" "${output_file}"
}

require_command pandoc
require_command python3
require_command soffice

require_file "${PPTX}"
require_file "${ORAL_MD}"
require_file "${ORAL_DOCX}"
require_file "${REMARKABLE_DOCX}"

if ! python3 - "${REMARKABLE_MIN_FONT_PT}" <<'PY'
import sys

try:
    value = float(sys.argv[1])
except ValueError:
    sys.exit(1)

if value <= 0:
    sys.exit(1)
PY
then
  echo "REMARKABLE_MIN_FONT_PT ha de ser un número positiu." >&2
  exit 1
fi

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT
CONVERT_DIR="${TMP_DIR}/convert"
mkdir -p "${CONVERT_DIR}"

NORMAL_REFERENCE_DOCX="${TMP_DIR}/reference-normal.docx"
REMARKABLE_REFERENCE_DOCX="${TMP_DIR}/reference-remarkable.docx"

cp "${ORAL_DOCX}" "${NORMAL_REFERENCE_DOCX}"
cp "${REMARKABLE_DOCX}" "${REMARKABLE_REFERENCE_DOCX}"

echo "Generant DOCX del guió oral..."
pandoc "${ORAL_MD}" \
  --standalone \
  --reference-doc="${NORMAL_REFERENCE_DOCX}" \
  -o "${ORAL_DOCX}"

echo "Generant DOCX per a reMarkable..."
pandoc "${ORAL_MD}" \
  --standalone \
  --reference-doc="${REMARKABLE_REFERENCE_DOCX}" \
  -o "${REMARKABLE_DOCX}"

echo "Ajustant format DOCX..."
python3 - "${ORAL_DOCX}" "${REMARKABLE_DOCX}" "${REMARKABLE_MIN_FONT_PT}" <<'PY'
import sys
import zipfile
from pathlib import Path
from tempfile import TemporaryDirectory
from xml.etree import ElementTree as ET

W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
NS = {"w": W}
ET.register_namespace("w", W)

normal_docx = Path(sys.argv[1])
remarkable_docx = Path(sys.argv[2])
min_font_pt = float(sys.argv[3])
min_half_points = int(round(min_font_pt * 2))


def rewrite_docx(path, updater):
    with TemporaryDirectory() as tmp:
        tmp_dir = Path(tmp)
        with zipfile.ZipFile(path) as source:
            source.extractall(tmp_dir)

        updater(tmp_dir)

        tmp_docx = path.with_suffix(path.suffix + ".tmp")
        with zipfile.ZipFile(tmp_docx, "w", zipfile.ZIP_DEFLATED) as target:
            for item in sorted(tmp_dir.rglob("*")):
                if item.is_file():
                    target.write(item, item.relative_to(tmp_dir).as_posix())

        tmp_docx.replace(path)


def set_child(parent, tag_name):
    child = parent.find(f"w:{tag_name}", NS)
    if child is None:
        child = ET.SubElement(parent, f"{{{W}}}{tag_name}")
    return child


def set_a4_layout(tmp_dir):
    document_xml = tmp_dir / "word" / "document.xml"
    tree = ET.parse(document_xml)
    root = tree.getroot()
    body = root.find("w:body", NS)
    if body is None:
        return

    section = body.find("w:sectPr", NS)
    if section is None:
        section = ET.SubElement(body, f"{{{W}}}sectPr")

    page_size = set_child(section, "pgSz")
    page_size.set(f"{{{W}}}w", "11906")
    page_size.set(f"{{{W}}}h", "16838")

    margins = set_child(section, "pgMar")
    for attr, value in {
        "top": "1134",
        "right": "1134",
        "bottom": "1134",
        "left": "1134",
        "header": "708",
        "footer": "708",
        "gutter": "0",
    }.items():
        margins.set(f"{{{W}}}{attr}", value)

    tree.write(document_xml, encoding="UTF-8", xml_declaration=True)


def ensure_style_defaults(root):
    doc_defaults = set_child(root, "docDefaults")
    run_defaults = set_child(doc_defaults, "rPrDefault")
    run_properties = set_child(run_defaults, "rPr")
    for tag_name in ("sz", "szCs"):
        size = set_child(run_properties, tag_name)
        current = size.get(f"{{{W}}}val")
        try:
            current_value = int(current) if current is not None else None
        except ValueError:
            current_value = None
        if current_value is None or current_value < min_half_points:
            size.set(f"{{{W}}}val", str(min_half_points))


def enforce_min_font_size(tmp_dir):
    for xml_file in sorted((tmp_dir / "word").glob("*.xml")):
        tree = ET.parse(xml_file)
        root = tree.getroot()

        if xml_file.name == "styles.xml":
            ensure_style_defaults(root)

        changed = False
        for tag_name in ("sz", "szCs"):
            for node in root.findall(f".//w:{tag_name}", NS):
                value = node.get(f"{{{W}}}val")
                if value is None:
                    continue
                try:
                    numeric_value = int(value)
                except ValueError:
                    continue
                if numeric_value < min_half_points:
                    node.set(f"{{{W}}}val", str(min_half_points))
                    changed = True

        if changed or xml_file.name == "styles.xml":
            tree.write(xml_file, encoding="UTF-8", xml_declaration=True)


rewrite_docx(normal_docx, set_a4_layout)
rewrite_docx(remarkable_docx, enforce_min_font_size)
PY

echo "Generant PDF imprimible del guió oral..."
convert_to_pdf "${ORAL_DOCX}" "${ORAL_PDF}"

echo "Generant PDF per a reMarkable..."
convert_to_pdf "${REMARKABLE_DOCX}" "${REMARKABLE_PDF}"

echo "Generant PDF de la presentació..."
convert_to_pdf "${PPTX}" "${PRESENTATION_PDF}"

echo
echo "Materials derivats generats:"
echo "- ${ORAL_DOCX}"
echo "- ${ORAL_PDF}"
echo "- ${REMARKABLE_DOCX} (mida mínima: ${REMARKABLE_MIN_FONT_PT} pt)"
echo "- ${REMARKABLE_PDF}"
echo "- ${PRESENTATION_PDF}"
