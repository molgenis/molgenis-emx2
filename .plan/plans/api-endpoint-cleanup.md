# API Endpoint Cleanup Plan

## Goal

Standardize all REST API endpoints with consistent naming:
- Format-specific paths: `/api/{format}/{table}`
- System endpoints use `_` prefix: `/_members`, `/_settings`, etc.
- Clean separation between schema metadata and data endpoints

## Breaking Changes

This is a **breaking change** affecting:
- Frontend apps (Import.vue, TableExplorer.vue, ListTables.vue)
- Python client (client.py)
- Documentation (use_usingapis.md, dev_batchapi.md, dev_rdf.md, use_updownload.md)
- Any external integrations using current endpoints

## Complete Endpoint Migration Table

| Category | Current | Target | Status |
|----------|---------|--------|--------|
| **CSV** | | | |
| Schema metadata | `/api/csv` | `/api/csv/_schema` | 🔄 rename |
| All data | - | - | ❌ N/A (single-table format) |
| Complete export | - | - | ❌ N/A (single-table format) |
| Members | `/api/csv/members` | `/api/csv/_members` | 🔄 rename |
| Settings | `/api/csv/settings` | `/api/csv/_settings` | 🔄 rename |
| Changelog | `/api/csv/changelog` | `/api/csv/_changelog` | 🔄 rename |
| Table | `/api/csv/{table}` | `/api/csv/{table}` | ✅ keep |
| Row | - | - | ❌ N/A (tabular format) |
| **JSON** | | | |
| Schema metadata | `/api/json` | `/api/json/_schema` | 🔄 rename |
| All data | - | `/api/json/_data` | ➕ new |
| Complete export | - | `/api/json/_all` | ➕ new |
| Members | - | `/api/json/_members` | ➕ new |
| Settings | - | `/api/json/_settings` | ➕ new |
| Changelog | - | `/api/json/_changelog` | ➕ new |
| Table | - | `/api/json/{table}` | ➕ new |
| Row | - | `/api/json/{table}/{id}` | ➕ new |
| **JSON-LD** | | | |
| Schema metadata | - | `/api/jsonld/_schema` | ➕ new |
| All data | `/api/ttl2/_json` | `/api/jsonld/_data` | 🔄 rename |
| Complete export | - | `/api/jsonld/_all` | ➕ new |
| Context only | `/api/ttl2/_context` | `/api/jsonld/_context` | 🔄 rename |
| Members | - | `/api/jsonld/_members` | ➕ new |
| Settings | - | `/api/jsonld/_settings` | ➕ new |
| Changelog | - | `/api/jsonld/_changelog` | ➕ new |
| Table | `/api/jsonld/{table}` (RDF) | `/api/jsonld/{table}` | 🔄 extend POST/PUT/DELETE |
| Row | `/api/jsonld/{table}/{row}` (RDF) | `/api/jsonld/{table}/{id}` | ✅ keep |
| **TTL** | | | |
| Schema metadata | - | `/api/ttl/_schema` | ➕ new |
| All data | `/api/ttl2/_all` | `/api/ttl/_data` | 🔄 rename |
| Complete export | - | `/api/ttl/_all` | ➕ new |
| Members | - | `/api/ttl/_members` | ➕ new |
| Settings | - | `/api/ttl/_settings` | ➕ new |
| Changelog | - | `/api/ttl/_changelog` | ➕ new |
| Table | `/api/ttl/{table}` (RDF) | `/api/ttl/{table}` | 🔄 extend POST/PUT/DELETE |
| Row | `/api/ttl/{table}/{row}` (RDF) | `/api/ttl/{table}/{id}` | ✅ keep |
| **Excel** | | | |
| Schema metadata | `/api/excel` | `/api/excel/_schema` | 🔄 rename |
| All data | - | `/api/excel/_data` | ➕ new |
| Complete export | - | `/api/excel/_all` | ➕ new |
| Members | - | `/api/excel/_members` | ➕ new |
| Settings | - | `/api/excel/_settings` | ➕ new |
| Changelog | - | `/api/excel/_changelog` | ➕ new |
| Table | `/api/excel/{table}` GET | `/api/excel/{table}` GET, POST, DELETE | 🔄 extend POST/DELETE |
| Row | - | - | ❌ N/A (tabular format) |
| **ZIP** | | | |
| Schema metadata | `/api/zip` | `/api/zip/_schema` | 🔄 rename |
| All data | - | `/api/zip/_data` | ➕ new |
| Complete export | `/api/zip` (current) | `/api/zip/_all` | 🔄 rename |
| Members | - | `/api/zip/_members` | ➕ new |
| Settings | - | `/api/zip/_settings` | ➕ new |
| Changelog | - | `/api/zip/_changelog` | ➕ new |
| Table | `/api/zip/{table}` GET | `/api/zip/{table}` GET, POST, DELETE | 🔄 extend POST/DELETE |
| Row | - | - | ❌ N/A (tabular format) |
| **YAML** | | | |
| Schema metadata | `/api/yaml` | `/api/yaml/_schema` | 🔄 rename |
| All data | - | `/api/yaml/_data` | ➕ new |
| Complete export | - | `/api/yaml/_all` | ➕ new |
| Members | - | `/api/yaml/_members` | ➕ new |
| Settings | - | `/api/yaml/_settings` | ➕ new |
| Changelog | - | `/api/yaml/_changelog` | ➕ new |
| Table | - | `/api/yaml/{table}` | ➕ new |
| Row | - | `/api/yaml/{table}/{id}` | ➕ new |
| **Data (content-neg)** | | | |
| Schema metadata | - | `/api/data/_schema` | ➕ new |
| All data | - | `/api/data/_data` | ➕ new |
| Complete export | - | `/api/data/_all` | ➕ new |
| Table | - | `/api/data/{table}` | ➕ new |
| Row | - | `/api/data/{table}/{id}` | ➕ new |
| **GraphQL** | | | |
| Database | `/api/graphql` | `/api/graphql` | ✅ keep |
| Schema | `/{schema}/graphql` | `/{schema}/graphql` | ✅ keep |
| Database + LD | - | `/api/graphql-ld` | ➕ new |
| Schema + LD | - | `/{schema}/api/graphql-ld` | ➕ new |
| **Reports** | | | |
| JSON | `/api/reports/json` | `/api/reports/json` | ✅ keep |
| Excel | `/api/reports/excel` | `/api/reports/excel` | ✅ keep |
| ZIP | `/api/reports/zip` | `/api/reports/zip` | ✅ keep |
| **Deprecated** | | | |
| RDF | `/api/rdf` | - | ❌ remove (use /ttl or /jsonld) |
| TTL2 | `/api/ttl2/*` | - | ❌ remove (temp) |

