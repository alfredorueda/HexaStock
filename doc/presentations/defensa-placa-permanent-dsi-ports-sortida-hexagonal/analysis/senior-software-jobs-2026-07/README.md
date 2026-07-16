# Estudi d'ofertes sènior d'Enginyeria del Software — juliol de 2026

Aquest directori conté els materials reproduïbles de l'estudi utilitzat a l'Annex D de la defensa de trajectòria.

## Fitxers

- `analyze_jobs.py`: normalització, criteris d'inclusió, deduplicació, codificació i càlculs.
- `results/offers-coded.csv`: metadades i variables codificades de les 222 ofertes incloses; no conté les descripcions completes.
- `results/summary.csv`: prevalences per oferta i intervals de Wilson del 95%.
- `results/summary-company-balanced.csv`: sensibilitat amb el mateix pes per empresa i intervals bootstrap.
- `results/summary-by-tier.csv`: resultats descriptius per nivell de seniority.
- `results/metadata.json`: flux de selecció, fonts i controls de sensibilitat.
- `informe-competencies-socioemocionals-ofertes-senior-software-2026-07.md`: informe complet.
- `informe-competencies-socioemocionals-ofertes-senior-software-2026-07.pdf`: versió PDF de l'informe.

## Reproducció

La instantània bruta de les API no es versiona perquè conté el text íntegre d'anuncis de tercers i perquè les fonts són dinàmiques. Amb una instantània equivalent dins un directori local:

```bash
python3 analyze_jobs.py /ruta/a/raw ./results
```

La data de tall està fixada al codi: 16 de juliol de 2026, amb una finestra retrospectiva de 90 dies.

## Fonts públiques

- [Arbeitnow](https://www.arbeitnow.com/blog/job-board-api)
- [Himalayas](https://himalayas.app/api)
- [Jobicy](https://jobicy.com/jobs-rss-feed)
- [Remotive](https://github.com/remotive-io/remote-jobs-api)
- [Remote OK](https://remoteok.com/api)
- [The Muse](https://www.themuse.com/developers/api/v2)

L'estudi és observacional i multifuente. No constitueix una mostra probabilística de totes les vacants del mercat.
