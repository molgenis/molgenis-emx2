# Row-Level Permissions: Scenarios, Requirements & Design

## 1. Summary

MOLGENIS EMX2 currently has schema-level roles (Viewer, Editor, Manager, Owner) that apply to ALL data in a schema. This design adds **row-level permissions** so different groups can work in the same schema with controlled access per row, per table, and per column.

### Feature overview

**Row-level ACL columns** (per table, opt-in):
- `mg_can_edit TEXT[]` — roles that can read+write this row (owning group)
- `mg_can_view TEXT[]` — roles that can read this row (additional read-only grants)

**Per-table visibility pattern** (different tables in the same schema can use different patterns):
- **Pattern A** (public read, group write): everyone can SELECT, mg_can_edit members OR schema-level users can modify
- **Pattern B** (group read+write): mg_can_edit/mg_can_view members OR schema-level users can SELECT, mg_can_edit members OR schema-level users can modify
- **No RLS**: table has no mg_can_edit column, no row-level filtering (e.g. shared ontology tables)

Schema-level users (system roles like Viewer/Editor/Owner AND custom roles with `isRowLevel=false`) always bypass row-level filtering — they see and edit all rows according to their table-level permissions.

**Permission granularity** (all using PostgreSQL native GRANT/REVOKE):
- **Table-level**: GRANT SELECT/INSERT/UPDATE/DELETE ON table TO role
- **Column-level**: GRANT SELECT/UPDATE(column) ON table TO role
- **Row-level**: RLS policies filtering by mg_can_edit/mg_can_view arrays

**Workflow support**:
- `mg_status` column with state machine (Draft -> Submitted -> Published -> Withdrawn) — new feature, does NOT replace existing mg_draft (to be discussed)
- Column-level permission controls who can transition states
- Append-only provenance table for audit trail

**Role management** (custom roles via SqlRoleManager):
- Create/delete custom roles per schema
- Assign per-table and per-column permissions
- Add/remove user membership
- Row-level roles tagged via MG_ROWLEVEL marker

### Naming: Linux ACL-inspired

| Column | Linux analogy | Controls |
|--------|---------------|----------|
| `mg_can_edit` | Owning group (rw) | Who can read+write this row |
| `mg_can_view` | Extended ACL / read group (r) | Who can read this row (in addition to mg_can_edit) |
| Pattern A/B | "other" permissions | World-readable (A) or private (B) |

Both columns are optional `TEXT[]` with GIN index. `mg_can_view` is useful on any RLS-enabled table where read/write separation is needed (most common with Pattern B, but also valid with Pattern A for explicit read-only grants).

---

## 2. Scenarios

These scenarios were developed using **synthetic personas** — AI-generated domain experts prompted with realistic profiles. They are NOT real interviews. The goal is to surface requirements, edge cases, and design tensions.

### Personas

| Persona | Profile | Scenario |
|---------|---------|----------|
| **Data Manager** | UMC, manages multi-institute registries. Imports CSVs daily. | Import/export, validation, bulk ops |
| **Cohort PI** | Leads 5-hospital cohort, 1200 subjects. Data governance. | Access tiers, sharing agreements |
| **Catalogue Owner** | National health data catalogue, 45+ organizations. | Submission workflow, publication |
| **Clinician** | Hospital rare disease registry. Non-technical. | Privacy, draft mode, trust |
| **Registry Owner** | European rare disease registry (ERN/RD3). | Fine-grained access, DAC workflow |
| **Research PI** | University PI, 5-10 concurrent studies. | Cross-study queries, embargo |
| **Repository Admin** | Institutional FAIR data repository, 200+ groups. | DOI persistence, curation |

### Scenario 1: Catalogue with submissions (Catalogue Owner)

**Current pain**: Each organization has its own schema (CohortsStaging, UMCGCohortsStaging, etc.). Data is siloed, cross-organization linking impossible. 45+ orgs x quarterly updates = unscalable.

**With RLS**: Single schema, all organizations contribute to same tables.

#### Variant A: Trust model (self-service)

For large catalogues where per-submission review doesn't scale.

