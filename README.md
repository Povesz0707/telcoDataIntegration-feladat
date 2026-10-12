# Telco Data Integration

Full-stack referencia-implementáció két eltérő termékadatforrás feltöltéséhez, normalizálásához és lekérdezéséhez.

## Technológiák

- Backend: Java 21, Spring Boot, Spring Data JPA, PostgreSQL, MapStruct, Lombok
- API dokumentáció: OpenAPI 3, Springdoc Swagger UI
- Frontend: Angular, PrimeNG, PrimeFlex, TypeScript
- Tesztelés: JUnit 5, Spring Boot Test, Playwright
- Minőségellenőrzés: JaCoCo

## Futtatás

### Docker Compose

```text
docker compose up --build
```

- Frontend: `http://localhost:4200`
- Backend: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- PostgreSQL: `localhost:5432`, adatbázis `telco`, felhasználó `telco`

Az adatbázis adatai a `postgres-data` Docker volume-ban maradnak meg.

### Backend fejlesztői futtatása

```text
cd backend
mvn spring-boot:run
```

A backend alapértelmezett címe: `http://localhost:8080`. A lokálisan indított backend PostgreSQL-hez csatlakozik a `localhost:5432` címen.

### Frontend

```text
cd frontend
npm ci
npm start
```

A frontend alapértelmezett címe: `http://localhost:4200`.

## Tesztelés

### JaCoCo coverage riport

A coverage riport újrafuttatható PowerShell szkripttel:

```text
.\scripts\coverage.ps1
```

Az újrafuttatható script lefuttatja a backend unit teszteket, majd elkészíti a
JaCoCo HTML és XML riportot. Az elkészült fájlok:

- `reports/jacoco/index.html` – böngészhető JaCoCo riport.
- `reports/jacoco/jacoco.xml` – géppel feldolgozható coverage riport.

### Backend unit tesztek

```text
cd backend
mvn test
```

A unit tesztek lefedik többek között a SKU-normalizálást, a kötelező mezők validációját,
a fallback működését, a forrásütközéseket, a duplikációkat és a nem támogatott pénznemet.
Emellett a service réteg pozitív és negatív ágai is tesztelve vannak: fájlfeltöltés/parszolás,
terméklekérdezés, szűrés és rendezés, feldolgozási futás, hiányzó erőforrások és hibás bemenetek.

### Backend integrációs tesztek

A `mvn test` parancs a gyors unit teszteket futtatja. Az integrációs tesztek a teljes HTTP réteget (`MockMvc`), a Spring kontextust, a JPA perzisztenciát
és a PostgreSQL adatbázist együtt ellenőrzik. H2 nincs használatban. A tesztek futtatása előtt a
Docker Compose PostgreSQL szolgáltatását el kell indítani:

```text
docker compose up -d postgres
cd backend
mvn verify
```

A tesztprofil alapértelmezés szerint a különálló `localhost:5432/telco_test` adatbázist használja,
így a tesztek nem módosítják és nem törlik a fejlesztési `telco` adatbázis adatait. Külön tesztadatbázis
beállítható a `TEST_DB_URL`, `TEST_DB_USERNAME` és `TEST_DB_PASSWORD` környezeti változókkal.

Az integrációs tesztek lefedik a sikeres CSV/JSON feltöltést és normalizálást, az elsődleges forrás
és a fallback működését, a legutóbbi futás nélküli lekérdezést, az érvénytelen JSON feltöltést,
valamint a hibás forráshoz tartozó batch elutasítását.

### Playwright felületi tesztek

A Playwright tesztek futtatásához a PostgreSQL, backend és frontend szolgáltatásoknak futniuk kell:

```text
docker compose up -d --build
cd frontend
npm ci
npx playwright install chromium
npm run e2e
```

A felületi tesztek lefedik a legutóbbi feldolgozás betöltését, a szűrést és üres eredményt,
az issue dialogot, a feldolgozások accordioni működését, a hibás fájltípust és az API-hiba megjelenítését.

## Működés

