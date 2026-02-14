# Row-Level Permissions: Scenarios & Requirements v2.8.0

## 1. Summary

MOLGENIS EMX2 currently has schema-level roles (Viewer, Editor, Manager, Owner) that apply to ALL data in a schema. This design adds **row-level permissions** so different groups can work in the same schema with controlled access per row, per table, and per column.

### Feature overview

- **Row-level access**: `mg_roles TEXT[]` column stores which roles can access each row (see spec section 5)
- **One role per schema**: each user has exactly one role per schema, deterministic from membership (see spec section 1)
- **Per-operation permissions**: each of SELECT/INSERT/UPDATE/DELETE can be TABLE (bypass RLS), ROW (filtered), or null (see spec section 1)
- **Column-level access**: per-column editable/readonly/hidden overrides (see spec section 4)

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

| Role | Permissions | mg_roles | Result |
|------|------------|----------|--------|
| HospitalA | select=ROW, insert=ROW, update=ROW | Listed in row | See/edit own rows, can't delete |
| Researcher | select=ROW | Listed in row | Read-only on tagged rows |
| DataMonitor | select=TABLE | N/A (bypasses) | Read all rows |
| Publisher | select=TABLE, update=TABLE | N/A (bypasses) | Read all, edit all, change status |
| Viewer (system) | select=TABLE | N/A (system role) | Read all rows |

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

| Role | Permissions | Result |
|------|------------|--------|
| OrgA | Patients: select=ROW, insert=ROW, update=ROW | See/edit own rows |
| Publisher | Patients: select=TABLE, update=TABLE, delete=TABLE | See/edit all rows |
| Viewer | Patients: select=TABLE | Read all rows |

Organizations responsible for own quality. Automated validation + periodic audits.

#### Variant B: Curated model (editor in chief)

For catalogues requiring central curation before publishing.

| Role | Permissions | Result |
|------|------------|--------|
| OrgA | Patients: select=ROW, insert=ROW, update=ROW | See/edit own rows |
| Editor in Chief | Patients: select=TABLE, update=TABLE, delete=TABLE | See/edit all rows |
| Viewer | Patients: select=TABLE | Read all rows |

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
| InstituteA | select=TABLE | select=ROW, insert=ROW, update=ROW |
| Metadata Viewer | select=TABLE | select=TABLE (count-only at API level) |
| Data Monitor | select=TABLE | select=TABLE |
| Manager | select=TABLE | select=TABLE, update=TABLE |

**Key perspectives:**
- Cohort PI: per-institute consent for researcher access, time-limited, minimum cell size >=5
- Clinician: deny by default, draft mode, emergency break-glass, export own data anytime
- Registry Owner: 5 access tiers, DAC workflow, 3-layer model (row + column + app-level), institute sovereignty

### Scenario 3: Research group with studies (Research PI)

**Current pain**: 5-10 studies in separate schemas. Cross-study queries impossible, reference tables duplicated.

| Role | Permissions | Result |
|------|------------|--------|
| StudyA team | select=ROW, insert=ROW, update=ROW | See/edit own rows |
| StudyB team (embargoed) | select=ROW | Read own rows only |
| Lab member | select=TABLE | Read all non-embargoed |
| PI | select=TABLE, update=TABLE | Read all, edit all |

Cross-study analysis = automatic union of allowed rows. Embargo lifecycle via mg_status.

### Scenario 4: Public data repository (Repository Admin)

**With RLS**: Single schema, 200+ depositor groups. Metadata always public (FAIR), data access varies.

| Role | Metadata tables | Data tables |
|------|----------------|-------------|
| GroupA Depositor | select=TABLE, insert=ROW, update=ROW | insert=ROW, update=ROW |
| Curator | select=TABLE, update=TABLE | select=TABLE |
| Anonymous | select=TABLE | null (no access) |

DOI = never delete. Curators edit metadata only (column-level).

---

## 3. Requirements

### 3.1 Row-level access (mg_roles)

**mg_roles** (which roles can access this row):
- Auto-assigned on INSERT from user's role (one role per schema)
- Multiple values = shared access: `['HospitalA', 'HospitalB']`
- NULL = visible to schema-level users only (safe default)

See `rowlevel-spec.md` section 5 for RLS policy SQL, GIN index, and implementation details.

**Role determination:**
- One role per user per schema, from member record
- No role switching needed -- role is deterministic
- If user needs both visibility and data entry for a group: two accounts (standard in healthcare)

### 3.2 Per-operation permission levels

