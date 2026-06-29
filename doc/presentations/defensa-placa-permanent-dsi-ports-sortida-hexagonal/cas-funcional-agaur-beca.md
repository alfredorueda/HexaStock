# Cas funcional AGAUR: avaluació econòmica d'una beca

Aquest document recull la base pública que permet explicar, amb prudència institucional, el cas funcional utilitzat a la microlliçó: un procediment de beca en què cal avaluar requisits econòmics de renda i patrimoni, incloent-hi informació patrimonial vinculada a béns immobles i valors cadastrals.

L'objectiu no és documentar el detall intern dels sistemes d'AGAUR, ni mostrar cap dada sensible. L'objectiu docent és explicar primer el problema funcional i després el problema arquitectònic: una lògica administrativa que necessita informació externa no hauria de quedar acoblada a PICA, SOAP/XML, DTOs o clients tècnics concrets.

## 1. Narrativa funcional per a la classe

Una estudiant presenta una sol·licitud de beca. Aquesta sol·licitud genera un expedient administratiu que s'ha d'avaluar d'acord amb requisits generals, econòmics i acadèmics.

En el cas de la beca general, els requisits econòmics inclouen renda i patrimoni de la unitat familiar. El patrimoni pot incloure finques urbanes i rústiques, valorades mitjançant valors cadastrals, així com altres indicadors patrimonials.

Per tant, abans de parlar d'arquitectura, el cas funcional es pot explicar així:

1. La persona sol·licitant presenta la beca.
2. El sistema crea o recupera l'expedient.
3. Es comproven requisits generals.
4. Es revisen requisits econòmics de renda i patrimoni.
5. Per revisar renda i patrimoni cal obtenir dades administratives externes.
6. Aquestes dades poden incloure informació fiscal i informació patrimonial vinculada a béns immobles.
7. Si els requisits econòmics no es compleixen, l'expedient no avança a la revisió acadèmica.
8. Si es compleixen, el procediment continua i pot arribar a una proposta de resolució.

## 2. Fets públicament verificables

| Fet | Font pública | Ús docent |
|---|---|---|
| AGAUR indica que, un cop comprovats els requisits generals, es revisen requisits econòmics de renda i patrimoni. | AGAUR, pàgina de requisits econòmics de la beca general. | Justifica que el cas funcional no és inventat: el procediment real té una fase econòmica. |
| AGAUR indica que si no es compleixen els requisits econòmics, l'expedient no avança al pas següent i no es revisen els requisits acadèmics. | AGAUR, pàgina de requisits econòmics. | Permet explicar que la fase econòmica té impacte directe en la tramitació. |
| AGAUR descriu límits patrimonials per finques urbanes, finques rústiques, rendiments de capital mobiliari i volum de facturació. | AGAUR, pàgina de requisits econòmics. | Permet explicar què vol dir "patrimoni" en aquest context. |
| AGAUR explicita que les finques urbanes es valoren amb la suma dels valors cadastrals, excloent l'habitatge habitual. | AGAUR, pàgina de requisits econòmics. | Justifica la connexió funcional entre beca i informació cadastral. |
| AGAUR indica que la sol·licitud autoritza a consultar dades de renda i patrimoni de la unitat familiar. | AGAUR, preguntes freqüents de la beca general. | Permet explicar per què el sistema necessita consultar fonts administratives externes. |
| AGAUR indica que la informació econòmica és proporcionada per l'administració tributària corresponent. | AGAUR, preguntes freqüents. | Permet explicar la dependència funcional de dades administratives externes. |
| Una addenda pública AGAUR-Departament d'Educació documenta BOGA, manteniment de processos d'interoperabilitat a través de PICA i certificat de dades de cadastre disponibles per AEAT. | AGAUR, quarta addenda al conveni BOGA 2020. | Permet connectar, amb base pública, BOGA, PICA, dades de cadastre i càlcul de renda/patrimoni. |
| El BOE 2025-2026 estableix umbrals de renda i patrimoni familiar per a beques i ajuts a l'estudi. | BOE, Reial decret 163/2025. | Dona context normatiu general dels llindars econòmics. |
| El Catastro és un registre administratiu de béns immobles rústics, urbans i de característiques especials amb atributs físics, jurídics i econòmics. | Dirección General del Catastro. | Permet definir "Cadastre / Catastro" sense improvisació. |
| PICA és una plataforma corporativa d'interoperabilitat que permet accedir a informació d'altres administracions i integrar sistemes departamentals amb la tramitació corporativa. | CTTI, pàgina pública de PICA. | Permet definir PICA amb una font oficial. |