| Role | SELECT | INSERT | UPDATE | DELETE | mg_status |
|------|--------|--------|--------|--------|-----------|
| OrgA (row-level) | All rows | Own rows only | Own rows only | None | Draft/Submitted only |
| Publisher (schema-level) | All rows | All rows | All rows | All rows | Any state |
| Viewer (schema-level) | All rows | - | - | - | - |

Pattern A. Organizations responsible for own quality. Automated validation + periodic audits.

#### Variant B: Curated model (editor in chief)

For catalogues requiring central curation before publishing.

| Role | SELECT | INSERT | UPDATE | DELETE | mg_status |
|------|--------|--------|--------|--------|-----------|
| OrgA (row-level) | All rows | Own rows only | Own Draft rows only | None | Draft/Submitted only |
| Editor in Chief (schema-level) | All rows | All rows | All rows | All rows | Any state |
| Viewer (schema-level) | Published only | - | - | - | - |

Pattern A with status-aware visibility. Editor controls full lifecycle:

```
Draft -> Submitted -> Published -> Withdrawn
  ^         |             |
  |    Revision needed    |
  |         |             |
  +----<----+       Revision needed -> Draft
```

| mg_status | Org can edit? | Visible to Viewers? | Who can set? |
|-----------|---------------|---------------------|--------------|
| Draft | Yes | No | Org (on create), Editor (on revision) |
| Submitted | No | No | Org (from Draft) |
| Revision needed | Yes | No | Editor only |
| Published | No | Yes | Editor only |
| Withdrawn | No | No (tombstone) | Editor only |

### Scenario 2: Multi-institute registry (Cohort PI, Clinician, Registry Owner)

**Tables in two categories**: metadata (non-sensitive) and subject data (sensitive).

| Role | Metadata tables | Subject tables |
|------|----------------|----------------|
| InstituteA (row-level) | SELECT all | SELECT own, COUNT all, INSERT/UPDATE own |
| Metadata Viewer (schema-level) | SELECT all | COUNT only |
| Data Monitor (schema-level) | SELECT all | SELECT all (read-only) |
| Manager (schema-level) | SELECT all | SELECT all, UPDATE all |

Pattern B on subject tables, Pattern A on metadata tables.

**Key perspectives:**
- Cohort PI: per-institute consent for researcher access, time-limited, minimum cell size >=5
- Clinician: deny by default, draft mode, emergency break-glass, export own data anytime
- Registry Owner: 5 access tiers, DAC workflow, 3-layer model (row + column + app-level), institute sovereignty

### Scenario 3: Research group with studies (Research PI)

**Current pain**: 5-10 studies in separate schemas. Cross-study queries impossible, reference tables duplicated.

| Role | All tables |
|------|------------|
| StudyA team (row-level) | SELECT own, INSERT/UPDATE own |
| StudyB team (row-level, embargoed) | SELECT own only |
| Lab member (schema-level Viewer) | SELECT all non-embargoed |
| PI (schema-level Manager) | SELECT all, UPDATE all |

Pattern B. Cross-study analysis = automatic union of allowed rows. Embargo lifecycle via mg_status.

### Scenario 4: Public data repository (Repository Admin)

**With RLS**: Single schema, 200+ depositor groups. Metadata always public (FAIR), data access varies.

| Role | Metadata tables | Data tables |
|------|----------------|-------------|
| GroupA Depositor (row-level) | SELECT all, INSERT/UPDATE own | INSERT/UPDATE own |
| Curator (schema-level) | SELECT/UPDATE all | SELECT all, no DELETE |
| Anonymous | SELECT all | No access |

Pattern A for metadata, Pattern B for data. DOI = never delete. Curators edit metadata only (column-level).

---

## 3. Requirements

### 3.1 Row-level ACL

**mg_can_edit** (owning group, read+write):
- `TEXT[] DEFAULT NULL`, GIN indexed
- Controls INSERT/UPDATE/DELETE access to the row
- Also grants SELECT (members of mg_can_edit can always read their rows)
- Auto-assigned on INSERT if user has exactly 1 row-level role
- Multiple values = shared ownership: `['HospitalA', 'HospitalB']`
- NULL = visible to schema-level users only (safe default)

