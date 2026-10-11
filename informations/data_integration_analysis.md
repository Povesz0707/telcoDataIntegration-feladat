# Telco adatforrások elemzése

## Összefoglaló

A feladat célja két, szándékosan eltérő termékforrás egységesítése egy BI-réteg számára fogyasztható API-n keresztül. A CSV bruttó árakat, magyar mezőneveket és részben hiányos készletadatot tartalmaz. A JSON API nettó árakat, angol mezőneveket, EAN-kódot és frissítési időbélyeget ad.

A források nem egyszerűen egymás másolatai. Az egyező termékeknél is vannak készlet- és gyártónév-eltérések, ezért az integrációnak a normalizálás mellett adatminőségi állapotot és forrásprovenienciát is érdemes megőriznie.

## A feladat lényege

### Backend

- Java 17+, Spring Boot, Spring Data JPA.
- A két forrás beolvasása és egységesítése.
- Bruttó és nettó árak kezelése.
- Eltérő mezőnevek, SKU-formátumok, duplikátumok és hiányzó mezők kezelése.
- Normalizált mezők:
  `sku`, `name`, `manufacturer`, `finalPriceHuf`, `stock`, `ean`, `updatedAt`, `source`.
- API-végpont: `GET /products?filter=&sort=&onlyValid=`.

### Frontend

- React, Angular vagy Vue; TypeScript ajánlott.
- Terméklista megjelenítése API-hívásból.
- Szűrés, keresés és rendezés.
- Betöltési és hibaállapot.
- Hibás rekordok látható jelölése, például külön szekcióban.
- Futtathatóság `npm start` paranccsal és rövid README.

### Opcionális full-stack elemek

- Integrált indítás Spring Boot és frontend paranccsal.
- Dokumentált közös API-szerződés, például README-ben vagy Swaggerben.
- Egyszerű Dockerfile vagy docker-compose.

## Források és elemszámok

| Forrás | Rekordszám | Fő mezők | Árértelmezés | Időbélyeg |
|---|---:|---|---|---|
| CSV | 12 sor | `sku`, `product_name`, `gross_price_huf`, `stock_qty`, `brand` | bruttó HUF, szóközzel tagolt szám | nincs |
| JSON | 10 rekord | `id`, `name`, `netPrice`, `currency`, `vatRate`, `quantityAvailable`, `manufacturer`, `ean`, `updatedAt` | nettó ár, HUF, ÁFA-kulccsal | van, UTC |

### CSV adattisztaság

- 11 sorban van SKU, 1 sorban hiányzik.
- A `P-1002` SKU kétszer szerepel, készletértékei 8 és 7.
- A `P-1005` rekord készlete hiányzik.
- A `P_1006` SKU elválasztójele eltér a többi CSV-rekordétól.
- A `gross_price_huf` értékek szövegként, szóközzel tagolva érkeznek, például `12 700`.
- A CSV nem tartalmaz EAN-t vagy frissítési időbélyeget.
- A hiányzó SKU-val érkező rekord (`Ismeretlen termék`) nem kapcsolható megbízhatóan másik forráshoz.

### JSON adattisztaság

- 10 rekord érkezik.
- A `SKU-XYZ` rekordnál hiányzik a nettó ár, a gyártó és az EAN.
- A `SKU1010` rekord készlete `null`.
- A `SKU1003` rekord EAN-ja `null`.
- A JSON azonosítók formátuma vegyes: például `SKU1001` és `SKU-1004`.
- Minden árnál `HUF` és `0.27` ÁFA-kulcs szerepel, kivéve hogy az ismeretlen rekord ára hiányzik.

## Rekordkapcsolás

A források között a nyers SKU-szöveg nem használható közvetlen kulcsként. Célszerű egy determinisztikus normalizáló kulcs:

1. trim és nagybetűsítés,
2. a nem alfanumerikus elválasztók eltávolítása,
3. a forrásprefix eltávolítása (`P`, illetve `SKU`),
4. a termékszám megtartása.

Így például az alábbiak egyező termékre mutatnak:

| CSV | JSON | Normalizált kulcs |
|---|---|---|
| `P-1001` | `SKU1001` | `1001` |
| `P-1004` | `SKU-1004` | `1004` |
| `P_1006` | `SKU-1006` | `1006` |

Az egyezési eredmény:

- 9 logikai termék található mindkét forrásban: 1001–1004 és 1006–1010.
- A `P-1005` csak CSV-ben található meg.
- A `SKU-XYZ` csak JSON-ban található meg, és nincs használható numerikus kapcsolókulcse.
- A `P-1002` ismétlődése miatt a CSV oldalon nem egyértelmű az egyetlen kanonikus rekord.

Az API-ban érdemes a kanonikus SKU-t egységesen `P-1001` formában visszaadni, miközben a nyers forrásazonosítókat és a normalizált kapcsolókulcsot belső provenance-adatként meg lehet őrizni.

