# Phase 5: Frontend — Detailed Plan

**Parent plan**: `.plan/plans/yaml-profile-format-v8.md`
**Spec**: `.plan/specs/yaml-profile-format-v8.md`

## Status: COMPLETE

All sub-phases (5A–5I) implemented and staged.

## Overview

The backend now supports multiple inheritance, PROFILE/PROFILES column types, and TableType.INTERNAL. The frontend needs to:
1. Consume the new `inheritNames` array (was `inheritName` string)
2. Support PROFILE/PROFILES column types in forms
3. Respect TableType.INTERNAL in the schema editor
4. Update diagrams and print views

---

## 5A: Type Definitions & GraphQL Query — COMPLETE

**Goal**: Update the data contract between backend and frontend.

### Files to change:

| File | Change |
|---|---|
| `apps/metadata-utils/src/types.ts:93-103` | `ITableMetaData`: add `inheritNames: string[]`, keep `inheritName` as computed/derived for backward compat |
| `apps/schema/src/utils.ts:4-66` | GraphQL `_schema` query: add `inheritNames` field (keep `inheritName` too until migration complete) |

### Notes:
- Backend exposes both `inheritName` (first parent, backward compat) and `inheritNames` (full array)
- Frontend should prefer `inheritNames` everywhere but can fall back to `inheritName`

---

## 5B: Schema Editor — Table Editing — COMPLETE

**Goal**: Support multiple parents in table editor, display INTERNAL type.

### Files to change:

| File | Line(s) | Change |
|---|---|---|
| `apps/schema/src/components/TableEditModal.vue` | 29-35 | Change `InputSelect` for `inheritName` to multi-select for `inheritNames` |
| `apps/schema/src/components/TableEditModal.vue` | 130-143 | `inheritOptions` computed: return array of valid parent tables |
| `apps/schema/src/components/TableEditModal.vue` | 140 | Remove force-assign `this.table.inheritName = result[0]` |
| `apps/schema/src/components/TableView.vue` | 104-124 | Display `extends Parent1, Parent2` instead of single parent |
| `apps/schema/src/components/Schema.vue` | 103-117, 164-237 | Update `convertToSubclassTables()` call and save logic for multi-parent |

---

## 5C: Schema Utility Functions — COMPLETE

**Goal**: Multi-parent inheritance tree logic.

### Files to change:

| File | Function | Change |
|---|---|---|
| `apps/schema/src/utils.ts:103-130` | `convertToSubclassTables()` | Handle `inheritNames` array — a table is a subclass if ANY of its `inheritNames` matches a parent. Build tree with possible diamond. |
| `apps/schema/src/utils.ts:132-142` | `getSubclassTables()` | Filter: `table.inheritNames?.includes(tableName)` instead of `table.inheritName === tableName` |
| `apps/schema/src/utils.ts:206-223` | `addTableIdsLabelsDescription()` | Support `inheritIds` as array of PascalCase names |

### Key design consideration:
- Diamond inheritance (WGS extends both sampling and sequencing) means a table can appear in multiple subtrees
- The UI tree should show each table once, under its first parent (or grouped), with visual indication of other parents

---

## 5D: Diagrams — COMPLETE

**Goal**: Show multiple inheritance edges.

### Files to change:

| File | Line(s) | Change |
|---|---|---|
| `apps/schema/src/components/SchemaDiagram.vue` | 115-118 | Loop `inheritNames`, draw edge for each parent: `parent <\|-- child` |
| `apps/schema/src/components/NomnomDiagram.vue` | 94 | Loop `inheritNames`, draw edge for each parent |

---

## 5E: Print Views — COMPLETE

**Goal**: Display multiple parents in print output.

### Files to change:

| File | Line(s) | Change |
|---|---|---|
| `apps/schema/src/components/PrintViewTable.vue` | 70 | Display `inheritNames.join(", ")` — also fix inconsistent field name (`inherit` vs `inheritName`) |
| `apps/schema/src/components/PrintViewList.vue` | 72 | Same: display comma-separated parents |

---

## 5F: Profile Manager — COMPLETE

**Goal**: INTERNAL tables not shown as selectable profiles.

