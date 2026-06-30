# Diapositiva a diapositiva

Versió simplificada per a exposició oral. Les diapositives han de ser suport visual, no document de lectura.

## 1. Portada

Títol:
`Disseny de ports de sortida en arquitectura hexagonal`

Subtítol:
`desacoblament entre casos d'ús, domini i APIs externes`

Funció oral:
Presentar que es tracta d'una microlliçó dins Disseny de Sistemes d'Informació, no d'una explicació completa de tota l'arquitectura hexagonal.

## 2. On som dins l'assignatura

Text visible:

```text
Disseny de Sistemes d'Informació
3r curs · 6 ECTS

Ports de sortida
en arquitectura hexagonal

No explicarem tota l'arquitectura hexagonal.
Ens centrem en una decisió de disseny concreta.
```

Funció oral:
Connectar amb l'assignatura i amb el bloc de clean / hexagonal architecture, ports, adaptadors i DDD.

## 3. Objectiu d'aprenentatge

Text visible:

```text
El servei d'aplicació necessita una capacitat, no una tecnologia.

Identificar acoblament
Definir un port
Substituir adaptadors
```

Funció oral:
Explicar què hauria de saber fer l'estudiant en acabar la sessió.

## 4. Avaluació econòmica d'una beca

Text visible:

```text
Sol·licitud → Expedient → Requisits generals
→ Requisits econòmics → Revisió acadèmica
→ Resolució

Si no es compleixen requisits econòmics,
l'expedient no avança.
```

Diagrama:
`diagrams/rendered/agaur-functional-scholarship.png`

Funció oral:
Explicar primer el cas funcional AGAUR: el procediment avalua requisits econòmics de renda i patrimoni abans de continuar.

## 5. Quina informació externa necessita el procediment?

Text visible:

```text
NECESSITAT
- Renda familiar
- Patrimoni
- Béns immobles

FONTS
- AEAT
- Cadastre
- PICA

El procediment necessita informació administrativa,
no una API concreta.
```

Diagrama:
`diagrams/rendered/agaur-data-interoperability.png`

Funció oral:
Definir breument PICA, Cadastre, AEAT i SOAP/XML, i explicar que la necessitat és funcional abans de ser tècnica.

## 6. Quan canvia la integració, canvia massa codi

Text visible:

```text
Cas d'ús → SOAP/XML → PICA → AEAT

El cas d'ús coneix la infraestructura
El canvi tecnològic impacta massa
Errors tècnics afecten el procediment

Separar casos d'ús, criteris del procediment i infraestructura.
```

Funció oral:
Fer el diagnòstic arquitectònic sense afirmar una retirada pública de SOAP. El problema és l'acoblament, no l'existència de sistemes externs.

## 7. Port de sortida i adaptador

Text visible:

```text
Cas d'ús
↓
Port de sortida
↓
Adaptador
↓
Sistema extern

El port defineix què.
L'adaptador resol com.
```

Funció oral:
Presentar la solució hexagonal de manera general.

## 8. De la interface al port

Text visible:

```text
Una interface desacobla codi.
Un port desacobla arquitectura.
```

Diagrama:
`diagrams/rendered/output-port-pattern.png`

Funció oral:
Explicar que no tot ús d'una interface és arquitectura hexagonal; el port expressa una frontera.

## 9. Transferència a HexaStock

Text visible:

```text
Mateix problema,
diferent domini

Cas d'ús: venda d'accions
Necessitat: preu actual
No depèn de proveïdors concrets
```

Imatge:
`assets/hexastock-sellstocks-arquitectura-vpd.png`

Funció oral:
Traslladar el patró del cas AGAUR a un domini financer: el cas d'ús necessita un preu, no un proveïdor concret.

## 10. Flux i codi essencial

Text visible:

```java
stockPrice = port.fetch(ticker);

result = portfolio.sell(...);
```

Punts:

```text
El servei coordina
El port obté informació
El domini decideix
```

Funció oral:
Mostrar el punt on el servei d'aplicació obté informació externa i delega la decisió al domini ric.

## 11. Demo controlada

Text visible:

```text
HexaStock és un projecte open source propi utilitzat en docència universitària
i formació o consultoria amb institucions financeres.

Mateix cas d'ús
Mateix servei
Mateix domini

Canvi: adaptador real ↔ adaptador mock

Canvia la infraestructura; el cas d'ús roman estable.
```

Funció oral:
Executar o explicar la demo sense dependència d'internet ni claus reals.

## 12. Conclusió

Text visible:

```text
L'arquitectura no elimina el canvi.
El localitza.

Quan el proveïdor canvia,
volem canviar l'adaptador,
no el cas d'ús ni el model de domini.
```

Funció oral:
Tancar la part tècnica amb la idea principal.

## 13. Agraïment

Text visible:

```text
Vull expressar el meu agraïment al TecnoCampus per l'oportunitat
d'aprendre, créixer com a docent i transferir coneixement entre
l'empresa i la universitat.

Aquesta experiència m'ha permès portar casos reals, criteri professional
i arquitectura aplicada a l'aula.

Agraïment especial al Dr. Josep Roure, company professor del TecnoCampus,
amb qui he tingut l'oportunitat d'aprendre molt sobre arquitectura i software.
```

Funció oral:
Tancar en registre institucional i personal, sense to promocional.
