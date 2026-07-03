# Materials orals canònics per a la classe magistral de 20 minuts

Fonts canòniques utilitzades:

- `IMPORTANTE_MANUAL_defensa-dsi-ports-sortida-hexagonal.pptx`
- `informe-acreditatiu-agaur-arquitectura-hexagonal.pdf`
- `pla-docent-dsi-103322-2025-26.pdf`
- `HexaStock_SellStocks.vpd` i el seu render PNG equivalent
- `Get Your Hands Dirty on Clean Architecture.pdf`

Documents antics ignorats com a font: `outline.md`, `slide-by-slide.md`, `guió-oral-20-minuts.md` i altres esborranys previs.

## 1. Diagnòstic general

La presentació té un fil narratiu acadèmicament defensable: situa la classe dins Disseny de Sistemes d'Informació, formula un objectiu d'aprenentatge concret, parteix d'un cas real d'administració pública, diagnostica l'acoblament tecnològic, presenta el port de sortida, transfereix el patró a un domini financer i el concreta en la demo de HexaStock. Aquesta estructura és adequada per a un tribunal perquè no presenta només tecnologia, sinó una decisió de disseny vinculada a docència, experiència professional i rigor arquitectònic.

El punt més fort és la connexió entre el pla docent i l'informe AGAUR. El pla docent situa l'assignatura a tercer curs, 6 ECTS, amb continguts de clean o hexagonal architecture, ports i adaptadors, mapping entre capes, organització en mòduls, microserveis i DDD. L'informe AGAUR acredita una activitat professional especialitzada vinculada a arquitectura hexagonal i sistemes d'informació.

El punt conceptual que cal preservar és la distinció entre tres plans diferents: flux d'execució, dependència de codi i dependència arquitectònica. Aquesta millora ja queda incorporada als diagrames principals: quan la fletxa representa implementació, apunta de l'adaptador cap al port; quan representa ús o flux d'execució, pot sortir del servei cap al port i cap al sistema extern. Les dues lectures són compatibles, però s'han de distingir oralment amb precisió.

S'han revisat també els detalls formals del text visible de la presentació: ortografia, accents, apòstrofs, cometes, noms propis i numeració visible de les diapositives. La terminologia de desacoblament ja queda formulada de manera correcta i coherent.

## 2. Guió oral complet imprimible

Temps total previst: 20:00.

### Diapositiva 1. Portada - 1:45

Bon dia, membres del tribunal. [pausa breu, mirada al tribunal]

La classe que presento porta per títol `Disseny de ports de sortida en arquitectura hexagonal`, i se centra en el desacoblament entre casos d'ús, domini i APIs externes. [assenyalar el títol]

Abans d'entrar en el contingut tècnic, voldria fer una precisió de context. A la documentació de la convocatòria he incorporat un informe acreditatiu de la meva activitat de consultoria especialitzada per a la Generalitat de Catalunya, específicamente l'Agència de Gestió d'Ajuts Universitaris i de Recerca. [pausa breu]

Ho explico perquè em semblava molt enriquidor plantejar aquesta sessió com una trobada entre dues dimensions.

D'una banda, la dimensió acadèmica: l'assignatura de Disseny de Sistemes d'Informació, on treballem conceptes com arquitectura hexagonal, ports, adaptadors i inversió de dependències.

I de l'altra, la dimensió professional: un cas real que ens permet veure com aquests conceptes s'aterren en reptes concrets d'arquitectura de software. [mirada al tribunal]

En vint minuts no seria rigorós intentar explicar tota l'arquitectura hexagonal. Per això em centraré en una decisió concreta: com dissenyar un port de sortida quan un cas d'ús necessita informació que es troba fora de l'aplicació.

### Diapositiva 2. On som dins l'assignatura - 1:10

La sessió se situa dins Disseny de Sistemes d'Informació, una assignatura obligatòria de tercer curs, de 6 ECTS. [assenyalar la part esquerra]

El pla docent de l'assignatura explica que s'hi treballa una visió global d'arquitectures de sistemes d'informació: arquitectures per capes, arquitectures clean o hexagonals, i arquitectures basades en microserveis. Dins el bloc d'arquitectures clean o hexagonals hi apareixen explícitament ports i adaptadors, mapping entre capes i organització en mòduls. [to calmat, explicatiu]

Per tant, aquesta no és una sessió afegida artificialment al temari. És una classe versemblant dins l'assignatura. [pausa breu]