### Files to change:

| File | Line(s) | Change |
|---|---|---|
| `apps/schema/src/components/ProfileManager.vue` | 258 | CSV export: handle comma-separated `inheritNames` |
| `apps/schema/src/components/ProfileManager.vue` | (filter logic) | Filter out `tableType === 'INTERNAL'` from profile selector options |

---

## 5G: Form Components — PROFILE/PROFILES Input — COMPLETE

**Goal**: PROFILE column renders as dropdown, PROFILES as multi-select. Options = non-INTERNAL child table names.

### Files to change:

| File | Change |
|---|---|
| `apps/molgenis-components/src/components/forms/FormInput.vue:41-82` | Add `PROFILE` and `PROFILES` to `typeToInputMap`. PROFILE → single select (InputSelect or similar), PROFILES → multi-select |
| New or existing composable | Compute profile options: non-INTERNAL child tables of the root table. Needs schema metadata context. |

### Design notes:
- PROFILE extends STRING — the value stored is the child table name (e.g. "WGS")
- Options come from `rootTable.getSubclassTables().filter(t => t.tableType !== 'INTERNAL').map(t => t.name)`
- This requires schema metadata available in the form context
- May need a new prop or composable to pass available profile options

---

## 5H: Column Edit Modal — COMPLETE

**Goal**: Column type dropdown includes PROFILE/PROFILES options.

### Files to change:

| File | Line(s) | Change |
|---|---|---|
| `apps/schema/src/components/ColumnEditModal.vue` | 605-613 | `getColumnsForTable()` — handle multiple parents when looking up inherited columns |
| `apps/schema/src/components/ColumnEditModal.vue` | (type dropdown) | Add PROFILE/PROFILES to column type options |

---

## 5I: Subclass Column Handling (tailwind-components) — COMPLETE

**Goal**: Column collection works with multi-parent hierarchy.

### Files to change:

| File | Line(s) | Change |
|---|---|---|
| `apps/tailwind-components/app/composables/getSubclassColumns.ts` | 16-17 | Filter: check if table's `inheritNames` includes the target, not just `inheritId === tableId` |
| `apps/tailwind-components/app/composables/getSubclassColumns.ts` | 28-36 | Recursive column collection: handle diamond (dedup columns from multiple parent paths) |

---

## Execution Order

Phases should be done in dependency order:

```
5A (types + query)        ← foundation, everything depends on this
  ↓
5B + 5C (editor + utils)  ← can be done together
  ↓
5D + 5E (diagrams + print) ← cosmetic, independent
5F (profile manager)       ← independent
5G (form input)            ← needs 5A, can parallel with 5B/5C
5H (column edit)           ← needs 5A
5I (subclass columns)      ← needs 5A
```

Suggested batching:
1. **Batch 1**: 5A (types + GraphQL query)
2. **Batch 2**: 5B + 5C + 5H (schema editor core)
3. **Batch 3**: 5D + 5E + 5F (diagrams, print, profile manager)
4. **Batch 4**: 5G + 5I (form components, subclass columns)

---

## Testing Strategy

| Sub-phase | Test type | What to verify |
|---|---|---|
| 5A | Unit test | `ITableMetaData` accepts `inheritNames` array |
| 5B | Story file | TableEditModal with multi-parent selection |
| 5C | Unit test | `convertToSubclassTables()` with diamond inheritance |
| 5D | Visual | Mermaid/Nomnoml diagrams show multiple edges |
| 5E | Visual | Print views show comma-separated parents |
| 5F | Visual | INTERNAL tables hidden from profile selector |
| 5G | Story file | PROFILE dropdown, PROFILES multi-select with correct options |
| 5H | Visual | Column type dropdown includes PROFILE/PROFILES |
| 5I | Unit test | Subclass columns collected across multi-parent hierarchy |

---

## Field Name Inconsistency (Cleanup)

Found during exploration — components use different names:
- `inheritName` — most components
- `inherit` — PrintViewTable.vue:70, PrintViewList.vue:72

Standardize on `inheritNames` (array) as primary, with `inheritName` (first entry) as convenience accessor in the type definition.
