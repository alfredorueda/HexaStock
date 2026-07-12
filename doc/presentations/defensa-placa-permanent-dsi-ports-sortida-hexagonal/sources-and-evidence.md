# Fonts i evidències

## 1. Informació oficial de l'assignatura DSI

**Font oficial incorporada**

`assets/evidencies/pla-docent-dsi-103322-2025-26.pdf`

**URL oficial**

`https://pladocent.tecnocampus.cat/pdfs/2025/18/103322_ca.pdf`

**Informació confirmada**
- Assignatura 103322: Disseny de Sistemes d'Informació.
- Grau en Enginyeria Informàtica de Gestió i Sistemes d'Informació.
- Curs acadèmic 2025/26.
- Assignatura obligatòria, tercer curs, segon trimestre, 6 ECTS.
- Professorat: Josep Roure Alcobé i Alfredo Rueda Unsain.
- Continguts oficials alineats amb la microlliçó: arquitectures clean o hexagonals, ports i adaptadors, mapping entre capes, organització en mòduls, microserveis i DDD.

**Ús en la presentació**
La diapositiva 2 cita aquesta guia docent per mostrar que la microlliçó no és un tema aliè a la convocatòria, sinó una sessió versemblant dins una assignatura que Alfredo imparteix i que encaixa amb el temari oficial.

**Pendent**
- Revisar si cal citar també la fitxa pública HTML `https://www.tecnocampus.cat/node/18815`.

## 2. Informació acreditada per l'informe AGAUR

**Fitxer incorporat**

`assets/evidencies/informe-acreditatiu-agaur-arquitectura-hexagonal.pdf`

**Fitxer original**

`/Users/alfre/Library/CloudStorage/OneDrive-TecnocampusMataro-Maresme/Doctorat/Tesis Doctoral/RepositorioGitHub/Preparación documentación Plaza Profesor TecnoCampus/CV TecnoCampus/Consultoria Empresarial/Informe acreditatiu AGAUR arquitectura hexagonal.pdf`

**Metadades observades**
- Títol normalitzat per a la defensa: informe acreditatiu d'activitat de formació i consultoria especialitzada en enginyeria del software.
- Autor: Agència de Gestió d'Ajuts Universitaris i de Recerca.
- Data del document: Barcelona, 15 de juny de 2026.
- Signatura digital indicada al PDF: 16 de juny de 2026.
- Extensió: 2 pàgines.

**Informació acreditada**
- Alfredo Rueda Unsain va desenvolupar una activitat de formació i consultoria especialitzada adreçada al Departament de Desenvolupament de Software de l'AGAUR.
- L'activitat es va centrar en serveis REST i arquitectura hexagonal aplicada a sistemes d'informació.
- Es va desenvolupar entre maig i juliol de 2025.
- Durada total: 60 hores, en sessions de treball de dues hores.
- Va combinar formació tècnica especialitzada, exercicis pràctics i consultoria aplicada.
- Es van treballar REST, OpenAPI, arquitectura hexagonal, ports i adaptadors, inversió de dependències, separació entre domini, aplicació i infraestructura, casos d'ús, proves, refactorització de serveis SOAP cap a REST, reducció d'acoblament, mantenibilitat, escalabilitat i evolució de sistemes.
- El document acredita explícitament la refactorització de serveis SOAP existents cap a tecnologia REST i l'ús de l'arquitectura hexagonal per reduir l'acoblament.

**Ús prudent**
La presentació utilitza aquesta font per acreditar la realitat de l'activitat professional, no per exposar detalls interns de sistemes.

## 3. Informació professional aportada per Alfredo Rueda Unsain

**Origen actual:** relat professional aportat per Alfredo en l'encàrrec.

