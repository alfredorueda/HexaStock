# Esquema de la presentació

Presentació per a una classe magistral reduïda de 20 minuts en el marc de la defensa d'una plaça de professorat permanent al TecnoCampus.

## Criteri de disseny

La versió actual prioritza una presentació oral. Les diapositives no han de contenir el guió complet, sinó sostenir-lo visualment amb idees breus, diagrames i fragments de codi molt seleccionats.

Registre:

- català;
- acadèmic;
- sobri;
- tècnic;
- institucional;
- sense to comercial.

## Fil narratiu

1. Situar la microlliçó dins Disseny de Sistemes d'Informació.
2. Fer explícit que no s'explica tota l'arquitectura hexagonal.
3. Presentar el cas funcional AGAUR: sol·licitud, expedient i avaluació econòmica.
4. Mostrar la necessitat d'informació externa: renda, patrimoni, béns immobles, AEAT, Cadastre i PICA.
5. Diagnosticar el problema d'acoblament entre cas d'ús i infraestructura.
6. Presentar la solució: port de sortida i adaptador.
7. Precisar la diferència entre una interface local i un port com a frontera arquitectònica.
8. Transferir el patró a HexaStock.
9. Mostrar el cas d'ús de venda d'accions i el port de preus.
10. Fer una demo controlada amb adaptador substituïble.
11. Tancar amb conclusió tècnica i agraïment institucional.

## Estructura actual

| # | Diapositiva | Temps orientatiu | Funció |
|---|---|---:|---|
| 1 | Portada | 0:45 | Presentar tema i context |
| 2 | On som dins l'assignatura | 1:00 | Situar la sessió dins DSI |
| 3 | Objectiu d'aprenentatge | 1:00 | Fixar el resultat docent |
| 4 | Avaluació econòmica d'una beca | 2:00 | Explicar el cas funcional AGAUR |
| 5 | Quina informació externa necessita el procediment? | 1:45 | Introduir renda, patrimoni, Cadastre, AEAT i PICA |
| 6 | Quan canvia la integració, canvia massa codi | 2:00 | Fer visible el risc d'acoblament |
| 7 | Port de sortida i adaptador | 1:30 | Presentar la solució arquitectònica |
| 8 | De la interface al port | 1:15 | Precisar la frontera arquitectònica |
| 9 | Transferència a HexaStock | 2:00 | Traslladar el patró al domini financer |
| 10 | Flux i codi essencial | 1:45 | Mostrar el punt de decisió tècnica |
| 11 | Demo controlada | 1:30 | Demostrar substitució d'adaptador |
| 12 | Conclusió | 1:00 | Fixar la idea principal |
| 13 | Agraïment | 0:30 | Tancar institucionalment |

Temps total estimat: 18:15. La resta queda per a transicions, respiració i adaptació al ritme del tribunal.

## Idea final

L'arquitectura no elimina la dependència del món exterior. La fa explícita, substituïble i localitzada.
