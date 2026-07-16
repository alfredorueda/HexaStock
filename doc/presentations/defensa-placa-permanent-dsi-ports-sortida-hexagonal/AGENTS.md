# Instructions for agents working on the TecnoCampus defense materials

This directory contains the canonical materials for two TecnoCampus defense presentations:

1. The 10-minute trajectory defense.
2. The 20-minute reduced lecture on output ports in hexagonal architecture.

## Canonical sources

The canonical sources are only:

- `output/defensa-trajectoria-10-minuts.pptx`
- `output/IMPORTANTE_MANUAL_defensa-dsi-ports-sortida-hexagonal.pptx`
- `output/materials-orals-canonics-10-minuts-trajectoria.md`
- `output/materials-orals-canonics-20-minuts.md`

Treat these four files as the source of truth for slides and oral scripts.

Do not treat rendered PDFs, DOCX files, reMarkable exports, generated images, old drafts, or auxiliary files as canonical sources.

## Derived materials

Derived materials must be generated only through these project scripts when Alfredo explicitly asks for derived generation:

- `src/generate_ALL_derived_materials.sh` (preferred: runs both scripts below sequentially and refuses to start if LibreOffice is open)
- `src/generate_derived_materials_trajectoria.sh`
- `src/generate_derived_materials.sh`

Never run the two individual scripts in parallel: they share headless LibreOffice and the second one aborts halfway, leaving stale PDFs.

Do not manually regenerate derived artefacts with ad hoc commands when these scripts cover the requested output.

Do not manually render all PlantUML diagrams or run separate diagram-generation steps as part of a normal derived-material or commit/push flow unless Alfredo explicitly asks for diagram rendering. Diagram sources and rendered images are supporting resources, not canonical presentation sources.

When working on either presentation, oral rehearsal, speaker notes, slide improvements, or related exports, always read and preserve the corresponding canonical PPTX and Markdown first.

Key reminders:

- Keep the tone academic, sober, institutional, and in Catalan unless the user explicitly asks otherwise.
- Use `software` consistently in this defense presentation and related oral materials because the official call is for a position in Enginyeria del Software.
- Work directly on the canonical PowerPoints for small final-stage slide edits.
- Do not regenerate PDFs, DOCX files, reMarkable exports, or other derived materials unless Alfredo explicitly asks for regeneration; Alfredo may handle derived exports himself.
- Do not recreate or run a PowerPoint generator unless Alfredo explicitly asks to rebuild that workflow from the current canonical PPTX.
- Do not use old drafts as authoritative sources if they conflict with the canonical PowerPoints or canonical Markdown oral scripts.
- Static images or embedded diagrams already placed in the presentation may remain in English when they show software engineering/code artifacts. Do not replace static images in the presentation without Alfredo's explicit permission.
- During rehearsal or presentation-improvement work, actively remind Alfredo to preserve and explain the slide improvements recorded in `recordatori-millores-diapositives.md`.
- Pay special attention to slides 8, 9, and 10 of the 20-minute deck (Port de sortida i adaptador, Port patrimonial, Domini financer): distinguish execution flow from code dependency and architectural dependency.
- For output ports, if an arrow represents code dependency or implementation, it should point from the adapter to the port.
- This arrow-direction improvement is already applied in the current canonical presentation; do not reintroduce it as a pending task.
- The core message to preserve is: the application service needs a capability, not a technology.
