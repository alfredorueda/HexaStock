# Esquema narratiu

## Decisió pedagògica

La classe es presenta com una microlliçó dins Disseny de Sistemes d'Informació, assignatura 103322 del Grau en Enginyeria Informàtica de Gestió i Sistemes d'Informació. La guia docent oficial 2025/26 la situa a tercer curs, segon trimestre, 6 ECTS, i inclou Alfredo Rueda Unsain entre el professorat.

La microlliçó s'ubica dins els continguts oficials sobre arquitectures clean o hexagonals, ports i adaptadors, mapping entre capes, organització en mòduls, microserveis i DDD.

En 20 minuts no és rigorós explicar tota l'arquitectura hexagonal. El focus és una decisió concreta:

**com un cas d'ús pot necessitar informació d'una API externa sense acoblar el cas d'ús ni el domini a la tecnologia, el protocol, el format o el proveïdor concret.**

## Fil narratiu

1. Situar la classe dins l'assignatura.
2. Explicar el límit temporal i el focus.
3. Presentar el problema professional AGAUR com a motivació.
4. Diagnosticar el problema arquitectònic: massa dependència dels detalls externs.
5. Introduir port de sortida i adaptador com a frontera.
6. Precisar que una interface Java no és, per si sola, una arquitectura.
7. Transferir el patró a HexaStock.
8. Mostrar el cas d'ús de venda d'accions.
9. Mostrar el codi essencial i la substitució de proveïdors.
10. Tancar amb la idea que l'arquitectura localitza el canvi.

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

## Estructura de 10 diapositives

| # | Títol | Temps | Funció docent |
|---|-------|-------|---------------|
| 1 | Portada | 1:00 | Presentar tema i context institucional |
| 2 | On som dins l'assignatura | 1:30 | Situar la microlliçó dins la guia docent oficial |
| 3 | Objectiu d'aprenentatge | 1:30 | Definir competència observable |
| 4 | El problema en sistemes reals | 2:30 | Motivar amb AGAUR i integracions externes |
| 5 | Quan l'acoblament es fa risc d'evolució | 2:00 | Mostrar la lectura institucional prudent: PICA, web services, REST/API Manager i necessitat de frontera |
| 6 | Solució AGAUR: port i adaptador | 2:00 | Introduir la frontera hexagonal |
| 7 | De la interface al port | 2:00 | Precisar el salt de contracte local a frontera |
| 8 | Transferència a HexaStock | 2:30 | Connectar el patró amb el projecte docent |
| 9 | Flux i codi essencial | 3:00 | Mostrar el servei coordinant i el domini decidint |
| 10 | Conclusió | 2:00 | Tancar amb localització del canvi |