El que farem avui és acotar molt el focus. No analitzarem tota l'arquitectura hexagonal. Ens centrarem en els ports de sortida: aquells ports que permeten que l'aplicació obtingui o persisteixi informació mitjançant sistemes externs, sense que el cas d'ús quedi lligat a una tecnologia concreta.

### Diapositiva 3. Objectiu de la sessió - 0:55

Abans d'entrar en el cas, deixo formulat l'objectiu de la sessió. [pausa breu]

El principi de treball és aquest: el cas d'ús necessita una capacitat, no una tecnologia. [mirar el tribunal]

Per tant, el que aprendrem és a fer tres operacions: identificar una necessitat externa del cas d'ús, expressar-la com a port de sortida i implementar-la mitjançant adaptadors substituïbles. [assenyalar les tres caixes]

No desenvoluparé encara tota l'abstracció. Primer veurem el problema en un cas real d'administració pública, i a partir d'aquí formularem la decisió arquitectònica. [pausa breu]

### Diapositiva 4. Avaluació econòmica d'una beca - 1:50

Abans de parlar d'arquitectura de software, cal entendre mínimament el procediment administratiu. [to més lent]

En un procediment de beca, una sol·licitud dona lloc a un expedient. Aquest expedient passa per diferents fases: requisits generals, requisits econòmics, revisió acadèmica i resolució. [assenyalar la seqüència de la dreta]

La part que ens interessa és l'avaluació econòmica. En aquest punt es comproven aspectes com la renda familiar i el patrimoni. Quan no s'acredita el compliment dels requisits econòmics, l'expedient no obté una valoració favorable en aquesta fase. [pausa breu]

La idea docent important és que aquí encara no hem parlat de SOAP, ni de REST, ni de PICA, ni de cap base de dades. Estem parlant de la necessitat funcional del procediment: per poder aplicar uns criteris econòmics, el sistema necessita dades fiables sobre renda i patrimoni.

Aquest pas és essencial. Si comencem directament per la tecnologia, correm el risc de construir el cas d'ús al voltant de la integració. En canvi, si comencem pel procediment, podem distingir entre la decisió administrativa que volem modelar i el mecanisme tècnic que ens proporciona la informació.

### Diapositiva 5. Quina informació externa necessita el procediment? - 2:00

Per avaluar renda i patrimoni, el procediment pot necessitar informació que no neix dins l'aplicació. [assenyalar la columna de necessitats]

En termes funcionals, podem parlar de renda familiar, patrimoni i béns immobles. En termes de fonts administratives, apareixen organismes i plataformes com l'AEAT, el Cadastre i PICA. [assenyalar la columna de fonts]

PICA és la Plataforma d'Integració i Col·laboració Administrativa de la Generalitat de Catalunya. Per a aquesta classe, no cal entrar en tots els detalls tècnics de PICA. El que ens interessa és que actua com a mecanisme corporatiu d'interoperabilitat administrativa.

La distinció clau és aquesta: el procediment necessita informació administrativa; no necessita, en el seu llenguatge propi, una API concreta. [pausa llarga; mirar el tribunal]

Això no vol dir que el mecanisme tècnic concret no sigui important. Ho és, i molt. Però encara no som en aquest nivell de decisió. En aquest moment, el que volem destacar és que el cas d'ús ha d'expressar una necessitat del procediment, no una dependència tecnològica concreta.

El cas d'ús hauria de poder formular-se així: `necessito informació patrimonial avaluable`. Encara no estem decidint si aquesta informació arribarà a través de PICA, d'un servei SOAP, d'una API REST, d'un certificat, d'una base de dades corporativa o d'un altre mecanisme futur.

Quan aquesta separació no es respecta, apareix el problema arquitectònic que veurem ara.

### Diapositiva 6. Dependència directa entre procediment i integració - 2:10

Imaginem una situació en què el cas d'ús queda vinculat directament a la cadena tècnica: SOAP/XML, PICA, AEAT o Cadastre. [assenyalar la cadena central]

El problema no és consumir PICA. Ho remarco perquè és important. PICA, en aquest context, és una plataforma corporativa d'interoperabilitat. El problema arquitectònic apareix quan la lògica del procediment administratiu depèn directament dels detalls de la integració: DTOs externs, estructures XML, clients SOAP, codis d'error tècnics o convencions d'una API concreta. [pausa breu]

Quan això passa, l'evolució tecnològica travessa la frontera del cas d'ús. Si un servei evoluciona, si canvia el contracte tècnic, si cal passar de SOAP a REST, o si apareix un altre mecanisme corporatiu, el canvi pot impactar codi que hauria d'estar expressant criteris del procediment.

