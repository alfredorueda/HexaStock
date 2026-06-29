# Guió oral de 20 minuts

## 1. Portada, 1:00

Aquesta microlliçó s'emmarca dins l'assignatura Disseny de Sistemes d'Informació, en el bloc dedicat a arquitectures clean o hexagonals. En vint minuts no seria rigorós intentar explicar tota l'arquitectura hexagonal. Per això em centraré en una decisió concreta, però molt rellevant en sistemes reals: com dissenyar un port de sortida quan un cas d'ús necessita informació d'una API externa.

## 2. On som dins l'assignatura, 1:30

Aquesta sessió no és una explicació genèrica d'arquitectura. Està situada dins la guia docent oficial 2025/26 de Disseny de Sistemes d'Informació, assignatura de tercer curs, segon trimestre i 6 ECTS, en la qual consto com a part del professorat.

En una seqüència docent real, aquesta sessió vindria després d'haver treballat arquitectura per capes, limitacions dels models massa dependents de la infraestructura i introducció als ports i adaptadors. Avui no explicaré totes les peces del patró, sinó un punt específic: la relació entre casos d'ús, domini i sistemes externs.

## 3. Objectiu d'aprenentatge, 1:30

L'objectiu és que l'estudiant sigui capaç d'identificar quan una API externa comença a condicionar massa el disseny intern, definir un port de sortida en llenguatge de l'aplicació i implementar adaptadors substituïbles sense modificar el domini.

## 4. Cas funcional AGAUR, 2:30

En una experiència real de formació i consultoria per a AGAUR, vam treballar sobre una problemàtica habitual en sistemes d'informació corporatius de l'administració: procediments que necessiten informació de sistemes externs, però que no haurien de quedar acoblats als detalls tècnics d'aquestes integracions.

Ho cito de manera breu perquè hi ha una evidència documental: un informe acreditatiu de l'AGAUR sobre una activitat de 60 hores, entre maig i juliol de 2025, centrada en serveis REST i arquitectura hexagonal aplicada a sistemes d'informació. No és el focus de la classe, però sí que justifica que el problema que presento prové d'un context professional real.

Ho podem explicar amb un cas funcional molt comprensible: una sol·licitud de beca genera un expedient administratiu. Primer es comproven requisits generals. Després es revisen requisits econòmics de renda i patrimoni. Les fonts públiques d'AGAUR indiquen que, si aquests requisits econòmics no es compleixen, l'expedient no avança a la revisió acadèmica.

Per tant, abans de parlar de tecnologia, el punt funcional és aquest: el procediment ha de decidir si una persona compleix o no unes condicions econòmiques. Aquestes condicions inclouen renda familiar, patrimoni i, en determinats casos, béns immobles valorats amb valors cadastrals.

## 5. Dades externes i interoperabilitat, 2:00

El pas següent és entendre per què aquest procediment necessita informació externa. La mateixa documentació pública d'AGAUR explica que, quan es presenta la sol·licitud, s'autoritza la consulta de dades de renda i patrimoni de la unitat familiar. També indica que la informació econòmica la proporciona l'administració tributària corresponent.

Aquí apareixen els acrònims, però ara ja tenen sentit funcional. PICA és una plataforma corporativa d'interoperabilitat per accedir a informació d'altres administracions. El Cadastre, o Catastro en la denominació estatal, és un registre administratiu de béns immobles. I una addenda pública d'AGAUR sobre BOGA documenta processos d'interoperabilitat a través de PICA, incloent-hi certificats de dades de cadastre disponibles per AEAT.

La necessitat funcional, per tant, no és "cridar SOAP". La necessitat funcional és obtenir informació administrativa fiable per avaluar l'expedient.

## 6. Diagnòstic, 2:00

Aquí és on l'acoblament deixa de ser una preocupació abstracta i es converteix en un risc d'evolució. La formulació institucional prudent és aquesta: PICA continua essent una plataforma corporativa d'interoperabilitat; la documentació tècnica pública mostra integracions amb web services i antecedents SOAP; i, alhora, els principis arquitectònics del CTTI orienten cap a serveis desacoblats, REST/JSON, API Manager i EventHub/Kafka.

