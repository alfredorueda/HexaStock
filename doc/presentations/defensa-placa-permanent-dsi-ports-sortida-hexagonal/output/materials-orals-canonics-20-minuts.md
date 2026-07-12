# Materials orals canònics per a la classe magistral de 20 minuts

Fonts canòniques utilitzades:

* `IMPORTANTE_MANUAL_defensa-dsi-ports-sortida-hexagonal.pptx`
* `informe-acreditatiu-agaur-arquitectura-hexagonal.pdf`
* `pla-docent-dsi-103322-2025-26.pdf`
* `HexaStock_Hexagonal_Diagram_Code_Dependencies.png` i la seva còpia d'assets per a la presentació
* `Get Your Hands Dirty on Clean Architecture.pdf`

Documents antics ignorats com a font: `outline.md`, `slide-by-slide.md`, `guió-oral-20-minuts.md` i altres esborranys previs.

Nota de versió: la demo queda fora del cos principal de la classe. El tancament formal és la diapositiva `Idea clau i tancament`. La demo es conserva com a annex opcional, només per activar-la si el tribunal la demana o si hi ha temps explícitament disponible després del tancament.

---

## 1. Guió oral complet imprimible

Temps total previst d'assaig del cos principal: aproximadament 16:00-17:15, sense demo en viu. La lectura neta del text queda al voltant de 12:00-13:00 segons el ritme; la validació definitiva s'ha de fer amb assaig oral real.

La demo no forma part del temps canònic de la classe. S'ha de considerar un annex opcional posterior al tancament.

### Diapositiva 1. Portada - 1:35

Bon dia, membres del tribunal. [pausa breu, mirada al tribunal]

La classe que presento porta per títol `Disseny de ports de sortida en arquitectura hexagonal`, i se centra en el desacoblament entre casos d'ús, domini i serveis externs. [assenyalar el títol]

Abans d'entrar en el contingut tècnic, voldria fer una precisió de context. A la documentació de la convocatòria he incorporat un informe acreditatiu de la meva activitat de consultoria especialitzada per a la Generalitat de Catalunya, concretament l'Agència de Gestió d'Ajuts Universitaris i de Recerca. [pausa breu]

Ho explico perquè em sembla molt enriquidor plantejar aquesta sessió com una trobada entre dues dimensions.

D'una banda, la dimensió acadèmica: l'assignatura de Disseny de Sistemes d'Informació, on treballem conceptes com arquitectura hexagonal, ports, adaptadors i inversió de dependències.

I de l'altra, la dimensió professional: un cas real que ens permet veure com aquests conceptes s'aterren en reptes concrets d'arquitectura de software. [mirada al tribunal]

En vint minuts no seria rigorós intentar explicar tota l'arquitectura hexagonal. Per això em centraré en una decisió concreta: com dissenyar un port de sortida quan un cas d'ús necessita informació que es troba fora de l'aplicació.

### Diapositiva 2. On som dins l'assignatura - 1:05

La sessió se situa dins Disseny de Sistemes d'Informació, una assignatura de tercer curs, de 6 ECTS. [assenyalar la part esquerra]

El pla docent de l'assignatura treballa una visió global d'arquitectures de sistemes d'informació: arquitectures per capes, Clean Architecture, arquitectura hexagonal, ports i adaptadors, mapping, modularització i Domain-Driven Design. [to calmat, explicatiu]


### Diapositiva 3. Objectiu de la sessió - 0:55

Abans d'entrar en el cas, deixo formulat l'objectiu de la sessió. [pausa breu]

El principi de treball és aquest: el cas d'ús necessita una capacitat, no una tecnologia específica. [mirar el tribunal]

La sessió d'avui s’organitza al voltant de tres decisions de disseny: identificar una necessitat externa del cas d'ús, expressar-la com a port de sortida i implementar-la mitjançant adaptadors substituïbles. [assenyalar les tres caixes]

No desenvoluparé encara tota l'abstracció. Primer veurem el problema en un cas real d'administració pública, i a partir d'aquí formularem la decisió arquitectònica. [pausa breu]

### Diapositiva 4. Avaluació econòmica d'una beca - 1:45

