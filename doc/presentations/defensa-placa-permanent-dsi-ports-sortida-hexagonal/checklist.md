# Checklist final

## Adequació institucional

- [ ] La portada inclou TecnoCampus, defensa de plaça i nom del candidat.
- [ ] El logotip prové de la pàgina oficial d'imatge corporativa.
- [ ] L'estètica és acadèmica, sòbria i no comercial.
- [ ] No hi ha elements decoratius innecessaris.
- [ ] La durada és compatible amb 20 minuts.

## Adequació docent

- [ ] La sessió està situada dins Disseny de Sistemes d'Informació.
- [ ] S'explica que no es pretén cobrir tota l'arquitectura hexagonal.
- [ ] Hi ha un objectiu d'aprenentatge observable.
- [ ] La seqüència és problema real, diagnòstic, solució, HexaStock, conclusió.
- [ ] El cas AGAUR motiva el problema, però no desvia la classe cap a mèrits documentals.

## Rigor tècnic

- [ ] Es diferencia interface Java de port arquitectònic.
- [ ] El port de sortida expressa una capacitat requerida per l'aplicació.
- [ ] L'adaptador encapsula tecnologia, protocol, format, errors i mapping.
- [ ] El servei d'aplicació coordina, però no concentra la decisió principal del domini.
- [ ] El domini ric executa la decisió de venda mitjançant `Portfolio.sell(...)` i `Holding.sell(...)`.
- [ ] S'evita reduir arquitectura hexagonal a "posar interfaces".

## Prudència amb AGAUR

- [ ] La informació acreditada pel PDF està separada de la informació professional aportada.
- [ ] No es mostren dades personals, NIFs, endpoints, logs, credencials ni captures internes.
- [ ] El cas es presenta com un problema habitual de sistemes corporatius, no com una crítica.
- [ ] BOGA, PICA i informació catastral es tracten com a context professional, pendent de revisió final amb Alfredo.

## HexaStock

- [ ] Es mostren només fragments curts de codi.
- [ ] Es confirma `PortfolioStockOperationsUseCase` com a port d'entrada.
- [ ] Es confirma `PortfolioStockOperationsService` com a servei d'aplicació.
- [ ] Es confirma `StockPriceProviderPort` com a port de sortida.
- [ ] Es confirmen Finnhub, Alpha Vantage i mock com a adaptadors intercanviables.
- [ ] La demo, si es fa, utilitza `mockfinhub` o un adaptador determinista.

## Llenguatge

- [ ] Tot el material està en català.
- [ ] S'utilitza terminologia tècnica: acoblament, dependència, frontera, infraestructura i adaptador.
- [ ] Les frases clau apareixen de manera consistent.
- [ ] Les diapositives tenen una idea principal cadascuna.
