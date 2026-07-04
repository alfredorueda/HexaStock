# Materials orals canònics per a la classe magistral de 20 minuts

Fonts canòniques utilitzades:

* `IMPORTANTE_MANUAL_defensa-dsi-ports-sortida-hexagonal.pptx`
* `informe-acreditatiu-agaur-arquitectura-hexagonal.pdf`
* `pla-docent-dsi-103322-2025-26.pdf`
* `HexaStock_SellStocks.vpd` i el seu render PNG equivalent
* `Get Your Hands Dirty on Clean Architecture.pdf`

Documents antics ignorats com a font: `outline.md`, `slide-by-slide.md`, `guió-oral-20-minuts.md` i altres esborranys previs.

Nota de versió: la demo queda fora del cos principal de la classe. El tancament formal és la diapositiva d'agraïment. La demo es conserva com a annex opcional, només per activar-la si el tribunal la demana o si hi ha temps explícitament disponible després del tancament.

---

## 1. Guió oral complet imprimible

Temps total previst del cos principal: 20:00 aproximadament, sense demo en viu.

La demo no forma part del temps canònic de la classe. S'ha de considerar un annex opcional posterior al tancament.

### Diapositiva 1. Portada - 1:35

Bon dia, membres del tribunal. [pausa breu, mirada al tribunal]

La classe que presento porta per títol `Disseny de ports de sortida en arquitectura hexagonal`, i se centra en el desacoblament entre casos d'ús, domini i APIs externes. [assenyalar el títol]

Abans d'entrar en el contingut tècnic, voldria fer una precisió de context. A la documentació de la convocatòria he incorporat un informe acreditatiu de la meva activitat de consultoria especialitzada per a la Generalitat de Catalunya, concretament l'Agència de Gestió d'Ajuts Universitaris i de Recerca. [pausa breu]

Ho explico perquè em sembla molt enriquidor plantejar aquesta sessió com una trobada entre dues dimensions.

D'una banda, la dimensió acadèmica: l'assignatura de Disseny de Sistemes d'Informació, on treballem conceptes com arquitectura hexagonal, ports, adaptadors i inversió de dependències.

I de l'altra, la dimensió professional: un cas real que ens permet veure com aquests conceptes s'aterren en reptes concrets d'arquitectura de software. [mirada al tribunal]

En vint minuts no seria rigorós intentar explicar tota l'arquitectura hexagonal. Per això em centraré en una decisió concreta: com dissenyar un port de sortida quan un cas d'ús necessita informació que es troba fora de l'aplicació.

### Diapositiva 2. On som dins l'assignatura - 1:05

La sessió se situa dins Disseny de Sistemes d'Informació, una assignatura de tercer curs, de 6 ECTS. [assenyalar la part esquerra]

El pla docent de l'assignatura explica que s'hi treballa una visió global d'arquitectures de sistemes d'informació: arquitectures per capes, arquitectures hexagonals i arquitectures basades en microserveis. Dins el bloc d'arquitectures hexagonals hi apareixen explícitament ports i adaptadors. [to calmat, explicatiu]


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

### Diapositiva 5. Quina informació externa necessita el procediment? - 1:55

Per avaluar renda i patrimoni, el procediment pot necessitar informació que no neix dins l'aplicació. [assenyalar la columna de necessitats]

En termes funcionals, podem parlar de renda familiar, patrimoni i béns immobles. En termes de fonts administratives, apareixen organismes i plataformes com l'AEAT, el Cadastre i PICA. [assenyalar la columna de fonts]

PICA és la Plataforma d'Integració i Col·laboració Administrativa de la Generalitat de Catalunya. Per a aquesta classe, no cal entrar en tots els detalls tècnics de PICA. El que ens interessa és que actua com a mecanisme corporatiu d'interoperabilitat administrativa.

La distinció clau és aquesta: el procediment necessita informació administrativa; no necessita, en el seu llenguatge propi, una API concreta. [pausa llarga; mirar el tribunal]

