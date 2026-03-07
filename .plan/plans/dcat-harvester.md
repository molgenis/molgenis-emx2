# DCAT Harvester — Plan

**Spike**: `spike/etl_pilot` (building blocks)
**Target branch**: `mswertz/feat/dcat-harvester` (from remote master)
**Module**: `backend/molgenis-emx2-dcat-harvester/`

## Context

The `spike/etl_pilot` branch built a full declarative ETL framework (FAIRmapper) with 7 step types, CLI, security, and 4 example bundles. That's too much generality for a first deliverable. We need a **focused DCAT harvester** that can fetch RDF from FAIR Data Points and import into the MOLGENIS DataCatalogue model — cherry-picking only what's needed from the spike.

## Decisions (opinionated)

| Question | Decision | Rationale |
|----------|----------|-----------|
| Scope | Focused harvester, not full FAIRmapper | Ship something usable. The generic engine can come later. |
| Integration | Embedded in MOLGENIS server (Java task/service) | CLI-only means ops overhead. Server-embedded = trigger from UI/API, logs visible, reuses existing auth. |
| Target model | DataCatalogue profile (Resources + Organisations + Contacts) | This is the existing catalogue model with 221 columns. Start with core fields, expand later. |
| Mapping approach | JSLT transform (from spike) | Proven in spike. Declarative YAML mapping is elegant but adds complexity. JSLT is simpler, testable, sufficient. |
| First source | Any DCAT-AP compliant FDP | Don't hardcode to one FDP. Frame-driven fetch handles structural differences. |
| Trigger | REST endpoint + manual UI button (later: scheduled) | MVP = API endpoint. Scheduling is a separate concern. |
| Error handling | Log warnings per record, don't fail batch | Partial harvest > no harvest. Log what was skipped and why. |

## Architecture

```
POST /{schema}/api/harvest/dcat?url=https://fdp.example.org/catalog/123
  │
  ├─ 1. RdfFetcher: GET url (Turtle/JSON-LD, content negotiation)
  ├─ 2. FrameDrivenFetcher: follow @embed links recursively
  ├─ 3. JsonLdFramer: apply catalog-with-datasets.jsonld frame
  ├─ 4. JSLT transform: framed JSON-LD → MOLGENIS mutation shape
  └─ 5. Java API: upsert Resources, Organisations, Contacts
```

## What to take from the spike

### Keep (copy & adapt)
| Component | Spike path (relative to etl_pilot/) | Purpose |
|-----------|--------------------------------------|---------|
| `RdfFetcher` | `backend/.../fairmapper/rdf/RdfFetcher.java` | HTTP GET with retries, content negotiation |
| `FrameAnalyzer` | `backend/.../fairmapper/rdf/FrameAnalyzer.java` | Extract @embed predicates from frame |
| `FrameDrivenFetcher` | `backend/.../fairmapper/rdf/FrameDrivenFetcher.java` | Recursive link following |
| `JsonLdFramer` | `backend/.../fairmapper/rdf/JsonLdFramer.java` | Titanium JSON-LD framing |
| `RdfToJsonLd` | `backend/.../fairmapper/rdf/RdfToJsonLd.java` | RDF Model → JSON-LD conversion |
| `RdfSource` | `backend/.../fairmapper/rdf/RdfSource.java` | Interface for testability |
| `UrlValidator` | `backend/.../fairmapper/security/UrlValidator.java` | SSRF protection |
| `JsltTransformEngine` | `backend/.../fairmapper/JsltTransformEngine.java` | JSLT execution |
| `catalog-with-datasets.jsonld` | `fair-mappings/dcat-harvester/src/` | JSON-LD frame |
| `to-molgenis.jslt` | `fair-mappings/dcat-harvester/src/` | DCAT→MOLGENIS transform |
| Test fixtures | `fair-mappings/dcat-harvester/test/` | Unit test data |

### Drop (not needed for MVP)
- BundleLoader, fairmapper.yaml, CLI framework (Picocli)
- MappingEngine, MappingStep, MappingScope
- SparqlEngine, SparqlConstructStep, SqlQueryStep
- ContentNegotiator, PathValidator
- All 4 bundles as bundles (inline dcat-harvester logic)
- GraphqlClient (use MOLGENIS Java API directly)

## DCAT → DataCatalogue Field Mapping (MVP)

### Resources table
| DCAT predicate | Resources column | Notes |
|----------------|-----------------|-------|
| `@id` | `id` | Extract slug from IRI |
| `rdf:type` (dcat:Catalog/Dataset) | `type` | Map to ontology: Catalogue/Databank |
| `dcterms:title` | `name` | Required |
| `dcterms:description` | `description` | |
| `dcterms:identifier` | `pid` | DOI or other PID |
| `dcat:keyword` | `keywords` | String array |
| `dcat:landingPage` | `website` | URL |
| `dcterms:issued` | `issued` | Datetime |
| `dcterms:modified` | `modified` | Datetime |
| `dcat:dataset` | `data resources` | Ref array (Catalogues→Datasets) |

