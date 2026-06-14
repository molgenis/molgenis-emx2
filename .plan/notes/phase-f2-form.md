# Phase F2 — module-content reveal in tailwind record form (recon + design)

## ⚠️ CORRECTED FEATURE REQUEST (owner, 2026-06-14) — READ THIS, do not drift
The earlier draft proposed an **Accordion / collapsible "block" per module**. **REJECTED.** The real request:

> Module content columns behave **exactly like normal form columns whose `visible` is gated by which MODULE(_ARRAY) values are selected** — they show/hide INLINE, **interleaved (mixed) with the other root columns by `position`**. NOT a collapsible block, NOT a labeled fieldset, NOT grouped.

Two hard owner clarifications:
1. **No Accordion / no group block.** "These columns might be mixed with other columns, it is not like showing/hiding a block." → module cols slot into the normal column list at their `position` and render via the existing FormField loop.
2. **Gate as a SEPARATE signal, do NOT synthesize a `visible` expression.** "I don't think it wise to add visible expressions 'under the hood' … may conflict with data-modeler-provided expressions." → effective visibility of a module col = `(modeler's own visible expr, evaluated as today) AND (owning module is active)`. The module-active part is its OWN computed boolean, AND-combined with the normal visibility computed. NEVER build/append a synthetic `column.visible` string.

## Why `position` is the interleave key (verified)
E6 (`Emx2CrossModuleColumnOrderTest`) locks that `position` is a **single global file-row-order scale shared across root + module cols** — explicit-position test interleaves ROOT colA=0, MODULE colX=1, ROOT colB=2, MODULE colY=3. gql `metadata.js` exposes `position` (+ `section`,`heading`,`visible`,`required`,`values`) on EVERY column incl. module-table cols. So: merge root cols + active module cols, **stable-sort by `position`** ⇒ correct interleave. (In `diamond_showcase` the modules are declared after Subject, so there module cols land after root cols — interleaving is a capability driven by file row order, not always exercised.)

## Architecture facts (verified)
- Chain: `EditModal.vue` → `Form.vue` → `form/Fields.vue` → `form/FormField.vue` → `Field.vue` → `Input.vue`.
- `useForm(tableMetadata, formValues)` builds visibilityMap/requiredMap/requiredFields/validation/`visibleColumns` ALL over `metadata.value.columns` = ROOT cols only. MODULE_ARRAY col itself IS a root col → renders as the picker (Input.vue MODULE_ARRAY arm → InputEnumArray, F1 done). Module CONTENT cols live under each MODULE table's entry in `schema.tables`, NOT in root cols.
- `fetchMetadata(schemaId)` → `ISchemaMetaData{tables}` incl MODULE tables. Only `Form.story.vue` (`schemaMeta`) + fetchMetadata callers have the full schema; `EditModal/Form/Fields` carry only single-table metadata ⇒ thread an OPTIONAL `schema` (absent ⇒ no module cols ⇒ all existing consumers byte-identical).
- `Fields.vue` renders `form.visibleColumns` via FormField (+ intersection observer for section nav). `visibleColumns` is currently `metadata.value.columns.filter(visible)` (array order = position order from backend).
- Submit: `insertInto`/`updateInto` do `toFormData(formValues.value)` and `toFormData` sends EVERY key incl null ⇒ inactive module cols must be DROPPED from the submitted record (a pure filter), not merely nulled.
- `Accordion.vue` exists but is NOT used here (pivot).

## Pure helpers — ALREADY DONE in `apps/metadata-utils/src/moduleColumns.ts` (stopped agent, 34 vitest green, reusable)
`expandModuleColumns`, `activeModules` (F1); `moduleColumnIds(groups)`, `moduleColumnOwner(groups)→Map<colId,moduleName>`, `activeModuleGroups(groups,active)`, `buildSubmitValues(formValues,rootTable,groups)` (drops keys that are module cols of an INACTIVE module; keeps root cols + discriminator + active module cols). All exported via `index.ts`. These survive the pivot — REUSE them; do not rewrite.

## Design — what to build (CORRECTED)

### 1. `useForm.ts` — add optional 3rd param `schema?: MaybeRef<ISchemaMetaData | undefined>`
- `moduleGroups = expandModuleColumns(metadata.value, unref(schema))` when schema present, else `[]` (schema is static once loaded → groups are static; only ACTIVE set is reactive via formValues).
- `moduleContentColumns` = flat `moduleGroups.flatMap(g => g.columns)`; `ownerMap = moduleColumnOwner(moduleGroups)`.
- `allFormColumns` (static universe) = `metadata.value.columns ∪ moduleContentColumns`. Build visibilityMap, requiredMap, and the expression `formValueKeys` over THIS union (so expressions can reference module col ids too). Seed `formValues` to null for module content cols (mirror the existing root seed loop).
- `activeModuleNames = computed(() => activeModules(formValues.value, metadata.value))` (reactive — driven by the MODULE_ARRAY picker).
- **Gating (SEPARATE AND-signal, NOT a synthetic expr):**
  - `visibilityMap[moduleCol.id] = computed(() => normalVisible(moduleCol) && activeModuleNames.value.has(ownerMap.get(moduleCol.id)))`.
  - `requiredMap[moduleCol.id] = computed(() => normalRequired(moduleCol) && activeModuleNames.value.has(ownerMap.get(moduleCol.id)))`.
  - Root cols: unchanged.