Amb aquesta lectura, la pregunta tècnica és clara: si la lògica administrativa de l'expedient coneix directament PICA, SOAP/XML, DTOs externs, mapping i errors tècnics, què passa quan evoluciona la plataforma, el protocol o el mecanisme d'interoperabilitat? La resposta és incòmoda: canvia massa codi. No només canvia un client tècnic. Canvien proves, fluxos de cas d'ús i parts del codi que haurien d'expressar criteris del procediment, no detalls d'infraestructura.

Aquest és el motiu arquitectònic de la consultoria. No es tractava només de "fer REST". Es tractava de desacoblar la lògica administrativa i els casos d'ús dels detalls concrets de PICA i de la tecnologia de la integració, perquè SOAP, REST, API Manager o qualsevol altre mecanisme quedin al costat de la infraestructura.

La literatura de clean i hexagonal architecture insisteix en una idea: les dependències han d'apuntar cap al nucli i el domini ha de poder evolucionar sense dependre dels detalls externs.

## 7. Solució amb port de sortida, 2:00

La solució és expressar la necessitat del cas d'ús com un port de sortida. El port no diu "crida PICA amb SOAP i XML". El port diu què necessita l'aplicació en el seu llenguatge. L'adaptador és qui resol com obtenir aquesta informació amb la tecnologia concreta.

Això no elimina la dependència del món exterior. La localitza en una peça que podem substituir, provar i mantenir de manera separada.

## 8. Interface i port, 2:00

En Java, l'ús d'interfícies per desacoblar components és una pràctica coneguda. Però l'arquitectura hexagonal fa un pas més: no utilitza contractes només com una bona pràctica local, sinó com una manera d'organitzar les fronteres del sistema.

Una interface pot desacoblar dues classes. Un port de sortida defineix què necessita l'aplicació del món exterior. No tot ús d'una interface és arquitectura hexagonal, però en una arquitectura hexagonal els ports solen materialitzar-se com a interfícies que expressen capacitats requerides o ofertes per l'aplicació.

## 9. Transferència a HexaStock, 2:30

Aquest patró no és exclusiu de l'administració pública. En un domini financer passa exactament el mateix. Un cas d'ús pot necessitar informació externa, com el preu actual d'una acció, però no hauria de dependre del proveïdor concret que dona aquesta informació.

Per a la part aplicada utilitzarem HexaStock, un projecte docent i professional que he desenvolupat per treballar DDD i arquitectura hexagonal amb un cas de domini financer. El cas de venda d'accions forma part de la documentació extensa del projecte i permet observar una decisió arquitectònica concreta: obtenir informació d'una API externa sense acoblar el cas d'ús ni el domini al proveïdor concret.

## 10. Flux i codi, 3:00

El controller invoca el port d'entrada `PortfolioStockOperationsUseCase`. El servei `PortfolioStockOperationsService` recupera el portfolio, demana el preu actual al port de sortida `StockPriceProviderPort`, obté un `Price` i delega la venda al domini.

El fragment important és aquest: primer `fetchStockPrice(ticker)`, després `price()`, i finalment `portfolio.sell(ticker, quantity, price)`. Aquí veiem la frontera. El servei coordina. El domini decideix. La infraestructura adapta.

Els adaptadors `FinhubStockPriceAdapter`, `AlphaVantageStockPriceAdapter` i `MockFinhubStockPriceAdapter` implementen el mateix port. Canviar de proveïdor és una decisió d'infraestructura, no una modificació de la regla de domini.

## 11. Tancament, 1:30

El que hem vist avui no és només una manera d'organitzar carpetes. És una decisió arquitectònica que protegeix la lògica de domini davant canvis tecnològics. Quan canvia el proveïdor extern, volem canviar l'adaptador, no el cas d'ús ni el model de domini.

L'arquitectura no evita el canvi. El que fa és localitzar-lo.
