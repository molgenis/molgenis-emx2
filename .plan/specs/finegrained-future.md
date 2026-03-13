# Row-Level Permissions: Future Enhancements v1.1.0

Long-term enhancement ideas gathered from 6 concurrent reviews of the row-level permissions design.

For each idea: what, why, complexity estimate (Low/Medium/High), and which review surfaced it.

## Security Enhancements

### FORCE RLS with MG_APPLICATION role
Replace ENABLE with FORCE so even table owner respects policies. Requires new MG_APPLICATION role for backend operations. The current ENABLE mode means superuser/table-owner connections bypass all policies silently.
- Complexity: High
- Source: Architecture Review, Security Review, PG Research

### GraphQL introspection filtering for denyColumns
Filter GraphQL type definitions so denied columns do not appear in schema introspection. Currently denied columns return null but are still visible in the schema, leaking structural information.
- Complexity: Medium
- Source: Security Review

### Rate limiting on role creation
Max roles per schema (e.g., 1000) to prevent pg_authid bloat. Without limits, programmatic role creation could exhaust PG catalog capacity.
- Complexity: Low
- Source: Security Review

### Role name validation regex
Restrict to `[a-zA-Z0-9_-]+` for defense in depth. Prevents injection via crafted role names even if parameterized queries are used everywhere.
- Complexity: Low
- Source: Security Review

## Access Control Models

### OpenFGA / Zanzibar / ReBAC
Relationship-Based Access Control as superset of RBAC. Enables role inheritance, delegation, implicit relations without role explosion. Would allow "user:alice has role:manager for org:hosp-a" tuples. Google Zanzibar pattern proven at scale (billions of objects).
- Complexity: High
- Source: API Patterns Research

### Role inheritance / delegation
Allow roles to inherit permissions from other roles (`GRANT role1 TO role2`). PG-native via GRANT chains. Useful for large deployments (200+ groups) where flat roles create management overhead.
- Complexity: Medium
- Source: API Patterns Research, API Review

### Permission delegation (WITH GRANT OPTION)
Allow Managers to delegate specific permissions to other roles without admin involvement. PG-native. Reduces bottleneck on schema admins for day-to-day permission management.
- Complexity: Medium
- Source: Architecture Review

### ABAC conditions
Attribute-Based Access Control predicates beyond role membership. E.g., "access if org_id = :org AND status = 'active'". Enables context-aware policies (time of day, IP range, data classification level).
- Complexity: High
- Source: API Patterns Research

## Healthcare / Standards Integration

### SMART on FHIR scopes
OAuth 2.0 scopes tied to FHIR resource types (patient/Patient.read). Standardized consent negotiation for healthcare interop. Would map EMX2 tables to FHIR resource types with scope-based policy generation.
- Complexity: High
- Source: API Patterns Research

### SCIM provisioning
System for Cross-domain Identity Management. Map Member to SCIM User, Role to SCIM Group. REST /scim/v2/ endpoints for Okta/Azure AD/Keycloak integration. Automates user lifecycle management.
- Complexity: Medium
- Source: API Patterns Research

### GA4GH Passports and Visas
Standard for genomic data access decisions. Relevant for rare disease registries. Encodes researcher credentials, affiliations, and data access committee approvals as verifiable JWT claims.
- Complexity: High
- Source: API Patterns Research

## Compliance

### GDPR-compliant provenance
Pseudonymize audit entries (hash row identifiers), implement "forget" functionality, or encrypt provenance with per-subject keys destroyed on erasure request. Current provenance stores plaintext identifiers which conflicts with right-to-erasure.
- Complexity: High
- Source: Security Review

### Time-limited access
First-class expiration field on role membership. PG roles do not support this natively. Options: role description JSON with expires field + scheduled cleanup job, or dedicated expiration table. Essential for data access agreements with fixed terms.
- Complexity: Medium
- Source: Scenarios, API Patterns Research
- PO Decision: Out of scope for RLS feature

### Consent tracking
Record WHY a user can see a row (consent path, DAC approval reference). Extends provenance. Required for audit trails in regulated environments where access justification must be documented.
- Complexity: Medium
- Source: API Patterns Research

## Workflow

### Provenance / Audit Trail
Append-only provenance table per schema targeting 21 CFR Part 11 compliance. Records: who, what, when, old value, new value, reason. Non-alterable audit trail. Automatic entries via triggers or application layer on: record creation, field changes, deletion, mg_roles changes. Permissions: all roles INSERT only. No UPDATE/DELETE. Schema-level users SELECT all. Row-level users SELECT own rows.
- Complexity: High
- Source: Scenarios (all personas), Security Review
- PO Decision: Out of scope for RLS feature, separate epic

### mg_status state machine
Column with configurable states (Draft, Submitted, Published, Withdrawn) for submission/publication workflows. Does NOT replace existing mg_draft. Separate from RLS -- uses column-level permissions to control who can transition states. Relevant for catalogue submissions, embargo lifecycle, curation workflows.
- Complexity: High
- Source: Scenarios (Catalogue Owner, Repository Admin)
- PO Decision: Out of scope for RLS feature

