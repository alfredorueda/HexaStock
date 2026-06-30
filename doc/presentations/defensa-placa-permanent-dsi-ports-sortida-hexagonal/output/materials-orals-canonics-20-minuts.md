# Materials orals canònics per a la classe magistral de 20 minuts

Fonts canòniques utilitzades:

- `IMPORTANTE_MANUAL_defensa-dsi-ports-sortida-hexagonal.pptx`
- `informe-acreditatiu-agaur-arquitectura-hexagonal.pdf`
- `pla-docent-dsi-103322-2025-26.pdf`
- `HexaStock_SellStocks.vpd` i el seu render PNG equivalent
- `Get Your Hands Dirty on Clean Architecture.pdf`

Documents antics ignorats com a font: `outline.md`, `slide-by-slide.md`, `guió-oral-20-minuts.md` i altres esborranys previs.

## 1. Diagnòstic general

La presentació té un fil narratiu acadèmicament defensable: situa la microlliçó dins Disseny de Sistemes d'Informació, formula un objectiu d'aprenentatge concret, parteix d'un cas real d'administració pública, diagnostica l'acoblament tecnològic, presenta el port de sortida i transfereix el patró a HexaStock. Aquesta estructura és adequada per a un tribunal perquè no presenta només tecnologia, sinó una decisió de disseny vinculada a docència, experiència professional i rigor arquitectònic.

El punt més fort és la connexió entre el pla docent i l'informe AGAUR. El pla docent situa l'assignatura a tercer curs, 6 ECTS, amb continguts de clean o hexagonal architecture, ports i adaptadors, mapping entre capes, organització en mòduls, microserveis i DDD. L'informe AGAUR acredita una activitat real de 60 hores, entre maig i juliol de 2025, centrada en serveis REST i arquitectura hexagonal aplicada a sistemes d'informació, amb refactorització de serveis SOAP cap a REST, reducció d'acoblament, separació de responsabilitats, proves i mantenibilitat.

El risc principal és que algunes diapositives poden confondre tres plans diferents: flux d'execució, dependència de codi i dependència arquitectònica. La recomanació del Dr. Josep Roure és conceptualment correcta: en ports de sortida, l'adaptador implementa el port definit per l'aplicació. Per tant, si la fletxa representa dependència de codi o realització d'interfície, ha d'anar de l'adaptador cap al port, no del port cap a l'adaptador. Si la fletxa representa flux d'execució, aleshores pot anar del servei cap al port, del port cap a l'adaptador i de l'adaptador cap al sistema extern. Les dues lectures són compatibles, però s'han de distingir visualment.

S'han revisat també els detalls formals del text visible de la presentació: `Generarlitat` s'ha corregit per `Generalitat`; `tecnología` per `tecnologia`; `necesita` per `necessita`; `d´ús` per `d'ús`; i s'ha regularitzat la numeració visible de les diapositives. La correcció `desacoplada` -> `desacoblada` ja s'havia aplicat a la diapositiva 2.

## 2. Millores proposades per diapositiva

**Diapositiva 1. Portada**

Mantindria el títol. És clar, tècnic i ajustat. Afegiria oralment, no necessàriament a la diapositiva, que l'informe AGAUR està incorporat a la documentació del tribunal com a evidència de la connexió entre docència i pràctica professional. No carregaria la portada amb més text.

**Diapositiva 2. On som dins l'assignatura**

La diapositiva funciona. Correcció aplicada: `desacoplada` s'ha substituït per `desacoblada`. També s'ha reforçat el vincle amb el pla docent amb una referència discreta: `Pla docent 103322: clean/hexagonal architecture, ports i adaptadors, mapping, mòduls i DDD`. No cal citar tot el pla docent; amb aquesta referència n'hi ha prou.

**Diapositives 3 i 4. Principi de disseny i objectiu d'aprenentatge**