Abans de parlar d'arquitectura de software, cal entendre mínimament el procediment administratiu. [to més lent]

En un procediment de beca, una sol·licitud dona lloc a un expedient. Aquest expedient passa per diferents fases: requisits generals, requisits econòmics, revisió acadèmica i resolució. [assenyalar la seqüència de la dreta]

La part que ens interessa és l'avaluació econòmica. En aquest punt es comproven aspectes com la renda familiar i el patrimoni. Quan no s'acredita el compliment dels requisits econòmics, l'expedient no obté una valoració favorable en aquesta fase. [pausa breu]

La idea docent important és que aquí encara no hem parlat de SOAP, ni de REST, ni de cap base de dades. Estem parlant de la necessitat funcional del procediment: per poder aplicar uns criteris econòmics, el sistema necessita dades fiables sobre renda i patrimoni.

Aquest pas és essencial. Si comencem directament per la tecnologia, correm el risc de construir el cas d'ús al voltant de la integració. En canvi, si comencem pel procediment, podem distingir entre la decisió administrativa que volem modelar i el mecanisme tècnic que ens proporciona la informació.

### Diapositiva 5. Quina informació externa necessita el procediment? - 1:45

Per avaluar renda i patrimoni, el procediment pot necessitar informació que no neix dins l'aplicació. [assenyalar la columna de necessitats]

En termes funcionals, podem parlar de renda familiar, patrimoni i béns immobles. Les fonts administratives poden ser organismes com l'AEAT o el Cadastre. PICA, en canvi, no és una font material del mateix tipus: és un canal o plataforma corporativa d'interoperabilitat. [assenyalar la separació visual]

PICA és la Plataforma d'Integració i Col·laboració Administrativa de la Generalitat de Catalunya. A efectes docents, el que ens interessa és que actua com a mecanisme corporatiu per consultar o intercanviar informació entre administracions.

La distinció clau és aquesta: el procediment necessita informació administrativa; no necessita, en el seu llenguatge propi, una API concreta. [pausa llarga; mirar el tribunal]

Això no vol dir que el mecanisme tècnic concret no sigui important. Ho és, i molt. Però encara no som en aquest nivell de decisió. En aquest moment, el que volem destacar és que el cas d'ús ha d'expressar una necessitat del procediment, no una dependència tecnològica concreta.

