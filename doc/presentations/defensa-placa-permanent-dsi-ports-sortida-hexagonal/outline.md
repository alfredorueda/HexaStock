# Esquema narratiu

## Decisió pedagògica

La classe es presenta com una microlliçó dins Disseny de Sistemes d'Informació, assignatura 103322 del Grau en Enginyeria Informàtica de Gestió i Sistemes d'Informació. La guia docent oficial 2025/26 la situa a tercer curs, segon trimestre, 6 ECTS, i inclou Alfredo Rueda Unsain entre el professorat.

La microlliçó s'ubica dins els continguts oficials sobre arquitectures clean o hexagonals, ports i adaptadors, mapping entre capes, organització en mòduls, microserveis i DDD.

En 20 minuts no és rigorós explicar tota l'arquitectura hexagonal. El focus és una decisió concreta:

**com un cas d'ús pot necessitar informació d'una API externa sense acoblar el cas d'ús ni el domini a la tecnologia, el protocol, el format o el proveïdor concret.**

## Fil narratiu

1. Situar la classe dins l'assignatura.
2. Explicar el límit temporal i el focus.
3. Presentar el cas funcional AGAUR: sol·licitud de beca, expedient, requisits econòmics i proposta de resolució.
4. Mostrar quina informació externa necessita el procediment: renda, patrimoni, valors cadastrals i interoperabilitat.
5. Diagnosticar el problema arquitectònic: la lògica administrativa no hauria de dependre de PICA, SOAP/XML, DTOs o mapping.
6. Introduir port de sortida i adaptador com a frontera.
7. Precisar que una interface Java no és, per si sola, una arquitectura.
8. Transferir el patró a HexaStock.
9. Mostrar el cas d'ús de venda d'accions.
10. Mostrar el codi essencial i la substitució de proveïdors.
11. Tancar amb la idea que l'arquitectura localitza el canvi.

## Missatge tècnic central

El servei d'aplicació necessita una capacitat del món exterior, no una tecnologia concreta.

## Terminologia que s'ha d'usar

- port d'entrada;
- port de sortida;
- adaptador entrant;
- adaptador sortint;
- servei d'aplicació;
- cas d'ús;
- model de domini;
- domini ric;
- dependència;
- inversió de dependències;
- frontera arquitectònica;
- detall d'infraestructura;
- mapping entre models;
- substituïbilitat d'adaptadors.

## Terminologia a evitar

- No reduir l'arquitectura hexagonal a "posar interfaces".
- No presentar AGAUR com un cas problemàtic o com una crítica.
- No mostrar dades, endpoints, logs, credencials ni detalls interns.
- No fer servir metàfores imprecises aplicades al domini; usar acoblament, dependència i detalls d'infraestructura.

## Estructura de 11 diapositives

| # | Títol | Temps | Funció docent |
|---|-------|-------|---------------|
| 1 | Portada | 0:45 | Presentar tema i context institucional |
| 2 | On som dins l'assignatura | 1:15 | Situar la microlliçó dins la guia docent oficial |
| 3 | Objectiu d'aprenentatge | 1:00 | Definir competència observable |
| 4 | Cas funcional: avaluació econòmica d'una beca | 2:15 | Explicar el procediment abans de parlar de tecnologia |
| 5 | Quina informació externa necessita el procediment? | 2:00 | Connectar renda, patrimoni, valors cadastrals, PICA i AEAT |
| 6 | Quan l'acoblament es fa risc d'evolució | 2:00 | Diagnosticar la barreja entre lògica administrativa i infraestructura |
| 7 | Solució AGAUR: port i adaptador | 1:45 | Introduir la frontera hexagonal |
| 8 | De la interface al port | 1:30 | Precisar el salt de contracte local a frontera |
| 9 | Transferència a HexaStock | 2:15 | Connectar el patró amb el projecte docent |
| 10 | Flux i codi essencial | 2:45 | Mostrar el servei coordinant i el domini decidint |
| 11 | Conclusió | 1:15 | Tancar amb localització del canvi |

Total orientatiu: 18:45. El marge restant permet respirar, fer transicions i adaptar el ritme al tribunal.
