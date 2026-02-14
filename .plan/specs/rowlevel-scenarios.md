# Row-Level Permissions: Scenarios & Requirements v2.1.0

## 1. Summary

MOLGENIS EMX2 currently has schema-level roles (Viewer, Editor, Manager, Owner) that apply to ALL data in a schema. This design adds **row-level permissions** so different groups can work in the same schema with controlled access per row, per table, and per column.

### Feature overview

**Row-level access column** (per table, opt-in):
- `mg_roles TEXT[]` -- which roles can access this row
- What a role can DO (SELECT, INSERT, UPDATE, DELETE) is controlled by table-level GRANTs
- The row says WHO has access; the grant says WHAT they can do

**Per-table visibility pattern** (different tables in the same schema can use different patterns):
- **Pattern A** (public read, group write): everyone can SELECT, mg_roles members OR schema-level users can modify
- **Pattern B** (group read+write): mg_roles members OR schema-level users can SELECT and modify
- **No RLS**: table has no mg_roles column, no row-level filtering (e.g. shared ontology tables)

Schema-level users (the 8 system roles: Viewer/Editor/Owner etc.) always bypass row-level filtering -- they see and edit all rows according to their table-level permissions. Custom roles can be schema-level (default) OR row-level (via rowLevel flag on Permission). Only row-level custom roles are tagged MG_ROWLEVEL internally and participate in RLS filtering.

**Permission granularity**:
- **Table-level**: GRANT SELECT/INSERT/UPDATE/DELETE ON table TO role (PG native)
- **Column-level**: editColumns/denyColumns in Permission (app-enforced via GraphQL/Java, not PG column-level GRANTs)
- **Row-level**: RLS policies filtering by mg_roles array (PG native)

**Workflow support**:
- `mg_status` column with state machine (Draft -> Submitted -> Published -> Withdrawn) -- new feature, does NOT replace existing mg_draft (to be discussed)
- Column-level permission controls who can transition states
- Append-only provenance table for audit trail (tamper-evident, not tamper-proof)

**Role management** (custom roles via SqlRoleManager):
- Create/delete custom roles per schema
- Assign per-table and per-column permissions
- Add/remove user membership
- Row-level roles tagged via MG_ROWLEVEL marker

### How mg_roles + table GRANTs work together

| Role | Table GRANT | mg_roles | Result |
|------|------------|----------|--------|
| HospitalA | SELECT, INSERT, UPDATE | Listed in row | Can read, insert, update this row (not delete) |
| Researcher | SELECT | Listed in row | Can read this row only |
| Publisher | SELECT, UPDATE(mg_status) | Schema-level (bypasses) | Can read all rows, change status |
| Viewer | SELECT | Schema-level (bypasses) | Can read all rows |

Single column. Row says WHO. Grant says WHAT.

---

## 2. Scenarios

These scenarios were developed using **synthetic personas** -- AI-generated domain experts prompted with realistic profiles. They are NOT real interviews. The goal is to surface requirements, edge cases, and design tensions.

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

| Role | Table GRANTs | mg_roles | mg_status |
|------|-------------|----------|-----------|
| OrgA (row-level) | SELECT, INSERT, UPDATE | Listed in own rows | Draft/Submitted only |
| Publisher (schema-level) | SELECT, INSERT, UPDATE, DELETE | Bypasses | Any state |
| Viewer (schema-level) | SELECT | Bypasses | - |

Pattern A. Organizations responsible for own quality. Automated validation + periodic audits.

#### Variant B: Curated model (editor in chief)

For catalogues requiring central curation before publishing.

| Role | Table GRANTs | mg_roles | mg_status |
|------|-------------|----------|-----------|
| OrgA (row-level) | SELECT, INSERT, UPDATE | Listed in own rows | Draft/Submitted only |
| Editor in Chief (schema-level) | SELECT, INSERT, UPDATE, DELETE | Bypasses | Any state |
| Viewer (schema-level) | SELECT | Bypasses | - |

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

### 3.1 Row-level access (mg_roles)

**mg_roles** (which roles can access this row):
- `TEXT[] DEFAULT NULL`, GIN indexed
- Lists role names that have row-level access to this row
- What each role can DO is controlled by table-level GRANTs (SELECT, INSERT, UPDATE, DELETE)
- Auto-assigned on INSERT if user has exactly 1 row-level role
- Multiple values = shared access: `['HospitalA', 'HospitalB']`
- NULL = visible to schema-level users only (safe default)

See `rowlevel-spec.md` for RLS policy SQL and implementation details.