**Contingut**
- Context de sistemes de gestió d'ajuts i beques.
- Referències a BOGA i a processos vinculats a avaluació de condicions de concessió.
- Integració amb PICA com a plataforma d'interoperabilitat.
- Possibilitat de consultar informació administrativa externa, inclosa informació catastral rellevant per condicions patrimonials.
- Existència d'integracions basades en SOAP i XML en el context treballat.
- Motiu professional de la consultoria: desacoblar la lògica administrativa i els casos d'ús dels detalls concrets de PICA, SOAP/XML, DTO, mapping i errors tècnics, i preparar l'evolució cap a serveis REST sense arrossegar els casos d'ús.

**Ús en la presentació**
S'utilitza com a motivació conceptual del problema arquitectònic: procediments administratius que necessiten informació externa sense dependre directament dels detalls tècnics d'aquesta integració.

**Definicions breus per a la diapositiva 4**
- PICA: plataforma d'interoperabilitat que permet accedir a informació d'organismes de la Generalitat i d'altres administracions públiques i institucions.
- Cadastre / Catastro: registre administratiu on es descriuen béns immobles mitjançant atributs físics, jurídics i econòmics.
- SOAP/XML: protocol de missatgeria i marc d'intercanvi d'informació estructurada basat en tecnologies XML.

**Fonts públiques per a aquestes definicions**
- CTTI, Plataforma d'Integració i Col·laboració Administrativa (PICA): `https://ctti.gencat.cat/ca/detalls/detallarticle/pica`
- CTTI / Canigó, connector PICA: `https://canigo.ctti.gencat.cat/plataformes/canigo/documentacio-per-versions/3.8LTS/3.8.0/moduls/moduls-integracio/modul-pica/`
- Portal de desenvolupadors d'APIs del CTTI: `https://portal.db40-c57f0fcb.eu-de.apiconnect.appdomain.cloud/ctti/public-pre/product`
- Dirección General del Catastro, "El Catastro en cifras": `https://www.catastro.hacienda.gob.es/es-ES/catastroencifras.html`
- W3C, SOAP Version 1.2 Part 1: Messaging Framework: `https://www.w3.org/TR/soap12-part1/`

**Contrast públic consultat sobre PICA i APIs**
- La pàgina pública del CTTI defineix PICA com una plataforma d'accés a informació d'organismes de la Generalitat i d'altres administracions, i explicita que pot transformar múltiples tecnologies dels emissors cap a un conjunt fix de tecnologies que faciliten l'accés a les aplicacions de la Generalitat.
- La documentació pública de Canigó per al connector PICA descriu l'accés a PICA mitjançant web service síncron i web service asíncron.
- El Portal de desenvolupadors d'APIs del CTTI mostra productes amb REST i WSDL/SOAP, fet que reforça el context de coexistència o transició tecnològica entre models d'integració.
- La publicació de Canigó 3.6.0 documenta mòduls d'integració amb serveis web SOAP, incloent-hi PICA.
- Els principis d'arquitectura del CTTI recomanen desacoblar frontend i backend, exposar la lògica mitjançant serveis, principalment REST, i usar preferentment API Manager i EventHub/Kafka per a interoperabilitat.
- No s'ha localitzat, en fonts públiques obertes, una afirmació que acrediti la retirada total dels serveis SOAP de PICA. Per tant, la presentació no formula aquesta afirmació.

**Precaucions**
- No mostrar dades personals.
- No mostrar NIFs.
- No mostrar endpoints.
- No mostrar logs.
- No mostrar credencials.
- No mostrar captures internes.
- Formular el cas com a problema habitual en sistemes corporatius, no com a crítica a AGAUR.

## 4. Informació pública sobre el cas funcional de beca

**Document de suport creat**

`cas-funcional-agaur-beca.md`