Correcció aplicada. La diapositiva 3 es presenta ara com a `Principi de disseny`: el servei d'aplicació necessita una capacitat, no una tecnologia. La diapositiva 4 es presenta com a `Objectiu d'aprenentatge`: aplicar aquest principi en tres operacions observables. També s'ha corregit la numeració del peu de la diapositiva 4.

**Diapositiva 5. Avaluació econòmica d'una beca**

La diapositiva situa bé el cas funcional abans de la tecnologia. Correcció aplicada: la frase `no es pot concedir la beca` s'ha substituït per una formulació més institucional i prudent: `Quan no s'acredita el compliment dels requisits econòmics, l'expedient no obté una valoració favorable en aquesta fase.` El diagrama petit pot ser difícil de llegir; si no es retoca, cal assenyalar només el bloc d'avaluació econòmica.

**Diapositiva 6. Informació externa**

La idea és molt bona: necessitat funcional abans que API concreta. Correcció aplicada: `Generarlitat` s'ha substituït per `Generalitat` i la definició de PICA s'ha simplificat per evitar una nota massa llarga. La resta s'explica oralment.

**Diapositiva 7. Dependència directa**

Funciona com a diapositiva de problema. Recomanació: si no es vol aportar una font pública específica sobre `principis del CTTI`, suavitzar la frase inicial: `En arquitectures corporatives, l'orientació habitual és reduir l'acoblament i encapsular la integració amb sistemes externs`. Mantindria la frase final: `El problema no és consumir PICA; és que el procediment en depengui directament`.

**Diapositiva 8. Port de sortida i adaptador**

Aquí cal aplicar la millora de fletxes. La seqüència vertical actual explica bé el flux d'execució, però no la dependència de codi. Proposta visual:

- Fletxa continua i fina cap avall: `flux d'execució`.
- Fletxa discontinua o d'un altre color de l'adaptador cap al port: `dependència de codi / implementa`.
- Text lateral: `El servei usa el port; l'adaptador implementa el port`.

Això permet dir: el cas d'ús crida una capacitat definida pel port, però la implementació concreta depèn del contracte de l'aplicació.

**Diapositiva 9. Port de sortida per a informació patrimonial**

La frase superior és molt bona. El diagrama, però, manté `implemented by` amb fletxes que visualment semblen sortir del port cap a l'adaptador. Proposta: invertir aquestes fletxes o canviar-ne l'etiqueta a `implementa`, amb la direcció adaptador -> port. Mantenir les fletxes de `uses` des del servei cap als ports si es vol representar el flux d'execució.

**Diapositiva 10. Transferència a domini financer**

La transferència és clara. El diagrama HexaStock, com el d'AGAUR, hauria de distingir flux d'execució i dependència de codi. Especialment en `StockPriceProviderPort`, convé que `FinhubStockPriceAdapter`, `AlphaVantageStockPriceAdapter` i `MockFinhubStockPriceAdapter` apareguin com a implementacions que apunten cap al port. El missatge central és correcte i s'ha de mantenir: el cas d'ús necessita el preu actual, no un proveïdor concret.

**Diapositiva 11. Flux i codi essencial**

La diapositiva és forta perquè mostra el flux temporal. Afegiria oralment una frase preventiva: `Aquest diagrama de seqüència representa ordre d'execució, no direcció de dependències de codi`. Això evita contradiccions amb les diapositives 8-10.

**Diapositiva 12. Demo**

Funciona com a demostració controlada. Substituiria `consultoria empresarial` per `formació o consultoria professional`, perquè en aquest context la referència principal és administració pública i universitat. Si es fa demo real, cal tenir preparada una versió sense internet ni claus.

**Diapositiva 13. Agraïment**

Correcta i institucional. El tancament s'ha reformulat per evitar un to de comiat o de petició explícita: agraeix l'entorn que encoratja el creixement docent i acadèmic, en present, i manté el vincle amb l'equip docent. El pla docent identifica el professor com a Josep Roure Alcobé; per coherència documental, es pot fer servir `Dr. Josep Roure Alcobé`. Millor tancar mirant el tribunal, no llegint la diapositiva.

## 3. Guió oral complet imprimible

Temps total previst: 20:00.

### Diapositiva 1. Portada - 1:45

Bon dia, membres del tribunal. [pausa breu, mirada al tribunal]

La microlliçó que presento porta per títol `Disseny de ports de sortida en arquitectura hexagonal`, i se centra en el desacoblament entre casos d'ús, domini i APIs externes. [assenyalar el títol]