**mg_can_view** (extended ACL, read-only):
- `TEXT[] DEFAULT NULL`, GIN indexed, optional
- Grants SELECT access in addition to mg_can_edit
- Only needed on Pattern B tables where read/write separation is required
- Use cases: approved researchers, departed collaborators, cross-institute projects
- Time-limited: managed via role expiration + cleanup job

**RLS policies:**

Pattern A (public read, group write):
```sql
CREATE POLICY select_all ON table FOR SELECT USING (true);
CREATE POLICY group_write ON table FOR INSERT
  WITH CHECK (is_schema_level_user() OR mg_can_edit && current_user_roles());
CREATE POLICY group_update ON table FOR UPDATE
  USING (is_schema_level_user() OR mg_can_edit && current_user_roles());
CREATE POLICY group_delete ON table FOR DELETE
  USING (is_schema_level_user() OR mg_can_edit && current_user_roles());
```

Pattern B (group read + group write):
```sql
CREATE POLICY group_or_schema_read ON table FOR SELECT
  USING (is_schema_level_user()
    OR mg_can_edit && current_user_roles()
    OR mg_can_view && current_user_roles()
    OR mg_can_edit IS NULL);
CREATE POLICY group_write ON table FOR INSERT
  WITH CHECK (is_schema_level_user() OR mg_can_edit && current_user_roles());
CREATE POLICY group_update ON table FOR UPDATE
  USING (is_schema_level_user() OR mg_can_edit && current_user_roles());
CREATE POLICY group_delete ON table FOR DELETE
  USING (is_schema_level_user() OR mg_can_edit && current_user_roles());
```

`is_schema_level_user()` returns true for any user who has a non-MG_ROWLEVEL role in this schema. This includes the 8 system roles (Viewer through Owner) AND custom roles created with `isRowLevel=false`.

### 3.2 Table-level permissions

Per-table GRANT/REVOKE per custom role. SqlRoleManager API:
```java
grantTablePermission(schema, role, table, privilege)   // SELECT, INSERT, UPDATE, DELETE
revokeTablePermission(schema, role, table, privilege)
```

Enables: different roles see different tables. InstituteA gets SELECT on metadata tables, SELECT+INSERT+UPDATE on subject tables. Researcher gets SELECT only.

### 3.3 Column-level permissions

Per-column GRANT/REVOKE per custom role. PostgreSQL native, same pattern as table-level:
```java
grantColumnPermission(schema, role, table, column, privilege)   // SELECT, UPDATE
revokeColumnPermission(schema, role, table, column, privilege)
```
```sql
GRANT SELECT(age, sex, diagnosis) ON subjects TO "MG_ROLE_rd3/ResearcherX";
GRANT UPDATE(mg_status) ON datasets TO "MG_ROLE_catalogue/Publisher";
```

Enables:
- Researchers: demographics but not genomics
- Curators: edit metadata columns but not data columns
- Publisher: only role that can change mg_status
- Stored in PG catalog (`information_schema.column_privileges`)

### 3.4 Per-table RLS pattern

Same schema can have tables with different patterns:
- Pattern A (public read): catalogues, metadata, reference tables
- Pattern B (group read): patient data, sensitive tables, embargoed data
- No RLS: shared ontology tables, system tables

Configured via `table.getMetadata().enableRowLevelSecurity(pattern)`.

### 3.5 mg_status state machine (to be discussed)

**Note**: mg_status is a proposed NEW feature, separate from existing mg_draft. mg_draft (boolean) continues to work for simple draft/final toggling. mg_status adds a richer workflow for schemas that need it (catalogue submissions, embargo lifecycle). They coexist — mg_status does NOT replace mg_draft. The exact design and scope of mg_status needs further discussion.

Proposed column with configurable states:

| State | Meaning | Typical transitions |
|-------|---------|---------------------|
| Draft | Being edited by owner | -> Submitted |
| Submitted | Awaiting review | -> Published, Revision needed |
| Revision needed | Returned for edits | -> Draft |
| Published | Visible, immutable for owner | -> Withdrawn, Revision needed |
| Withdrawn | Tombstone, not visible | Terminal |