Des del punt de vista de l'enginyeria de software, això incrementa tres riscos. Primer, acoblament tecnològic: el cas d'ús coneix massa detalls externs. Segon, fragilitat del manteniment: canvis d'infraestructura obliguen a revisar lògica d'aplicació. I tercer, risc de continuïtat operativa: una evolució tècnica pot afectar un procediment que hauria de romandre estable. [assenyalar les tres caixes]

La pregunta docent, per tant, és: com podem permetre que el cas d'ús necessiti dades externes, però sense dependre directament de la tecnologia que les proporciona?

La resposta és introduir un port de sortida.

### Diapositiva 7. Port de sortida i adaptador - 0:55

Abans d'aplicar-ho al cas patrimonial, fixem una distinció important.

Una cosa és el flux d'execució: el cas d'ús demana una informació i aquesta informació pot acabar venint d'un sistema extern. [assenyalar la primera línia]

Una altra cosa és la dependència de codi: el cas d'ús depèn del port, i l'adaptador implementa aquest port. [assenyalar la segona línia]

Aquesta és la inversió de dependències: la infraestructura depèn del port definit per l'aplicació, no a l'inrevés. [mirada al tribunal]

Amb aquesta idea clara, ara podem portar el patró al cas administratiu.

### Diapositiva 8. Port de sortida per a informació patrimonial - 2:05

Portem aquesta idea al cas administratiu. [assenyalar el diagrama]

El servei d'aplicació que avalua econòmicament una beca no hauria de conèixer directament els detalls de PICA, SOAP/XML, REST o JDBC. Hauria de dependre d'un contracte expressat en el llenguatge de l'aplicació. Per exemple: obtenir informació patrimonial, recuperar dades de renda o carregar l'expedient administratiu.

En el diagrama, el nucli de l'aplicació conté el servei d'aplicació i el model de domini. Al voltant hi ha ports: un port d'entrada per invocar el cas d'ús i ports de sortida per obtenir o persistir informació. Fora del nucli hi ha els adaptadors: el controlador REST, l'adaptador JDBC, l'adaptador d'interoperabilitat amb PICA o qualsevol altre mecanisme concret.

El punt tècnic més important és que l'adaptador de PICA o l'adaptador JDBC no són el centre de l'arquitectura. Són substituïbles. El que és estable és el contracte que el cas d'ús necessita. [pausa breu]

Aquesta distinció queda reflectida en la direcció de les fletxes. Quan indiquen `implemented by`, les fletxes van de l'adaptador cap al port. El servei usa el port. L'adaptador implementa el port. I el domini no coneix ni l'adaptador, ni PICA, ni SOAP, ni REST.

Aquesta mateixa estructura la podem transferir ara a un domini diferent: una aplicació de gestió d'una cartera d'inversió personal.

### Diapositiva 9. Transferència a domini financer - 1:45

Ara fem un canvi de domini. Ja no parlem d'una sol·licitud de beca, sinó d'una aplicació de gestió d'una cartera d'inversió personal. [assenyalar el títol]

Imaginem un cas d'ús molt concret: vendre accions d'una cartera. Per executar aquesta operació correctament, l'aplicació pot necessitar consultar el preu actual de l'acció que l'usuari vol vendre.