El cas d'ús hauria de poder formular-se així: `necessito informació patrimonial d'un ciutadà`. Encara no estem decidint si aquesta informació arribarà a través de PICA, d'un servei SOAP, d'una API REST, d'una base de dades corporativa o d'un altre mecanisme.

Quan aquesta separació no es respecta, apareix el problema arquitectònic que veurem ara.

### Diapositiva 6. Dependència directa entre procediment i integració - 1:55

Imaginem una situació en què el cas d'ús queda vinculat directament a la cadena tècnica: SOAP/XML, PICA i serveis externs diferenciats com l'AEAT o el Cadastre. Això no és la solució proposada; és el problema que volem corregir. [assenyalar la cadena central]

El problema no és consumir PICA. Ho remarco perquè és important. PICA, en aquest context, és una plataforma corporativa d'interoperabilitat. El problema arquitectònic apareix quan la lògica del procediment administratiu depèn directament dels detalls de la integració: estructures XML, clients SOAP, codis d'error tècnics o convencions d'una API concreta. [pausa breu]

Quan això passa, l'evolució tecnològica travessa la frontera del cas d'ús. Si un servei evoluciona, si canvia el contracte tècnic o si el mecanisme d'integració es transforma, el canvi pot impactar codi que hauria d'estar expressant criteris del procediment.

Des del punt de vista de l'enginyeria del software, això incrementa tres riscos. Primer, acoblament tecnològic: el cas d'ús coneix massa detalls externs. Segon, manteniment més fràgil: canvis d'infraestructura obliguen a revisar lògica d'aplicació. I tercer, més superfície d'impacte davant de canvis externs. [assenyalar les tres caixes]

La pregunta docent, per tant, és: com podem permetre que el cas d'ús necessiti dades externes, però sense dependre directament de la tecnologia que les proporciona?

La resposta és introduir un port de sortida.

### Diapositiva 7. Port de sortida i adaptador - 2:20

Abans d'aplicar-ho al cas patrimonial, fixem una distinció important.

Una cosa és el flux d'execució: el cas d'ús demana una informació i aquesta informació pot acabar venint d'un sistema extern.
[assenyalar la primera línia]

Una altra cosa és la dependència de codi: el cas d'ús depèn del port, i l'adaptador implementa aquest port.
[assenyalar la segona línia]

Per situar bé el concepte, quan aquí parlem d'un port no estem parlant d'un element físic ni d'un detall d'infraestructura. Conceptualment, el port és un contracte; en aquesta implementació Java, el representem amb una interfície.

És a dir, el cas d'ús no depèn directament d'una base de dades, d'una API externa o d'un client concret. El cas d'ús depèn d'una interfície que defineix què necessita: per exemple, obtenir una determinada informació econòmica, consultar un expedient o recuperar unes dades administratives.

Després, l'adaptador és la peça que implementa aquesta interfície i sap com anar realment a buscar aquella informació al sistema extern corresponent.

Això ens ajuda a entendre el valor arquitectònic de la interfície en aquest context. Defineix un contracte estable: què es necessita i què es retorna, però sense obligar el cas d'ús a conèixer com es resol tècnicament aquesta necessitat.

I això no és només una idea pròpia de la programació. És un principi molt general d'enginyeria: separar el contracte d'ús de la implementació concreta. Quan fem això, els components poden evolucionar, substituir-se o adaptar-se amb molt menys impacte sobre la resta del sistema.

Aquesta és la inversió de dependències: la infraestructura depèn del port definit per l'aplicació, no a l'inrevés.
[mirada al tribunal]

Amb aquesta idea clara, ara podem portar el patró al cas administratiu.

### Diapositiva 8. Port de sortida per a informació patrimonial - 1:45

Portem aquesta idea al cas administratiu. [assenyalar el diagrama]

El servei d'aplicació que avalua econòmicament una beca no hauria de conèixer directament els detalls de PICA, SOAP/XML, REST o JDBC. Hauria de dependre d'un contracte expressat en el llenguatge de l'aplicació. Per exemple: obtenir informació patrimonial, recuperar dades de renda o carregar l'expedient administratiu.

En el diagrama, el nucli de l'aplicació conté el servei d'aplicació i el model de domini. Al voltant hi ha ports de sortida que representen necessitats de l'aplicació. Fora del nucli hi ha adaptadors que resolen persistència, interoperabilitat amb PICA o altres mecanismes concrets.

El punt tècnic més important és que l'adaptador de PICA o l'adaptador JDBC no són el centre de l'arquitectura. Són detalls externs que poden evolucionar. El que volem mantenir estable és el contracte que el cas d'ús necessita. [pausa breu]

Aquesta distinció queda reflectida en la direcció de les fletxes. Quan indiquen `implemented by`, les fletxes van de l'adaptador cap al port. El servei usa el port. L'adaptador implementa el port. I el domini no coneix ni l'adaptador, ni PICA, ni SOAP, ni REST.

Aquesta mateixa estructura la podem transferir ara al domini financer: una aplicació de gestió d'una cartera d'inversió personal.

### Diapositiva 9. Transferència al domini financer - 1:25

Ara transferim la mateixa decisió arquitectònica a un domini diferent: una aplicació de gestió d'una cartera d'inversió.

El cas d'ús de venda d'accions necessita conèixer el preu actual, però no necessita saber quin proveïdor de dades utilitzem, quina API concreta consultem ni quin format retorna. Aquesta necessitat s'expressa mitjançant `StockPriceProviderPort`.

Els adaptadors de Finnhub, Alpha Vantage o un adaptador mock implementen aquest mateix contracte i encapsulen els detalls de cada integració.

En aquesta implementació, el servei d'aplicació coordina la consulta del preu i la recuperació de la cartera, mentre que la decisió de venda es manté dins del domini. El servei coordina, el domini decideix i l'adaptador integra.

Canvia el domini, però es manté la decisió arquitectònica: primer identifiquem la necessitat funcional, després definim el port i finalment resolem la integració. [pausa breu]

La substitució és viable mentre es mantinguin el contracte funcional i la semàntica que necessita l'aplicació.

Amb això podem tancar la classe recuperant les tres decisions essencials.

### Diapositiva 10. Idea clau i tancament - 0:55

Per tancar, voldria recuperar les tres decisions que hem treballat.

Primer, el cas d'ús expressa una necessitat funcional, no una tecnologia concreta. Segon, el port defineix el contracte que necessita l'aplicació. I tercer, l'adaptador encapsula la integració amb el sistema extern.

L'arquitectura hexagonal no elimina la dependència del món exterior ni evita tots els canvis. El que ens permet és situar aquesta dependència en una frontera explícita i localitzar millor l'impacte de l'evolució tecnològica.

Vull expressar el meu agraïment al TecnoCampus per aquest entorn docent i acadèmic, i al Dr. Josep Roure pel treball compartit en arquitectura de software i metodologies actives.

Moltes gràcies.

---

## Annex A. Demo opcional

Aquest annex no forma part del cos principal de la classe magistral de 20 minuts. Només s'ha d'utilitzar si el tribunal ho demana o si, després del tancament formal, es considera oportú mostrar breument la demostració.

### Annex A. Demo opcional - 1:30 a 2:30

Si es vol veure la translació pràctica d'aquesta idea, tinc preparada una demo molt breu amb HexaStock.

La demo no pretén impressionar per complexitat tècnica. Té una funció docent molt concreta: mostrar que podem mantenir el mateix cas d'ús, el mateix servei d'aplicació i el mateix domini, canviant només l'adaptador. [assenyalar les tres caixes]

En HexaStock, això es pot veure amb perfils diferents: un adaptador Finnhub, un adaptador Alpha Vantage o un adaptador mock. El port que veu el servei és el mateix: `StockPriceProviderPort`. La implementació concreta canvia segons la configuració.

Aquests noms són implementacions concretes. El que vull que observeu és que el contracte que veu el cas d'ús no canvia.

Aquest punt és especialment rellevant en docència. Permet fer proves amb un adaptador mock, sense claus reals, sense disponibilitat d'un proveïdor extern i sense contaminar el domini amb detalls tècnics. [mirada al tribunal]

El missatge de la demo és el mateix que hem treballat des del principi: canvia la infraestructura; el cas d'ús i el domini romanen estables.

Això no vol dir que l'arquitectura elimini el canvi. El canvi continua existint. El que fa una bona arquitectura és localitzar-lo.

---

## 2. Notes del presentador per diapositiva

### 1. Portada - 1:35

* Classe de DSI, no explicació completa d'arquitectura hexagonal.
* Informe incorporat a la documentació de la convocatòria: consultoria especialitzada per a la Generalitat/AGAUR.
* Trobada entre dimensió acadèmica i dimensió professional.
* Cas real com a base docent, sense entrar en detalls interns.
* Transició: ens centrarem en una decisió concreta, el port de sortida.

### 2. On som dins l'assignatura - 1:05

* Pla docent: 3r curs, 6 ECTS, assignatura obligatòria.
* Bloc: Clean Architecture i arquitectura hexagonal, ports i adaptadors, mapping, modularització i Domain-Driven Design.
* Justificar que la sessió encaixa en el temari.
* No panoràmica completa; focus en ports de sortida.
* Transició: objectiu mínim i entrada ràpida al cas real.

### 3. Objectiu de la sessió - 0:55

* Passar-hi ràpid: és una diapositiva pont, no una explicació teòrica llarga.
* Frase clau: el cas d'ús necessita una capacitat, no una tecnologia.
* Tres accions: identificar necessitat, definir port, implementar adaptadors.
* Anunciar metodologia: primer cas real, després abstracció arquitectònica.
* Transició: començar pel procediment de beca.

### 4. Avaluació econòmica - 1:45

* Primer entendre procediment, després tecnologia.
* Sol·licitud, expedient, requisits generals, econòmics, revisió, resolució.
* Renda i patrimoni com a decisió funcional.
* Evitar començar per SOAP/REST/PICA.
* Transició: quines dades externes fan falta?

### 5. Informació externa - 1:45

* Necessitats: renda, patrimoni, béns immobles.
* Fonts administratives: AEAT i Cadastre.
* Canal o plataforma d'interoperabilitat: PICA.
* No presentar PICA com a font original del mateix tipus.
* Frase clau: informació administrativa, no API concreta.
* Transició: si no separem, apareix dependència directa.

### 6. Dependència directa - 1:55

* El problema no és PICA; és l'acoblament directe.
* La cadena SOAP/XML -> PICA -> AEAT i/o Cadastre és el problema, no la solució.
* Cas d'ús coneix SOAP/XML, DTO, clients, errors tècnics.
* Evolució tecnològica travessa frontera.
* Tres riscos: acoblament, manteniment fràgil i més superfície d'impacte davant canvis externs.
* Transició: resposta arquitectònica, port de sortida.

### 7. Port i adaptador - 2:20

* Fer-la servir com a frontissa conceptual forta, no com a tràmit ràpid.
* Flux d'execució: el cas d'ús demana informació cap enfora.
* Dependència de codi: el cas d'ús depèn del port; l'adaptador implementa el port.
* Conceptualment, el port és un contracte; en Java, aquí s'expressa com una interfície.
* L'adaptador implementa aquesta interfície.
* La interfície funciona com a contracte desacoblador entre components.
* Inversió: la infraestructura depèn del port definit per l'aplicació.
* Transició: aplicar-ho al cas patrimonial.

### 8. Port patrimonial - 1:45

* Servei d'aplicació depèn de contractes propis.
* Adaptadors fora del nucli.
* PICA/JDBC/SOAP/REST són detalls externs que poden evolucionar.
* Fletxa correcta d'implementació: adaptador -> port.
* Transició: mateix patró en un domini financer.

### 9. Domini financer - 1:25

* Canvi explícit de domini: beca -> cartera d'inversió personal.
* Cas d'ús: vendre accions.
* Necessitat: preu actual de l'acció.
* Port de sortida: `StockPriceProviderPort`.
* Separar proveïdor de dades, API concreta, contracte tècnic i adaptador.
* Servei d'aplicació coordina consulta del preu i recuperació de cartera.
* Domini decideix; adaptador integra.
* Transició: conclusió tècnica.

### 10. Idea clau i tancament - 0:55

* Tres decisions: necessitat funcional, port i adaptador.
* El cas d'ús expressa una necessitat funcional.
* El port defineix el contracte de l'aplicació.
* L'adaptador encapsula la integració externa.
* Frase clau: l'arquitectura no elimina el canvi; el localitza.
* Agraïment breu al TecnoCampus i al Dr. Josep Roure.
* Tancar mirant el tribunal: `Moltes gràcies`.
* No obrir la demo després del tancament si el tribunal no la demana o si no hi ha temps clar.

### Annex A. Demo opcional - 1:30 a 2:30

* Annex posterior al tancament, no part del cos principal.
* Només mostrar si el tribunal ho demana o si hi ha temps explícit després de la classe.
* Mateix cas d'ús, servei i domini.
* Canvia l'adaptador.
* Perfils: Finnhub, Alpha Vantage, mock.
* Finnhub i Alpha Vantage són implementacions concretes; el contracte no canvia.
* Utilitat docent: proves amb adaptador mock i sense claus reals.
* Missatge: canvia la infraestructura; el cas d'ús i el domini romanen estables.
* No convertir l'annex en una segona explicació llarga.

---

## 3. Esquema de memorització


| Diapositiva | Missatge central                                 | Concepte tècnic imprescindible    | Connexió anterior       | Connexió següent                |
| ----------- | ------------------------------------------------ | ---------------------------------- | ------------------------ | --------------------------------- |
| 1           | Classe des d'un cas real acreditat               | Ports de sortida com a focus       | Inici                    | Situar dins DSI                   |
| 2           | La classe encaixa en el pla docent               | Clean Architecture, ports, adaptadors | Cas real + docència     | Objectiu breu                     |
| 3           | Objectiu: necessitat, port, adaptador            | Capacitat, no tecnologia           | Assignatura              | Cas funcional                     |
| 4           | Primer procediment, després tecnologia          | Necessitat funcional               | Objectiu                 | Fonts externes                    |
| 5           | El procediment necessita dades, no API           | Fonts administratives i interoperabilitat | Procediment              | Risc d'acoblament                 |
| 6           | La dependència directa fa fràgil el cas d'ús  | Acoblament tecnològic             | Dades externes           | Port de sortida                   |
| 7           | Port diu què; adaptador diu com                 | Contracte, interfície, inversió  | Problema                 | Aplicació al cas patrimonial     |
| 8           | AGAUR: contracte d'aplicació, no detall tècnic | Adaptador implementa port          | Port genèric            | Transferència al domini financer |
| 9           | Mateix patró en domini financer                 | Necessitat, port i adaptador       | Cas AGAUR                | Conclusió                         |
| 10          | L'arquitectura localitza el canvi                | Frontera explícita                 | Domini financer          | Tancament                         |
| Annex A     | Demostració opcional del canvi d'adaptador      | Adaptador substituïble            | Només si escau          | Preguntes / discussió            |

Mantra de memòria del cos principal: `context -> assignatura -> objectiu -> procediment -> informació externa -> acoblament -> port -> cas patrimonial -> domini financer -> conclusió`.

Mantra de l'annex opcional: `mateix cas d'ús -> mateix servei -> mateix domini -> canvia l'adaptador`.

---

## 4. Taula temporal actualitzada

La taula següent calcula només el cos principal. No inclou la demo opcional. Les estimacions de 125, 130 i 135 paraules per minut corresponen a lectura neta del text; en assaig real cal afegir pauses, respiració, mirada al tribunal, canvi de diapositiva i assenyalament dels elements visuals.

| Diapositiva | Bloc | Paraules | 125 ppm | 130 ppm | 135 ppm | Acumulat a 130 ppm |
| ----------- | ---- | -------- | ------- | ------- | ------- | ------------------ |
| 1 | Portada | 177 | 1:25 | 1:22 | 1:19 | 1:22 |
| 2 | On som dins l'assignatura | 45 | 0:22 | 0:21 | 0:20 | 1:43 |
| 3 | Objectiu de la sessió | 80 | 0:38 | 0:37 | 0:36 | 2:20 |
| 4 | Avaluació econòmica d'una beca | 168 | 1:21 | 1:18 | 1:15 | 3:38 |
| 5 | Informació externa | 211 | 1:41 | 1:37 | 1:34 | 5:15 |
| 6 | Dependència directa | 204 | 1:38 | 1:34 | 1:31 | 6:49 |
| 7 | Port de sortida i adaptador | 263 | 2:06 | 2:01 | 1:57 | 8:50 |
| 8 | Port patrimonial | 196 | 1:34 | 1:30 | 1:27 | 10:20 |
| 9 | Domini financer | 158 | 1:16 | 1:13 | 1:10 | 11:33 |
| 10 | Idea clau i tancament | 104 | 0:50 | 0:48 | 0:46 | 12:21 |
| **Total** | **Cos principal** | **1.606** | **12:51** | **12:21** | **11:54** | **12:21** |

Estimació anterior de treball: 18:15-19:00. Temps eliminat del cos principal: una diapositiva prevista d'1:25. Nova estimació prudent amb pauses reals: 16:00-17:15. Marge respecte dels 20 minuts: aproximadament 2:45-4:00, pendent de validació amb assaig oral real.

---

## 5. Pla d'entrenament comunicatiu

### Entrenament inicial

Llegeix el guió complet del cos principal en veu alta dues vegades sense cronòmetre. L'objectiu no és memoritzar, sinó detectar frases que no sonen naturals. Marca amb llapis les frases que vols dir exactament igual: la definició inicial, la distinció entre flux i dependència, la idea del port com a contracte representat amb una interfície, i la conclusió final. La resta pot ser més flexible.

Després fes una lectura cronometrada només del cos principal, sense annex. Ritme orientatiu: 125-135 paraules per minut, amb pauses reals. No intentis ocupar exactament vint minuts. Si el temps queda per sota del marge previst, no afegeixis contingut tècnic nou; amplia pauses, mirada, assenyalament de diagrames i transicions.

La demo opcional s'ha d'assajar per separat. No s'ha d'incloure en el cronòmetre principal.

### Entrenament intermedi

Practica per blocs, no tota la presentació sempre sencera:

* Bloc 1: diapositives 1-3, context, assignatura i objectiu breu.
* Bloc 2: diapositives 4-8, cas AGAUR, problema i port de sortida.
* Bloc 3: diapositives 9-10, domini financer, conclusió i agraïment.
* Annex: demo opcional, en una versió de màxim 2 minuts.

En cada bloc, treballa tres coses: mirada, pausa i gest. La mirada ha d'anar al tribunal en les frases conceptuals; a la pantalla només quan assenyales una part concreta. Les mans han d'ajudar a separar conceptes: una mà per `necessitat funcional`, l'altra per `tecnologia concreta`. Evita caminar mentre expliques una distinció fina; atura't, formula-la i continua.

### Assaig final

Fes com a mínim tres passades completes del cos principal:

* Primera passada: amb guió complet a la mà.
* Segona passada: només amb notes del presentador.
* Tercera passada: només amb l'esquema de memorització.

Grava una passada en vídeo. Revisa només quatre indicadors: si mires massa la pantalla, si acceleres a les diapositives 6-9, si les pauses existeixen de veritat, i si la frase `flux d'execució no és dependència de codi` queda clara.