- `visibleColumns` = `allFormColumns.filter(visibilityMap…)` keep the existing mg_/AUTO_ID filters, then **stable-sort by `position`** (numeric asc; treat missing as +∞ or 0 consistently — root cols already have positions). Inactive module col ⇒ visibility false ⇒ filtered out. Reveal/hide is reactive & inline. ⇒ **`Fields.vue` needs NO change** (module cols flow through `visibleColumns`).
- `requiredFields` = `allFormColumns` filtered by the gated maps (inactive module cols auto-excluded; active+required included).
- `validateAllColumns`: validate root cols (as today) + **active** module cols only; NEVER validate an inactive module col (getColumnError sees its raw `required`, so skipping is what makes "inactive+omit→ok"). Simplest: iterate `visibleColumns` ∪ root cols, or iterate `allFormColumns` and `continue` when it's an inactive module col.
- `visibleColumnErrors` already intersects `visibleColumns` ⇒ active module col errors surface automatically.
- Submit: `insertInto`/`updateInto` use `toFormData(buildSubmitValues(formValues.value, metadata.value, moduleGroups))`. EXPOSE a `submitValues` computed for the unit test.
- **Deselect clears (UX):** watch `activeModuleNames`; when a module leaves the set, null out its content cols in `formValues` (re-select starts fresh). `buildSubmitValues` is the authoritative omission; this is UX + belt-and-suspenders.
- Backward compatible: no schema ⇒ moduleGroups=[] ⇒ identical existing behavior. Add new returns (`submitValues`; optionally `activeModuleNames`) to the `UseForm` interface. NO `activeModuleGroups` rendering needed.

### 2. `EditModal.vue` — accept optional `schema?: ISchemaMetaData` prop → `useForm(props.metadata, formValues, props.schema)`
Pure pass-through. Do NOT wire apps/ui callers (F3/F4, out of scope). In `Form.story.vue`, pass the already-available `schemaMeta` into its direct `useForm(...)` calls so the story demonstrates live inline reveal.

### 3. `Fields.vue` — expected NO change. Confirm module cols render inline via `visibleColumns` and the intersection observer still tracks them without error. (No Accordion, no ModuleGroup.vue.)

## Tests (vitest — all green under `pnpm run test-ci`, exit 0)
1. **active-set / submit payload (pure, moduleColumns.test.ts — already largely done):** `buildSubmitValues` includes active module cols, EXCLUDES deselected, always keeps root + discriminator (none/one/two-axis/module-extends-module). Keep the 34 existing.
2. **inline visibility + interleave (useForm.spec.ts):** with `schema`, a module col appears in `visibleColumns` ONLY when its module is active; toggling the discriminator value adds/removes it live; `visibleColumns` is ordered by `position` (assert a module col with a position between two root cols sorts between them).
3. **required-gating (useForm.spec.ts):** module col required=true ⇒ ACTIVE+omit ⇒ in `emptyRequiredFields`/`requiredFields` & `isValid()` false; INACTIVE+omit ⇒ NOT required, `isValid()` true, not validated.
4. **story:** extend `Form.story.vue` (or add a focused story) on a root w/ a MODULE_ARRAY axis: selecting a module value reveals its columns INLINE (no block); deselect hides them. Follow Form.story / Enum.story convention.

## Hard constraints (owner)
- NO dev server / gradle / pnpm dev / Playwright. ONLY `pnpm run test-ci`, `pnpm format`, `pnpm lint` (exit 0; fix pre-existing lint in touched files).
- NO git stash/reset/checkout/restore. `git add <explicit touched paths>` only. Do NOT commit. Never stage nuxt.config.ts.
- Preserve existing comments; no NEW code comments (self-documenting names). Reuse Field/Input/FormField. Every catch ≥ console.error.

## Spec rows satisfied on completion
- Row 110 (REWORDED): module content cols render INLINE, interleaved with root cols by `position`, shown/hidden by which MODULE(_ARRAY) values are selected (like visibility, via a separate AND-gate — not a synthetic visible expr); writes discriminator + active module col values to the root mutation; deselect omits them (→ engine C6 hard-delete).
- Row 111: module col effective visible/required gated by module-active (separate AND signal; never required/validated when inactive). Mirrors engine C5.