Això no vol dir que el mecanisme tècnic concret no sigui important. Ho és, i molt. Però encara no som en aquest nivell de decisió. En aquest moment, el que volem destacar és que el cas d'ús ha d'expressar una necessitat del procediment, no una dependència tecnològica concreta.

El cas d'ús hauria de poder formular-se així: `necessito informació patrimonial d'un ciutadà`. Encara no estem decidint si aquesta informació arribarà a través de PICA, d'un servei SOAP, d'una API REST, d'una base de dades corporativa o d'un altre mecanisme futur.

Quan aquesta separació no es respecta, apareix el problema arquitectònic que veurem ara.

### Diapositiva 6. Dependència directa entre procediment i integració - 2:05

Imaginem una situació en què el cas d'ús queda vinculat directament a la cadena tècnica: SOAP/XML, PICA, AEAT o Cadastre. [assenyalar la cadena central]

El problema no és consumir PICA. Ho remarco perquè és important. PICA, en aquest context, és una plataforma corporativa d'interoperabilitat. El problema arquitectònic apareix quan la lògica del procediment administratiu depèn directament dels detalls de la integració: estructures XML, clients SOAP, codis d'error tècnics o convencions d'una API concreta. [pausa breu]

Quan això passa, l'evolució tecnològica travessa la frontera del cas d'ús. Si un servei evoluciona, si canvia el contracte tècnic, si cal passar de SOAP a REST, o si apareix un altre mecanisme corporatiu, el canvi pot impactar codi que hauria d'estar expressant criteris del procediment.

Des del punt de vista de l'enginyeria de software, això incrementa tres riscos. Primer, acoblament tecnològic: el cas d'ús coneix massa detalls externs. Segon, fragilitat del manteniment: canvis d'infraestructura obliguen a revisar lògica d'aplicació. I tercer, risc de continuïtat operativa: una evolució tècnica pot afectar un procediment que hauria de romandre estable. [assenyalar les tres caixes]

La pregunta docent, per tant, és: com podem permetre que el cas d'ús necessiti dades externes, però sense dependre directament de la tecnologia que les proporciona?

La resposta és introduir un port de sortida.

### Diapositiva 7. Port de sortida i adaptador - 2:20

Abans d'aplicar-ho al cas patrimonial, fixem una distinció important.

Una cosa és el flux d'execució: el cas d'ús demana una informació i aquesta informació pot acabar venint d'un sistema extern.
[assenyalar la primera línia]

Una altra cosa és la dependència de codi: el cas d'ús depèn del port, i l'adaptador implementa aquest port.
[assenyalar la segona línia]

Per situar bé el concepte, quan aquí parlem d'un port no estem parlant d'un element físic ni d'un detall d'infraestructura. En termes de Java, aquest port s'expressa com una interfície.

És a dir, el cas d'ús no depèn directament d'una base de dades, d'una API externa o d'un client concret. El cas d'ús depèn d'una interfície que defineix què necessita: per exemple, obtenir una determinada informació econòmica, consultar un expedient o recuperar unes dades administratives.

Després, l'adaptador és la peça que implementa aquesta interfície i sap com anar realment a buscar aquella informació al sistema extern corresponent.

Això ens ajuda a entendre el valor arquitectònic de la interfície. Una interfície és una eina d'enginyeria que permet comunicar components sense acoblar-los directament entre ells. Defineix un contracte estable: què es necessita i què es retorna, però sense obligar el cas d'ús a conèixer com es resol tècnicament aquesta necessitat.

I això no és només una idea pròpia de la programació. És un principi molt general d'enginyeria: separar el contracte d'ús de la implementació concreta. Quan fem això, els components poden evolucionar, substituir-se o adaptar-se amb molt menys impacte sobre la resta del sistema.

Aquesta és la inversió de dependències: la infraestructura depèn del port definit per l'aplicació, no a l'inrevés.
[mirada al tribunal]

Amb aquesta idea clara, ara podem portar el patró al cas administratiu.

### Diapositiva 8. Port de sortida per a informació patrimonial - 1:55

Portem aquesta idea al cas administratiu. [assenyalar el diagrama]

El servei d'aplicació que avalua econòmicament una beca no hauria de conèixer directament els detalls de PICA, SOAP/XML, REST o JDBC. Hauria de dependre d'un contracte expressat en el llenguatge de l'aplicació. Per exemple: obtenir informació patrimonial, recuperar dades de renda o carregar l'expedient administratiu.

En el diagrama, el nucli de l'aplicació conté el servei d'aplicació i el model de domini. Al voltant hi ha ports: un port d'entrada per invocar el cas d'ús i ports de sortida per obtenir o persistir informació. Fora del nucli hi ha els adaptadors: el controlador REST, l'adaptador JDBC, l'adaptador d'interoperabilitat amb PICA o qualsevol altre mecanisme concret.

El punt tècnic més important és que l'adaptador de PICA o l'adaptador JDBC no són el centre de l'arquitectura. Són substituïbles. El que és estable és el contracte que el cas d'ús necessita. [pausa breu]

Aquesta distinció queda reflectida en la direcció de les fletxes. Quan indiquen `implemented by`, les fletxes van de l'adaptador cap al port. El servei usa el port. L'adaptador implementa el port. I el domini no coneix ni l'adaptador, ni PICA, ni SOAP, ni REST.

Aquesta mateixa estructura la podem transferir ara a un domini diferent: una aplicació de gestió d'una cartera d'inversió personal.

### Diapositiva 9. Transferència a domini financer - 1:35

Ara fem un canvi de domini. Ja no parlem d'una sol·licitud de beca, sinó d'una aplicació de gestió d'una cartera d'inversió personal. [assenyalar el títol]

Imaginem un cas d'ús molt concret: vendre accions d'una cartera. Per executar aquesta operació correctament, l'aplicació pot necessitar consultar el preu actual de l'acció que l'usuari vol vendre.

Ara bé, el cas d'ús no hauria de dependre directament d'un proveïdor concret de dades financeres, ni d'una API específica, ni del format tècnic de la resposta. El cas d'ús només hauria d'expressar la seva necessitat: `necessito obtenir el preu actual d'una acció concreta`.