## Árnormalizálás

A CSV már bruttó árat tartalmaz. A JSON esetén:

```text
finalPriceHuf = netPrice * (1 + vatRate)
```

Egész HUF-ra kerekítve az összes párosított JSON-ár megfelel a CSV bruttó árának:

| Kulcs | CSV bruttó | JSON-ból számított bruttó | Eredmény |
|---|---:|---:|---|
| 1001 | 12 700 | 12 700 | egyezik |
| 1002 | 25 400 | 25 400 | egyezik |
| 1003 | 3 175 | 3 175 | egyezik |
| 1004 | 1 905 | 1 905 | egyezik |
| 1006 | 6 350 | 6 350 | egyezik |
| 1007 | 2 540 | 2 540 | egyezik |
| 1008 | 63 500 | 63 500 | egyezik |
| 1009 | 95 250 | 95 250 | egyezik |
| 1010 | 12 700 | 12 700 | egyezik |

Implementációban `BigDecimal` használata javasolt, explicit kerekítési szabállyal, például `HALF_UP` egész HUF-ra. A pénznemet ellenőrizni kell, és HUF-tól eltérő értéket nem szabad csendben átszámítani.

## Mezőtérkép

| Normalizált mező | CSV forrás | JSON forrás | Kezelés |
|---|---|---|---|
| `sku` | `sku` | `id` | normalizált kapcsolás, kanonikus formátum |
| `name` | `product_name` | `name` | név normalizálható, de a forrásérték maradjon visszakereshető |
| `manufacturer` | `brand` | `manufacturer` | whitespace/case normalizálás; aliasok kezelése |
| `finalPriceHuf` | `gross_price_huf` | `netPrice`, `vatRate` | CSV parse, JSON bruttósítás |
| `stock` | `stock_qty` | `quantityAvailable` | egész szám; hiányzó érték nem nulla |
| `ean` | nincs | `ean` | CSV oldalon kezdetben null, JSON-ból kitölthető |
| `updatedAt` | nincs | `updatedAt` | JSON UTC timestamp; CSV-only rekordnál null |
| `source` | CSV | JSON | párosított rekordnál például `CSV+JSON`, egyforrásúnál `CSV` vagy `JSON` |

## Párosított rekordok eltérései

Az ár minden közös rekordnál konzisztens, de a készlet nem mindenhol:

| Kulcs | CSV készlet | JSON készlet | Megjegyzés |
|---|---:|---:|---|
| 1001 | 15 | 14 | időbeli vagy forrásoldali eltérés |
| 1002 | 8 és 7 | 9 | CSV-duplikátum és forrásütközés |
| 1003 | 120 | 118 | eltérés |
| 1004 | 0 | 0 | egyezik |
| 1006 | 42 | 41 | eltérés |
| 1007 | 200 | 200 | egyezik |
| 1008 | 5 | 4 | eltérés |
| 1009 | 3 | 3 | egyezik |
| 1010 | 30 | null | JSON oldalon hiányzó készlet |

Gyártónév-eltérés is van, de ezek jellemzően formázási vagy alias-eltérések:

- `FixIt` és `Fix-It`.
- `Wire&Co` és `WIRECO`.
- `VIEWTEK` és `ViewTek`.

Az eltérő készletértékeket nem szabad automatikusan összeadni. A rendszernek választott forrásprioritást vagy időbélyeg-alapú szabályt kell alkalmaznia, és az eltérést hibajelként vagy figyelmeztetésként meg kell őriznie. Mivel a CSV-hez nincs időbélyeg, a JSON nem bizonyíthatóan frissebb minden esetben, még akkor sem, ha a mintában van `updatedAt`.

## Javasolt egységesítési és validációs szabályok

1. A bemeneteket külön forrásadapter olvassa be.
2. A CSV ármezőt először tisztítani kell a szóközöktől, majd egész HUF-ként parse-olni.
3. A JSON nettó árat és ÁFA-kulcsot `BigDecimal` mezőként kell kezelni.
4. A termékazonosítót normalizált kapcsolókulccsal kell egyeztetni.
5. Duplikált forrásrekordnál nem szabad first-match választást alkalmazni. A rekord kapjon `DUPLICATE_SKU` vagy `CONFLICTING_SOURCE_VALUES` hibát.
6. A hiányzó kötelező mező ne alakuljon át nullából nullsztringgé vagy nullából nullává tett készletként 0-vá.
7. A `stock = 0` valódi érték, nem hiányzó adat.
8. A `finalPriceHuf` és a normalizált SKU a minimális érvényességhez szükséges.
9. A név, gyártó és készlet hiánya külön validációs hibaként jelenjen meg.
10. Az EAN hiányát csak akkor kell érvénytelenségnek tekinteni, ha az üzleti szerződés kötelező mezővé teszi. A mintafeladat ezt nem mondja ki.
11. Az `updatedAt` hiánya CSV-only rekordnál várható, ezért önmagában ne tegye érvénytelenné a rekordot.
12. Minden normalizált rekordhoz célszerű `validationErrors` vagy hasonló belső mezőt vezetni, még ha ez nem is része a minimális publikus szerződésnek.

