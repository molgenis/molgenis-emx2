# FAIRmapper Demo Script

## Story: Publishing a FAIR Data Point

"We have a Data Catalogue with networks and cohorts. We want to expose this as a FAIR Data Point so other systems can discover and harvest our metadata."

### Prerequisites
- MOLGENIS running locally (http://localhost:8080)
- Schema with DATA_CATALOGUE profile (e.g., `catalogue`)

---

## Demo 1: FDP Root (The Entry Point)

**Story:** "First, let's see the FDP root - this is where harvesters start"

```bash
# JSON-LD (default)
curl http://localhost:8080/catalogue/api/fdp

# Turtle (RDF format)
curl -H "Accept: text/turtle" http://localhost:8080/catalogue/api/fdp

# Validate against FDP SHACL
curl "http://localhost:8080/catalogue/api/fdp?validate=fdp-1.2"
```

**What to show:**
- FDP metadata: title, description, publisher
- Links to catalogs (`fdp-o:metadataCatalog`)
- FDP spec conformance

---

## Demo 2: Catalog (A Network/Catalogue)

**Story:** "Each network or catalogue becomes a dcat:Catalog with its datasets"

```bash
# Get a specific catalog (replace ID with actual)
curl http://localhost:8080/catalogue/api/fdp/catalog/umcg-lifelines

# Show linked datasets
curl -H "Accept: application/ld+json" http://localhost:8080/catalogue/api/fdp/catalog/umcg-lifelines
```

**What to show:**
- Catalog metadata: title, description, license
- Nested datasets (`dcat:dataset`)
- Link back to FDP root (`dct:isPartOf`)

---

## Demo 3: Dataset (A Cohort/Biobank)

**Story:** "Each cohort or biobank is exposed as a dcat:Dataset"

```bash
# Get a specific dataset
curl http://localhost:8080/catalogue/api/fdp/dataset/lifelines

# With validation
curl "http://localhost:8080/catalogue/api/fdp/dataset/lifelines?validate=fdp-1.2"
```

**What to show:**
- Dataset metadata: title, description
- Link to parent catalog (`dct:isPartOf`)
- FDP metadata (identifier, issued, modified)

---

## Demo 4: SQL vs GraphQL+JSLT Comparison

**Story:** "Data managers can choose SQL for simpler mappings"

Show the SQL file:
```bash
cat fair-mappings/dcat-fdp-sql/src/queries/get-fdp-root.sql
```

**Key points:**
- Single SQL query builds JSON-LD directly
- Uses `json_build_object()` for structure
- Parameterized with `${schema}`, `${base_url}`
- No JSLT transform needed

---

## Demo 5: SHACL Validation

**Story:** "We can validate our output against FDP SHACL shapes"

```bash
# Valid output shows success
curl "http://localhost:8080/catalogue/api/fdp?validate=fdp-1.2"

# Invalid output shows violations (if any)
```

**What to show:**
- Validation report format
- SHACL constraint checking
- How to fix violations

---

## Quick Test Commands

```bash
# Test all endpoints exist
curl -I http://localhost:8080/catalogue/api/fdp
curl -I http://localhost:8080/catalogue/api/fdp/catalog/TEST_ID
curl -I http://localhost:8080/catalogue/api/fdp/dataset/TEST_ID

# Find catalog/dataset IDs
curl http://localhost:8080/catalogue/graphql -d '{"query":"{ Resources(limit:5) { id name type { name } } }"}' -H "Content-Type: application/json"
```

---

## Future Demo: Self-Harvest

"Now let's harvest our own FDP back into MOLGENIS..."

```bash
# Harvest from our own endpoint
./fairmapper run \
  --bundle fair-mappings/dcat-harvester \
  --source http://localhost:8080/catalogue/api/fdp \
  --target http://localhost:8080/harvest-test
```
