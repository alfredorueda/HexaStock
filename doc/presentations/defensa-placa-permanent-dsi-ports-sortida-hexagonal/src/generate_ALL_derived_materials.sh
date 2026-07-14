#!/usr/bin/env bash
set -euo pipefail

# Regenera TOTS els materials derivats de les dues presentacions, en seqüència:
#   1) Classe magistral de 20 minuts (generate_derived_materials.sh)
#   2) Trajectòria de 10 minuts (generate_derived_materials_trajectoria.sh)
#
# L'ordre és obligatori: el script de trajectòria fa servir els DOCX de
# 20 minuts com a referència d'estils. I no es poden executar en paral·lel:
# comparteixen LibreOffice headless i el segon falla silenciosament a mig fer.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if pgrep -x soffice >/dev/null 2>&1 || pgrep -f "soffice.bin" >/dev/null 2>&1; then
  echo "Hi ha una instància de LibreOffice en execució. Tanca-la abans de regenerar: la conversió headless hi entra en conflicte i deixa PDF sense actualitzar." >&2
  exit 1
fi

echo "==> [1/2] Materials derivats de la classe magistral (20 minuts)"
bash "${SCRIPT_DIR}/generate_derived_materials.sh"

echo
echo "==> [2/2] Materials derivats de la trajectòria (10 minuts)"
bash "${SCRIPT_DIR}/generate_derived_materials_trajectoria.sh"

echo
echo "Tots els materials derivats s'han regenerat correctament."