Ara bé, el cas d'ús no hauria de dependre directament d'un proveïdor concret de dades financeres, ni d'una API específica, ni del format tècnic de la resposta. El cas d'ús només hauria d'expressar la seva necessitat: `necessito obtenir el preu actual d'una acció concreta`.

Aquesta necessitat es pot representar amb un port de sortida. Aquest port no diu quin proveïdor s'ha de consultar, ni quin endpoint s'ha d'utilitzar, ni si la resposta vindrà en JSON, XML o qualsevol altre format. Només defineix una capacitat que l'aplicació necessita.

L'adaptador serà la peça que implementarà aquest port i coneixerà el detall tècnic concret: quin servei extern es consulta, com es fa la petició, quin format retorna la resposta i com es transforma aquesta informació perquè el cas d'ús la pugui utilitzar.

El patró és idèntic al cas AGAUR. En el cas administratiu, el procediment necessita informació patrimonial, no una tecnologia concreta. En el domini financer, el cas d'ús necessita un preu actual, no un proveïdor concret. [mirada al tribunal]

Per tant, el missatge docent és transferible: primer identifiquem la necessitat funcional; després definim el port; finalment implementem adaptadors.

### Diapositiva 10. Flux i codi essencial - 1:45

A partir d'aquí ja podem baixar al projecte concret de la demo: HexaStock. Aquesta diapositiva mostra el flux temporal del cas d'ús de venda. [assenyalar el diagrama de seqüència]

Convé aclarir que, en un diagrama de seqüència, les fletxes representen ordre d'execució, no dependències de codi. Això és perfectament compatible amb la inversió de dependències que acabem d'explicar.

El client o controlador invoca el port d'entrada, `PortfolioStockOperationsUseCase`. La implementació és el servei d'aplicació, `PortfolioStockOperationsService`. Aquest servei recupera el portfolio mitjançant un port de persistència, consulta el preu mitjançant el port de preus, i després crida el domini amb l'acció, la quantitat i el preu. [pausa breu]

La separació de responsabilitats és molt important. El servei coordina. El port obté informació. L'adaptador integra amb la infraestructura. Però la decisió de domini, la venda i el càlcul del resultat, no es resolen ni al controlador ni a l'adaptador de preus. Es resolen al model de domini, dins l'agregat `Portfolio`.

Dit de manera sintètica: l'adaptador pot saber com obtenir un preu; no hauria de decidir com es ven una acció dins la cartera. [pausa]

### Diapositiva 11. Demo - 1:45

La demo, en aquesta classe, no pretén impressionar per complexitat tècnica. Té una funció docent molt concreta: mostrar que podem mantenir el mateix cas d'ús, el mateix servei d'aplicació i el mateix domini, canviant només l'adaptador. [assenyalar les tres caixes]

En HexaStock, això es pot veure amb perfils diferents: un adaptador Finnhub, un adaptador Alpha Vantage o un adaptador mock. El port que veu el servei és el mateix: `StockPriceProviderPort`. La implementació concreta canvia segons la configuració.

Aquest punt és especialment rellevant en docència. Permet fer proves amb un adaptador mock, sense claus reals, sense disponibilitat d'un proveïdor extern i sense contaminar el domini amb detalls tècnics. [mirada al tribunal]

El missatge de la demo és el mateix que hem treballat des del principi: canvia la infraestructura; el cas d'ús i el domini romanen estables.

Això no vol dir que l'arquitectura elimini el canvi. El canvi continua existint. El que fa una bona arquitectura és localitzar-lo.

### Diapositiva 12. Agraïment - 1:20

Per tancar, voldria recuperar aquesta idea final. [pausa breu]

L'arquitectura no elimina la dependència del món exterior. Les aplicacions reals necessiten bases de dades, APIs, plataformes corporatives, proveïdors externs i mecanismes d'integració. El que fa l'arquitectura hexagonal és convertir aquesta dependència en una frontera explícita, substituïble i comprovable.

Quan canvia un proveïdor, una API o una tecnologia, volem canviar l'adaptador. No volem reescriure el cas d'ús ni deformar el model de domini. [pausa]

Vull expressar el meu agraïment al TecnoCampus per un entorn que m'encoratja a créixer, tant en la docència com en l'àmbit acadèmic, al costat d'un equip del qual aprenc i amb qui comparteixo coneixement, responsabilitat i compromís amb la formació universitària. [pausa breu; mirar el tribunal]

També vull fer un agraïment especial al Dr. Josep Roure, company professor del TecnoCampus, amb qui he tingut l'oportunitat d'aprendre sobre arquitectura de software, metodologies actives d'aprenentatge i treball en equip. [mirada al tribunal]

Moltes gràcies.

## 3. Notes del presentador per diapositiva

### 1. Portada - 1:45

- Classe de DSI, no explicació completa d'arquitectura hexagonal.
- Informe AGAUR incorporat al tribunal: activitat de formació i consultoria especialitzada.
- Connexió: docència amb Dr. Josep Roure + consultoria en sistemes d'informació.
- No revelar dades internes; cas real com a base docent.
- Transició: ens centrarem en una decisió concreta, el port de sortida.

### 2. On som dins l'assignatura - 1:10

- Pla docent: 3r curs, 6 ECTS, assignatura obligatòria.
- Bloc: clean/hexagonal, ports i adaptadors, mapping, mòduls, DDD.
- Justificar que la sessió encaixa en el temari.
- No panoràmica completa; focus en ports de sortida.
- Transició: objectiu mínim i entrada ràpida al cas real.

### 3. Objectiu de la sessió - 0:55

- Passar-hi ràpid: és una diapositiva pont, no una explicació teòrica llarga.
- Frase clau: el cas d'ús necessita una capacitat, no una tecnologia.
- Tres accions: identificar necessitat, definir port, implementar adaptadors.
- Anunciar metodologia: primer cas real, després abstracció arquitectònica.
- Transició: començar pel procediment de beca.

### 4. Avaluació econòmica - 1:50

- Primer entendre procediment, després tecnologia.
- Sol·licitud, expedient, requisits generals, econòmics, revisió, resolució.
- Renda i patrimoni com a decisió funcional.
- Evitar començar per SOAP/REST/PICA.
- Transició: quines dades externes fan falta?

### 5. Informació externa - 2:00

- Necessitats: renda, patrimoni, béns immobles.
- Fonts: AEAT, Cadastre, PICA.
- PICA com a interoperabilitat administrativa.
- Frase clau: informació administrativa, no API concreta.
- Transició: si no separem, apareix dependència directa.

### 6. Dependència directa - 2:10

- El problema no és PICA; és l'acoblament directe.
- Cas d'ús coneix SOAP/XML, DTOs, clients, errors tècnics.
- Evolució tecnològica travessa frontera.
- Tres riscos: acoblament, manteniment fràgil, continuïtat operativa.
- Transició: resposta arquitectònica, port de sortida.

### 7. Port i adaptador - 0:55

- Fer-la servir com a frontissa conceptual, no com a explicació llarga.
- Flux d'execució: el cas d'ús demana informació cap enfora.
- Dependència de codi: el cas d'ús depèn del port; l'adaptador implementa el port.
- Inversió: la infraestructura depèn del port definit per l'aplicació.
- Transició: aplicar-ho al cas patrimonial.

### 8. Port patrimonial - 2:05

- Servei d'aplicació depèn de contractes propis.
- Adaptadors fora del nucli.
- PICA/JDBC/SOAP/REST són detalls substituïbles.
- Fletxa correcta d'implementació: adaptador -> port.
- Transició: mateix patró en un domini financer.

### 9. Domini financer - 1:45

- Canvi explícit de domini: beca -> cartera d'inversió personal.
- Cas d'ús: vendre accions.
- Necessitat: preu actual de l'acció que l'usuari vol vendre.
- Port de sortida: defineix aquesta capacitat.
- Transició: ara sí, concretar-ho en HexaStock.

### 10. Flux i codi - 1:45

- Diagrama de seqüència = flux temporal, no dependència de codi.
- Controller -> port d'entrada -> servei.
- Servei consulta ports i delega venda al domini.
- Domini decideix; adaptador integra.
- Transició: demo controlada amb adaptador substituïble.

### 11. Demo - 1:45

- Mateix cas d'ús, servei i domini.
- Canvia l'adaptador.
- Perfils: Finnhub, Alpha Vantage, mock.
- Utilitat docent: proves amb adaptador mock i sense claus reals.
- Transició: conclusió final.

### 12. Agraïment - 1:20

- Arquitectura no elimina canvi; el localitza.
- Canviar adaptador, no cas d'ús ni domini.
- Agraïment TecnoCampus.
- Agraïment Dr. Josep Roure.
- Tancar mirant el tribunal: `Moltes gràcies`.

## 4. Esquema de memorització


| Diapositiva | Missatge central                                 | Concepte tècnic imprescindible    | Connexió anterior       | Connexió següent                |
| ----------- | ------------------------------------------------ | ---------------------------------- | ------------------------ | --------------------------------- |
| 1           | Classe des d'un cas real acreditat               | Ports de sortida com a focus       | Inici                    | Situar dins DSI                   |
| 2           | La classe encaixa en el pla docent               | Clean/hexagonal, ports, adaptadors | Cas real + docència     | Objectiu breu                     |
| 3           | Objectiu: necessitat, port, adaptador            | Capacitat, no tecnologia           | Assignatura              | Cas funcional                     |
| 4           | Primer procediment, després tecnologia          | Necessitat funcional               | Objectiu                 | Fonts externes                    |
| 5           | El procediment necessita dades, no API           | Interoperabilitat administrativa   | Procediment              | Risc d'acoblament                 |
| 6           | La dependència directa fa fràgil el cas d'ús  | Acoblament tecnològic             | Dades externes           | Port de sortida                   |
| 7           | Port diu què; adaptador diu com                 | Inversió de dependències         | Problema                 | Aplicació al cas patrimonial     |
| 8           | AGAUR: contracte d'aplicació, no detall tècnic | Adaptador implementa port          | Port genèric            | Transferència al domini financer |
| 9           | Mateix patró en domini financer                 | Necessitat, port i adaptador       | Cas AGAUR                | Projecte concret                  |
| 10          | Servei coordina, domini decideix                 | Seqüència vs dependència        | Transferència genèrica | Demo                              |
| 11          | Canvia infraestructura, no cas d'ús             | Adaptador substituïble            | Flux                     | Conclusió                        |
| 12          | L'arquitectura localitza el canvi                | Frontera explícita                | Demo                     | Tancament                         |

Mantra de memòria: `context -> assignatura -> objectiu breu -> procediment -> dada externa -> acoblament -> port -> AGAUR -> domini financer -> HexaStock -> demo -> conclusió`.

## 5. Pla d'entrenament comunicatiu

**Entrenament inicial**

Llegeix el guió complet en veu alta dues vegades sense cronòmetre. L'objectiu no és memoritzar, sinó detectar frases que no sonen naturals. Marca amb llapis les frases que vols dir exactament igual: la definició inicial, la distinció entre flux i dependència, i la conclusió final. La resta pot ser més flexible.

Després fes una lectura cronometrada. Ritme orientatiu: 125-135 paraules per minut, amb pauses reals. Si surt per sobre de 22 minuts, retalla exemples; si surt per sota de 18:30, no afegeixis contingut tècnic, amplia pauses i transicions.

**Entrenament intermedi**

Practica per blocs, no tota la presentació sempre sencera:

- Bloc 1: diapositives 1-3, context, assignatura i objectiu breu.
- Bloc 2: diapositives 4-8, cas AGAUR, problema i port de sortida.
- Bloc 3: diapositives 9-12, domini financer, HexaStock, demo i tancament.

En cada bloc, treballa tres coses: mirada, pausa i gest. La mirada ha d'anar al tribunal en les frases conceptuals; a la pantalla només quan assenyales una part concreta. Les mans han d'ajudar a separar conceptes: una mà per `necessitat funcional`, l'altra per `tecnologia concreta`. Evita caminar mentre expliques una distinció fina; atura't, formula-la i continua.

**Assaig final**

Fes com a mínim tres passades completes:

- Primera passada: amb guió complet a la mà.
- Segona passada: només amb notes del presentador.
- Tercera passada: només amb l'esquema de memorització.

Grava una passada en vídeo. Revisa només quatre indicadors: si mires massa la pantalla, si acceleres a les diapositives 6-10, si les pauses existeixen de veritat, i si la frase `flux d'execució no és dependència de codi` queda clara.