1. CSV és JSON fájl feltöltése külön upload műveletben.
2. A feltöltött rekordok forrásonként külön táblába kerülnek.
3. A felhasználó kiválasztja az elsődleges adatforrást.
4. Opcionálisan engedélyezi a hiányzó mezők pótlását a másodlagos forrásból.
5. A backend létrehoz egy normalizálási futást.
6. A normalizált rekordok és validációs hibák külön tárolódnak.
7. A `GET /api/products` a megadott normalizálási futás eredményét olvassa; `runId` nélkül automatikusan a legutóbbi sikeres futást adja vissza.

### Felület működése

- Az Angular felület PrimeNG alapkomponenseket és a hivatalos Aura témát használja, egyedi komponensfelülírás nélkül.
- Az adatforrás-feltöltés a főoldali gomb mögötti PrimeNG dialogban történik.
- A feltöltés és normalizálás alatt a táblázat PrimeNG loading/skeleton állapotot jelenít meg.
- Az alkalmazás induláskor automatikusan betölti a legutóbbi sikeres normalizálási futást.
- A legutóbbi eredményhez tartozó valid/hibás darabszám, keresés, rendezés és `Csak valid` szűrés azonnal elérhető.
- Új feldolgozáskor a feltöltött fájlokból létrejövő futás válik az aktuális eredménnyé.
- A táblázat fejlécében oszloponkénti szűrők vannak, a rendezés a fejléc címére kattintva történik.
- Több oszlop szűrője egyszerre használható; ezek együttesen, összetett `AND` feltételként érvényesülnek.
- Az Issue-k oszlopban egy darabszámot mutató gomb nyitja meg az issue dialogot; az ERROR-ok és WARNING-ok külön csoportban, ebben a sorrendben jelennek meg.

## API

Az interaktív API dokumentáció a Swagger UI felületén érhető el:

```text
http://localhost:8080/swagger-ui/index.html
```

Az OpenAPI 3 specifikáció JSON formátumban:

```text
http://localhost:8080/v3/api-docs
```

### Fájlok feltöltése

```http
POST /api/uploads/csv
POST /api/uploads/json
```

Mindkét végpont `multipart/form-data` formátumban, `file` mezőnévvel várja a fájlt.

### Normalizálási futás indítása

```http
POST /api/processing-runs
Content-Type: application/json

{
  "csvBatchId": 1,
  "jsonBatchId": 2,
  "primarySource": "JSON",
  "fallbackMissingValues": true
}
```

### Termékek lekérdezése

```http
GET /api/products?runId=1&filter=monitor&sort=finalPriceHuf:asc&onlyValid=false
```

A `runId` opcionális. Ha elhagyjuk, a végpont a legutóbbi sikeres normalizálási futás eredményét adja vissza:

```http
GET /api/products?filter=&sort=&onlyValid=
```

Paraméterek:

- `runId`: a lekérdezendő feldolgozási futás azonosítója; hiányában a legutóbbi sikeres futás kerül kiválasztásra.
- `filter`: szabad szöveges szűrő; üres értéknél nincs szűrés.
- `sort`: rendezés `mező:asc` vagy `mező:desc` formátumban; üres értéknél az alapértelmezett rendezés érvényesül.
- `onlyValid`: `true` esetén csak a valid rekordok kerülnek visszaadásra; üres vagy `false` értéknél minden rekord visszakerül.

A felület által használt, adatbázisoldali szűrést és lapozást biztosító végpont:

```http
GET /api/products/page?runId=1&page=0&size=10&sort=sku:asc&sku=SKU1001&valid=true
```

A `page` nulla alapú, a `size` értéke 1 és 100 közötti lehet. Támogatott szűrőparaméterek:
`filter`, `sku`, `name`, `manufacturer`, `finalPriceHuf`, `stock`, `ean`, `source`, `valid` és
`onlyValid`. A válasz `content`, `page`, `size`, `totalElements` és `totalPages` mezőket tartalmaz.
Az eredeti, lapozás nélküli `GET /api/products` végpont továbbra is elérhető.

Engedélyezett rendezési mezők: `sku`, `name`, `manufacturer`, `finalPriceHuf`, `stock`, `updatedAt`, `source`.
