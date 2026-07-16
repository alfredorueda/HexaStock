---
title: "Competències socioemocionals en ofertes sènior d'Enginyeria del Software"
subtitle: "Estudi observacional multifuente · 17 d'abril–16 de juliol de 2026"
author: "Material d'evidència per a la defensa de trajectòria"
date: "16 de juliol de 2026"
lang: ca
geometry: margin=2.2cm
fontsize: 11pt
toc: true
toc-title: "Contingut"
---

# Resum executiu

Aquest estudi examina si les ofertes actuals per a perfils sènior d'Enginyeria del Software demanen de manera explícita competències socioemocionals, a més de la base tècnica. S'han carregat 2.610 anuncis públics de sis fonts, se n'han aplicat criteris conservadors d'inclusió i s'han analitzat **222 ofertes úniques de 102 empreses**, publicades durant els 90 dies anteriors al 16 de juliol de 2026.

El resultat confirma la hipòtesi dins el marc observat:

- **84,7%** de les ofertes contenen almenys una demanda socioemocional nuclear (188/222; IC95% de Wilson: **79,4–88,8%**).
- Amb un criteri més exigent, **60,8%** en contenen almenys dues (135/222; IC95%: **54,3–67,0%**).
- Donant el mateix pes a cada empresa, les estimacions continuen sent elevades: **79,1%** per a una o més competències (IC95% bootstrap: **71,1–86,4%**) i **64,9%** per a dues o més (IC95%: **55,2–73,5%**).
- Limitant l'anàlisi a un màxim de tres anuncis per empresa, el resultat és molt semblant: **83,3%** amb una o més competències i **70,3%** amb dues o més (n = 138).

La conclusió defensable no és que una proporció exacta del treball sigui «emocional», sinó que aquestes capacitats apareixen com a **requisits professionals observables i recurrents** en rols tècnicament exigents. Les competències tècniques continuen sent la condició de base.

# Pregunta i hipòtesi

**Pregunta.** En quina mesura les ofertes actuals per a perfils sènior d'Enginyeria del Software expliciten competències comunicatives, relacionals, de lideratge o d'aprenentatge?

**Hipòtesi operacional.** Més de la meitat de les ofertes incloses demanen almenys una competència socioemocional nuclear. Com a comprovació més estricta, també s'avalua si més de la meitat en demanen almenys dues.

La unitat d'anàlisi és l'oferta única, no una frase, una tasca ni una hora de treball.

# Disseny de l'estudi

## Marc temporal i fonts

La recollida correspon a una instantània del **16 de juliol de 2026** i inclou anuncis publicats entre el **17 d'abril i el 16 de juliol de 2026**. Es van consultar sis fonts públiques amb accés estructurat:

| Font | Ofertes incloses | Documentació pública |
|---|---:|---|
| The Muse | 115 | <https://www.themuse.com/developers/api/v2> |
| Himalayas | 51 | <https://himalayas.app/api> |
| Arbeitnow | 24 | <https://www.arbeitnow.com/blog/job-board-api> |
| Jobicy | 23 | <https://jobicy.com/jobs-rss-feed> |
| Remotive | 8 | <https://github.com/remotive-io/remote-jobs-api> |
| Remote OK | 1 | <https://remoteok.com/api> |
| **Total** | **222** | |

La cobertura és internacional i combina ofertes europees, nord-americanes i remotes. Per tant, no s'ha d'interpretar com una estimació específica d'Espanya.