Abans d'entrar en el contingut tècnic, voldria fer una precisió de context. A la documentació del tribunal he incorporat un informe acreditatiu de la meva activitat de formació i consultoria especialitzada per a l'AGAUR, l'Agència de Gestió d'Ajuts Universitaris i de Recerca. Aquest informe acredita una activitat de 60 hores, desenvolupada entre maig i juliol de 2025, centrada en serveis REST, arquitectura hexagonal i sistemes d'informació. [pausa breu]

Ho explico al principi perquè aquesta sessió està plantejada deliberadament a partir d'una doble vinculació. D'una banda, és una microlliçó situada dins l'assignatura Disseny de Sistemes d'Informació, que imparteixo juntament amb el Dr. Josep Roure. De l'altra, parteix d'un cas professional real en l'àmbit de l'administració pública catalana. [mirada al tribunal]

La intenció no és revelar cap detall intern, ni cap endpoint, ni cap dada sensible. La intenció és docent: mostrar com conceptes que treballem a l'aula, com ports, adaptadors, inversió de dependències i separació entre domini, aplicació i infraestructura, apareixen també en problemes reals d'evolució de sistemes d'informació. [pausa]

En vint minuts no seria rigorós intentar explicar tota l'arquitectura hexagonal. Per això em centraré en una decisió concreta: com dissenyar un port de sortida quan un cas d'ús necessita informació que es troba fora de l'aplicació.

### Diapositiva 2. On som dins l'assignatura - 1:15

La sessió se situa dins Disseny de Sistemes d'Informació, una assignatura obligatòria de tercer curs, de 6 ECTS. [assenyalar la part esquerra]

El pla docent de l'assignatura explica que s'hi treballa una visió global d'arquitectures de sistemes d'informació: arquitectures per capes, arquitectures clean o hexagonals, i arquitectures basades en microserveis. Dins el bloc d'arquitectures clean o hexagonals hi apareixen explícitament ports i adaptadors, mapping entre capes i organització en mòduls. [to calmat, explicatiu]

Per tant, aquesta no és una sessió afegida artificialment al temari. És una microlliçó versemblant dins l'assignatura. [pausa breu]

El que farem avui és acotar molt el focus. No analitzarem tota l'arquitectura hexagonal. Ens centrarem en els ports de sortida: aquells ports que permeten que l'aplicació obtingui o persisteixi informació mitjançant sistemes externs, sense que el cas d'ús quedi lligat a una tecnologia concreta.

### Diapositiva 3. Principi de disseny - 1:10

La idea central de la sessió és aquesta: el servei d'aplicació necessita una capacitat, no una tecnologia. [pausa breu; mirar el tribunal]

