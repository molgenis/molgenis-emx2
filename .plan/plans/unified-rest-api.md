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

| # | Issue | File | Action |
|---|-------|------|--------|
| 1 | Endpoint renaming impact | General | Verify renamed endpoints not used externally |
| 2 | RDF API breaking changes | General | Consider major version release |
| 3 | UUID type mapping wrong | JsonLdSchemaGenerator | UUID → IRI (`urn:uuid:`), not Literal |

### 🟠 Must Fix (Code Quality)

| # | Issue | File | Action |
|---|-------|------|--------|
| 4 | `sendJsonMessage` unused | DownloadApiUtils | Remove |
| 5 | `setDownloadHeaders` unused | DownloadApiUtils | Remove |
| 6 | `getXsdType` unused | JsonLdSchemaGenerator | Remove + ColumnType addition |
| 7 | Dead validation code | RestOverGraphql | Remove |
| 8 | Test assertions weak | WebApiSmokeTests | Fix - pass even when not logged in |
| 9 | Service naming inconsistent | GraphqlApiService | Rename - breaks convention |
| 10 | Hardcoded "my:" prefix | JsonLdSchemaGenerator | Use NamespaceMapper instead |

### 🟡 Technical Improvements

| # | Issue | File | Action |
|---|-------|------|--------|
| 11 | Leverage Jackson more | JsonLdValidator | Simplify with Jackson features |
| 12 | Recursion depth risk | JsonLdValidator | Add safeguard or document depth |
| 13 | Load all for conversion | RestOverGraphql | Consider streaming instead |
| 14 | Turtle-only conversion | RestOverGraphql | Support any RDF format |
| 15 | Manual timing calls | DownloadApiUtils | Use Javalin response headers |
| 16 | DateTime format | TypeUtils | Ensure `YYYY-MM-DDThh:mm:ss` |
| 17 | URI scheme validation limited | RestOverGraphql | Expand allowed schemes |

### 🟢 Nice to Have

| # | Issue | File | Action |
|---|-------|------|--------|
| 18 | Repeated strings | WebApiSmokeTests | Extract constants |
| 19 | Needs specific tests | JsonLdValidator | Add unit tests |
| 20 | Code extraction | GraphqlTableFieldFactory | Extract to own place |
| 21 | Trivial method extraction | DownloadApiUtils | Review if method adds value |

### Review Response Status

- [ ] Respond to reviewers on GitHub
- [ ] Critical items resolved
- [ ] Must-fix items resolved
- [ ] Request re-review