Com a context extern, les estadístiques experimentals europees basades en anuncis en línia mostren el valor d'aquestes dades per observar habilitats amb detall i rapidesa, però també adverteixen de biaixos de portal, duplicats i diferències respecte de les vacants oficials. Aquest estudi adopta explícitament aquestes cauteles. Vegeu [Skills-OVATE de Cedefop/Eurostat](https://cros.ec.europa.eu/topic/skills-ovate), la [metodologia d'Eurostat sobre anuncis en línia](https://ec.europa.eu/eurostat/web/experimental-statistics/online-job-advertisement-rate) i la [síntesi del Joint Research Centre](https://joint-research-centre.ec.europa.eu/projects-and-activities/employment/skills-intelligence-online-job-advertisements_en).

## Criteris d'inclusió

Una oferta havia de complir tots els criteris següents:

1. publicació dins la finestra de 90 dies;
2. rol de desenvolupament de software: software, backend, frontend, full stack, web, mòbil, iOS/Android o desenvolupament d'aplicacions;
3. seniority explícita al títol —Senior, Sr., Staff, Principal o Lead— o etiqueta sènior del portal amb evidència d'almenys quatre anys d'experiència;
4. descripció suficient per codificar requisits;
5. exclusió de direcció, product management i perfils adjacents: dades, ML/IA purs, DevOps/SRE, QA, seguretat no software, sistemes, xarxes, suport, vendes, maquinari, firmware i enginyeries no informàtiques.

## Flux de selecció

| Etapa | Registres |
|---|---:|
| Registres carregats | 2.610 |
| Fora de la finestra temporal | −498 |
| No sènior | −1.009 |
| Seniority insuficient malgrat l'etiqueta del portal | −221 |
| Protecció davant taxonomies massa àmplies d'una font | −398 |
| Rol no específic de desenvolupament de software | −212 |
| Perfil adjacent, directiu o no software | −24 |
| Duplicats | −26 |
| **Ofertes úniques analitzades** | **222** |

La deduplicació combina l'URL, l'empremta del text i la coincidència empresa–títol–ubicació. D'aquesta manera, una mateixa descripció replicada per diverses ciutats no pesa diverses vegades.

# Llibre de codis

La codificació és multietiqueta: una oferta pot contenir diverses competències. Només es compten formulacions explícites en anglès, alemany o castellà/català.

| Competència | Indicadors típics |
|---|---|
| Comunicació | comunicació escrita/oral; explicar conceptes tècnics; comunicar amb claredat |
| Col·laboració | col·laborar o treballar estretament amb altres equips; treball en equip |
| Alineament amb stakeholders | treball directe amb clients, usuaris, producte o negoci; traducció de necessitats |
| Mentoria | mentoritzar, fer coaching o guiar perfils júnior i altres enginyers |
| Lideratge i influència | direcció tècnica, influència sobre decisions, ownership i rendició de comptes |
| Aprenentatge i adaptabilitat | curiositat, aprenentatge continu, mentalitat de creixement, adaptació |
| Conflicte o negociació | negociació relacional, resolució de conflictes, desacord constructiu |
| Empatia o escolta | empatia aplicada a la relació professional i escolta activa |

La revisió de codi i el feedback es codifiquen per separat. Per prudència, **no formen part del càlcul nuclear** d'«almenys una» o «almenys dues», perquè una revisió de codi també pot interpretar-se com una pràctica tècnica.

# Resultats

## Prevalença per oferta

| Indicador | n / 222 | % | IC95% |
|---|---:|---:|---:|
| Almenys una competència socioemocional nuclear | 188 | **84,7** | 79,4–88,8 |
| Almenys dues competències nuclears | 135 | **60,8** | 54,3–67,0 |
| Col·laboració | 142 | **64,0** | 57,5–70,0 |
| Lideratge, influència o ownership | 87 | **39,2** | 33,0–45,7 |
| Feedback o revisió de codi | 82 | **36,9** | 30,9–43,5 |
| Comunicació explícita | 67 | **30,2** | 24,5–36,5 |
| Mentoria o coaching | 64 | **28,8** | 23,3–35,1 |
| Aprenentatge o adaptabilitat | 35 | **15,8** | 11,6–21,1 |
| Alineament explícit amb stakeholders | 33 | **14,9** | 10,8–20,1 |
| Empatia o escolta activa explícites | 4 | **1,8** | 0,7–4,5 |
| Conflicte o negociació explícits | 2 | **0,9** | 0,2–3,2 |

Els dos indicadors principals tenen el límit inferior de l'interval per sobre del 50%. Això confirma la hipòtesi operacional dins la mostra observada, inclús amb el criteri exigent de dues competències.

## Sensibilitat a la concentració per empresa

La mostra conté 102 empreses, però algunes publiquen moltes posicions. Per evitar que els resultats depenguin d'aquests clústers es van aplicar dues comprovacions:

| Comprovació | ≥1 competència | ≥2 competències |
|---|---:|---:|
| Totes les ofertes, pes igual per anunci (n = 222) | 84,7% | 60,8% |
| Pes igual per empresa (102 empreses) | 79,1% | 64,9% |
| Màxim de tres ofertes per empresa (n = 138) | 83,3% | 70,3% |

Amb pes igual per empresa, l'interval bootstrap del 95% és **71,1–86,4%** per a una o més competències i **55,2–73,5%** per a dues o més. La conclusió, per tant, no depèn d'un únic gran ocupador.

## Diferències descriptives per seniority

| Indicador | Senior (n = 160) | Lead/Staff/Principal (n = 62) |
|---|---:|---:|
| Almenys una competència nuclear | 81,2% | **93,5%** |
| Almenys dues competències nuclears | 59,4% | **64,5%** |
| Lideratge, influència o ownership | 33,1% | **54,8%** |
| Mentoria o coaching | 25,6% | **37,1%** |

La tendència descriptiva és coherent amb l'escalada de responsabilitat: els nivells Lead, Staff i Principal expliciten més lideratge i mentoria. No es presenta com una relació causal ni com una comparació poblacional.

# Controls de qualitat

- El llibre de codis es va definir abans de fixar els resultats finals i es va aplicar igual a totes les fonts.
- Es van revisar manualment 60 ofertes seleccionades amb una llavor fixa, tant per verificar la inclusió com per inspeccionar els fragments que activaven cada categoria.
- L'auditoria va detectar dos falsos positius de la versió inicial: un rol de radiofreqüència confós amb «frontend» i l'expressió alemanya *verhandlungssicher* referida al nivell d'idioma. Ambdues regles es van corregir i tota l'anàlisi es va executar de nou.
- La mesura principal exclou feedback/revisió de codi per evitar que una pràctica híbrida infli la dimensió socioemocional.
- Es publiquen les 222 files codificades, els URLs, les empremtes del text i el codi d'anàlisi, però no les descripcions completes de tercers.

# Interpretació

La dada central és robusta: en aquesta mostra actual, internacional i multifuente, **vuit de cada deu** ofertes sènior de desenvolupament de software expliciten almenys una capacitat comunicativa, relacional, de lideratge o d'aprenentatge. Fins i tot exigint-ne dues, el resultat supera la meitat de les ofertes.

Això sosté la rellevància professional de treballar aquestes competències de manera intencional dins la formació universitària en Enginyeria del Software. No substitueix l'arquitectura, la programació, la qualitat o el domini tecnològic: les complementa en situacions on cal coordinar equips, prendre decisions, explicar compromisos, mentoritzar i alinear el software amb necessitats reals.

El baix percentatge d'empatia, escolta activa o resolució de conflictes formulades literalment no invalida la hipòtesi general. Indica, més aviat, que la indústria acostuma a expressar la dimensió socioemocional amb vocabulari operacional —col·laborar, liderar, comunicar, influir, mentoritzar— i no necessàriament amb etiquetes acadèmiques.

# Limitacions

1. **No és una mostra probabilística.** Els portals i les empreses visibles en línia no representen totes les vacants.
2. **Cobertura internacional.** No permet estimar específicament el mercat espanyol ni comparar països.
3. **Dependència entre anuncis.** Diverses ofertes poden compartir estil corporatiu; per això es reporten resultats equilibrats per empresa.
4. **Mesura textual.** Es detecta allò que l'anunci explicita, no el comportament real en el lloc de treball ni la importància relativa de cada competència.
5. **Classificador lexical.** Pot ometre sinònims o contextos implícits; l'auditoria manual redueix, però no elimina, aquest risc.
6. **Intervals condicionals.** Els intervals quantifiquen incertesa dins el conjunt observat i les comprovacions de sensibilitat; no converteixen la mostra en representativa de tot el mercat.

# Conclusió

L'estudi confirma la hipòtesi amb un resultat prou ampli per justificar l'Annex D: les competències socioemocionals apareixen de manera explícita i recurrent en ofertes actuals de perfils sènior d'Enginyeria del Software. La formulació acadèmicament prudent és:

> En una mostra observacional de 222 ofertes úniques de 102 empreses i sis fonts públiques, el 84,7% explicitava almenys una competència socioemocional nuclear i el 60,8% n'explicitava almenys dues. La conclusió es manté en equilibrar el pes de les empreses, però no s'ha d'interpretar com una estimació probabilística de tot el mercat.

# Reproduïbilitat i fitxers

El codi, les files codificades i els resums es troben al directori `analysis/senior-software-jobs-2026-07/` de la presentació. L'script principal és `analyze_jobs.py`; els resultats tabulars són `results/offers-coded.csv`, `results/summary.csv`, `results/summary-company-balanced.csv`, `results/summary-by-tier.csv` i `results/metadata.json`.
