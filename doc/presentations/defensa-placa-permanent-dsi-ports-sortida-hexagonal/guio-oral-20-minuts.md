# Guió oral de 20 minuts

## 1. Portada, 1:00

Aquesta microlliçó s'emmarca dins l'assignatura Disseny de Sistemes d'Informació, en el bloc dedicat a arquitectures clean o hexagonals. En vint minuts no seria rigorós intentar explicar tota l'arquitectura hexagonal. Per això em centraré en una decisió concreta, però molt rellevant en sistemes reals: com dissenyar un port de sortida quan un cas d'ús necessita informació d'una API externa.

## 2. On som dins l'assignatura, 1:30

Aquesta sessió no és una explicació genèrica d'arquitectura. Està situada dins la guia docent oficial 2025/26 de Disseny de Sistemes d'Informació, assignatura de tercer curs, segon trimestre i 6 ECTS, en la qual consto com a part del professorat.

En una seqüència docent real, aquesta sessió vindria després d'haver treballat arquitectura per capes, limitacions dels models massa dependents de la infraestructura i introducció als ports i adaptadors. Avui no explicaré totes les peces del patró, sinó un punt específic: la relació entre casos d'ús, domini i sistemes externs.

## 3. Objectiu d'aprenentatge, 1:30

L'objectiu és que l'estudiant sigui capaç d'identificar quan una API externa comença a condicionar massa el disseny intern, definir un port de sortida en llenguatge de l'aplicació i implementar adaptadors substituïbles sense modificar el domini.

## 4. Problema real AGAUR, 2:30

En una experiència real de formació i consultoria per a AGAUR, vam treballar sobre una problemàtica habitual en sistemes d'informació corporatius de l'administració: procediments que necessiten informació de sistemes externs, però que no haurien de quedar acoblats als detalls tècnics d'aquestes integracions.

Ho cito de manera breu perquè hi ha una evidència documental: un informe acreditatiu de l'AGAUR sobre una activitat de 60 hores, entre maig i juliol de 2025, centrada en serveis REST i arquitectura hexagonal aplicada a sistemes d'informació. No és el focus de la classe, però sí que justifica que el problema que presento prové d'un context professional real.

Ho podem explicar amb un cas funcional molt comprensible: una sol·licitud de beca genera un expedient administratiu, s'avaluen requisits i condicions patrimonials, i finalment es prepara una proposta de resolució, de concessió o de denegació. Les regles del procediment poden ser complexes, però no haurien de quedar barrejades amb clients tècnics, XML, DTOs externs o tractament d'errors de la integració.

Quan apareixen acrònims, els situo només com a context. PICA és una plataforma d'interoperabilitat per consultar o intercanviar dades entre administracions. El Cadastre, o Catastro en la denominació estatal, és un registre administratiu de béns immobles. SOAP/XML és un mecanisme de missatgeria i format de dades propi d'integracions entre sistemes. El punt important, des del punt de vista arquitectònic, no és el detall administratiu concret, sinó la frontera entre cas d'ús, domini i infraestructura.

## 5. Diagnòstic, 2:00

Aquí és on l'acoblament deixa de ser una preocupació abstracta i es converteix en un risc d'evolució. La formulació institucional prudent és aquesta: PICA continua essent una plataforma corporativa d'interoperabilitat; la documentació tècnica pública mostra integracions amb web services i antecedents SOAP; i, alhora, els principis arquitectònics del CTTI orienten cap a serveis desacoblats, REST/JSON, API Manager i EventHub/Kafka.

Amb aquesta lectura, la pregunta tècnica és clara: si la lògica administrativa de l'expedient coneix directament PICA, SOAP/XML, DTOs externs, mapping i errors tècnics, què passa quan evoluciona la plataforma, el protocol o el mecanisme d'interoperabilitat? La resposta és incòmoda: canvia massa codi. No només canvia un client tècnic. Canvien proves, fluxos de cas d'ús i parts del codi que haurien d'expressar criteris del procediment, no detalls d'infraestructura.

Aquest és el motiu arquitectònic de la consultoria. No es tractava només de "fer REST". Es tractava de desacoblar la lògica administrativa i els casos d'ús dels detalls concrets de PICA i de la tecnologia de la integració, perquè SOAP, REST, API Manager o qualsevol altre mecanisme quedin al costat de la infraestructura.

La literatura de clean i hexagonal architecture insisteix en una idea: les dependències han d'apuntar cap al nucli i el domini ha de poder evolucionar sense dependre dels detalls externs.

## 6. Solució amb port de sortida, 2:00

La solució és expressar la necessitat del cas d'ús com un port de sortida. El port no diu "crida PICA amb SOAP i XML". El port diu què necessita l'aplicació en el seu llenguatge. L'adaptador és qui resol com obtenir aquesta informació amb la tecnologia concreta.

Això no elimina la dependència del món exterior. La localitza en una peça que podem substituir, provar i mantenir de manera separada.

## 7. Interface i port, 2:00

En Java, l'ús d'interfícies per desacoblar components és una pràctica coneguda. Però l'arquitectura hexagonal fa un pas més: no utilitza contractes només com una bona pràctica local, sinó com una manera d'organitzar les fronteres del sistema.

Una interface pot desacoblar dues classes. Un port de sortida defineix què necessita l'aplicació del món exterior. No tot ús d'una interface és arquitectura hexagonal, però en una arquitectura hexagonal els ports solen materialitzar-se com a interfícies que expressen capacitats requerides o ofertes per l'aplicació.

## 8. Transferència a HexaStock, 2:30

Aquest patró no és exclusiu de l'administració pública. En un domini financer passa exactament el mateix. Un cas d'ús pot necessitar informació externa, com el preu actual d'una acció, però no hauria de dependre del proveïdor concret que dona aquesta informació.

Per a la part aplicada utilitzarem HexaStock, un projecte docent i professional que he desenvolupat per treballar DDD i arquitectura hexagonal amb un cas de domini financer. El cas de venda d'accions forma part de la documentació extensa del projecte i permet observar una decisió arquitectònica concreta: obtenir informació d'una API externa sense acoblar el cas d'ús ni el domini al proveïdor concret.

## 9. Flux i codi, 3:00

El controller invoca el port d'entrada `PortfolioStockOperationsUseCase`. El servei `PortfolioStockOperationsService` recupera el portfolio, demana el preu actual al port de sortida `StockPriceProviderPort`, obté un `Price` i delega la venda al domini.

El fragment important és aquest: primer `fetchStockPrice(ticker)`, després `price()`, i finalment `portfolio.sell(ticker, quantity, price)`. Aquí veiem la frontera. El servei coordina. El domini decideix. La infraestructura adapta.

Els adaptadors `FinhubStockPriceAdapter`, `AlphaVantageStockPriceAdapter` i `MockFinhubStockPriceAdapter` implementen el mateix port. Canviar de proveïdor és una decisió d'infraestructura, no una modificació de la regla de domini.

## 10. Tancament, 2:00

El que hem vist avui no és només una manera d'organitzar carpetes. És una decisió arquitectònica que protegeix la lògica de domini davant canvis tecnològics. Quan canvia el proveïdor extern, volem canviar l'adaptador, no el cas d'ús ni el model de domini.

L'arquitectura no evita el canvi. El que fa és localitzar-lo.