Aquesta necessitat es pot representar amb un port de sortida. Aquest port no diu quin proveïdor s'ha de consultar, ni quin endpoint s'ha d'utilitzar, ni si la resposta vindrà en JSON, XML o qualsevol altre format. Només defineix una capacitat que l'aplicació necessita.

L'adaptador serà la peça que implementarà aquest port i coneixerà el detall tècnic concret: quin servei extern es consulta, com es fa la petició, quin format retorna la resposta i com es transforma aquesta informació perquè el cas d'ús la pugui utilitzar.

El patró és idèntic al cas AGAUR. En el cas administratiu, el procediment necessita informació patrimonial, no una tecnologia concreta. En el domini financer, el cas d'ús necessita un preu actual, no un proveïdor concret. [mirada al tribunal]

Per tant, el missatge docent és transferible: primer identifiquem la necessitat funcional; després definim el port; finalment implementem adaptadors.

### Diapositiva 10. Flux i codi essencial - 1:35

A partir d'aquí ja podem baixar al projecte concret HexaStock. Aquesta diapositiva mostra el flux temporal del cas d'ús de venda. [assenyalar el diagrama de seqüència]

El client o controlador invoca el port d'entrada, `PortfolioStockOperationsUseCase`. La implementació és el servei d'aplicació, `PortfolioStockOperationsService`. Aquest servei recupera el portfolio mitjançant un port de persistència, consulta el preu mitjançant el port de preus, i després crida el domini amb l'acció, la quantitat i el preu. [pausa breu]