Controlled via column-level permissions: `GRANT UPDATE(mg_status)` to specific roles with app-level validation of allowed transitions.

RLS write policy can be conditioned on status: org can UPDATE only when mg_status IN ('Draft', 'Revision needed').

### 3.6 Provenance table (append-only)

Tracks all state changes, ownership transfers, and significant edits. Rows can never be edited or deleted.

| Column | Type | Description |
|--------|------|-------------|
| id | AUTO | Primary key |
| timestamp | DATETIME | When |
| user | TEXT | Who |
| table_name | TEXT | Which table |
| row_id | TEXT | Which row |
| action | TEXT | created, submitted, published, revision_requested, withdrawn, deleted, field_changed |
| mg_can_edit | TEXT[] | Group of row at time of action |
| details | JSONB | Old/new values, notes, reason |

Permissions: all roles INSERT only. No UPDATE/DELETE. Schema-level users SELECT all. Row-level users SELECT own rows (filtered by mg_can_edit).

Automatic entries via triggers or application layer on: record creation, status change, group change, deletion, key field changes.

### 3.7 Group inheritance across tables

When child rows reference parent rows (Subject -> Samples -> Experiments):
- Option A: Explicit mg_can_edit per row (user sets each)
- Option B: Inherit from parent on INSERT (auto-copy mg_can_edit from referenced row)
- Option C: Mixed (inherit by default, allow override)

Open question: which default? Inheritance simplifies but may not fit all cases (e.g., sample sent to external lab).

### 3.8 Role lifecycle

- **Create**: custom role per schema, tagged row-level via MG_ROWLEVEL marker
- **Members**: add/remove users to role
- **Permissions**: grant per-table and per-column privileges
- **Archive**: remove all members but keep role in pg_roles (mg_can_edit references preserved)
- **Delete**: only if no mg_can_edit/mg_can_view references exist, else warn
- **Expiration**: role description JSON with expires field, scheduled cleanup job
- **Templates**: predefined permission bundles ("Phenotype Analyst" = SELECT on tables X,Y,Z)
- **Bulk creation**: 200+ groups via script/CSV import

---

## 4. Cross-cutting themes

### Ownership transfer (all personas)
Researcher leaves, institute exits consortium, project ends, patient referred.
- Bulk transfer: UPDATE mg_can_edit replacing GroupA with GroupB
- Split ownership: append GroupB to mg_can_edit array
- Archive role: remove members, keep role for provenance
- UI needed long-term, SQL-first acceptable for v1

### Time-limited access (Cohort PI, Registry Owner, Research PI, Repository Admin)
External researchers, departed collaborators, approved projects.
- PG roles don't support membership expiration natively
- Role description JSON: `{"expires": "2027-02-13", "dac_ref": "DAC-2026-042"}`
- Scheduled cleanup job removes expired memberships + cleans mg_can_view arrays

### COUNT-only access (Cohort PI, Clinician, Registry Owner, Research PI, Repository Admin)
Feasibility queries, discovery, institutional reporting.
- Enforced at GraphQL/API level (not PG level)
- Separate epic: materialized views with permissions for scalable aggregation
- Minimum cell size rule (>=5) for privacy
- Per-institute aggregation with masking for small groups

### Import/export with mg_can_edit (Data Manager, Cohort PI, Repository Admin)
- Auto-assign if user has exactly 1 row-level role
- Explicit mg_can_edit column in CSV for multi-role users
- "Import as" context for managers importing on behalf of institute
- Validation: reject unknown role values with clear error

### Deletion restrictions (all personas)
- Row-level users cannot delete (not even own rows)
- Only schema-level Editor/Manager can delete
- Provenance entry required with reason for every deletion
- Published/DOI records: app-level enforcement prevents deletion

---

## 5. References

### Existing MOLGENIS data models relevant to RLS

