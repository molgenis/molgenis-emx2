# FAIRmapper MVP Review - SQL Expert Data Manager

**Reviewer perspective:** Thinks in tables/joins/queries, used dbt, prefers declarative

## 1. Documentation Clarity: 3/5

**What works:**
- Getting Started is solid, step-by-step
- Test structure (input.json → output.json) immediately makes sense - like dbt fixtures
- Schema Reference comprehensive with nice table layout

**What's confusing:**
- "Bundle" terminology unclear - why not "project" or "pipeline definition"?
- Data flow not explicit - had to hunt for "output of each step becomes input to next"
- Missing: What does MOLGENIS schema look like? No table structures or ERD
- README says "without writing Java" but I'm learning JSLT, JSON-LD, GraphQL = 3 new languages

**Critical gap:** No ERD or schema diagram. In SQL land, I'd start with `SHOW CREATE TABLE`.

## 2. Pipeline Syntax: Declarative but verbose

**Feels familiar:**
```yaml
steps:
  - fetch: ${SOURCE_URL}          # Like FROM clause
  - transform: transform.jslt     # Like SELECT with CTEs
  - mutate: mutation.gql          # Like INSERT/UPDATE
```

**But:**
- `fetch` + `frame` is two-step concept crammed into one
- Tests nested under transforms - in dbt, tests are separate files
- `${SOURCE_URL}` feels like bash scripting, not declarative

**What I'd prefer (SQL-like):**
```yaml
sources:
  fdp: ${SOURCE_URL}

pipeline:
  - from: fdp
    filter: frame.jsonld
  - select: transform.jslt
  - insert: mutation.gql
```

Clearer verbs: FROM, SELECT, INSERT instead of fetch, transform, mutate.

## 3. GraphQL Impression: Readable with caveats

**Good:**
- GraphQL queries look like SQL SELECT
- Mutations are clean

**Confusing:**
- Filter structure is opaque - where's the filter grammar?
- Nested objects - does this JOIN automatically? LEFT JOIN?
- No aggregation examples (COUNT, GROUP BY)

**For SQL people:** GraphQL fine for reading, writing filters requires trial-and-error.

## 4. Top 3 Syntax Simplifications

**1. Replace JSLT with SQL-like syntax**
```sql
SELECT
  split_part(json_extract(data, '@id'), '/', -1) AS id,
  json_extract(data, 'dcterms:title') AS name
FROM input
```
Or jq which more data people know. JSLT is Schibsted-specific.

**2. Make tests first-class, not nested**
```yaml
transforms:
  - file: transform.jslt

tests:
  transform:
    - case: basic
      input: test/basic-in.json
      expect: test/basic-out.json
```

**3. Explicit data contracts between steps**
```yaml
steps:
  - fetch: ${SOURCE_URL}
    output_schema: schemas/framed-catalog.json

  - transform: transform.jslt
    input_schema: schemas/framed-catalog.json
    output_schema: schemas/molgenis-resources.json
```
Like dbt's `schema.yml`.

## 5. Overall Impression: 6/10 - Functional but not delightful

**Would I use this over:**
- **Python + pandas?** Yes, for repetitive ETL
- **dbt?** No. dbt has better docs, testing, lineage, and I know SQL
- **Custom Java service?** Hell yes. Don't want to wait for devs

**Dealbreakers:**
1. JSLT is niche - if learning new language, prefer jq or SQL
2. No schema visibility without clicking around web UI
3. Error messages not shown in docs

**Would adopt IF:**
- Already in MOLGENIS ecosystem
- Do frequent DCAT/RDF transforms
- Someone else maintains bundles

**Would NOT adopt IF:**
- Doing general ETL (pandas easier)
- Need to onboard junior analysts
- Want SQL-native transforms (use dbt)

**Strengths:**
- Solves real problem (API adapters without Java)
- Declarative config is right direction
- Testing built-in

**Weaknesses:**
- Niche transformation language (JSLT)
- Missing schema docs
- Tests buried in config
- "No code" claim misleading

**Critical question:** Why not support SQL via DuckDB?
