# FAIRmapper Demo Script

## FDP Endpoints (SQL-based)

| Endpoint | Link |
|----------|------|
| FDP Root | http://localhost:8080/catalogue/api/fdp |
| Catalog: ATHLETE | http://localhost:8080/catalogue/api/fdp/catalog/ATHLETE |
| Catalog: LifeCycle | http://localhost:8080/catalogue/api/fdp/catalog/LifeCycle |
| Dataset: EDEN | http://localhost:8080/catalogue/api/fdp/dataset/EDEN |
| Dataset: GECKO | http://localhost:8080/catalogue/api/fdp/dataset/GECKO |
| Validate FDP | http://localhost:8080/catalogue/api/fdp?validate=fdp-v1.2 |

---

## Demo 1: FDP Root (Entry Point)

**Story:** "The FDP root is where harvesters start - it lists all available catalogs"

http://localhost:8080/catalogue/api/fdp

**What you see:**
- FDP metadata (title, description, publisher)
- List of catalogs: ATHLETE, LifeCycle, LongITools, etc.
- FDP spec conformance

---

## Demo 2: Catalog (Network)

**Story:** "Each network becomes a dcat:Catalog listing its cohorts/biobanks"

http://localhost:8080/catalogue/api/fdp/catalog/ATHLETE

**What you see:**
- Network description
- 12 linked datasets (EDEN, INMA, DNBC, etc.)
- Link back to FDP root

---

## Demo 3: Dataset (Cohort)

**Story:** "Each cohort is exposed as dcat:Dataset with rich metadata"

http://localhost:8080/catalogue/api/fdp/dataset/EDEN

**What you see:**
- Cohort description
- Link to parent catalog (ATHLETE)
- FDP metadata (issued, modified dates)

---

## Demo 4: Navigate the links

**Story:** "All internal URLs in the RDF are clickable and resolve"

1. Start at: http://localhost:8080/catalogue/api/fdp
2. Click any catalog link → e.g., LifeCycle
3. Click any dataset link → e.g., GECKO
4. Click `dct:isPartOf` → back to catalog

---

## Demo 5: SHACL Validation

**Story:** "We can validate output against FDP SHACL shapes"

http://localhost:8080/catalogue/api/fdp?validate=fdp-v1.2

---

## Beacon v2 Endpoints (rd3 schema)

| Endpoint | Link |
|----------|------|
| Beacon Info | http://localhost:8080/rd3/api/beacon |
| Individuals | http://localhost:8080/rd3/api/beacon/individuals |
| Individual: Case1F | http://localhost:8080/rd3/api/beacon/individuals/Case1F |
| Pagination | http://localhost:8080/rd3/api/beacon/individuals?skip=5&limit=3 |
| Beacon Map | http://localhost:8080/rd3/api/beacon/map |

---

## Demo 6: Beacon Queries

**Story:** "Beacon allows federated queries across biobanks with privacy controls"

**Basic query:**
http://localhost:8080/rd3/api/beacon/individuals

**With pagination:**
http://localhost:8080/rd3/api/beacon/individuals?skip=5&limit=3

**Specific individual:**
http://localhost:8080/rd3/api/beacon/individuals/Case1F

**What you see:**
- 23 individuals in rd3 dataset
- Sex, age info per individual
- Pagination support

---

## Demo 7: Beacon Granularity (POST only)

**Story:** "Privacy controls via granularity - from records to counts to yes/no"

```bash
# Count only
curl -X POST http://localhost:8080/rd3/api/beacon/individuals \
  -H "Content-Type: application/json" \
  -d '{"query":{"requestedGranularity":"count"}}'

# Boolean only
curl -X POST http://localhost:8080/rd3/api/beacon/individuals \
  -H "Content-Type: application/json" \
  -d '{"query":{"requestedGranularity":"boolean"}}'
```

---

## Sample IDs

**Catalogs:** ATHLETE, LifeCycle, LongITools, EUChildNetwork

**Datasets:** EDEN, INMA, DNBC, GenR, MoBa, GECKO

**Individuals:** Case1F, Case1C, Case2M, Case3C