**Important:** UPDATE policy has both `USING` (which rows you can update) AND `WITH CHECK` (what the row must look like after update). This prevents a user from removing themselves from mg_roles or adding other groups. Additional application-layer validation in `SqlTable.update()`: reject changes to mg_roles unless user is schema Manager+.

### 3.2 Table-level permissions

Per-table GRANT/REVOKE per custom role. SqlRoleManager API:
```java
grantTablePermission(schema, role, table, privilege)   // SELECT, INSERT, UPDATE, DELETE
revokeTablePermission(schema, role, table, privilege)
```

This is how read vs write is controlled per role:
- Role "HospitalA" with `GRANT SELECT, INSERT, UPDATE` on patients -> can read+insert+update own rows, cannot delete
- Role "Researcher" with `GRANT SELECT` on patients -> read-only on own rows

### 3.3 Column-level permissions (deny-list)

Column restrictions stored in column metadata, enforced at app layer (GraphQL/Java). Two lists per role+table:

- **`editColumns`** (allow-list): if set, only these columns are updatable; rest is view-only
- **`denyColumns`** (deny-list): if set, these columns are hidden entirely

Both can be combined. Unlisted columns inherit table-level permission.

```graphql
change(permissions: [
  { role: "HospitalA", table: "patients", select: true, insert: true, update: true,
    editColumns: ["name", "dob", "mg_status"], denyColumns: ["ssn"] }
])
```
HospitalA can: see all columns except ssn, edit only name/dob/mg_status, view rest read-only.

Benefits over PG column-level GRANTs:
- `editColumns` is compact for "restrict editing to a few" (3 items vs 47 exceptions)
- No need to update GRANTs when schema changes (new columns visible+view-only by default)
- No `SELECT *` breakage
- Stored alongside existing column metadata

Trade-off: not PG-enforced -- direct SQL users bypass column filtering (documented residual risk, same as COUNT-only access).

### 3.4 Per-table RLS pattern

Same schema can have tables with different patterns:
- Pattern A (public read): catalogues, metadata, reference tables
- Pattern B (group read): patient data, sensitive tables, embargoed data
- No RLS: shared ontology tables, system tables

Configured via `table.getMetadata().enableRowLevelSecurity(pattern)`.

### 3.5 mg_status state machine (to be discussed)

**Note**: mg_status is a proposed NEW feature, separate from existing mg_draft. mg_draft (boolean) continues to work for simple draft/final toggling. mg_status adds a richer workflow for schemas that need it (catalogue submissions, embargo lifecycle). They coexist -- mg_status does NOT replace mg_draft. The exact design and scope of mg_status needs further discussion.

Proposed column with configurable states:

| State | Meaning | Typical transitions |
|-------|---------|---------------------|
| Draft | Being edited by owner | -> Submitted |
| Submitted | Awaiting review | -> Published, Revision needed |
| Revision needed | Returned for edits | -> Draft |
| Published | Visible, immutable for owner | -> Withdrawn, Revision needed |
| Withdrawn | Tombstone, not visible | Terminal |

Controlled via column-level permissions: `GRANT UPDATE(mg_status)` to specific roles with app-level validation of allowed transitions.

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
| mg_roles | TEXT[] | Group of row at time of action |
| details | JSONB | Old/new values, notes, reason |

Permissions: all roles INSERT only. No UPDATE/DELETE. Schema-level users SELECT all. Row-level users SELECT own rows (filtered by mg_roles).

Automatic entries via triggers or application layer on: record creation, status change, group change, deletion, key field changes.

### 3.7 Group inheritance across tables

When child rows reference parent rows (Subject -> Samples -> Experiments):
- Option A: Explicit mg_roles per row (user sets each)
- Option B: Inherit from parent on INSERT (auto-copy mg_roles from referenced row)
- Option C: Mixed (inherit by default, allow override)

Open question: which default? Inheritance simplifies but may not fit all cases (e.g., sample sent to external lab).

### 3.8 Role lifecycle

**Authorization**: Schema role operations (create, delete, permissions, members) require Manager or Owner role in that schema, or database admin. Global (cross-schema) role operations require database admin.

- **Create**: custom role per schema, tagged row-level via MG_ROWLEVEL marker
- **Members**: add/remove users to role
- **Permissions**: grant per-table and per-column privileges
- **Archive**: remove all members but keep role in pg_roles (mg_roles references preserved)
- **Delete**: only if no mg_roles references exist, else warn
- **Expiration**: role description JSON with expires field, scheduled cleanup job
- **Templates**: predefined permission bundles ("Phenotype Analyst" = SELECT on tables X,Y,Z)
- **Bulk creation**: 200+ groups via script/CSV import

