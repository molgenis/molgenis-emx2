# PR Splitting Strategy for #5495

## Status: PR-1 ready for review

## Overview
Split large PR #5495 (71 files, ~5800 lines) into incremental PRs.
New endpoints run **side-by-side** with existing ones — no replacements until proven equivalent.

## Branch Strategy
- `feat/rest-json-ld-graphql` = full reference (kept intact)
- `feat/rest-json-ld-graphql-backup` = safety copy
- New branches from `origin/master` per PR

## Side-by-Side Strategy
New endpoints use `-rest` suffix to coexist with existing RDFApi:
```
Existing (RDFApi, unchanged):     /{schema}/api/jsonld/   /{schema}/api/ttl/
New (JsonLdApi):                  /{schema}/api/jsonld-rest/   /{schema}/api/ttl-rest/
```
Benefits:
- Both available simultaneously for comparison
- Tests can be parameterized to hit both old and new endpoints
- Semantic comparison (parse both as RDF, compare triples) proves equivalence
- When confident: swap paths, old becomes `-legacy`, tests still pass

## Completed

### Phase 0: Prepare
- [x] Add backward-compat route aliases (commit `30fa46de6`)
- [x] Push branch as backup

### Phase 1: GraphQL Fragments (#5762)
- [x] Merged to master
- [x] GraphqlTableFragmentGenerator + GraphqlExecutor fragment resolution

### PR-1: JSON-LD + TTL REST Endpoints (~1265 lines)
Branch: `mswertz/feat-jsonld-rest-api` (from `origin/master`)
Status: **implementation done, tests passing, ready for commit + PR**

New files:
- `rdf/jsonld/JsonLdSchemaGenerator.java` — generates @context from schema metadata
- `graphql/jsonld/JsonLdValidator.java` — validates JSON-LD structure
- `graphql/jsonld/RestOverGraphql.java` — GraphQL → JSON-LD/TTL bridge
- `webapi/web/JsonLdApi.java` — REST endpoints (jsonld-rest + ttl-rest)
- `graphql/jsonld/TestJsonLdSchemaGenerator.java` — unit tests

Modified files:
- build.gradle x2 (dependency inversion: graphql → rdf)
- `GraphqlExecutor.java` (+schema field, query helpers)
- `ApplicationCachePerUser.java` (+JSON-LD context cache)
- `Constants.java` x2 (+ACCEPT_JSONLD, ACCEPT_TTL, MG_ID)
- `DownloadApiUtils.java` (+shared helpers)
- `MolgenisWebservice.java` (+JsonLdApi registration)
- `WebApiSmokeTests.java` (+3 smoke tests)

Endpoints:
```
/{schema}/api/jsonld-rest/_schema     GET/POST/DELETE
/{schema}/api/jsonld-rest/_all        GET
/{schema}/api/jsonld-rest/_context    GET
/{schema}/api/jsonld-rest/{table}     GET/POST/DELETE
/{schema}/api/jsonld-rest/{table}/*   GET/PUT/DELETE
/{schema}/api/ttl-rest/_schema        GET
/{schema}/api/ttl-rest/_all           GET
/{schema}/api/ttl-rest/{table}        GET
/{schema}/api/ttl-rest/{table}/*      GET
```

## Upcoming

### PR-2: Comparison Tests + Equivalence Proof
Branch: TBD (from master after PR-1 merges)

Parameterized tests that hit both old (RDFApi) and new (JsonLdApi) endpoints:
- Parse both outputs as RDF models
- Compare triple sets for semantic equivalence
- Document any intentional differences
- This proves the new implementation is correct before any path swapping

### PR-3: Core Infrastructure
Branch: TBD

Move PrimaryKey to core module, TypeUtils enhancements, MolgenisException updates.
Only needed when we start building the full multi-format RestApi.

### PR-4: Content-Negotiated Data API
Branch: TBD

`DataApi.java` — single endpoint serving JSON/JSON-LD/TTL/CSV/Excel based on Accept header.
Depends on: PR-1, PR-3

### PR-5: Full REST API + Legacy Aliases
Branch: TBD

`RestApi.java` — full multi-format REST with all system endpoints (_members, _settings, _changelog).
RDFApi routes move to `-legacy` suffix.
CsvApi/ExcelApi/ZipApi refactoring.
Depends on: PR-2 proving equivalence, PR-4

### PR-6: Docs + Frontend
Branch: TBD

API docs, SHACL UI path updates, Python client, Import.vue cleanup.
Depends on: PR-5

## Merge Order
```
#5762 (graphql-fragments) ✅
    ↓
PR-1 (jsonld-rest endpoints, side-by-side)  ← WE ARE HERE
    ↓
PR-2 (comparison tests proving equivalence)
    ↓
PR-3 (core infrastructure)
    ↓
PR-4 (content-negotiated data API)
    ↓
PR-5 (full REST API, old paths → legacy)
    ↓
PR-6 (docs + frontend)
```

## After All PRs Merge
- `feat/rest-json-ld-graphql` branch becomes obsolete
- Master will contain all changes
- Old `-legacy` endpoints can be removed after deprecation period