La separació de responsabilitats és molt important. El servei coordina. El port obté informació. L'adaptador integra amb la infraestructura. Però la decisió de domini, la venda i el càlcul del resultat, no es resolen ni al controlador ni a l'adaptador de preus. Es resolen al model de domini, dins l'agregat `Portfolio`.

Dit de manera sintètica: l'adaptador pot saber com obtenir un preu; no hauria de decidir com es ven una acció dins la cartera. [pausa]

Aquesta idea ens porta al tancament: l'arquitectura no elimina la dependència respecte del món exterior, però sí que ens ajuda a ubicar-la en una frontera explícita.

### Diapositiva 11. Agraïment - 1:40

Per tancar, voldria recuperar aquesta idea final. [pausa breu]

L'arquitectura no elimina la dependència del món exterior. Les aplicacions reals necessiten bases de dades, APIs, plataformes corporatives, proveïdors externs i mecanismes d'integració. El que fa l'arquitectura hexagonal és convertir aquesta dependència en una frontera explícita, substituïble i comprovable.

Quan canvia un proveïdor, una API o una tecnologia, volem canviar l'adaptador. No volem reescriure el cas d'ús ni deformar el model de domini. [pausa]

Vull expressar el meu agraïment al TecnoCampus per un entorn que m'encoratja a créixer, tant en la docència com en l'àmbit acadèmic, al costat d'un equip del qual aprenc i amb qui comparteixo coneixement, responsabilitat i compromís amb la formació universitària. [pausa breu; mirar el tribunal]

També vull fer un agraïment especial al Dr. Josep Roure, company professor del TecnoCampus, amb qui he tingut l'oportunitat d'aprendre sobre arquitectura de software, metodologies actives d'aprenentatge i treball en equip. [mirada al tribunal]

Moltes gràcies.

---

## Annex A. Demo opcional

Aquest annex no forma part del cos principal de la classe magistral de 20 minuts. Només s'ha d'utilitzar si el tribunal ho demana o si, després del tancament formal, es considera oportú mostrar breument la demostració.

### Annex A. Demo opcional - 1:30 a 2:30

Si es vol veure la translació pràctica d'aquesta idea, tinc preparada una demo molt breu amb HexaStock.

La demo no pretén impressionar per complexitat tècnica. Té una funció docent molt concreta: mostrar que podem mantenir el mateix cas d'ús, el mateix servei d'aplicació i el mateix domini, canviant només l'adaptador. [assenyalar les three caixes]

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
* Bloc: clean/hexagonal, ports i adaptadors, mapping, mòduls, DDD.
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

### 5. Informació externa - 1:55

* Necessitats: renda, patrimoni, béns immobles.
* Fonts: AEAT, Cadastre, PICA.
* PICA com a interoperabilitat administrativa.
* Frase clau: informació administrativa, no API concreta.
* Transició: si no separem, apareix dependència directa.

### 6. Dependència directa - 2:05

* El problema no és PICA; és l'acoblament directe.
* Cas d'ús coneix SOAP/XML, DTOs, clients, errors tècnics.
* Evolució tecnològica travessa frontera.
* Tres riscos: acoblament, manteniment fràgil, continuïtat operativa.
* Transició: resposta arquitectònica, port de sortida.

### 7. Port i adaptador - 2:20

* Fer-la servir com a frontissa conceptual forta, no com a tràmit ràpid.
* Flux d'execució: el cas d'ús demana informació cap enfora.
* Dependència de codi: el cas d'ús depèn del port; l'adaptador implementa el port.
* En Java, el port s'expressa com una interfície.
* L'adaptador implementa aquesta interfície.
* La interfície funciona com a contracte desacoblador entre components.
* Inversió: la infraestructura depèn del port definit per l'aplicació.
* Transició: aplicar-ho al cas patrimonial.

### 8. Port patrimonial - 1:55

