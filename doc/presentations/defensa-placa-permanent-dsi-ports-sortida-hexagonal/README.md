# Materials de defensa: trajectòria i ports de sortida

Aquesta carpeta conté dues presentacions independents. Per preparar-les, comença pel PowerPoint i el guió oral corresponents; els esborranys i les exportacions no substitueixen aquestes fonts.

| Sessió | Presentació canònica | Guió oral canònic |
|---|---|---|
| Trajectòria, 10 minuts | [PowerPoint](output/defensa-trajectoria-10-minuts.pptx) | [Guió i respostes de reserva](output/materials-orals-canonics-10-minuts-trajectoria.md) |
| Ports de sortida, 20 minuts | [PowerPoint](output/IMPORTANTE_MANUAL_defensa-dsi-ports-sortida-hexagonal.pptx) | [Guió, notes i assaig](output/materials-orals-canonics-20-minuts.md) |

La trajectòria té sis diapositives principals i quatre annexos per al torn de preguntes. La classe té dotze diapositives principals i un annex de demo opcional. Els annexos no formen part del temps principal.

## Preparació per etapes

1. Llegeix només el guió principal de la sessió que prepararàs.
2. Assaja cada bloc amb les notes del presentador i comprova les transicions.
3. Fes una passada cronometrada amb pauses reals.
4. Prepara els annexos per separat, segons les preguntes o el temps disponible.

Per a la classe, conserva i explica les millores del [recordatori de diapositives](output/recordatori-millores-diapositives.md). A les diapositives 8, 9 i 10, diferencia flux d'execució i dependència de codi: el servei usa el port i l'adaptador l'implementa. La idea central és que el cas d'ús necessita una capacitat, no una tecnologia.

## Materials de suport

* [Cas funcional AGAUR](cas-funcional-agaur-beca.md): context docent i fonts públiques, sense detalls operatius interns.
* [Fonts i evidències](sources-and-evidence.md): registre de preparació; contrasta les referències temporals abans de reutilitzar-lo.
* [Checklist](checklist.md): comprovacions per a l'assaig final.
* [Estudi d'ofertes de juliol de 2026](analysis/senior-software-jobs-2026-07/README.md): instantània amb dades, protocol i limitacions.
* `outline.md` i `slide-by-slide.md`: esborranys històrics; no descriuen la versió canònica actual.

Els PDF, DOCX i fitxers reMarkable són derivats. La data d'una exportació no la converteix en font canònica. Les imatges i els diagrames són recursos de suport.

## Manteniment

Segueix [AGENTS.md](AGENTS.md). Les correccions de les diapositives es fan sobre el PowerPoint canònic; no es reconstrueix amb generadors antics. Conserva el registre acadèmic, sobri i institucional, i utilitza `hardware` i `software`.

Quan Alfredo demani explícitament regenerar els derivats, utilitza el procés conjunt des de l'arrel del repositori, amb LibreOffice tancat:

```bash
doc/presentations/defensa-placa-permanent-dsi-ports-sortida-hexagonal/src/generate_ALL_derived_materials.sh
```

El procés executa les dues generacions seqüencialment. No les executis en paral·lel. Els requisits inclouen `pandoc`, `python3` i LibreOffice (`soffice`). La regeneració de derivats o diagrames no és un pas automàtic d'un commit de documentació.