**Fonts principals**
- AGAUR, requisits econòmics de la beca general: `https://agaur.gencat.cat/ca/beques-i-ajuts/pagines-especials/beques-i-ajuts-per-estudis-universitaris1/beca-general-ministeri/Requisits-economics/index.html`
- AGAUR, preguntes freqüents de la beca general: `https://agaur.gencat.cat/ca/beques-i-ajuts/beca-general-generalitat/estudiants-domicili-catalunya/quan-solicitud-presentada/preguntes-frequents/`
- AGAUR, quarta addenda al conveni BOGA 2020: `https://agaur.gencat.cat/web/.content/Documents/AGAUR/transparencia/convenis/2020/03_2020_4a_Addenda_Ensenyament_CA.pdf`
- BOE, Reial decret 163/2025, de 4 de març: `https://www.boe.es/diario_boe/txt.php?id=BOE-A-2025-4320`
- Dirección General del Catastro, "El Catastro en cifras": `https://www.catastro.hacienda.gob.es/es-ES/catastroencifras.html`

**Fets verificables incorporats a la presentació**
- AGAUR descriu una fase de requisits econòmics de renda i patrimoni.
- Si no es compleixen aquests requisits econòmics, l'expedient no avança a la revisió acadèmica.
- Els límits patrimonials poden incloure finques urbanes i rústiques, valorades amb valors cadastrals.
- La consulta de dades de renda i patrimoni de la unitat familiar es realitza d'acord amb la base jurídica aplicable.
- L'addenda pública AGAUR-Departament d'Educació documenta BOGA i processos d'interoperabilitat a través de PICA; el catàleg AOC tracta les dades tributàries de l'AEAT i les dades cadastrals de la Direcció General del Cadastre com a serveis diferenciats.

**Formulació prudent**
Es pot afirmar que el procediment de beca necessita informació administrativa externa per avaluar renda i patrimoni, incloent-hi informació patrimonial relacionada amb valors cadastrals. També es pot afirmar que hi ha documentació pública que vincula BOGA i PICA, i que el catàleg d'interoperabilitat distingeix les dades tributàries de l'AEAT de les dades cadastrals de la Direcció General del Cadastre.

No s'ha de formular com una afirmació sobre endpoints interns, protocols exactes en producció o captures dels sistemes d'AGAUR.

## 5. Informació extreta del repositori local HexaStock

**Repositori local**

`/Users/alfre/IdeaProjects/DSI2025-2026/HexaStock`

**Estat observat**
- Branca: `main`.
- Commit curt: `9dd2644`.
- Canvi no versionat preexistent: `doc/Get Your Hands Dirty on Clean Architecture.pdf`.

**Fitxers clau confirmats**
- Port d'entrada: `application/src/main/java/cat/gencat/agaur/hexastock/application/port/in/PortfolioStockOperationsUseCase.java`.
- Servei d'aplicació: `application/src/main/java/cat/gencat/agaur/hexastock/application/service/PortfolioStockOperationsService.java`.
- Port de sortida: `application/src/main/java/cat/gencat/agaur/hexastock/application/port/out/StockPriceProviderPort.java`.
- Adaptador Finnhub: `adapters-outbound-market/src/main/java/cat/gencat/agaur/hexastock/adapter/out/rest/FinhubStockPriceAdapter.java`.
- Adaptador Alpha Vantage: `adapters-outbound-market/src/main/java/cat/gencat/agaur/hexastock/adapter/out/rest/AlphaVantageStockPriceAdapter.java`.
- Adaptador mock: `adapters-outbound-market/src/main/java/cat/gencat/agaur/hexastock/adapter/out/rest/MockFinhubStockPriceAdapter.java`.
- Domini ric: `Portfolio`, `Holding`, `Lot`, `SellResult`, `Price`, `Ticker`, `ShareQuantity`, `StockPrice`.
- Wiring: `bootstrap/src/main/java/cat/gencat/agaur/hexastock/config/SpringAppConfig.java`.
- Perfils documentats: `finhub`, `alphaVantage`, `mockfinhub`, combinables amb `jpa` o `mongodb`.
- Diagrama de dependències de codi del tutorial Sell Stocks incorporat a la presentació: `assets/hexastock-hexagonal-code-dependencies.png`.