### 3.9 Security considerations

Key security requirements:
- RLS protects authorized users from exceeding their data scope (PG-enforced policies)
- UPDATE policies must include WITH CHECK to prevent privilege escalation via mg_roles modification
- Helper functions (is_schema_level_user, current_user_roles) must execute once per transaction, not per row

See `rowlevel-spec.md` for detailed security model, threat analysis, and implementation details.

---

## 4. Cross-cutting themes

### Ownership transfer (all personas)
Researcher leaves, institute exits consortium, project ends, patient referred.
- Bulk transfer: UPDATE mg_roles replacing GroupA with GroupB
- Split ownership: append GroupB to mg_roles array
- Archive role: remove members, keep role for provenance
- UI needed long-term, SQL-first acceptable for v1

### Time-limited access (Cohort PI, Registry Owner, Research PI, Repository Admin)
External researchers, departed collaborators, approved projects.
- PG roles don't support membership expiration natively
- Role description JSON: `{"expires": "2027-02-13", "dac_ref": "DAC-2026-042"}`
- Scheduled cleanup job removes expired memberships

### COUNT-only access (Cohort PI, Clinician, Registry Owner, Research PI, Repository Admin)
Feasibility queries, discovery, institutional reporting.
- Enforced at GraphQL/API level (not PG level)
- Separate epic: materialized views with permissions for scalable aggregation
- Minimum cell size rule (>=5) for privacy
- Per-institute aggregation with masking for small groups
- Note: direct SQL users can bypass count restrictions (documented residual risk)

### Import/export with mg_roles (Data Manager, Cohort PI, Repository Admin)
- Auto-assign if user has exactly 1 row-level role
- Explicit mg_roles column in CSV for multi-role users
- "Import as" context for managers importing on behalf of institute
- Validation: reject unknown role values with clear error

### Deletion restrictions (all personas)
- Row-level users: deletion controlled by table-level GRANT (no GRANT DELETE = cannot delete)
- Schema-level Editor/Manager: can delete according to their grants
- Provenance entry required with reason for every deletion
- Published/DOI records: app-level enforcement prevents deletion

---

## 5. References

### Existing MOLGENIS data models relevant to RLS

| Model | Type | Scenario |
|-------|------|----------|
| DataCatalogue | Multi-org, shared | Scenario 1: catalogue submissions |
| CohortsStaging + variants | Multi-org, staging | Separate schema per org -- RLS could unify |
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

### Review findings

Three critical reviews conducted (synthetic AI reviewers, not human):

**PG Expert** -- challenged TEXT[] vs join table, helper function per-row execution risk, GIN index bloat on bulk updates, connection pooling + SET ROLE interaction, migration strategy for large tables. Key outcome: TEXT[] accepted with documented trade-offs; helper function performance is #1 implementation risk.

**Pragmatist** -- challenged scope (7 phases -> 3 suggested), mg_can_view as YAGNI, Pattern A as unnecessary. Key outcome: mg_can_edit/mg_can_view simplified to single mg_roles column; table-level GRANTs control read vs write. Ship minimal, pilot with users.

**Security Researcher** -- found: no threat model, ENABLE vs FORCE RLS gap, privilege escalation via UPDATE without WITH CHECK, deletion test contradicting spec, provenance not tamper-proof, GDPR erasure unresolved. Key outcome: threat model added (3.9), WITH CHECK on UPDATE policies, ENABLE accepted with documented residual risk, FORCE as future epic.

---

## Design decisions

### Resolved
1. No separate submission schema -- use mg_status in production schema
2. COUNT-only at API level -- not RLS policies; materialized views as separate epic
3. mg_roles=NULL default -- visible to schema-level users only
4. Role archival over deletion -- keep role in pg_roles for provenance
5. Pattern A vs B per-table -- essential, confirmed by all scenarios
6. Single mg_roles column -- table-level GRANTs control read vs write
7. Column-level via editColumns/denyColumns -- app-enforced, not PG column GRANTs
8. mg_status is a new feature -- does NOT replace mg_draft
9. Append-only provenance table -- tamper-evident, not tamper-proof
10. Deletion via table-level GRANT -- no GRANT DELETE = cannot delete
11. ENABLE RLS (not FORCE) -- accepted with documented residual risk
12. TEXT[] over join table -- atomic operations, import-friendly
13. WITH CHECK on UPDATE -- prevents privilege escalation
14. Helper functions via SET LOCAL -- must execute once per transaction
15. No user-defined role inheritance for v1 -- flat roles, multiple memberships
16. Custom roles can be schema-level (default) or row-level (via rowLevel flag)