**Ritme, veu i cos**

Mantingues un to més lent a les diapositives 3, 6, 7 i 8, perquè són les conceptualment més importants. Fes pauses llargues després de: `capacitat, no tecnologia`; `el problema no és consumir PICA`; `l'adaptador implementa el port`; `l'arquitectura no elimina el canvi, el localitza`.

La postura ha de ser estable, amb els peus oberts a amplada d'espatlles. Usa les mans per marcar fronteres: dins/fora, port/adaptador, flux/dependència. Si et perds, torna al mantra: necessitat, port, adaptador. Aquesta triada recupera tota la presentació.

## 6. Advertiments finals

Risc de temps: les diapositives 4-10 poden allargar-se fàcilment. Si vas tard, no retallis la conclusió; retalla detall del cas AGAUR i de la demo.

Risc conceptual: no diguis que PICA és el problema. Formula-ho sempre així: el problema és que el cas d'ús depengui directament dels detalls tècnics de la integració.

Risc de lectura de fletxes: no presentar totes les fletxes com si signifiquessin el mateix. Quan representen ús o flux d'execució, poden sortir del servei cap als ports. Quan representen implementació o dependència de codi, la lectura correcta és de l'adaptador cap al port.

Risc documental: el pla docent oficial diu que la llengua de docència és l'anglès. Si surt la pregunta, la resposta prudent és que la defensa és en català i que la classe és una adaptació per al context del tribunal; el contingut docent és el mateix.

Risc de nom: el pla docent identifica `Josep Roure Alcobé`. Si a altres documents apareix `Roura`, cal unificar abans de tancar la PPTX.

Risc de confidencialitat: no mencionar endpoints, credencials, captures internes ni detalls operatius d'AGAUR. L'informe acredita l'activitat; la classe només usa el cas com a abstracció docent.