### Embargo auto-lift
Automated embargo expiration. Options: scheduled job, database trigger, or manual release. Combined with mg_status state machine for embargo -> published transition.
- Complexity: Medium
- Source: Scenarios (Research PI)
- PO Decision: Out of scope for RLS feature

## Performance and Scale

### Materialized views for COUNT-only access
Pre-computed aggregations with permissions for scalable count/feasibility queries. Minimum cell size rules (>=5). Separate epic. Enables "how many patients match criteria X" without exposing individual records.
- Complexity: High
- Source: Scenarios

### RLS session variable caching
Cache `Map<fullRoleName, RlsLists>` in memory (the 4 per-operation table lists), skip the `rls_permissions` query on tx start. Invalidate when `DatabaseChangeListener` fires on permission changes. Data changes rarely (only admin actions) but is read on every tx. For now 1 query per tx is fine — simple indexed lookup on `role_name`. Clean optimization path for later.
- Complexity: Low
- Source: Architecture Review, PO Discussion

### Concurrent GIN index creation
Use CREATE INDEX CONCURRENTLY for production migrations on large tables. Prevents table locks during index builds on existing data.
- Complexity: Low
- Source: Architecture Review

### Schema-per-tenant option
For organizations needing strong isolation beyond row-level. Separate module. Each tenant gets its own PG schema with independent RLS policies, preventing any cross-tenant data leakage by design.
- Complexity: High
- Source: PG Research

## Admin Enhancements

### Unify system role grant management into SqlRoleManager
Currently system roles (Viewer, Editor, etc.) get PG grants via hardcoded logic in SqlSchemaMetadataExecutor/SqlDatabaseExecutor, while custom roles are managed by SqlRoleManager. Unifying both into SqlRoleManager would give a single code path for all grant management and simplify new table creation. Low urgency since getMyPermissions() already returns a unified view for both role types.
- Complexity: Medium (high regression risk, needs careful migration)
- Source: Phase 4c implementation review

### Global admin system role (`MG_ROLE_*/Admin`)
**MOVED TO MAIN SPEC** -- now part of the core design (finegrained-spec.md section 3).

### Admin role impersonation
Allow admin to "become" any other role for testing/debugging purposes. E.g., `SET LOCAL molgenis.impersonate = 'MG_ROLE_CohortA/HospitalA'` to see exactly what a HospitalA user sees. Must be admin-only, logged in audit trail.
- Complexity: Medium
- Source: PO Discussion

## API Enhancements

### CSV endpoint for bulk role+permission import/export
POST/GET `/<schema>/api/csv/roles` for bulk role+permission management via CSV files. Single denormalized file format for roles+permissions.
- Complexity: Medium
- Source: Phase 6 design
- PO Decision: Deferred from Phase 6

### Role templates / cloneFrom
Predefined permission bundles. Simplest approach: `cloneFrom` parameter on role creation copies permissions from existing role. For formal templates: stored named bundles ("DataSubmitter" = SELECT+INSERT+UPDATE on tables X,Y,Z). CSV import already handles bulk creation.
- Complexity: Low (cloneFrom), Medium (formal templates)
- Source: API Review, Scenarios
- PO Decision: Deferred from Phase 6

### Permission diff / dry-run
Preview impact of permission changes before applying. `change(roles: [...], dryRun: true)` returning summary of changes. Prevents accidental lockouts or over-permissioning.
- Complexity: Medium
- Source: API Review

### Structured error codes
GraphQL extensions with code, hint, and param fields for programmatic error handling. Enables clients to distinguish "no permission" from "role not found" from "invalid column" without parsing error message strings.
- Complexity: Low
- Source: API Review

### Authorization error context
Include current user identity and required role in permission denied errors. Helps developers and admins diagnose access issues without checking server logs.
- Complexity: Low
- Source: API Review

## Monitoring and Observability

### RLS timing metrics
Measure and log query overhead from RLS policies. Slow query warnings. Baseline measurements show 2-5% overhead for simple policies but complex multi-join policies could degrade without monitoring.
- Complexity: Low
- Source: Architecture Review

### PG log_row_security_statements
Enable PG 15+ logging of RLS policy evaluations. Provides audit trail of which policies were evaluated and which rows were filtered per query.
- Complexity: Low
- Source: Architecture Review

### Connection pool health checks
Validate current_user matches expected role on connection checkout. Detects stale SET ROLE state from previous request, preventing privilege leakage across requests sharing a pooled connection.
- Complexity: Low
- Source: Architecture Review, Security Review

## References

- Architecture Review: PG design patterns, performance analysis
- Security Review: Threat model, privilege escalation vectors
- API Review: Usability, industry comparison (Hasura, Auth0, AWS IAM)
- PG Research: Supabase, PostgREST, Citus patterns, benchmarks
- API Patterns Research: OpenFGA, SMART on FHIR, SCIM, GraphQL patterns
- Codebase Exploration: Integration points, existing patterns