**Flux confirmat**
`PortfolioStockOperationsService.sellStock(...)` recupera la cartera, consulta `StockPriceProviderPort.fetchStockPrice(ticker)`, extreu `Price`, invoca `portfolio.sell(ticker, quantity, price)`, desa la cartera i registra la transacció.

## 6. Informació extreta del repositori GitHub HexaStock

**URL revisada**

`https://github.com/alfredorueda/HexaStock/blob/main/doc/tutorial/sellStocks/SELL-STOCK-TUTORIAL.md`

**Imatge incorporada**

`https://raw.githubusercontent.com/alfredorueda/HexaStock/refs/heads/main/doc/tutorial/sellStocks/diagrams/Rendered/HexaStock_Hexagonal_Diagram_Code_Dependencies.png`

**Informació utilitzada**
- El tutorial de venda d'accions traça el cas d'ús de punta a punta.
- Documenta el paper del controller, port d'entrada, servei d'aplicació, ports de sortida, domini i adaptadors.
- Presenta el cas de venda com a exemple de DDD, arquitectura hexagonal i especificació executable.
- El diagrama `HexaStock_Hexagonal_Diagram_Code_Dependencies.png` s'utilitza com a visual principal per mostrar les dependències de codi, la independència del domini i la implementació dels ports per part dels adaptadors.

**Nota**
El codi local s'ha prioritzat com a font principal perquè pot contenir canvis més recents.

## 7. Informació extreta del GitBook d'Alfredo

**URL prevista**

`https://alfredo-rueda-unsain.gitbook.io/alfredo-rueda-unsain-docs`

**Estat**
Pendent de contrastar. L'eina d'accés web no ha retornat contingut útil en aquesta passada.

**Criteri aplicat**
S'ha utilitzat el repositori local i la documentació local `doc/tutorial/sellStocks/SELL-STOCK-TUTORIAL.md` com a font funcional equivalent per a aquesta primera versió.

## 8. Informació pendent de verificar

- Pla docent oficial de Disseny de Sistemes d'Informació si es vol citar literalment.
- Correspondència exacta entre GitBook i documentació local.
- Si Alfredo vol incloure una referència explícita al PDF AGAUR en una diapositiva o només en notes.
- Si el logotip oficial descarregat és la versió institucional preferida per a una defensa de plaça.
- Si es vol substituir `MockFinhubStockPriceAdapter` per una denominació més genèrica en la diapositiva oral, com ara "adaptador mock".

## 9. Fonts conceptuals complementàries

**Llibre local**

`doc/Get Your Hands Dirty on Clean Architecture.pdf`

**Referència**
Tom Hombergs, *Get Your Hands Dirty on Clean Architecture*, 2019.

**Ús en la presentació**
S'ha utilitzat com a suport terminològic i conceptual per reforçar:
- dependències cap al nucli;
- independència del domini respecte de frameworks, bases de dades, UI i sistemes externs;
- ports entrants i sortints;
- adaptadors que condueixen o són conduïts per l'aplicació;
- serveis de cas d'ús com a coordinadors;
- transaccions al servei d'aplicació;
- tests de domini, cas d'ús i adaptadors;
- enforcement amb build modules i proves d'arquitectura.

**Nota de copyright**
No s'han incorporat fragments extensos del llibre. La presentació en fa una síntesi pròpia orientada a docència.

## 10. Identitat visual TecnoCampus

**Font oficial**

`https://www.tecnocampus.cat/universitat/imatge-corporativa-tecnocampus`

**Informació observada**
La pàgina pública d'imatge corporativa ofereix manual corporatiu i descàrregues de logotips del Parc TecnoCampus i de TecnoCampus Centres Adscrits UPF en diferents formats.

**Fitxer descarregat**

`assets/logotip-oficial-tecnocampus-upf-horitzontal-color.png`
