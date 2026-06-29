# Diapositiva a diapositiva

## 1. Portada

**Títol**
Disseny de ports de sortida en arquitectura hexagonal: desacoblament entre casos d'ús, domini i APIs externes

**Subtítol**
Una microlliçó de Disseny de Sistemes d'Informació a partir d'un cas real de consultoria i del projecte HexaStock

**Elements**
- Alfredo Rueda Unsain
- Defensa de plaça de professorat permanent
- TecnoCampus
- Classe magistral reduïda, màxim 20 minuts
- Logotip oficial TecnoCampus / UPF

**Notes**
Obrir situant la sessió dins l'assignatura i marcant el focus. No prometre una visió completa de l'arquitectura hexagonal.

## 2. On som dins l'assignatura

**Text de diapositiva**
Aquesta sessió forma part del bloc d'arquitectures clean o hexagonals dins Disseny de Sistemes d'Informació.

**Ancoratge oficial**
- Guia docent oficial 2025/26.
- Assignatura 103322, tercer curs, segon trimestre, 6 ECTS.
- Professorat: Josep Roure Alcobé i Alfredo Rueda Unsain.

**Seqüència**
1. Arquitectura per capes i limitacions
2. Introducció a Clean / Hexagonal Architecture
3. Ports i adaptadors
4. Ports de sortida per a integracions externes
5. Testing, mocks i substitució d'infraestructura
6. Microserveis i DDD

**Notes**
Cal explicar que la sessió no és una peça aliena al pla docent, sinó una microlliçó situada dins una assignatura que Alfredo ja imparteix i que la guia oficial vincula explícitament amb arquitectures clean o hexagonals, ports i adaptadors, mapping, mòduls, microserveis i DDD.

## 3. Objectiu d'aprenentatge

**Text de diapositiva**
En acabar la sessió, l'estudiant serà capaç de dissenyar un port de sortida orientat al llenguatge de l'aplicació, implementar adaptadors intercanviables per a APIs externes i justificar com aquesta decisió redueix l'acoblament entre casos d'ús, domini i infraestructura.

**Tres resultats**
- Identificar el risc d'acoblament amb APIs externes.
- Dissenyar un port de sortida en llenguatge de l'aplicació.
- Substituir adaptadors sense modificar el domini.

**Notes**
El criteri d'èxit no és memoritzar una definició, sinó raonar una frontera arquitectònica.

## 4. Cas funcional: avaluació econòmica d'una beca

**Visual**
`diagrams/rendered/agaur-functional-scholarship.svg`

**Text de diapositiva**
Una sol·licitud de beca genera un expedient. Després dels requisits generals, AGAUR revisa requisits econòmics de renda i patrimoni. Si no es compleixen, l'expedient no avança a la revisió acadèmica.

**Punts**
- Renda familiar.
- Patrimoni de la unitat familiar.
- Finques urbanes i rústiques amb valors cadastrals.
- Proposta de concessió o denegació.

**Fonts públiques visibles**
AGAUR requisits econòmics i BOE 2025-2026 sobre llindars de renda i patrimoni.

**Notes**
Començar pel cas funcional abans d'entrar en tecnologia. Explicar que la decisió administrativa no és trivial: cal avaluar renda, patrimoni, llindars i condicions de la unitat familiar. Aquí encara no cal parlar de SOAP ni de ports; només cal entendre què necessita resoldre el procediment.

## 5. Quina informació externa necessita el procediment?

**Visual**
`diagrams/rendered/agaur-data-interoperability.svg`

**Text de diapositiva**
La necessitat funcional no és consumir una API concreta. És obtenir informació administrativa fiable per avaluar si una sol·licitud compleix els requisits econòmics.

**Punts**
- La sol·licitud autoritza la consulta de renda i patrimoni.
- El patrimoni pot incloure béns immobles.
- El valor cadastral pot influir en el resultat econòmic.
- La documentació pública vincula PICA amb dades de cadastre disponibles per AEAT.
- Glossari mínim: PICA és interoperabilitat administrativa; Cadastre és registre de béns immobles; SOAP/XML és missatgeria i format d'integració.

**Notes**
Formular-ho amb prudència: no afirmem endpoints ni detalls interns. Les fonts públiques permeten afirmar el patró funcional: AGAUR avalua renda i patrimoni, el patrimoni pot incloure valors cadastrals, i hi ha documentació pública que vincula BOGA, PICA i certificats de dades de cadastre disponibles per AEAT. Introduir els acrònims només després d'haver explicat la necessitat funcional.

## 6. Quan l'acoblament es fa risc d'evolució