## 3. Formulació prudent per a la presentació

Formulació recomanada:

> En un procediment de beca, AGAUR ha d'avaluar requisits econòmics de renda i patrimoni. Les fonts públiques indiquen que aquests requisits poden incloure finques urbanes i rústiques valorades amb valors cadastrals, i que la sol·licitud autoritza la consulta de dades de renda i patrimoni. També hi ha documentació pública d'AGAUR que vincula el sistema BOGA amb processos d'interoperabilitat a través de PICA i amb certificats de dades de cadastre disponibles per AEAT. Per tant, el cas funcional és clar: el procediment necessita informació administrativa externa per decidir si l'expedient compleix o no determinats requisits econòmics.

Formulació que cal evitar:

> El sistema intern d'AGAUR crida directament el Catastro per PICA amb un endpoint concret.

Per què evitar-la:

No tenim una font pública que documenti el detall intern exacte de la crida, l'endpoint, el protocol concret en cada moment o l'arquitectura interna desplegada. El que sí tenim és suficient per explicar la necessitat funcional i el problema arquitectònic sense exposar detalls interns.

## 4. Pont cap al diagnòstic tècnic

Un cop el tribunal o l'estudiant entén el cas funcional, es pot passar al diagnòstic tècnic:

> Si la lògica administrativa que avalua l'expedient queda barrejada amb PICA, SOAP/XML, DTOs externs, mapping i errors tècnics, qualsevol canvi en la tecnologia d'interoperabilitat impacta massa codi. El problema no és consultar informació externa. El problema és que la decisió administrativa quedi acoblada als detalls de la infraestructura.

## 5. Traducció arquitectònica

La necessitat funcional:

> Necessito informació administrativa fiable per avaluar requisits econòmics d'una sol·licitud de beca.

No s'hauria de formular dins el cas d'ús com:

> Necessito cridar PICA amb SOAP i XML i interpretar DTOs concrets.

La traducció hexagonal és:

- el cas d'ús depèn d'un port de sortida;
- el port expressa una necessitat de l'aplicació;
- l'adaptador encapsula PICA, SOAP/XML, REST, API Manager, mapping, errors tècnics i detalls del proveïdor;
- si canvia la tecnologia d'interoperabilitat, canvia l'adaptador, no la lògica administrativa ni el cas d'ús.

## 6. Fonts

- AGAUR, requisits econòmics de la beca general: `https://agaur.gencat.cat/ca/beques-i-ajuts/pagines-especials/beques-i-ajuts-per-estudis-universitaris1/beca-general-ministeri/Requisits-economics/index.html`
- AGAUR, preguntes freqüents de la beca general: `https://agaur.gencat.cat/ca/beques-i-ajuts/beca-general-generalitat/estudiants-domicili-catalunya/quan-solicitud-presentada/preguntes-frequents/`
- AGAUR, quarta addenda al conveni de col·laboració BOGA 2020: `https://agaur.gencat.cat/web/.content/Documents/AGAUR/transparencia/convenis/2020/03_2020_4a_Addenda_Ensenyament_CA.pdf`
- BOE, Reial decret 163/2025, de 4 de març: `https://www.boe.es/diario_boe/txt.php?id=BOE-A-2025-4320`
- Dirección General del Catastro, "El Catastro en cifras": `https://www.catastro.hacienda.gob.es/es-ES/catastroencifras.html`
- CTTI, Plataforma d'Integració i Col·laboració Administrativa (PICA): `https://ctti.gencat.cat/ca/detalls/detallarticle/pica`
- CTTI / Canigó, principis d'arquitectura de sistemes d'informació: `https://canigo.ctti.gencat.cat/arquitectura/principis/principis_arq/?cms=true`