### Organisations table
| DCAT predicate | Organisations column | Notes |
|----------------|---------------------|-------|
| `dcterms:publisher` → `foaf:name` | `id` | Org name as ID |

### Contacts table
| DCAT predicate | Contacts column | Notes |
|----------------|----------------|-------|
| `dcat:contactPoint` → `vcard:fn` | `first name` + `last name` | Parse or use as-is |
| `dcat:contactPoint` → `vcard:hasEmail` | `email` | |

## Implementation Steps

### Step 0: Worktree setup
```bash
cd /Users/m.a.swertz/git/molgenis-emx2
git fetch origin master
git worktree add /Users/m.a.swertz/git/molgenis-emx2/feat/dcat-harvester -b mswertz/feat/dcat-harvester origin/master
ln -s /Users/m.a.swertz/git/molgenis-emx2/master/.claude /Users/m.a.swertz/git/molgenis-emx2/feat/dcat-harvester/.claude
mkdir -p /Users/m.a.swertz/git/molgenis-emx2/feat/dcat-harvester/.plan/plans
cp /Users/m.a.swertz/git/molgenis-emx2/etl_pilot/.plan/plans/dcat-harvester.md /Users/m.a.swertz/git/molgenis-emx2/feat/dcat-harvester/.plan/plans/
```

### Step 1: Create module structure
- New Gradle submodule: `backend/molgenis-emx2-dcat-harvester/`
- Dependencies: rdf4j, titanium-json-ld, jackson, jslt, molgenis-emx2 core

### Step 2: Copy & adapt RDF infrastructure from spike
- Package: `org.molgenis.emx2.dcat.rdf`
- Copy: RdfFetcher, FrameAnalyzer, FrameDrivenFetcher, JsonLdFramer, RdfToJsonLd, RdfSource
- Copy: UrlValidator (simplified)
- Adapt: Remove CLI/Picocli deps, use MOLGENIS logging
- Copy tests

### Step 3: JSLT transform layer
- Package: `org.molgenis.emx2.dcat.transform`
- Copy JsltTransformEngine (simplified)
- Resource: `catalog-with-datasets.jsonld` frame
- Resource: `to-molgenis.jslt` transform (expand beyond spike)
- Unit tests with spike fixtures

### Step 4: Import service
- Package: `org.molgenis.emx2.dcat`
- `DcatHarvestService` — orchestrates fetch→frame→transform→import
- Uses MOLGENIS Java API directly (Schema, Table, Row)
- Upsert logic: match by `id`, create or update
- Returns harvest report (imported/updated/skipped/errors)

### Step 5: REST endpoint
- Add to `molgenis-emx2-webapi`: `DcatHarvestApi`
- `POST /{schema}/api/harvest/dcat` with body `{"url": "https://..."}`
- Requires EDITOR role on schema
- Returns JSON report

### Step 6: Expand JSLT mapping
- contactPoint → Contacts
- theme → ontology lookup
- temporal coverage (start/end year)
- Graceful handling of missing/malformed fields

### Step 7: Integration test
- Embedded PostgreSQL + DataCatalogue profile
- Local Turtle fixtures served via mock
- Harvest → verify rows in DB

## Verification
1. Unit tests: RDF fetch (mocked), framing, JSLT transform
2. Integration test: full pipeline with embedded PG + local RDF fixtures
3. Manual: `curl -X POST http://localhost:8080/catalogue/api/harvest/dcat -d '{"url":"https://..."}'`

## Interview Questions for Stakeholders

### Must-answer before starting
1. **Which FDPs do you want to harvest first?** (Specific URLs validate the frame against real data)
2. **How often should harvesting run?** (Once-off import vs. nightly sync changes architecture)
3. **What happens on re-harvest?** (Overwrite all? Only update changed? Never delete?)

### Field mapping depth
4. **Which DataCatalogue fields matter most?** (221 columns — what's minimum viable catalogue entry?)
5. **Do you need Organisation matching?** (Create new orgs, or match existing by name/ROR ID?)
6. **How to handle DCAT themes?** (EU URIs → CatalogueOntologies.Themes, or store as keywords?)

### Architecture
7. **Should harvested records be editable?** (Re-harvest could overwrite manual edits. Need "source" flag?)
8. **Multi-schema or single schema?** (One catalogue schema, or allow different targets?)
9. **Authentication to source FDPs?** (Most public, some may need API keys)

### Future direction
10. **Stepping stone to full FAIRmapper?** (Keep architecture compatible, or simplify aggressively?)
11. **Also need DCAT export?** (Ship together or separate?)
12. **SHACL validation before import?** (Validate incoming DCAT, or accept anything parseable?)