Each operation (SELECT/INSERT/UPDATE/DELETE) can be TABLE (bypass RLS), ROW (filtered by RLS), or null (no access). See spec section 1 (PermissionLevel enum) and section 4 (operation grants) for details.

### 3.3 Column-level access

Per-column access control via `columns` field containing three string arrays:

- `editable: [String]` -- editable columns (override default to EDITABLE)
- `readonly: [String]` -- read-only columns (override default to VIEW)
- `hidden: [String]` -- hidden columns (not visible in API)

Table-level permission sets the default for unlisted columns:
- `update: ROW/TABLE` -> unlisted = EDITABLE (use readonly/hidden to restrict)
- `update: null` -> unlisted = VIEW (use editable to promote)

```graphql
{ table: "patients", select: ROW, update: ROW,
  columns: { hidden: ["ssn", "genetic_markers"] } }
```

HospitalA can: see and edit all columns except ssn and genetic_markers (hidden).

```graphql
{ table: "patients", select: ROW,
  columns: { editable: ["name", "dob"], hidden: ["ssn"] } }
```

Researcher can: see all columns except ssn, edit only name and dob.

### 3.4 Per-table RLS enablement

Tables with at least one ROW-level permission get RLS enabled automatically. Different roles on the same table can have different access patterns (e.g., one role has select=TABLE while another has select=ROW). See spec section 5 for policy details.

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

With one-role-per-schema model:
- Default behavior: child rows inherit user's role on INSERT (mg_roles = ARRAY[user_role])
- Override: Manager can set mg_roles explicitly on any row
- Cross-group references: schema-level users can create references across groups

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
- Auto-assign from user's role on import
- Explicit mg_roles column in CSV for multi-role users
- Manager uses separate account or asks admin to import on their behalf
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

**v2.2 Review Round** (6 concurrent reviews: Architecture, Security, API Design, PG RLS Best Practices, Permission API Patterns, Codebase Exploration):
- Adopted one-role-per-schema model (resolves multi-role ambiguity, improves RLS performance 5-10x)
- Added WITH CHECK to RLS policies (prevents privilege escalation)
- Simplified to single unified RLS policy per table (was 4 separate)
- Added explicit revocation via drop(permissions) endpoint
- Added introspection queries (myPermissions, permissionsOf)
- Added CSV import/export for roles+permissions
- Long-term ideas moved to rowlevel-future.md

---

## Design decisions

### Resolved
1. No separate submission schema -- use mg_status in production schema
2. COUNT-only at API level -- not RLS policies; materialized views as separate epic
3. mg_roles=NULL default -- visible to schema-level users only
4. Role archival over deletion -- keep role in pg_roles for provenance
5. Pattern A vs B per-table -- essential, confirmed by all scenarios
6. Single mg_roles column -- table-level GRANTs control read vs write
7. Column-level via ColumnAccess (editable/readonly/hidden string arrays) -- app-enforced, not PG column GRANTs
8. mg_status is a new feature -- does NOT replace mg_draft
9. Append-only provenance table -- tamper-evident, not tamper-proof
10. Deletion via table-level GRANT -- no GRANT DELETE = cannot delete
11. ENABLE RLS (not FORCE) -- accepted with documented residual risk
12. TEXT[] over join table -- atomic operations, import-friendly
13. WITH CHECK on UPDATE -- prevents privilege escalation
14. Helper functions via SET LOCAL -- must execute once per transaction
15. No user-defined role inheritance for v1 -- flat roles, multiple memberships
16. Custom roles can be schema-level (default) or row-level (via rowLevel flag)
17. One role per user per schema -- deterministic from membership, no switching needed; simplifies auto-population, performance, and mental model
18. Cross-group queries via schema-level roles -- Viewer/Manager see all data, row-level users see their group only; two accounts for dual needs
19. Per-operation permission level (TABLE/ROW/null) -- replaces boolean rowLevel flag and per-table Pattern A/B; more flexible, same table can have different patterns per role
20. permission_metadata SELECT-only for PUBLIC -- prevents metadata tampering
21. Orphaned mg_roles cleanup on role deletion -- prevents role re-creation attack
22. Explicit revocation via drop(permissions) -- clearer than implicit isRevocation() pattern
23. CSV import for roles+permissions -- single denormalized file for bulk setup
24. Inverted column access model (editable/readonly/hidden string arrays) -- three lists grouped by access level, table-level permission sets default for unlisted columns, no separate readonly flag or wildcard needed