* Servei d'aplicació depèn de contractes propis.
* Adaptadors fora del nucli.
* PICA/JDBC/SOAP/REST són detalls substituïbles.
* Fletxa correcta d'implementació: adaptador -> port.
* Transició: mateix patró en un domini financer.

### 9. Domini financer - 1:35

* Canvi explícit de domini: beca -> cartera d'inversió personal.
* El diagrama correspon a HexaStock, però s'ha de llegir com a patró.
* Cas d'ús: vendre accions.
* Necessitat: preu actual de l'acció que l'usuari vol vendre.
* Port de sortida: defineix aquesta capacitat.
* Transició: concretar-ho en el flux de HexaStock.

### 10. Flux i codi - 1:35

* Diagrama de seqüència = flux temporal, no dependència de codi.
* Controller -> port d'entrada -> servei.
* Servei consulta ports i delega venda al domini.
* Domini decideix; adaptador integra.
* Frase clau: el servei coordina, el domini decideix, l'adaptador integra.
* Transició: conclusió i agraïment.

### 11. Agraïment - 1:40

* Arquitectura no elimina canvi; el localitza.
* Canviar adaptador, no cas d'ús ni domini.
* Agraïment TecnoCampus.
* Agraïment Dr. Josep Roure.
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
| 2           | La classe encaixa en el pla docent               | Clean/hexagonal, ports, adaptadors | Cas real + docència     | Objectiu breu                     |
| 3           | Objectiu: necessitat, port, adaptador            | Capacitat, no tecnologia           | Assignatura              | Cas funcional                     |
| 4           | Primer procediment, després tecnologia          | Necessitat funcional               | Objectiu                 | Fonts externes                    |
| 5           | El procediment necessita dades, no API           | Interoperabilitat administrativa   | Procediment              | Risc d'acoblament                 |
| 6           | La dependència directa fa fràgil el cas d'ús  | Acoblament tecnològic             | Dades externes           | Port de sortida                   |
| 7           | Port diu què; adaptador diu com                 | Interfície, contracte, inversió  | Problema                 | Aplicació al cas patrimonial     |
| 8           | AGAUR: contracte d'aplicació, no detall tècnic | Adaptador implementa port          | Port genèric            | Transferència al domini financer |
| 9           | Mateix patró en domini financer                 | Necessitat, port i adaptador       | Cas AGAUR                | Projecte concret                  |
| 10          | Servei coordina, domini decideix                 | Seqüència vs dependència        | Transferència genèrica | Conclusió                        |
| 11          | L'arquitectura localitza el canvi                | Frontera explícita                | Flux i codi              | Tancament                         |
| Annex A     | Demostració opcional del canvi d'adaptador      | Adaptador substituïble            | Només si escau          | Preguntes / discussió            |

Mantra de memòria del cos principal: `context -> assignatura -> objectiu breu -> procediment -> dada externa -> acoblament -> port -> AGAUR -> domini financer -> HexaStock -> conclusió`.

Mantra de l'annex opcional: `mateix cas d'ús -> mateix servei -> mateix domini -> canvia l'adaptador`.

---

## 4. Pla d'entrenament comunicatiu

### Entrenament inicial

Llegeix el guió complet del cos principal en veu alta dues vegades sense cronòmetre. L'objectiu no és memoritzar, sinó detectar frases que no sonen naturals. Marca amb llapis les frases que vols dir exactament igual: la definició inicial, la distinció entre flux i dependència, la idea del port com a interfície, i la conclusió final. La resta pot ser més flexible.

Després fes una lectura cronometrada només del cos principal, sense annex. Ritme orientatiu: 125-135 paraules per minut, amb pauses reals. Si surt per sobre de 21 minuts, retalla detall de les diapositives 5, 6, 8 o 9. Si surt per sota de 18:30, no afegeixis contingut tècnic nou; amplia pauses, mirada i transicions.

La demo opcional s'ha d'assajar per separat. No s'ha d'incloure en el cronòmetre principal.