**Summary:** ✅ keep: 14 | 🔄 rename/extend: 14 | ➕ new: 28 | ❌ remove: 2

## Endpoint Capability Matrix

| Endpoint | CSV | Excel | ZIP | JSON | YAML | JSONLD | TTL |
|----------|-----|-------|-----|------|------|--------|-----|
| `/_schema` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/_data` | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/_all` | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/{table}` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/{table}/{id}` | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ | ✅ |
| `/_members` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/_settings` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/_changelog` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

**Format limitations:**
- **CSV**: Single-table format, so no `/_data` or `/_all` (multi-table), no `/{table}/{id}` (row-level)
- **Excel/ZIP**: Tabular formats, no `/{table}/{id}` (row-level doesn't make sense)
- **JSON/YAML/JSONLD/TTL**: Full support including row-level

## HTTP Methods per Endpoint

| Endpoint | GET | POST | PUT | DELETE | Notes |
|----------|-----|------|-----|--------|-------|
| `/_schema` | Export metadata | Import/merge | - | Remove items | Schema migration |
| `/_data` | Export all rows | Import all rows | - | - | Data transfer between schemas |
| `/_all` | Complete export | Complete import | - | - | Backup/restore |
| `/_members` | List | Add | Update | Remove | Admin only |
| `/_settings` | List | Add | Update | Remove | Admin only |
| `/_changelog` | View | - | - | - | Read-only audit log |
| `/{table}` | Export | Insert | Upsert | Delete rows | Table data |
| `/{table}/{id}` | Get row | - | Update row | Delete row | Single row ops |

## Implementation Tasks

### Phase 1: Backend Changes

#### 1.1 CsvApi.java
- [ ] Rename `/members` → `/_members`
- [ ] Rename `/settings` → `/_settings`
- [ ] Rename `/changelog` → `/_changelog`
- [ ] Add `/_schema` endpoint for schema metadata (move from root)
- [ ] Keep old endpoints temporarily with deprecation warning

#### 1.2 JsonYamlApi.java
- [ ] Rename `/api/json` → `/api/json/_schema`
- [ ] Rename `/api/yaml` → `/api/yaml/_schema`
- [ ] Add new `/api/json/{table}` for plain JSON data export/import

#### 1.3 ExcelApi.java
- [ ] Rename `/api/excel` → `/api/excel/_schema`
- [ ] Add `/_data`, `/_all` endpoints
- [ ] Add `/_members`, `/_settings`, `/_changelog` endpoints
- [ ] Add POST, DELETE to `/{table}` endpoint

#### 1.4 ZipApi.java
- [ ] Rename `/api/zip` → `/api/zip/_schema`
- [ ] Add `/_data`, `/_all` endpoints
- [ ] Add `/_members`, `/_settings`, `/_changelog` endpoints
- [ ] Add POST, DELETE to `/{table}` endpoint

#### 1.5 YamlApi.java (new or extend JsonYamlApi.java)
- [ ] Rename `/api/yaml` → `/api/yaml/_schema`
- [ ] Add `/_data`, `/_all` endpoints
- [ ] Add `/_members`, `/_settings`, `/_changelog` endpoints
- [ ] Add `/{table}` endpoint with GET, POST, PUT, DELETE
- [ ] Add `/{table}/{id}` endpoint with GET, PUT, DELETE

#### 1.6 RDFApi.java / JsonldApi.java
- [ ] Merge or align `/api/ttl2` with `/api/ttl` and `/api/jsonld`
- [ ] Add POST/PUT/DELETE to existing RDF endpoints
- [ ] Add `/_schema`, `/_data`, `/_all` endpoints
- [ ] Add `/_members`, `/_settings`, `/_changelog` endpoints
- [ ] Add `/_context` endpoint for JSON-LD
- [ ] Remove `/api/rdf` (or keep as alias)

#### 1.8 New: Unified endpoint registration
- [ ] Consider creating base class or utility for consistent endpoint registration
- [ ] Ensure `_` prefixed routes registered BEFORE `{table}` routes

#### 1.9 New: DataApi.java (content-negotiated endpoint)
- [ ] Create `/api/data/{table}` endpoint with content negotiation
- [ ] Support Accept headers: text/csv, application/json, application/ld+json, text/turtle
- [ ] Add `/_schema`, `/_all`, `/{table}/{id}` routes
- [ ] Delegate to format-specific implementations based on Accept header

### Phase 2: Frontend Changes

#### 2.1 apps/updownload/src/components/Import.vue
- [ ] Update `/api/csv/members` → `/api/csv/_members`
- [ ] Update `/api/csv/settings` → `/api/csv/_settings`
- [ ] Update `/api/csv/changelog` → `/api/csv/_changelog`
- [ ] Add links for all formats: csv, json, jsonld, ttl, excel, zip
- [ ] Add export links for `_schema`, `_members`, `_settings`, `_changelog` per format

#### 2.2 apps/molgenis-components/src/components/tables/TableExplorer.vue
- [ ] Update `/api/csv/{table}` links
- [ ] Add `/api/json/{table}` link

#### 2.3 apps/tables/src/components/ListTables.vue
- [ ] Update `/api/jsonld` link

### Phase 3: Python Client

#### 3.1 tools/pyclient/src/molgenis_emx2_pyclient/client.py
- [ ] Update `/api/csv` → `/api/csv/_schema` for schema operations
- [ ] Data endpoints (`/api/csv/{table}`) unchanged
- [ ] Add support for new JSON endpoint

### Phase 4: Documentation

#### 4.1 docs/molgenis/use_usingapis.md
- [ ] Update all `/api/csv/Pet` examples
- [ ] Add examples for JSON, JSON-LD, TTL endpoints

#### 4.2 docs/molgenis/dev_batchapi.md
- [ ] Update example URLs

#### 4.3 docs/molgenis/dev_rdf.md
- [ ] Update `/api/jsonld` references
- [ ] Document new semantic API endpoints

#### 4.4 docs/molgenis/use_updownload.md
- [ ] Update `/api/csv/changelog` → `/api/csv/_changelog`

### Phase 5: Testing

- [ ] Update WebApiSmokeTests.java
- [ ] Add tests for new endpoints
- [ ] Add tests for backward compatibility (if keeping old endpoints temporarily)

## Migration Strategy

### Option A: Hard break (recommended)
- Change all endpoints at once
- Update all clients in same release
- Clear changelog/release notes

### Option B: Soft migration
- Keep old endpoints with deprecation warning header
- Log usage of deprecated endpoints
- Remove in future release

## Open Questions

1. Should we keep `/api/rdf` as an alias for content-negotiated endpoint?
2. How long to maintain deprecated endpoints?
3. Should `/api/json/_schema` return same format as current `/api/json`?
4. Do we need versioning (e.g., `/api/v2/json/{table}`)?

## Endpoint Summary (Target State)

```
/{schema}/api/csv/_schema          GET, POST, DELETE (table/column definitions)
/{schema}/api/csv/_data            GET, POST (all table rows)
/{schema}/api/csv/_all             GET, POST (schema + data)
/{schema}/api/csv/_members         GET
/{schema}/api/csv/_settings        GET
/{schema}/api/csv/_changelog       GET
/{schema}/api/csv/{table}          GET, POST, DELETE

/{schema}/api/json/_schema         GET, POST, DELETE
/{schema}/api/json/_data           GET, POST
/{schema}/api/json/_all            GET, POST
/{schema}/api/json/_members        GET
/{schema}/api/json/_settings       GET
/{schema}/api/json/_changelog      GET
/{schema}/api/json/{table}         GET, POST, PUT, DELETE
/{schema}/api/json/{table}/{id}    GET, PUT, DELETE

/{schema}/api/jsonld/_schema       GET
/{schema}/api/jsonld/_data         GET, POST
/{schema}/api/jsonld/_all          GET, POST
/{schema}/api/jsonld/_members      GET
/{schema}/api/jsonld/_settings     GET
/{schema}/api/jsonld/_changelog    GET
/{schema}/api/jsonld/{table}       GET, POST, PUT, DELETE
/{schema}/api/jsonld/{table}/{id}  GET, PUT, DELETE

/{schema}/api/ttl/_schema          GET
/{schema}/api/ttl/_data            GET, POST
/{schema}/api/ttl/_all             GET, POST
/{schema}/api/ttl/_members         GET
/{schema}/api/ttl/_settings        GET
/{schema}/api/ttl/_changelog       GET
/{schema}/api/ttl/{table}          GET, POST, PUT, DELETE
/{schema}/api/ttl/{table}/{id}     GET, PUT, DELETE

/{schema}/api/excel/_schema        GET, POST, DELETE
/{schema}/api/excel/_data          GET, POST
/{schema}/api/excel/_all           GET, POST
/{schema}/api/excel/_members       GET
/{schema}/api/excel/_settings      GET
/{schema}/api/excel/_changelog     GET
/{schema}/api/excel/{table}        GET, POST, DELETE

/{schema}/api/zip/_schema          GET, POST, DELETE
/{schema}/api/zip/_data            GET, POST
/{schema}/api/zip/_all             GET, POST
/{schema}/api/zip/_members         GET
/{schema}/api/zip/_settings        GET
/{schema}/api/zip/_changelog       GET
/{schema}/api/zip/{table}          GET, POST, DELETE

/{schema}/api/yaml/_schema         GET, POST, DELETE
/{schema}/api/yaml/_data           GET, POST
/{schema}/api/yaml/_all            GET, POST
/{schema}/api/yaml/_members        GET
/{schema}/api/yaml/_settings       GET
/{schema}/api/yaml/_changelog      GET
/{schema}/api/yaml/{table}         GET, POST, PUT, DELETE
/{schema}/api/yaml/{table}/{id}    GET, PUT, DELETE

/{schema}/api/graphql              POST (standard GraphQL)
/{schema}/api/graphql-ld           POST (GraphQL with @context response)

# Content-negotiated endpoint (canonical REST)
/{schema}/api/data/_schema         GET, POST, DELETE
/{schema}/api/data/_data           GET, POST
/{schema}/api/data/_all            GET, POST
/{schema}/api/data/{table}         GET, POST, PUT, DELETE
/{schema}/api/data/{table}/{id}    GET, PUT, DELETE
```

### Content Negotiation for `/api/data`

| Accept Header | Format |
|---------------|--------|
| `text/csv` | CSV |
| `application/json` | Plain JSON |
| `application/ld+json` | JSON-LD |
| `text/turtle` | Turtle/TTL |
| `application/vnd.ms-excel` | Excel |
| `application/zip` | ZIP (csv) |

Default: `application/json`

**Why both?**
- Format-specific endpoints (`/api/json/`) = discoverable, browser-friendly, explicit
- Content-negotiated endpoint (`/api/data/`) = canonical URL, RESTful, Linked Data compatible

## Why `_` Prefix is Safe

Table names must start with a letter per `TABLE_NAME_REGEX`:
```java
"^(?!.* _|.*_ )[a-zA-Z][a-zA-Z0-9 _]{0,30}$"
```

So `_members`, `_settings`, `_changelog`, `_schema`, `_all` can never conflict with user table names.
