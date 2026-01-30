# Emx2DataView Enhancements Plan

## Features to Add

### 1. Toolbar Actions (lift from TableEMX2)
| Feature | Source | Notes |
|---------|--------|-------|
| Columns button | `table/control/Columns.vue` | Column visibility + reorder |
| Filters button | `table/control/Columns.vue` | Extend to be generic |
| Add button | `TableEMX2.vue` + `EditModal` | Opens form modal |
| Edit button | `TableEMX2.vue` + `EditModal` | Row hover, pencil icon |
| Delete button | `TableEMX2.vue` + `DeleteModal` | Row hover, trash icon |

**Config props to add:**
- `showColumnSelector?: boolean`
- `showFilterSelector?: boolean`
- `isEditable?: boolean` (auto-detect from session roles)

### 2. Generic ColumnSelector Component
Extend `table/control/Columns.vue` to support both columns and filters:

```ts
props: {
  columns: IColumn[]
  mode: 'columns' | 'filters'  // determines icon, label, attribute
}
```

| Mode | Icon | Label | Attribute |
|------|------|-------|-----------|
| columns | columns | Columns | visible |
| filters | filter | Filters | showFilter |

**Add from ShowHide.vue:**
- All/none quick toggles
- Separate data vs metadata (mg_) sections

### 4. Nested Ref Filtering (Killer Feature)
Expand REF/REF_ARRAY columns to filter on referenced table's columns.

**Example:**
```
Pet table has: owner (REF → Person)
Person has: city, age

Filter UI shows:
- owner (expandable)
  - owner.city
  - owner.age
```

**Implementation approach:**
1. Detect REF/REF_ARRAY columns
2. Fetch referenced table metadata on expand
3. Build nested filter path: `owner.city`
4. Convert to GraphQL nested filter: `{ owner: { city: { equals: "X" } } }`

**Challenges:**
- Multi-level nesting? (owner.address.city) - limit to 1 level initially
- Performance: lazy load ref metadata on expand
- URL sync: encode as `owner.city=X`

## Implementation Order

| Phase | Features | Complexity |
|-------|----------|------------|
| 1 | Columns button, Show/hide filters | Low |
| 2 | Add/Edit/Delete buttons | Medium (lift existing) |
| 3 | Filter customization UI | Medium |
| 4 | Nested ref filtering | High |

## Decisions
- Phase 2: Auto-detect editable from session roles (Editor/Admin)
- Phase 3: Persist filter customization in URL params (shareable)
- Phase 4: Recursive unfolding - same pattern repeats, no hard limit
- Phase 4: Collapsed by default, user expands on demand

## Nested Ref Filter - Refined Design

```
FilterColumn (existing)
  └── if REF/REF_ARRAY: show expand chevron
      └── on expand: fetch ref table metadata
          └── render FilterColumn for each ref column (recursive)
              └── filter path: "owner.address.city"
              └── URL: ?owner.address.city=Amsterdam
```

**GraphQL output:**
```graphql
filter: {
  owner: {
    address: {
      city: { equals: "Amsterdam" }
    }
  }
}
```

Same `FilterColumn` component, just with path prefix. Clean.