### Entrenament intermedi

Practica per blocs, no tota la presentació sempre sencera:

* Bloc 1: diapositives 1-3, context, assignatura i objectiu breu.
* Bloc 2: diapositives 4-8, cas AGAUR, problema i port de sortida.
* Bloc 3: diapositives 9-11, domini financer, HexaStock, conclusió i agraïment.
* Annex: demo opcional, en una versió de màxim 2 minuts.

En cada bloc, treballa tres coses: mirada, pausa i gest. La mirada ha d'anar al tribunal en les frases conceptuals; a la pantalla només quan assenyales una part concreta. Les mans han d'ajudar a separar conceptes: una mà per `necessitat funcional`, l'altra per `tecnologia concreta`. Evita caminar mentre expliques una distinció fina; atura't, formula-la i continua.

### Assaig final

Fes com a mínim tres passades completes del cos principal:

* Primera passada: amb guió complet a la mà.
* Segona passada: només amb notes del presentador.
* Tercera passada: només amb l'esquema de memorització.

Grava una passada en vídeo. Revisa només quatre indicadors: si mires massa la pantalla, si acceleres a les diapositives 6-10, si les pauses existeixen de veritat, i si la frase `flux d'execució no és dependència de codi` queda clara.

Després grava una passada independent de l'annex opcional. La demo ha de poder explicar-se sense reobrir tota la classe. Ha de sonar com una verificació pràctica del que ja s'ha explicat, no com una secció nova.

### Ritme, veu i cos

Mantingues un to més lent a les diapositives 3, 6, 7 i 8, perquè són les conceptualment més importants. Fes pauses llargues després de: `capacitat, no tecnologia`; `el problema no és consumir PICA`; `en Java, el port s'expressa com una interfície`; `l'adaptador implementa el port`; `l'arquitectura no elimina el canvi, el localitza`.

La postura ha de ser estable, amb els peus oberts a amplada d'espatlles. Usa les mans per marcar fronteres: dins/fora, port/adaptador, flux/dependència. Si et perds, torna al mantra: necessitat, port, adaptador. Aquesta triada recupera tota la presentació.

---

## 5. Advertiments finals

Risc de temps: les diapositives 4-10 poden allargar-se fàcilment. Si vas tard, no retallis la conclusió; retalla detall del cas AGAUR, de la transferència al domini financer o de la lectura del diagrama de seqüència.

Risc de demo: la demo no forma part del cos principal. No s'ha d'obrir abans del tancament. Després de `Moltes gràcies`, només s'ha d'activar si el tribunal ho demana o si explícitament hi ha marge per fer-ho. Si no, la classe ja està tancada correctament.

Risc conceptual: no diguis que PICA és el problema. Formula-ho sempre així: el problema és que el cas d'ús depengui directament dels detalls tècnics de la integració.

Risc de lectura de fletxes: no presentar totes les fletxes com si signifiquessin el mateix. Quan representen ús o flux d'execució, poden sortir del servei cap als ports. Quan representen implementació o dependència de codi, la lectura correcta és de l'adaptador cap al port.

Risc de port massa abstracte: concretar-lo sempre amb Java. En aquesta explicació, el port s'expressa com una interfície i l'adaptador implementa aquesta interfície. Aquesta concreció evita que el concepte soni massa metafòric o purament teòric.

Risc documental: el pla docent oficial diu que la llengua de docència és l'anglès. Si surt la pregunta, la resposta prudent és que la defensa és en català i que la classe és una adaptació per al context del tribunal; el contingut docent és el mateix.

Risc de nom: el pla docent identifica `Josep Roure Alcobé`. Si a altres documents apareix `Roura`, cal unificar abans de tancar la PPTX.

Risc de confidencialitat: no mencionar endpoints, credencials, captures internes ni detalls operatius d'AGAUR. L'informe acredita l'activitat; la classe només usa el cas com a abstracció docent.