Després grava una passada independent de l'annex opcional. La demo ha de poder explicar-se sense reobrir tota la classe. Ha de sonar com una verificació pràctica del que ja s'ha explicat, no com una secció nova.

### Ritme, veu i cos

Mantingues un to més lent a les diapositives 3, 6, 7 i 8, perquè són les conceptualment més importants. Fes pauses llargues després de: `capacitat, no tecnologia`; `això és el problema, no la solució`; `el problema no és consumir PICA`; `el port és un contracte`; `l'adaptador implementa el port`; `l'arquitectura situa la dependència externa en una frontera explícita`.

La postura ha de ser estable, amb els peus oberts a amplada d'espatlles. Usa les mans per marcar fronteres: dins/fora, port/adaptador, flux/dependència. Si et perds, torna al mantra: necessitat, port, adaptador. Aquesta triada recupera tota la presentació.

---

## 6. Advertiments finals

Risc de temps: les diapositives 4-9 poden allargar-se fàcilment. Si vas tard, no retallis la conclusió tècnica; retalla detall del cas AGAUR o de la transferència al domini financer.

Risc de demo: la demo no forma part del cos principal. No s'ha d'obrir abans del tancament. Després de `Moltes gràcies`, només s'ha d'activar si el tribunal ho demana o si explícitament hi ha marge per fer-ho. Si no, la classe ja està tancada correctament.