| Model | Type | Scenario |
|-------|------|----------|
| DataCatalogue | Multi-org, shared | Scenario 1: catalogue submissions |
| CohortsStaging + variants | Multi-org, staging | Separate schema per org — RLS could unify |
| SharedStaging | Multi-org, communal | Simplest RLS case |
| PatientRegistry | Multi-institute | Scenario 2: registry |
| BIOBANK_DIRECTORY | Multi-org (BBMRI) | MIABIS standard, like catalogue |
| FAIRGenomes | Multi-org, genomic | Scenario 2 with genomic sensitivity |
| ProjectManager | Single owner | Scenario 3: research group |
| FAIRDataHub/Point | Multi-org, repository | Scenario 4: data repository |

Key insight: the "Staging" pattern (CohortsStaging, UMCGCohortsStaging, etc.) already solves multi-org via separate schemas. RLS could replace this with a single shared schema + row-level groups.

### Standards and literature

**Access control models:**
- NIST SP 800-162: Guide to Attribute Based Access Control (ABAC). Defines ABAC as generalization of ACL/RBAC. [nist.gov](https://csrc.nist.gov/pubs/sp/800/162/upd2/final)
- NIST SP 800-178: Comparison of ABAC Standards (XACML vs NGAC). [nist.gov](https://nvlpubs.nist.gov/nistpubs/specialpublications/nist.sp.800-178.pdf)
- HL7 Healthcare Access Control Catalog: extends RBAC with ABAC and Relationship-Based Access Control for healthcare. [hl7.org](https://www.hl7.org/implement/standards/product_brief.cfm?product_id=72)

**PostgreSQL RLS:**
- PostgreSQL Row Security Policies documentation. [postgresql.org](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)
- AWS: Multi-tenant Data Isolation with PostgreSQL Row Level Security. [aws.amazon.com](https://aws.amazon.com/blogs/database/multi-tenant-data-isolation-with-postgresql-row-level-security/)

**Clinical/registry data sharing:**
- European Health Data Space Regulation (EHDS, 2025): EU legal framework for health data secondary use. [ec.europa.eu](https://health.ec.europa.eu/ehealth-digital-health-and-care/european-health-data-space-regulation-ehds_en)
- FAIR Rare Disease Patient Registries (Data Science Journal, 2023): balancing FAIR with GDPR in registries. [codata.org](https://datascience.codata.org/articles/10.5334/dsj-2023-012)

**Provenance and audit:**
- W3C PROV-DM: standard data model for provenance (entities, activities, agents). [w3.org](https://www.w3.org/TR/prov-dm/)
- OWASP Logging Cheat Sheet: audit logging best practices including tamper-resistant storage. [owasp.org](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html)

**Column-level security:**
- Google Cloud Spanner: fine-grained access control on rows, columns, or cells. [cloud.google.com](https://cloud.google.com/spanner/docs/fgac-about)

---

## Design decisions

### Resolved
1. **No separate submission schema** — use mg_status in production schema
2. **COUNT-only at API level** — enforced in GraphQL, not RLS policies; materialized views with permissions as separate epic
3. **mg_can_edit=NULL default** — visible to schema-level users only (safe default)
4. **Role archival over deletion** — keep role in pg_roles for provenance
5. **Pattern A vs B per-table** — essential, confirmed by all scenarios
6. **Separate read/write columns** — mg_can_edit (rw) + mg_can_view (r), Linux ACL-inspired
7. **Column-level permissions** — same GRANT/REVOKE as table-level, PG native
8. **mg_status is a new feature** — does NOT replace mg_draft; coexists; exact design to be discussed
9. **Append-only provenance table** — no UPDATE/DELETE, tracks all changes
10. **Deletion by editor/admin only** — row-level users cannot delete

### Open (need product owner input)
1. **mg_status scope and design**: is this in scope for RLS? If so: fixed states or configurable? Interaction with mg_draft?
2. **Time-limited access**: role description JSON + cron? Or first-class expiration field?
3. **Group inheritance**: auto-inherit from parent FK? Explicit per row? Configurable?
4. **"Import as" context**: how does a Manager import on behalf of an institute?
5. **Embargo auto-lift**: scheduled job? Trigger? Manual?
6. **Role templates**: predefined permission bundles? Or always explicit per-table?
7. **Provenance granularity**: every field change or only status/group/key fields?