Aquesta frase és important perquè ens obliga a canviar la pregunta. La pregunta no hauria de ser, d'entrada, `quina API crido?`, o `quin client SOAP o REST faig servir?`. La pregunta inicial hauria de ser: `quina capacitat necessita el cas d'ús per poder completar la seva responsabilitat?`

En arquitectura hexagonal, aquest canvi de pregunta és fonamental. El nucli de l'aplicació ha de parlar en el llenguatge del problema, no en el llenguatge accidental de la infraestructura. [assenyalar el centre del diagrama]

El llibre de Tom Hombergs sobre Clean Architecture ho formula en la mateixa direcció conceptual: les dependències han d'apuntar cap al nucli, i el domini no ha de quedar condicionat per frameworks, bases de dades, interfícies d'usuari o sistemes externs. No es tracta d'ignorar que aquests sistemes existeixen. Es tracta de posar-los al lloc arquitectònic que els correspon.

### Diapositiva 4. Objectiu d'aprenentatge - 0:50

Per convertir aquesta idea en aprenentatge observable, podem formular tres accions. [assenyalar les tres caixes]

Primer, identificar la necessitat o capacitat. Segon, definir un port que expressi aquesta necessitat en el llenguatge de l'aplicació. I tercer, implementar adaptadors que resolguin aquesta necessitat amb tecnologies concretes.

Dit d'una altra manera: l'estudiant no només hauria de memoritzar que existeixen ports i adaptadors. Hauria de poder mirar un cas d'ús, detectar on apareix l'acoblament tecnològic, i proposar una frontera arquitectònica que permeti substituir la infraestructura sense modificar el cas d'ús ni el domini. [pausa breu]

Ara ho veurem amb un cas funcional.

### Diapositiva 5. Avaluació econòmica d'una beca - 1:45

Abans de parlar d'arquitectura de software, cal entendre mínimament el procediment administratiu. [to més lent]

En un procediment de beca, una sol·licitud dona lloc a un expedient. Aquest expedient passa per diferents fases: requisits generals, requisits econòmics, revisió acadèmica i resolució. [assenyalar la seqüència de la dreta]

La part que ens interessa és l'avaluació econòmica. En aquest punt es comproven aspectes com la renda familiar i el patrimoni. Quan no s'acredita el compliment dels requisits econòmics, l'expedient no obté una valoració favorable en aquesta fase. [pausa breu]

La idea docent important és que aquí encara no hem parlat de SOAP, ni de REST, ni de PICA, ni de cap base de dades. Estem parlant de la necessitat funcional del procediment: per poder aplicar uns criteris econòmics, el sistema necessita dades fiables sobre renda i patrimoni.

Aquest pas és essencial. Si comencem directament per la tecnologia, correm el risc de construir el cas d'ús al voltant de la integració. En canvi, si comencem pel procediment, podem distingir entre la decisió administrativa que volem modelar i el mecanisme tècnic que ens proporciona la informació.

### Diapositiva 6. Quina informació externa necessita el procediment? - 1:55

Per avaluar renda i patrimoni, el procediment pot necessitar informació que no neix dins l'aplicació. [assenyalar la columna de necessitats]

En termes funcionals, podem parlar de renda familiar, patrimoni i béns immobles. En termes de fonts administratives, apareixen organismes i plataformes com l'AEAT, el Cadastre i PICA. [assenyalar la columna de fonts]

PICA és la Plataforma d'Integració i Col·laboració Administrativa de la Generalitat de Catalunya. Per a aquesta microlliçó, no cal entrar en tots els detalls tècnics de PICA. El que ens interessa és que actua com a mecanisme corporatiu d'interoperabilitat administrativa.

La distinció clau és aquesta: el procediment necessita informació administrativa; no necessita, en el seu llenguatge propi, una API concreta. [pausa llarga; mirar el tribunal]

Això no vol dir que l'API concreta no sigui important. Ho és, i molt. Però pertany a un altre nivell de decisió. El cas d'ús ha de poder dir: `necessito informació patrimonial avaluable`. L'adaptador ja resoldrà si aquesta informació arriba per SOAP, REST, PICA, un certificat, una base de dades corporativa o un mecanisme futur.

Quan aquesta separació no es respecta, apareix el problema arquitectònic que veurem ara.

### Diapositiva 7. Dependència directa entre procediment i integració - 2:00

Imaginem una situació en què el cas d'ús queda vinculat directament a la cadena tècnica: SOAP/XML, PICA, AEAT o Cadastre. [assenyalar la cadena central]

El problema no és consumir PICA. Ho remarco perquè és important. PICA, en aquest context, és una plataforma corporativa d'interoperabilitat. El problema arquitectònic apareix quan la lògica del procediment administratiu depèn directament dels detalls de la integració: DTOs externs, estructures XML, clients SOAP, codis d'error tècnics o convencions d'una API concreta. [pausa breu]

Quan això passa, l'evolució tecnològica travessa la frontera del cas d'ús. Si un servei evoluciona, si canvia el contracte tècnic, si cal passar de SOAP a REST, o si apareix un altre mecanisme corporatiu, el canvi pot impactar codi que hauria d'estar expressant criteris del procediment.

Des del punt de vista de l'enginyeria de software, això incrementa tres riscos. Primer, acoblament tecnològic: el cas d'ús coneix massa detalls externs. Segon, fragilitat del manteniment: canvis d'infraestructura obliguen a revisar lògica d'aplicació. I tercer, risc de continuïtat operativa: una evolució tècnica pot afectar un procediment que hauria de romandre estable. [assenyalar les tres caixes]

La pregunta docent, per tant, és: com podem permetre que el cas d'ús necessiti dades externes, però sense dependre directament de la tecnologia que les proporciona?

La resposta és introduir un port de sortida.

### Diapositiva 8. Port de sortida i adaptador - 1:25

Un port de sortida és un contracte definit des de l'aplicació. [pausa breu]

El cas d'ús o el servei d'aplicació diu: `necessito aquesta capacitat`. Per exemple: obtenir informació patrimonial, recuperar un expedient, desar una transacció o consultar el preu actual d'una acció. El port expressa aquest `què`. [assenyalar el port]

L'adaptador, en canvi, resol el `com`. És la peça que coneix la tecnologia concreta: SOAP, REST, JDBC, JPA, un client HTTP, una estructura XML, JSON o el mecanisme que sigui necessari. [assenyalar adaptador i sistema extern]

Aquí convé distingir dues fletxes. Si parlem de flux d'execució, el servei pot cridar el port i arribar finalment a l'adaptador. Però si parlem de dependència de codi, l'adaptador depèn del port, perquè l'adaptador implementa la interfície definida per l'aplicació. [mirada al tribunal]

Aquesta és la inversió de dependències: el nucli no depèn de la implementació concreta; la implementació concreta depèn del contracte que defineix el nucli.

### Diapositiva 9. Port de sortida per a informació patrimonial - 1:55

Portem aquesta idea al cas administratiu. [assenyalar el diagrama]

El servei d'aplicació que avalua econòmicament una beca no hauria de conèixer directament els detalls de PICA, SOAP/XML, REST o JDBC. Hauria de dependre d'un contracte expressat en el llenguatge de l'aplicació. Per exemple: obtenir informació patrimonial, recuperar dades de renda o carregar l'expedient administratiu.

En el diagrama, el nucli de l'aplicació conté el servei d'aplicació i el model de domini. Al voltant hi ha ports: un port d'entrada per invocar el cas d'ús i ports de sortida per obtenir o persistir informació. Fora del nucli hi ha els adaptadors: el controlador REST, l'adaptador JDBC, l'adaptador d'interoperabilitat amb PICA o qualsevol altre mecanisme concret.

El punt tècnic més important és que l'adaptador de PICA o l'adaptador JDBC no són el centre de l'arquitectura. Són substituïbles. El que és estable és el contracte que el cas d'ús necessita. [pausa breu]

Per això la proposta de revisar les fletxes és encertada. Si la fletxa indica `implementa`, hauria d'anar de l'adaptador cap al port. El servei usa el port. L'adaptador implementa el port. I el domini no coneix ni l'adaptador, ni PICA, ni SOAP, ni REST.

Aquesta mateixa estructura la podem transferir ara a un domini financer docent: HexaStock.

### Diapositiva 10. Transferència a domini financer - 1:40

HexaStock ens permet treballar el mateix patró en un domini diferent: la venda d'accions. [assenyalar el títol]

Aquí el cas d'ús és vendre accions d'una cartera. Per fer-ho correctament, el servei d'aplicació necessita el preu actual del ticker. Però el cas d'ús no hauria de dependre de Finnhub, Alpha Vantage, tokens, endpoints, JSON, clients HTTP o disponibilitat d'internet. [pausa breu]

El port de sortida és `StockPriceProviderPort`. Aquest port expressa la capacitat que l'aplicació necessita: obtenir el preu d'una acció. Després podem tenir diferents adaptadors que implementen aquest port: `FinhubStockPriceAdapter`, `AlphaVantageStockPriceAdapter` o un adaptador mock per a proves i demos controlades.

El patró és idèntic al cas AGAUR. En el cas administratiu, el procediment necessita informació patrimonial, no una tecnologia concreta. En HexaStock, el cas d'ús necessita un preu actual, no un proveïdor concret. [mirada al tribunal]

Per tant, el missatge docent és transferible: primer identifiquem la necessitat funcional; després definim el port; finalment implementem adaptadors.

### Diapositiva 11. Flux i codi essencial - 1:40

Aquesta diapositiva mostra el flux temporal del cas d'ús de venda. [assenyalar el diagrama de seqüència]

Convé aclarir que, en un diagrama de seqüència, les fletxes representen ordre d'execució, no dependències de codi. Això és perfectament compatible amb la inversió de dependències que acabem d'explicar.

El client o controlador invoca el port d'entrada, `PortfolioStockOperationsUseCase`. La implementació és el servei d'aplicació, `PortfolioStockOperationsService`. Aquest servei recupera el portfolio mitjançant un port de persistència, consulta el preu mitjançant `StockPriceProviderPort`, i després crida el domini: `portfolio.sell(ticker, quantity, price)`. [pausa breu]

La separació de responsabilitats és molt important. El servei coordina. El port obté informació. L'adaptador integra amb la infraestructura. Però la decisió de domini, la venda i el càlcul del resultat, no es resolen ni al controlador ni a l'adaptador de preus. Es resolen al model de domini, dins l'agregat `Portfolio`.

Dit de manera sintètica: l'adaptador pot saber com obtenir un preu; no hauria de decidir com es ven una acció dins la cartera. [pausa]

### Diapositiva 12. Demo - 1:40

La demo, en aquesta microlliçó, no pretén impressionar per complexitat tècnica. Té una funció docent molt concreta: mostrar que podem mantenir el mateix cas d'ús, el mateix servei d'aplicació i el mateix domini, canviant només l'adaptador. [assenyalar les tres caixes]

En HexaStock, això es pot veure amb perfils diferents: un adaptador Finnhub, un adaptador Alpha Vantage o un adaptador mock. El port que veu el servei és el mateix: `StockPriceProviderPort`. La implementació concreta canvia segons la configuració.

Aquest punt és especialment rellevant en docència. Permet fer proves sense dependre d'internet, sense claus reals, sense disponibilitat d'un proveïdor extern i sense contaminar el domini amb detalls tècnics. [mirada al tribunal]

El missatge de la demo és el mateix que hem treballat des del principi: canvia la infraestructura; el cas d'ús i el domini romanen estables.

Això no vol dir que l'arquitectura elimini el canvi. El canvi continua existint. El que fa una bona arquitectura és localitzar-lo.

### Diapositiva 13. Agraïment - 1:00

Per tancar, voldria recuperar aquesta idea final. [pausa breu]

L'arquitectura no elimina la dependència del món exterior. Les aplicacions reals necessiten bases de dades, APIs, plataformes corporatives, proveïdors externs i mecanismes d'integració. El que fa l'arquitectura hexagonal és convertir aquesta dependència en una frontera explícita, substituïble i comprovable.

Quan canvia un proveïdor, una API o una tecnologia, volem canviar l'adaptador. No volem reescriure el cas d'ús ni deformar el model de domini. [pausa]

Vull expressar el meu agraïment al TecnoCampus per un entorn que m'encoratja a créixer, tant en la docència com en l'àmbit acadèmic, al costat d'un equip del qual aprenc i amb qui comparteixo coneixement, responsabilitat i compromís amb la formació universitària. [pausa breu; mirar el tribunal]

També vull fer un agraïment especial al Dr. Josep Roure, company professor del TecnoCampus, amb qui he tingut l'oportunitat d'aprendre sobre arquitectura de software, metodologies actives d'aprenentatge i treball en equip. [mirada al tribunal]

Moltes gràcies.

## 4. Notes del presentador per diapositiva

### 1. Portada - 1:45

- Microlliçó de DSI, no explicació completa d'arquitectura hexagonal.
- Informe AGAUR incorporat al tribunal: activitat real de 60 hores, maig-juliol 2025.
- Connexió: docència amb Dr. Josep Roure + consultoria en sistemes d'informació.
- No revelar dades internes; cas real com a base docent.
- Transició: ens centrarem en una decisió concreta, el port de sortida.

### 2. On som dins l'assignatura - 1:15

- Pla docent: 3r curs, 6 ECTS, assignatura obligatòria.
- Bloc: clean/hexagonal, ports i adaptadors, mapping, mòduls, DDD.
- Justificar que la sessió encaixa en el temari.
- No panoràmica completa; focus en APIs externes desacoblades.
- Transició: formular l'objectiu d'aprenentatge.

### 3. Principi de disseny - 1:10

- Frase clau: capacitat, no tecnologia.
- Canviar la pregunta: no `quina API?`, sinó `quina necessitat?`.
- El nucli parla llenguatge del problema.
- Dependències cap al nucli, no cap a la infraestructura.
- Transició: convertir el principi en accions observables.

### 4. Objectiu d'aprenentatge - 0:50

- Identificar necessitat.
- Definir port.
- Implementar adaptadors.
- L'estudiant ha de detectar acoblament i proposar frontera.
- Transició: aplicar-ho a un cas funcional.

### 5. Avaluació econòmica - 1:45

- Primer entendre procediment, després tecnologia.
- Sol·licitud, expedient, requisits generals, econòmics, revisió, resolució.
- Renda i patrimoni com a decisió funcional.
- Evitar començar per SOAP/REST/PICA.
- Transició: quines dades externes fan falta?

### 6. Informació externa - 1:55

- Necessitats: renda, patrimoni, béns immobles.
- Fonts: AEAT, Cadastre, PICA.
- PICA com a interoperabilitat administrativa.
- Frase clau: informació administrativa, no API concreta.
- Transició: si no separem, apareix dependència directa.

### 7. Dependència directa - 2:00

- El problema no és PICA; és l'acoblament directe.
- Cas d'ús coneix SOAP/XML, DTOs, clients, errors tècnics.
- Evolució tecnològica travessa frontera.
- Tres riscos: acoblament, manteniment fràgil, continuïtat operativa.
- Transició: resposta arquitectònica, port de sortida.

### 8. Port i adaptador - 1:25

- Port = què necessita l'aplicació.
- Adaptador = com s'integra tècnicament.
- Distingir flux d'execució i dependència de codi.
- Inversió: adaptador implementa port; nucli no depèn d'adaptador.
- Transició: aplicar-ho al cas patrimonial.

### 9. Port patrimonial - 1:55

- Servei d'aplicació depèn de contractes propis.
- Adaptadors fora del nucli.
- PICA/JDBC/SOAP/REST són detalls substituïbles.
- Fletxa correcta d'implementació: adaptador -> port.
- Transició: mateix patró en HexaStock.

### 10. HexaStock - 1:40

- Domini financer: vendre accions.
- Necessitat: preu actual del ticker.
- Port: `StockPriceProviderPort`.
- Adaptadors: Finnhub, Alpha Vantage, mock.
- Transició: veure el flux i el codi essencial.

### 11. Flux i codi - 1:40

- Diagrama de seqüència = flux temporal, no dependència de codi.
- Controller -> port d'entrada -> servei.
- Servei consulta ports i delega venda al domini.
- Domini decideix; adaptador integra.
- Transició: demo controlada amb adaptador substituïble.

### 12. Demo - 1:40

- Mateix cas d'ús, servei i domini.
- Canvia l'adaptador.
- Perfils: Finnhub, Alpha Vantage, mock.
- Utilitat docent: proves sense internet ni claus.
- Transició: conclusió final.

### 13. Agraïment - 1:00

- Arquitectura no elimina canvi; el localitza.
- Canviar adaptador, no cas d'ús ni domini.
- Agraïment TecnoCampus.
- Agraïment Dr. Josep Roure.
- Tancar mirant el tribunal: `Moltes gràcies`.

## 5. Esquema de memorització

| Diapositiva | Missatge central | Concepte tècnic imprescindible | Connexió anterior | Connexió següent |
|---|---|---|---|---|
| 1 | Classe des d'un cas real acreditat | Ports de sortida com a focus | Inici | Situar dins DSI |
| 2 | La microlliçó encaixa en el pla docent | Clean/hexagonal, ports, adaptadors | Cas real + docència | Objectiu d'aprenentatge |
| 3 | Capacitat, no tecnologia | Dependències cap al nucli | Assignatura | Accions observables |
| 4 | Identificar, definir, implementar | Port com a contracte | Principi | Cas funcional |
| 5 | Primer procediment, després tecnologia | Necessitat funcional | Objectiu | Fonts externes |
| 6 | El procediment necessita dades, no API | Interoperabilitat administrativa | Procediment | Risc d'acoblament |
| 7 | La dependència directa fa fràgil el cas d'ús | Acoblament tecnològic | Dades externes | Port de sortida |
| 8 | Port diu què; adaptador diu com | Inversió de dependències | Problema | Aplicació al cas patrimonial |
| 9 | AGAUR: contracte d'aplicació, no detall tècnic | Adaptador implementa port | Port genèric | Transferència a HexaStock |
| 10 | Mateix patró en domini financer | `StockPriceProviderPort` | Cas AGAUR | Flux temporal |
| 11 | Servei coordina, domini decideix | Seqüència vs dependència | Diagrama HexaStock | Demo |
| 12 | Canvia infraestructura, no cas d'ús | Adaptador substituïble | Flux | Conclusió |
| 13 | L'arquitectura localitza el canvi | Frontera explícita | Demo | Tancament |

Mantra de memòria: `cas real -> assignatura -> objectiu -> procediment -> dada externa -> acoblament -> port -> adaptador -> HexaStock -> flux -> demo -> conclusió`.

## 6. Pla d'entrenament comunicatiu

**Entrenament inicial**

Llegeix el guió complet en veu alta dues vegades sense cronòmetre. L'objectiu no és memoritzar, sinó detectar frases que no sonen naturals. Marca amb llapis les frases que vols dir exactament igual: la definició inicial, la distinció entre flux i dependència, i la conclusió final. La resta pot ser més flexible.

Després fes una lectura cronometrada. Ritme orientatiu: 125-135 paraules per minut, amb pauses reals. Si surt per sobre de 22 minuts, retalla exemples; si surt per sota de 18:30, no afegeixis contingut tècnic, amplia pauses i transicions.

**Entrenament intermedi**

Practica per blocs, no tota la presentació sempre sencera:

- Bloc 1: diapositives 1-4, context i objectiu.
- Bloc 2: diapositives 5-9, cas AGAUR i port de sortida.
- Bloc 3: diapositives 10-13, HexaStock, demo i tancament.

En cada bloc, treballa tres coses: mirada, pausa i gest. La mirada ha d'anar al tribunal en les frases conceptuals; a la pantalla només quan assenyales una part concreta. Les mans han d'ajudar a separar conceptes: una mà per `necessitat funcional`, l'altra per `tecnologia concreta`. Evita caminar mentre expliques una distinció fina; atura't, formula-la i continua.

**Assaig final**

Fes com a mínim tres passades completes:

- Primera passada: amb guió complet a la mà.
- Segona passada: només amb notes del presentador.
- Tercera passada: només amb l'esquema de memorització.

Grava una passada en vídeo. Revisa només quatre indicadors: si mires massa la pantalla, si acceleres a les diapositives 7-11, si les pauses existeixen de veritat, i si la frase `flux d'execució no és dependència de codi` queda clara.

