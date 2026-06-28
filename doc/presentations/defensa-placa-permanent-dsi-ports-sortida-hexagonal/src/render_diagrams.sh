#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PRESENTATION_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
DIAGRAM_DIR="${PRESENTATION_DIR}/diagrams"
RENDERED_DIR="${DIAGRAM_DIR}/rendered"
PLANTUML_IMAGE="${PLANTUML_IMAGE:-plantuml/plantuml:latest}"

mkdir -p "${RENDERED_DIR}"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker no està disponible. Instal·la Docker o renderitza PlantUML manualment." >&2
  exit 1
fi

echo "Renderitzant diagrames PlantUML amb ${PLANTUML_IMAGE}"

docker run --rm \
  -v "${DIAGRAM_DIR}:/diagrams" \
  "${PLANTUML_IMAGE}" \
  -tsvg \
  -o /diagrams/rendered \
  /diagrams/*.puml

docker run --rm \
  -v "${DIAGRAM_DIR}:/diagrams" \
  "${PLANTUML_IMAGE}" \
  -tpng \
  -o /diagrams/rendered \
  /diagrams/*.puml

echo "Diagrames renderitzats a ${RENDERED_DIR}"