## `onlyValid` értelmezése

Javasolt jelentés: `onlyValid=true` csak azokat a rekordokat adja vissza, amelyeknek van kapcsolható SKU-ja, neve, gyártója, bruttó HUF-ára és készletértéke, és nincs feloldatlan duplikációs vagy forrásütközési hibájuk.

Fontos különbség:

- A két forrás eltérő készlete önmagában lehet időbeli eltérés, nem feltétlenül sérült adat.
- Ha a terméklista egyetlen kanonikus készletértéket ad vissza, a választott prioritásnak dokumentáltnak kell lennie.
- Ha a `onlyValid` szigorúan forrásegyezést jelentene, a három készletben egyező közös rekordnál több termék kiesne. Ezért jobb a validitást a mezőteljességhez és a feloldatlan konfliktushoz kötni, az eltérést pedig külön jelölni.

## API-javaslat

### Kérés

```http
GET /products?filter=monitor&sort=finalPriceHuf:asc&onlyValid=true
```

Javasolt paraméterek:

- `filter`: SKU, név, gyártó vagy EAN szerinti case-insensitive keresés.
- `sort`: engedélyezett mezőnév és `asc`/`desc`; ismeretlen mező esetén 400-as válasz.
- `onlyValid`: alapértelmezésként `false`, mert a hibás rekordok frontend oldali megjelenítése is követelmény.

### Válaszrekord

```json
{
  "sku": "P-1001",
  "name": "ACME Widget Mini",
  "manufacturer": "ACME",
  "finalPriceHuf": 12700,
  "stock": 14,
  "ean": "5991234500011",
  "updatedAt": "2025-10-01T10:00:00Z",
  "source": "CSV+JSON",
  "valid": true,
  "issues": []
}
```

A `valid` és `issues` mező a frontend hibajelzését egyszerűbbé teszi. Ha a minimális szerződéshez ragaszkodni kell, ezek lehetnek belső mezők, vagy külön `validation` blokkban szerepelhetnek.

## Javasolt megoldási sorrend

1. Forrás DTO-k és parser-ek létrehozása.
2. CSV szám- és hiányzóérték-kezelés.
3. JSON ár- és timestamp-kezelés.
4. SKU-normalizáló és forrás-összekapcsoló réteg.
5. Gyártónév-aliasok és kanonikus megjelenítési szabály.
6. Konfliktus- és validációs modell.
7. Repository/service réteg és `GET /products`.
8. JUnit tesztek a normalizálásra, árkerekítésre, duplikátumokra, nullákra és szűrésre.
9. Frontend lista, szűrés, rendezés, betöltési/hibaállapot és hibás rekordok.
10. README-ben API-példák, indítási lépések és adatminőségi szabályok.

## Tesztelendő esetek

- `P-1001` és `SKU1001` egyezése.
- `P_1006` és `SKU-1006` egyezése.
- `P-1002` CSV-duplikációja.
- CSV-only `P-1005` hiányzó készlettel.
- JSON-only `SKU-XYZ` hiányzó árral és gyártóval.
- `stock = 0` megőrzése.
- JSON `null` készlet megkülönböztetése a nullától eltérő készlettől.
- Nettó ár bruttósítása és egész HUF-ra kerekítése.
- Magyar és angol terméknév megjelenítési szabálya.
- `onlyValid=false` esetén a hibás rekordok visszaadása.
- `onlyValid=true` esetén a hibás rekordok kizárása.
- Ismeretlen `sort` érték kezelése.

## Nyitott üzleti döntések

1. Készletütközés esetén a JSON vagy a CSV legyen-e elsődleges?
2. Időbélyeg hiányában milyen szabály alapján hasonlítható össze a CSV a JSON-nal?
3. A CSV és JSON közös rekordjánál a név magyar vagy angol változata legyen-e a kanonikus név?
4. Kötelező mező-e az EAN?
5. Kötelező mező-e az `updatedAt`?
6. A `source` egyetlen szöveg legyen-e, vagy a publikus szerződés engedjen forráslistát?
7. A készleteltérést validációs hibának, figyelmeztetésnek vagy csak provenance-adatnak kell-e tekinteni?

## Források

- Feladatleírás: `informations/Telco_Assessment_Task_Brief.docx`
- CSV forrás: `informations/incompatible_feeds_products.csv`
- JSON forrás: `informations/incompatible_feeds_products.json`
