# Unified REST API Plan

**Branch:** `feat/rest-json-ld-graphql`
**PR:** #5495

---

## Resolved Design Questions

| Question | Answer |
|----------|--------|
| Semantic annotations | **Optional** - auto from data model, or skip non-semantic parts |
| Refback columns | **Same as ref_array** in queries |
| FILE type in RDF | **Point to file URL** |
| Circular references | **Not an issue** - deferred foreign keys |
| DELETE with filter | **No** - based on pkey in data payload |

---

## Implementation Status

### Complete ✅

| Component | Notes |
|-----------|-------|
| **mg_id computed field** | PrimaryKey in core, GraphQL field added |
| **RestApi.java** | All formats: json, yaml, jsonld, ttl + SHACL endpoints |
| **DataApi.java** | Content-negotiation gateway |
| **CSV/Excel/ZIP APIs** | Underscore-prefixed system endpoints |
| **Python client endpoints** | Already uses new patterns (`/api/csv/_schema`, etc.) |

### In Progress 🔄

| Component | Issue |
|-----------|-------|
| **RDFApi.java** | Overlaps with RestApi - needs deprecation (Phase 1) |
| **SHACL UI** | Uses old `/api/rdf` paths - needs update to `/api/ttl` (Phase 3) |
| **Import.vue** | References deprecated `/api/ttl2` (Phase 3) |

---

## Remaining Work

### Phase 1: RDFApi Deprecation (Backend)

**Move RDFApi to legacy paths:**
```
/api/rdf        → /api/rdf-legacy
/api/ttl        → /api/ttl-legacy
/api/jsonld     → /api/jsonld-legacy
```

**Files:**
- [ ] `RDFApi.java` - change route constants to `-legacy` suffix
- [ ] Add `Deprecation` header in responses
- [ ] Keep all functionality for comparison testing

### Phase 2: Add SHACL to RestApi (Backend) ✅

**New endpoints in RestApi:**
```
GET /api/ttl?shacls                        → List available SHACL sets (YAML)
GET /api/jsonld?shacls                     → Same
GET /{schema}/api/ttl/_schema?validate=X   → Validate schema data against SHACL set
GET /{schema}/api/jsonld/_schema?validate=X → Same for JSON-LD
```

**Implementation:**
- [x] Add SHACL query param handling to RestApi.java
- [x] Reuse: `ShaclSelector`, `ShaclSet`, `RdfSchemaValidationService`
- [x] Return validation report in requested format (TTL or JSON-LD)

### Phase 3: Frontend + Python Client

**SHACL UI updates:**
- [ ] `apps/ui/app/pages/[schema]/shacl/index.vue` - change `/api/rdf?shacls` → `/api/ttl?shacls`
- [ ] `apps/ui/app/util/shaclUtils.ts` - change `/{schema}/api/rdf?validate=` → `/{schema}/api/ttl/_schema?validate=`

**Import.vue cleanup:**
- [ ] Remove `/api/ttl2` references
- [ ] Use `/api/jsonld/_context` for context file

**Python client:**
- [ ] Verify all endpoints still work (mostly already updated)
- [ ] Add JSON/YAML import/export methods
- [ ] Add SHACL validation method: `client.validate_shacl(schema, shacl_set_id)`
- [ ] Document new endpoint patterns

### Phase 4: Documentation

- [ ] `docs/molgenis/use_usingapis.md` - Update endpoint examples
- [ ] `docs/molgenis/dev_batchapi.md` - Update batch examples
- [ ] `docs/molgenis/dev_rdf.md` - Document semantic API + SHACL

### Phase 5: Testing

- [ ] Update `WebApiSmokeTests.java`
- [ ] Add SHACL validation tests via RestApi
- [ ] Test content negotiation (DataApi)
- [ ] Test filter/limit/offset for all formats

### Phase 6: OpenAPI (Low Priority)

- [ ] Add Javalin OpenAPI plugin
- [ ] Annotate REST endpoints
- [ ] Generate Swagger UI

---

## Target Architecture

```
RestApi.java (primary)
├── /api/json/...      → Plain JSON
├── /api/yaml/...      → YAML
├── /api/jsonld/...    → JSON-LD with @context
│   └── ?shacls        → List SHACL sets
│   └── ?validate=X    → SHACL validation
├── /api/ttl/...       → Turtle RDF
│   └── ?shacls        → List SHACL sets
│   └── ?validate=X    → SHACL validation
└── GraphQL-based queries internally

RDFApi.java (deprecated, for testing)
├── /api/rdf-legacy/...
├── /api/ttl-legacy/...
└── /api/jsonld-legacy/...

DataApi.java (content-negotiated)
└── /api/data/...      → Routes by Accept header
```