**Ritme, veu i cos**

Mantingues un to més lent a les diapositives 3, 7, 8 i 9, perquè són les conceptualment més importants. Fes pauses llargues després de: `capacitat, no tecnologia`; `el problema no és consumir PICA`; `l'adaptador implementa el port`; `l'arquitectura no elimina el canvi, el localitza`.

La postura ha de ser estable, amb els peus oberts a amplada d'espatlles. Usa les mans per marcar fronteres: dins/fora, port/adaptador, flux/dependència. Si et perds, torna al mantra: necessitat, port, adaptador. Aquesta triada recupera tota la presentació.

## 7. Advertiments finals

Risc de temps: les diapositives 5-11 poden allargar-se fàcilment. Si vas tard, no retallis la conclusió; retalla detall del cas AGAUR i de la demo.

Risc conceptual: no diguis que PICA és el problema. Formula-ho sempre així: el problema és que el cas d'ús depengui directament dels detalls tècnics de la integració.

Risc de fletxes: quan el tribunal vegi fletxes cap a fora, aclareix si representen flux d'execució. Quan parlis d'implementació o dependència de codi, la direcció correcta és adaptador cap al port.

Risc documental: el pla docent oficial diu que la llengua de docència és l'anglès. Si surt la pregunta, la resposta prudent és que la defensa és en català i que la microlliçó és una adaptació per al context del tribunal; el contingut docent és el mateix.

Risc de nom: el pla docent identifica `Josep Roure Alcobé`. Si a altres documents apareix `Roura`, cal unificar abans de tancar la PPTX.

Risc de confidencialitat: no mencionar endpoints, credencials, captures internes ni detalls operatius d'AGAUR. L'informe acredita l'activitat; la classe només usa el cas com a abstracció docent.