Risc conceptual: no diguis que PICA és el problema. Formula-ho sempre així: el problema és que el cas d'ús depengui directament dels detalls tècnics de la integració.

Risc de lectura de fletxes: no presentar totes les fletxes com si signifiquessin el mateix. Quan representen ús o flux d'execució, poden sortir del servei cap als ports. Quan representen implementació o dependència de codi, la lectura correcta és de l'adaptador cap al port.

Risc de port massa abstracte: concretar-lo amb Java sense fer-ne una definició dogmàtica. En aquesta explicació, el port és un contracte i en Java el representem amb una interfície; l'adaptador implementa aquesta interfície. Aquesta concreció evita que el concepte soni massa metafòric o purament teòric.

Risc documental: el pla docent oficial diu que la llengua de docència és l'anglès. Si surt la pregunta, la resposta prudent és que la defensa és en català i que la classe és una adaptació per al context del tribunal; el contingut docent és el mateix.

Risc de nom: el pla docent identifica `Josep Roure Alcobé`. Si a altres documents apareix `Roura`, cal unificar abans de tancar la PPTX.

Risc de confidencialitat: no mencionar endpoints, credencials, captures internes ni detalls operatius d'AGAUR. L'informe acredita l'activitat; la classe només usa el cas com a abstracció docent.
