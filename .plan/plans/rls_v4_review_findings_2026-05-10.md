# RLS v4 — 7-Agent Review Findings (2026-05-10)

Aggregated findings from 7 background reviewer agents:
psql/migration, security, java/jOOQ, graphql, docs/UX, perf, test-consolidation.

Sorted by severity, not by reviewer. Cross-cutting findings (flagged by ≥2
reviewers) noted with **[multi-reviewer]**.

This file feeds the Phase J pre-review triage. Once a finding is acted on
(fix, defer-with-issue, won't-do-with-rationale), strike it through and
add the decision. Spec updates land in `.plan/specs/rls_v4.md`.

---

## Critical — must fix before colleague review

> Real bugs / security gaps / contract violations / modeler-facing wrong docs.

### Security

1. **`mg_protect_system_roles` trigger does not block DELETE of system role rows**
   `migration32.sql:54-60`. Trigger fires `ON UPDATE OR DELETE` but raises only when `TG_OP = 'UPDATE'`. Schema Manager/Editor with write access to `role_permission_metadata` can DELETE a Viewer row and silently drop the read policy for all existing Viewers.
   Fix: extend condition to `OR TG_OP = 'DELETE'`. *(security reviewer)*

2. **`copyIn` bypasses FK-RLS write guard and runs on admin connection**
   `SqlTable.java:76-118`. `CopyManager` path issues `COPY … FROM STDIN` via `db.getJooq()` (admin connection). No `checkFkRlsWriteVisibility` call. Any user with `copyIn` access can insert rows referencing arbitrary FK targets without the guard firing.
   Fix: run guard on deserialised rows before COPY; route COPY through user-scoped connection. *(security reviewer)*

3. **`changePassword` allows non-admin to change another user's password**
   `GraphqlSessionFieldFactory.java:291-304`. `email` argument is declared in schema at build-time when `isAdmin()=true`, so it's always present at execution. Data fetcher uses `getArgument(EMAIL)` first with no caller-identity check, falling back to `getActiveUser()` only if null. A non-admin who sends `email: "victim"` skips the fallback.
   Fix: guard inside data fetcher — `!isAdmin() && email != null && !email.equals(activeUser) → throw`. *(security reviewer)*

4. **Composite-PK FK references bypass both read and write RLS guards** **[multi-reviewer]**
   `SqlTable.java:409-411` (write) and `SqlQuery.java:1120` (read) both `return` / skip when `column.getReferences().size() != 1`. RLS-enabled tables with composite PKs are completely unprotected by `mg_can_reference`.
   Currently documented as "matches R.3c — backlog if it surfaces". Decision needed: is this acceptable as documented, or does it block ship?
   *(security reviewer + psql reviewer + already noted in plan R.3c)*

5. **Ontology marker grants universal read; no admin-only guard on changing TableType to ONTOLOGIES**
   `GraphqlSessionFieldFactory.java:243-257`, `SqlQuery.java:199`. Manager-level user can change a sensitive table's TableType to `ONTOLOGIES` and instantly grant world-read.
   Fix: restrict TableType change to ONTOLOGIES to admin only. *(security reviewer)*

### Correctness

6. **`hasReferencePermission` ignores `select` scope — breaks spec contract**
   `GraphqlTableFieldFactory.java:365-371`. Field-level FK emission checks only explicit `reference != NONE`, but `_session.tablePermissions.canReference` computes `select.allowsRowAccess() || reference != NONE`. Net result: user with `select=ALL, reference=NONE` sees `canReference=true` in session but the schema has no FK field. UI breaks. *(graphql reviewer)*

7. **`mergePermissions` collapses OWN/GROUP UpdateScope to NONE in multi-role merge**
   `SqlRoleManager.java:456-468`. Merge for insert/update/delete is binary `ALL vs NONE`. A user holding two roles, one with `UpdateScope.OWN` and one with `NONE`, gets `NONE` after merge — silent permission stripping.
   Fix: use a higher-scope helper like `higherSelectScope` / `higherReferenceScope` already in the file. *(java reviewer)*

8. **`migration32.sql:38-46` — DO block swallows CHECK constraint when column exists**
   Single `EXCEPTION WHEN duplicate_column THEN NULL` catches `ADD COLUMN` failure and silently skips the immediately-following `ADD CONSTRAINT`. Re-running migration on an instance with the column but no constraint leaves the table unconstrained.
   Fix: split into two DO blocks (column + constraint). *(psql reviewer)*

### Docs / modeler-facing

9. **`OWN_OR_GROUP` documented but does not exist**
   `docs/molgenis/use_customroles.md:16` lists it as a valid select scope; no backend uses that name. Modeler writes `select: OWN_OR_GROUP` → gets cryptic error.
   Fix: remove row or rename to actual enum value. *(docs reviewer)*

10. **REFERENCE scope missing entirely from `use_customroles.md`**
    Modelers reading the tutorial entry point will never learn FK traversal has its own axis. Roles that work for direct SELECT will silently block child-row visibility through FKs. *(docs reviewer)*

11. **`use_customroles.md` examples use `email:`; `use_permissions.md` uses `user:`**
    `use_customroles.md:154` shows `change(members: [{ email: "alice", role: "researcher" }])`; `use_permissions.md:244,264,287-288` use `user:`. One is broken — copy-paste from one of these docs will fail. *(docs reviewer)*

12. **`mg_groups` column data-entry format never explained**
    No doc shows how to populate `mg_groups` at insert time (array of plain group names? prefixed?). The only example shows `change(members…group:"DEPT1")` but never how a row gets tagged to that group. *(docs reviewer)*

### GraphQL contract

13. **`MolgenisGroupInput.users: [String]` vs `MolgenisGroupMemberOutput {email, role}` — round-trip impossible**
    `GraphqlPermissionFieldFactory.java:170` vs `:140-149`. Client writes usernames, reads back objects with `email`+`role`. No test covers this asymmetry. *(graphql reviewer)*

---

## Important — fix before review if cheap, defer otherwise

> Smaller blast radius. Many should land in Phase J-pre.

### Security / correctness

14. **PG INSERT trigger hardcodes `p_changing_group := FALSE`**
    `migration32.sql:339`. INSERT allows arbitrary `mg_groups` value without `change_group` capability check. UPDATE correctly enforces. Asymmetric — intentional or bug?
    Decision needed; if intentional, document. *(psql reviewer)*

15. **SQL DDL string-concat in policy/trigger creation** **[multi-reviewer]**
    `SqlRoleManager.java:967-1029`. Schema/table names embedded in CREATE POLICY / CREATE TRIGGER strings via Java `+`. Mitigated today by `SCHEMA_NAME_REGEX` rejecting quotes, but fragile.
    Fix: route through jOOQ `param()` or `quote_literal()`. *(java reviewer + psql reviewer)*

16. **`mg_can_write_all` empty-array group containment**
    `migration32.sql:242-259`. `array_length(p_groups, 1) IS NULL OR = 0` treats empty `mg_groups` as group-scope-allow. Insert with `mg_groups = '{}'` should require `ALL`, not pass GROUP check. *(security reviewer)*

17. **`TestMgCanReference` has zero coverage of `SELECT_GROUP` scope**
    `mg_can_reference` GROUP branch (`migration32.sql:175`) is untested. The negative case (same user, row in different group) also missing.
    Fix: add 2 tests. *(psql reviewer)*

18. **`mg_can_read` leaks SELECT pass-through for privacy modes**
    `migration32.sql:145`. `select_scope IN ('EXISTS','COUNT','RANGE','AGGREGATE')` unconditionally allows policy to pass. Application-layer aggregation projection is the only guard. If any code path retrieves raw rows on such a table (e.g. sub-join), data leaks.
    Decision: this is documented in spec ("Direct-SQL SELECT * by privacy-scoped user leaks raw rows — documented out-of-scope"). Re-evaluate now that REFERENCE exists. *(security reviewer)*

### Java code quality

19. **Duplicate `getPermissions` / `getPermissionSet` on `SqlRoleManager`**
    Lines 610-615. Identical signatures and bodies. Remove one. *(java reviewer)*

20. **`createRole(Schema, String, String)` silently drops `description`**
    `SqlRoleManager.java:102-105`. Delegates to no-description overload; description never reaches metadata store. Either wire through or remove parameter. *(java reviewer)*

21. **`initSystemSchema` unreachable second branch**
    `SqlDatabase.java:252-261`. Second `if (!hasSchema(SYSTEM_SCHEMA))` can never be true. Dead code. *(java reviewer)*

22. **`addCustomRoleScope` fires two overlapping admin queries**
    `SqlRoleManager.java:722-759`. Two `getJooqAsAdmin` blocks hitting `role_permission_metadata` via different paths; they overlap and hide intent. Single join would be cleaner. *(java reviewer)*

23. **RESET ROLE / SET ROLE "inverted logic" claim — VERIFY FIRST**
    Java reviewer says the visibility check at `SqlTable.java:469-498` runs at superuser privilege (logic inverted). PSQL reviewer says the explicit `p_user` parameter on `mg_can_reference` is correctly designed for exactly this case. **Lead's read**: `mg_can_reference` takes `p_user` and does its own `pg_has_role(p_user, ...)` checks, so elevating only widens row visibility for the lookup itself — the user-permission check is parameterised. Likely **not a bug** but worth a code re-read with a senior pair. *(security reviewer notes try/finally is correct; java reviewer says logic is inverted; psql reviewer praises the design)*

### GraphQL

24. **`outputRolesType` (`MolgenisRolesType`) is dead code**
    `GraphqlSchemaFieldFactory.java:138`. Old type with both `system` and `isSystemRole` fields, never reachable from `_schema.roles` which now uses `MolgenisRoleOutput`. Pollutes introspection. *(graphql reviewer)*

25. **`change(roles:[...])` silently accepts unknown table names**
    `GraphqlPermissionFieldFactory.java:240-259`. Typo in table name → SUCCESS response, permission silently dropped. Add validation against schema. *(graphql reviewer)*

26. **`changePassword` schema shape is dynamic at build-time**
    `GraphqlSessionFieldFactory.java:291-292`. `email` arg added based on `isAdmin()` at factory construction, but schema is built once at startup with admin instance — so always present. Compounded with `ApplicationCachePerUser`. *(graphql reviewer)*

### Docs

27. **VIEW⊇REFERENCE carry rule buried, no visual callout**
    `use_permissions.md:447-449`. The single most non-obvious rule. Add note box: "Privacy scopes (EXISTS/COUNT/RANGE/AGGREGATE) do NOT carry REFERENCE." *(docs reviewer)*

28. **Ontology bypass not mentioned in any user-facing doc**
    Spec mentions it but `use_permissions.md` and `use_customroles.md` don't. Modelers waste time on permissions that have no effect. *(docs reviewer)*

29. **`GREATEST(COUNT(*), 10)` wording backwards**
    `use_permissions.md:189`. "real count when above floor; floor of 10 below" — GREATEST returns the LARGER value, so prose is inverted. *(docs reviewer)*

30. **SELECT scope ladder presents two enforcement mechanisms as one continuum**
    `use_permissions.md:179`. `NONE | EXISTS | COUNT | RANGE | AGGREGATE | OWN | GROUP | ALL` implies a progression but mixes projection layer and row-filter layer. Modeler may assume ranking. Suggest splitting presentation. *(docs reviewer)*

### Perf

31. **Add covering indexes on metadata tables** *(perf reviewer)*
    ```sql
    CREATE INDEX IF NOT EXISTS role_permission_covering_idx
      ON "MOLGENIS".role_permission_metadata (schema_name, table_name, role_name)
      INCLUDE (select_scope, insert_scope, update_scope, delete_scope,
               change_owner, change_group, reference_scope);

    CREATE INDEX IF NOT EXISTS group_membership_covering_idx
      ON "MOLGENIS".group_membership_metadata (user_name, schema_name, role_name)
      INCLUDE (group_name);
    ```
    Eliminates heap fetches in `mg_can_read` / `mg_can_reference` / `mg_can_write_all` metadata JOINs (per-row hot path).

32. **Write-path FK guard is O(rows × FKs) per batch**
    `SqlTable.java:455-500`. One `assertAllReferencedKeysVisible` query per FK column per batch, each evaluating `mg_can_reference` on every unique target key. Acceptable for small batches; will matter at scale.
    Decision: bench first, optimise only if profiling at production scale shows >3× overhead. *(perf reviewer)*

---

## Polish — defer to Phase J-post (post-review)

> Style / dead code / consistency. Worth doing but not blocking.

33. `SqlTable.java:115` — `copyIn` exception says `"copyOut failed"`. *(java)*
34. `SqlTable.java:598,602` — raw types in `updateBatch`; `.size() == 0` instead of `.isEmpty()`. *(java)*
35. `SqlRoleManager.java:441-451` — three `*ScopeName` wrappers are one-liners; inline `.name()` at call site. *(java)*
36. `MetadataUtils.java:1063` — `GROUP_MEMBERSHIP_SENTINEL_ROLE = "member"` collides visually with domain word. Rename. *(java)*
37. `migration32.sql:99-103` — dead `EXCEPTION WHEN others` after `DROP CONSTRAINT IF EXISTS`. *(psql)*
38. `migration32.sql:112` — `DROP FUNCTION IF EXISTS` then `CREATE FUNCTION` for `current_user_groups(TEXT)`; should be `CREATE OR REPLACE`. *(psql)*
39. `GraphqlPermissionFieldFactory.java:170` — `MolgenisGroupInput.users` → rename to `emails` for consistency. *(graphql)*
40. `TablePermissionsGraphqlTest:546` — `@Order(15)` weak assertion (`errors OR !contains b1`) hides data-leak regressions. *(graphql)*
41. `GraphqlConstants.java:4,60` — `TASK_ID = "id"` duplicates `ID = "id"`. *(graphql)*
42. No `.description()` strings on new permission types (`MolgenisTablePermissionInput`, `MolgenisRoleInput`, `MolgenisSelectScope`, etc.). *(graphql)*
43. Inconsistent key name: `tables:` (use_customroles example) vs `permissions:` (use_permissions example + schema). *(docs)*
44. `use_permissions.md:440` — `Enum: REFERENCE_NONE | REFERENCE_ALL` but actual enum values are `NONE` / `ALL`. *(docs)*
45. `use_permissions.md:124` — key-concepts table omits `reference_scope` row. *(docs)*

---

## Test consolidation — ~1225 LOC achievable (~15%)

> Scope-locked tests are no-touch (graphql-test-pattern, spec rows).

**Merges (~395 LOC):**
- `TestRlsLifecycle` → `TestRlsEnableDisableLifecycle` (55 LOC; one test, already covered as precondition)
- `TestCurrentUserGroups` ⨯ `TestGroupsMetadata.currentUserGroupsFunctionReturnsCorrectGroups` (60 LOC dedup)
- `TestRlsEnabledMetadataRoundtrip` → `TestRlsEnableDisableLifecycle` (50 LOC)
- `TestRlsEnabledScopeGuard` → `TestSqlRoleManager.grant_rlsScopeOnNonRlsTable_throws` (90 LOC; 2 unique assertions remain)
- `TestPolicyCount` → `TestRlsEnableDisableLifecycle` (85 LOC; already asserts 4-policies)
- `TestSystemRolesNoBypassRls` → `TestTableRoleManagement` as one test (55 LOC)

**Deletes (~485 LOC; duplicates of higher-confidence tests):**
- `TestSqlRoleManager` Viewer/Editor enforcement tests dup `TestTablePolicies` (~100 LOC)
- `TestSqlRoleManager` OWN/GROUP scope tests dup `TestUpdateScope` + `TestTablePolicies` (~120 LOC)
- `TestSelectScope` (~65 LOC; dup `TestPrivacy`)
- `TestChangeOwnerGroupSqlEnforcement` (~200 LOC; SQL-level dups Java-level `TestChangeOwner` + `TestChangeGroup`; preserve `setPermissions_changeOwnerFlagRoundTrip*` to `TestChangeOwner.changeOwnerFlagRoundTrip`)

**Parametrize (~195 LOC):**
- `TestAggregationPermission` flat 10 methods → 2 `@ParameterizedTest` (80 LOC)
- `TestSqlRoleManager` listener trio → one parametrised (55 LOC)
- `TestFkRlsWriteGuard` outcome-matrix tests → parametrised (60 LOC)

**TestRlsPerformance verdict (~150 LOC):**
- **NOT a JMH benchmark** — scenario test with `assertTrue(elapsed < N)`.
- Move concurrency/transactionality tests (B1, B2, C1, C2) to new `TestRlsConcurrencyAndTransactionality` (~300 LOC).
- Delete absolute-path Markdown report writer in `@AfterAll` (portability bug).
- Trim StringBuilder report ceremony (~150 LOC).
- Net: shrinks to ~400 LOC, correctness tests get proper home.

**Helper extraction (no LOC saving, debt prevention):**
- `tableHasRls`, `countPoliciesForTable`, `columnExists` — duplicated across `TestRlsEnableDisableLifecycle`, `TestRlsInheritanceCascade`, `TestRlsLifecycle`. Extract to package-private `RlsTestUtils`.

---

## Praise — keep doing this

- `mg_can_reference` 5-param overload with `p_user TEXT DEFAULT current_user` — testable from Java without SET ROLE gymnastics. *(psql)*
- `TestFkRlsWriteGuard` thoroughness (positive/negative, REF/REF_ARRAY, cross-schema, change_owner, admin bypass). *(psql)*
- `RESET ROLE` / `SET ROLE` try/finally bracket — connection never left elevated even on mid-query exception. *(security)*
- `FORCE ROW LEVEL SECURITY` + `BYPASSRLS=false` verified by test — solid DB-engine enforcement. *(security)*
- jOOQ parameter binding discipline in `SqlRoleManager` — `name()` / `inline()` everywhere except the noted DDL string-concat. *(java)*
- `TablePermission` builder design — immutable table field, fluent setters, NPE guards, proper equals/hashCode. *(java)*
- `buildTablePermissions` ontology auto-append — clean closure of the frontend special-case, with dedup. *(graphql)*
- Per-table scope enums auto-register from Java enum values — no manual sync drift. *(graphql)*
- `use_permissions.md:115-117` two-layer mental-model paragraph (GRANT vs RLS) — copy this prose to `use_customroles.md`. *(docs)*
- `use_schema.md` refLabel/REFERENCE warning — earns its place. *(docs)*

---

## Owner triage decisions (2026-05-10)

Owner walked all 45 items. Verification scout confirmed three findings worse
than first read (Q1, Q2, Q3 below). Decisions applied per item:

### Confirmed real bugs (J-pre)

- **#1 DELETE trigger** — fix.
- **#2 `copyIn` bypasses guard** — DELETE the unused method. Verified: only caller is `TestCopy.java` (the test for the orphan code itself). Net: drop `SqlTable.copyIn` + `TestCopy`.
- **#3 `changePassword` auth gap** — fix.
- **#4 Composite-PK FK guard** — fix (was previously "backlog if it surfaces"; promoting to J-pre).
- **#5 Ontology marker auth** — fix. Scout Q1 confirmed worse than first read: `SqlSchema.java:288` applies `setTableType()` unconditionally inside `migrate()`; `changeRoles()` requires Manager but `migrate()` has no guard. Anyone with schema-edit access can downgrade a sensitive table to ONTOLOGIES. Owner direction: same permission level as grants (Manager), but TableType change to ONTOLOGIES specifically should require admin or owner-level — TBD in fix design.
- **#6 `hasReferencePermission` ignores select scope** — fix.
- **#7 `mergePermissions` OWN/GROUP collapse** — fix.
- **#8 migration32 DO-block constraint swallow** — fix.
- **#9 `OWN_OR_GROUP` documented but doesn't exist** — fix docs.
- **#10 REFERENCE missing from `use_customroles.md`** — fix docs.
- **#11 `email` vs `user` inconsistency** — fix docs (verify which is actual GraphQL field name first).
- **#12 `mg_groups` data-entry format not documented** — fix docs.
- **#13 `MolgenisGroupInput` round-trip broken** — fix.
- **#14 INSERT trigger mg_groups unrestricted** — fix. Owner asserted "users can only insert to groups they are part of"; scout Q2 verified this is NOT enforced anywhere (no app-layer check in `SqlTable.insertBatch`, trigger explicitly skips via `p_changing_group := FALSE`). A user can tag a row to any group. Promoted from Important to J-pre.
- **#16 `mg_can_write_all` empty-array bypass** — fix. Scout Q3 confirmed it's a real bug: GROUP-scoped user inserting `mg_groups = '{}'` passes the policy and creates a world-visible row.
- **#17 `TestMgCanReference` SELECT_GROUP coverage** — add 2 tests.
- **#19 Duplicate `getPermissions`/`getPermissionSet`** — remove duplicate.
- **#20 `createRole(description)` silently drops** — fix.
- **#21 `initSystemSchema` dead branch** — clean up (cosmetic only; scout Q4 confirmed no delete/update parallel issue).

### Defer-with-rationale

- **#15 SQL DDL string-concat** — elegant to fix. Defer to J-post (post-review) unless quick; mitigated today by name regex.
- **#18 Privacy-mode SELECT pass-through** — won't-fix. Application-layer aggregation projection is sufficient guard; documented out-of-scope already.
- **#22 `addCustomRoleScope` two queries** — defer.
- **#23 RESET ROLE "inverted logic"** — invalid claim. Verified:
  - `migration32.sql:159-162` — `mg_can_reference` uses `pg_has_role(p_user, ...)`, so the user-permission check is parameterised on `p_user`. RESET ROLE only widens row visibility for the lookup; it does NOT bypass the permission check.
  - Test exists: `TestMgCanReference.mgCanReference_withExplicitUser_honorsPassedUser` (lines 156, 175, 182) covers both positive (alice with VIEW_ALL → true) and negative (no-perm user → false) cases.
  - Java reviewer's analysis missed the `p_user` param. No action needed.

### Polish — owner accepted (J-post)

Items #27–32 (docs visual callout, ontology bypass in docs, `GREATEST` wording, ladder presentation, covering indexes, FK write batch optimisation) — accepted for J-post.

### Test consolidation — defer entirely

Test consolidation (~1225 LOC) moves to **J-post**. Owner direction: get correctness right first; consolidate after first colleague review pass.

---

## Updated Phase J split (post-triage 2026-05-10)

### J-pre (must close before colleague review)
- **J-pre.1 Security & auth fixes** — items #1, #3, #5, #14, #16. _Status (2026-05-10): ALL DONE (Agent A: #1, #14, #16 / Agent B: #3, #5)._
  - **#1 DONE**: `mg_protect_system_roles` extended to `OR TG_OP = 'DELETE'`. Also fixed pre-existing semantic bug: admin bypass changed from `current_user <> 'admin'` to `current_user LIKE 'MG_USER_%'` (PG `current_user` is the PG role, not the app username).
  - **#3 DONE**: `changePassword` data fetcher in `GraphqlSessionFieldFactory.java` now guards admin before calling `setUserPassword`; non-admin with `email` arg → throw. 9 tests in `TestGraphqlSession`.
  - **#5 DONE**: `SqlSchema.java` adds Owner+/admin guard before TableType change TO `ONTOLOGIES`; other transitions stay Manager. 8 tests in `TestGraphqlSchemaTables`.
  - **#14+#16 DONE**: `p_changing_group` computed dynamically (rule A — `mg_groups ⊆ current_user_groups()`); empty-array short-circuit removed from `mg_can_write_all` GROUP-scope branch. 4 new tests in `TestChangeGroup` cover ban/allow paths.
  - Net diff: `migration32.sql` +18 lines, java auth files ~+57 lines, tests +269 lines.
- **J-pre.2 Delete unused `copyIn`** — item #2 (orphan code + its only test). _Status (2026-05-10): DONE._ 126 lines deleted: `SqlTable.copyIn` method (45 LOC, incl. `java.io.StringReader` import; retained `CopyManager`/`BaseConnection` imports used by `copyOut`) + `TestCopy.java` (81 LOC, `git rm`). Verified no production callers; compile clean; spec untouched (no rows referenced `copyIn`).
- **J-pre.3 Correctness & contract fixes** — items #4, #6, #7, #8, #13, #19, #20. _Status (2026-05-10): items #8, #20 DONE; #4, #6, #7, #13, #19 pending Wave 2._
  - **#8 DONE**: migration32.sql idempotency — split single DO block into two (column ADD vs constraint ADD), each catching its own duplicate exception. Test `migration32RerunRestoresConstraintWhenColumnAlreadyExists` covers re-run scenario.
  - **#20 DONE** (Agent B): `SqlRoleManager.createRole(Schema, name, description)` persists description via sentinel row in `role_permission_metadata`; `getPermissionSet()` reads it back. 32 tests in `TestSqlRoleManager`.
- **J-pre.4 Docs corrections** — items #9, #10, #11, #12. _Status (2026-05-10): DONE._ +39 lines (use_customroles.md +27, use_permissions.md +14). `OWN_OR_GROUP` row removed (zero hits in backend); 22-line REFERENCE section added after Ownership columns in `use_customroles.md` (independent axis, NONE default, ALL for lookup tables, VIEW⊇REFERENCE carry, privacy-scope exclusion, mutation example, cross-ref to `use_permissions.md#reference-scope`); 4-line `mg_groups: ["DEPT1"]` insert example added inside worked-example (c) of `use_permissions.md`. **Surface finding**: `MolgenisMembersInput` accepts BOTH `user` and `email` fields (memberInputToMember tries `user` first, falls back to `email`); `user` is canonical. `use_customroles.md:177` corrected `email:` → `user:`; `use_permissions.md` examples already used `user:`. Dual-field acceptance is likely back-compat; flag for J-post review (consider deprecating `email` field on input type if no migration concerns).
- **J-pre.6 Coverage gaps & dead code** — items #17, #21, **#46 (new)**. _Status (2026-05-10): #21 DONE (Agent B); #17, #46 pending Wave 2._
  - **#21 DONE** (Agent B): `SqlDatabase.initSystemSchema()` dead branch removed.
  - **#46 DONE** (2026-05-10 null-handling follow-up): `toPermissionSet` now passes `null` (not `""`) when input has no description field. `PermissionSet` default `description` changed from `""` to `null`; `setDescription` no longer coerces null→"". `SqlSchema.changeRoles` passes `role.description()` to `createRole` instead of hardcoded `""`. `SqlRoleManager.createRole` already guarded with `if (description != null && !description.isEmpty())` — null safe. Absent description serializes as absent JSON field (Jackson `NON_NULL` config omits nulls). Two new tests: `TestSqlRoleManager.createRole_withNullDescription_doesNotThrow` + `TestGraphqlSchemaRoles.changeRole_withoutDescription_roundTripsAsNull`. Existing `rolesQuery_description_roundTrip` assertion updated to expect absent/null for omitted description.

(No J-pre.5 — test consolidation moved to J-post.)

### J-post (post-review hygiene)
- Items #15, #22 (Important, defer-friendly)
- Items #27–32 (Polish + perf indexes)
- Items #33–45 (Polish)
- **Test consolidation pass** — ~1225 LOC reduction (moved from J-pre.5)
- Existing Phase J slices already drafted in `.plan/plans/rls_v4.md` §Phase J

### Won't-fix
- Item #18 (privacy-mode SELECT pass-through; app-layer protection sufficient)
- Item #23 (RESET ROLE "inverted"; invalid claim, verified)