**Títol**
Quan l'acoblament es fa risc d'evolució

**Subtítol**
Lectura institucional prudent: PICA, web services i orientació CTTI cap a APIs

**Estructura visual**
- Situació verificable: PICA continua descrita públicament com a plataforma corporativa d'interoperabilitat; la documentació tècnica pública mostra integració amb web services i antecedents SOAP.
- Risc arquitectònic: si el flux de l'expedient coneix PICA, SOAP/XML, DTOs, mapping i errors tècnics, l'evolució tecnològica travessa massa capes.
- Criteri docent: separar criteris del procediment, casos d'ús i infraestructura mitjançant ports de sortida i adaptadors substituïbles.

**Conclusió**
El motiu arquitectònic no és només "fer REST". És evitar que l'evolució tecnològica arrossegui els casos d'ús.

**Notes**
No afirmar una retirada pública total de SOAP en PICA, perquè no s'ha localitzat una font pública que ho acrediti. El PDF AGAUR acredita refactorització de serveis SOAP cap a REST i reducció de l'acoblament. Les fonts públiques del CTTI i Canigó permeten sostenir una lectura més precisa: PICA com a plataforma corporativa d'interoperabilitat, integracions web service/SOAP documentades i principis CTTI que orienten cap a desacoblament, REST/JSON, API Manager i EventHub/Kafka.

Connectar amb Hombergs: les arquitectures centrades en el domini busquen que el nucli no depengui de frameworks, bases de dades, UI ni sistemes externs. La idea no és eliminar el món exterior, sinó canviar la direcció i la localització de les dependències.

## 7. Solució AGAUR: port de sortida i adaptador

**Visual**
`diagrams/rendered/agaur-after-hexagonal.svg`

**Text de diapositiva**
El port defineix què necessita l'aplicació. L'adaptador resol com obtenir-ho amb una tecnologia concreta.

**Frase clau**
El cas d'ús necessita una capacitat externa, no una tecnologia externa concreta.

**Notes**
En el cas AGAUR, la necessitat pot ser obtenir informació administrativa rellevant per a l'avaluació d'un procediment. El cas d'ús no hauria de dependre directament de PICA, SOAP, XML ni de DTOs específics del proveïdor.

## 8. De la interface al port

**Visual**
`diagrams/rendered/output-port-pattern.svg`

**Text de diapositiva**
En Java, una interface defineix un contracte i permet substituir implementacions. L'arquitectura hexagonal fa un pas més: organitza les dependències al voltant de fronteres arquitectòniques.

**Frase clau**
Una interface pot desacoblar dues classes. Un port de sortida defineix una frontera arquitectònica.

**Notes**
No tot ús d'una interface és arquitectura hexagonal. En una arquitectura hexagonal, el port expressa una capacitat requerida o oferta per l'aplicació, i l'adaptador encapsula el detall tècnic.

## 9. Transferència a HexaStock

**Visual**
`assets/hexastock-sellstocks-arquitectura-vpd.png`

El diagrama PlantUML propi `diagrams/rendered/hexastock-sell-stock.svg` queda com a versió editable complementària.

**Text de diapositiva**
En HexaStock, el cas d'ús de venda d'accions necessita el preu actual d'un ticker, però no hauria de dependre de Finnhub, Alpha Vantage, REST, JSON, tokens, endpoints ni clients HTTP.

**Notes**
Presentar HexaStock com a producció docent i professional pròpia. No com a autopromoció, sinó com a material de transferència entre pràctica professional, arquitectura aplicada i docència universitària.

## 10. Flux i codi essencial

**Visual**
`diagrams/rendered/sell-stock-sequence.svg`

**Fragment curt**

```java
StockPrice stockPrice = stockPriceProviderPort.fetchStockPrice(ticker);
Price price = stockPrice.price();
SellResult sellResult = portfolio.sell(ticker, quantity, price);
```

**Notes**
Aquest és el punt didàctic central. El servei d'aplicació obté informació externa a través d'un port, però la decisió de domini queda dins el domini ric. `Portfolio.sell(...)` i `Holding.sell(...)` apliquen invariants i FIFO.

## 11. Conclusió

**Visual**
`diagrams/rendered/before-after-comparison.svg`

**Text de diapositiva**
L'arquitectura no elimina la dependència del món exterior. La fa explícita, substituïble i localitzada.

**Frase final**
Quan el proveïdor canvia, volem canviar l'adaptador, no el cas d'ús ni el model de domini.

**Notes**
Tancar tornant al problema inicial: el canvi no desapareix, però queda localitzat en una peça de la infraestructura.