---

## Endpoint Reference

### Format-Specific (RestApi)

| Endpoint | Methods | Notes |
|----------|---------|-------|
| `/{schema}/api/{format}/_schema` | GET, POST, DELETE | Schema metadata |
| `/{schema}/api/{format}/_data` | GET | All table data |
| `/{schema}/api/{format}/_all` | GET | Schema + data |
| `/{schema}/api/{format}/_members` | GET | Permissions |
| `/{schema}/api/{format}/_settings` | GET | Settings |
| `/{schema}/api/{format}/_changelog` | GET | Audit log |
| `/{schema}/api/{format}/_context` | GET | JSON-LD context (jsonld only) |
| `/{schema}/api/{format}/{table}` | GET, POST, DELETE | Table data |
| `/{schema}/api/{format}/{table}/{id}` | GET, PUT, DELETE | Row data |

**Formats:** json, yaml, jsonld, ttl

### Query Parameters

| Param | Applies to | Purpose |
|-------|-----------|---------|
| `filter` | `/{table}` | JSON filter (GraphQL syntax) |
| `limit` | `/{table}`, `/_changelog` | Max rows |
| `offset` | `/{table}`, `/_changelog` | Skip rows |
| `search` | `/{table}` | Text search |
| `query` | `/_data`, `/_all` | Custom GraphQL query |
| `shacls` | root | List SHACL sets |
| `validate` | schema root | Run SHACL validation |

### Content-Negotiated (DataApi)

| Accept Header | Format |
|---------------|--------|
| `application/json` | JSON (default) |
| `application/ld+json` | JSON-LD |
| `text/turtle` | TTL |
| `text/csv` | CSV |
| `application/vnd.ms-excel` | Excel |
| `application/zip` | ZIP |

---

## Semantic Output Modes

| Mode | When | Output |
|------|------|--------|
| **Full semantics** | Schema has semantic annotations | @context with actual semantic URIs |
| **Auto semantics** | No annotations | @context with `my:{field}` predicates |
| **Plain JSON** | `/api/json/` | No @context, no @type |

---

## mg_id Status

**Complete:**
- ✅ PrimaryKey class in core module
- ✅ Array ref bug fixed
- ✅ Null validation added
- ✅ mg_id computed field in GraphQL

**Future (low priority):**
- [ ] Maybe: Change delimiter `&` → `,` (less encoding)
- ~~Single pkey value only~~ → Rejected: keep `key=value` format
- Nobody uses mg_id URLs yet, so changes are low risk if needed

---

## Files to Modify

### Backend
| File | Change |
|------|--------|
| `RDFApi.java` | Routes → `-legacy` suffix |
| `RestApi.java` | Add `?shacls`, `?validate=` |

### Frontend
| File | Change |
|------|--------|
| `apps/ui/.../shacl/index.vue` | `/api/rdf?shacls` → `/api/ttl?shacls` |
| `apps/ui/.../shaclUtils.ts` | `/api/rdf?validate=` → `/api/ttl/_schema?validate=` |
| `apps/updownload/.../Import.vue` | Remove `/api/ttl2` refs |

### Python
| File | Change |
|------|--------|
| `client.py` | Add SHACL method, verify endpoints |

### Docs
| File | Change |
|------|--------|
| `use_usingapis.md` | Update examples |
| `dev_rdf.md` | Document SHACL |

---

## Cleanup Summary

| Remove | Reason |
|--------|--------|
| `/api/ttl2` refs in Import.vue | Deprecated experimental |
| `/api/rdf` (move to legacy) | Replaced by RestApi |
| Old plan files | Merged into this plan |

---

## PR Review Feedback (#5495)

**Reviewers:** jhhaanstra, svandenhoek

### 🔴 Critical (API/Breaking changes)

| # | Issue | File | Status |
|---|-------|------|--------|
| 1 | ~~Endpoint renaming impact~~ | General | ✅ Verified |
| 2 | ~~RDF API breaking changes~~ | General | ✅ Discussed with team |
| 3 | ~~Proper type mapping~~ | JsonLdSchemaGenerator | ✅ Uses ColumnTypeRdfMapper |

### 🟠 Must Fix (Code Quality)

| # | Issue | File | Status |
|---|-------|------|--------|
| 4 | ~~`sendJsonMessage` unused~~ | DownloadApiUtils | ✅ Removed |
| 5 | ~~`setDownloadHeaders` unused~~ | DownloadApiUtils | ✅ Removed |
| 6 | ~~`getXsdType` unused~~ | JsonLdSchemaGenerator | ✅ Removed |
| 7 | ~~Dead validation code~~ | RestOverGraphql | ✅ Removed |
| 8 | ~~Test assertions weak~~ | WebApiSmokeTests | ✅ Fixed |
| 9 | ~~Service naming inconsistent~~ | GraphqlApiService | ✅ GraphqlApi→GraphqlExecutor |
| 9b | ~~Test-only execute() methods~~ | GraphqlExecutor | ✅ Removed, callers updated |
| 10 | ~~Hardcoded "my:" prefix~~ | JsonLdSchemaGenerator | ✅ Uses NamespaceMapper |
| 10b | ~~Blank nodes~~ | RestOverGraphql | ✅ Never allow (error on blank nodes) |

### 🟡 Technical Improvements

| # | Issue | File | Status |
|---|-------|------|--------|
| 11 | Leverage Jackson more | JsonLdValidator | Low priority |
| 12 | Recursion depth risk | JsonLdValidator | Low priority |
| 13 | ~~Streaming conversion~~ | RestOverGraphql | ⏸️ POSTPONE: separate PR (see streaming-rdf-export.md) |
| 14 | ~~Format conversion~~ | RestOverGraphql | ✅ Done in DataApi content negotiation |
| 15 | Manual timing calls | DownloadApiUtils | Low priority |
| 16 | ~~DateTime format~~ | ColumnTypeRdfMapper | ✅ Uses `yyyy-MM-dd'T'HH:mm:ss` |
| 17 | URI scheme validation | RestOverGraphql | Low priority |

### 🟢 Nice to Have

| # | Issue | File | Status |
|---|-------|------|--------|
| 18 | ~~Media type constants~~ | WebApiSmokeTests | ✅ Using Constants.* |
| 19 | JsonLdValidator tests | JsonLdValidator | Low priority |
| 20 | Code extraction | GraphqlTableFieldFactory | Low priority |
| 21 | Trivial method extraction | DownloadApiUtils | Low priority |

### Remaining TODO (Priority Order)

All review items complete! ✅

---

## Implementation Plan for Remaining Items

### Task A: Refactor JsonLdSchemaGenerator to use ColumnTypeRdfMapper (#3)

**Problem:** JsonLdSchemaGenerator has incomplete `getXsdType()` duplicating logic from ColumnTypeRdfMapper

**Solution:**
1. Remove `getXsdType()` method from JsonLdSchemaGenerator
2. Use `ColumnTypeRdfMapper.getCoreDataType(columnType)` instead
3. Handle SKIP types (HEADING, SECTION) gracefully

**Files:**
- `backend/molgenis-emx2-rdf/src/main/java/org/molgenis/emx2/rdf/JsonLdSchemaGenerator.java`

**Benefits:**
- Single source of truth for type mappings
- All 33 ColumnTypes covered (currently only 11)
- UUID, FILE, dates automatically correct

### Task B: Use Media Type Constants (#18)

**Problem:** WebApiSmokeTests uses hardcoded strings instead of Constants.*

**Solution:**
1. Add missing constants to Constants.java:
   - `ACCEPT_PLAIN = "text/plain"`
   - `ACCEPT_N3 = "text/n3"`
2. Replace hardcoded strings in WebApiSmokeTests with constants

**Files:**
- `backend/molgenis-emx2-webapi/src/main/java/org/molgenis/emx2/web/Constants.java`
- `backend/molgenis-emx2-webapi/src/test/java/org/molgenis/emx2/web/WebApiSmokeTests.java`

**Counts:**
- "application/json" → ACCEPT_JSON (16 occurrences)
- "application/ld+json" → ACCEPT_JSONLD (9 occurrences)
- "text/turtle" → ACCEPT_TTL (6 occurrences)
- "text/csv" → ACCEPT_CSV (1 occurrence)
- "text/plain" → ACCEPT_PLAIN (5 occurrences) - NEW constant
- "text/n3" → ACCEPT_N3 (1 occurrence) - NEW constant

### Review Response Status

- [ ] Respond to reviewers on GitHub
- [x] Critical items resolved (1, 2, 3)
- [x] Must-fix items resolved
- [x] Nice-to-have 18 resolved
- [ ] Request re-review
