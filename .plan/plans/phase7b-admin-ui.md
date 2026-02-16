# Phase 7b: Admin UI Permission Matrix — Implementation Plan

## Goal

Add a "Roles" tab to the per-schema page in `/apps/ui` that lets Managers view and edit custom roles and their permission matrix. The UI shows a grid where rows are tables and columns are permission types (SELECT, INSERT, UPDATE, DELETE, GRANT).

## Where It Lives

- **Route**: `/[schema]/roles` (new page under the schema section)
- **Navigation**: Add a "Roles" tab to the schema page (alongside tables listing)
- **Access**: Manager+ only (same restriction as `_schema { roles }` GraphQL query)

## Architecture

### Pages
- `apps/ui/app/pages/[schema]/roles.vue` — main page with role selector + permission matrix

### Components (in `apps/ui/app/components/`)
- `PermissionMatrix.vue` — the grid table showing tables × permissions for one role
- `PermissionCell.vue` — individual cell: dropdown for SELECT (6 levels) or INSERT/UPDATE/DELETE (TABLE/ROW/—) or GRANT (yes/no/—)
- `RoleEditor.vue` — role CRUD panel: create/delete role, role selector dropdown

### Utilities (in `apps/ui/app/util/`)
- `roleUtils.ts` — GraphQL queries/mutations for roles + permissions

### Stories (in `apps/ui/app/pages/`)
- None needed for pages; optionally `PermissionMatrix.story.vue` for the matrix component

### Tests (in `apps/ui/tests/vitest/`)
- `roleUtils.test.ts` — unit tests for data transformation logic

## GraphQL Queries Needed

### Fetch roles + permissions for a schema
```graphql
{
  _schema {
    tables { id, label, tableType }
    roles {
      name
      description
      system
      permissions {
        table
        select
        insert
        update
        delete
        grant
        columns { editable, readonly, hidden }
      }
    }
  }
}
```

### Create/update role with permissions
```graphql
mutation {
  change(roles: [{
    name: "RoleName",
    description: "...",
    permissions: [{
      table: "TableName",
      select: "ROW",
      insert: "TABLE",
      update: "TABLE",
      delete: null,
      grant: true
    }]
  }]) { message }
}
```

### Delete role
```graphql
mutation { drop(roles: ["RoleName"]) { message } }
```

### Drop specific permission
```graphql
mutation {
  drop(permissions: [{role: "RoleName", table: "TableName"}]) { message }
}
```

## UI Design

### Layout
```
┌──────────────────────────────────────────────────────┐
│ Schema: MySchema                                      │
│ [Tables] [Roles]                                      │
├──────────────────────────────────────────────────────┤
│                                                       │
│ Role: [▼ Analysts    ] [+ New Role] [🗑 Delete]       │
│ Description: Data analysts team                       │
│                                                       │
│ ┌─────────┬────────┬────────┬────────┬────────┬─────┐│
│ │ Table   │ Select │ Insert │ Update │ Delete │Grant ││
│ ├─────────┼────────┼────────┼────────┼────────┼─────┤│
│ │ *       │ [ROW ▼]│ [ROW ▼]│ [--- ▼]│ [--- ▼]│[—▼] ││
│ │ Patient │ [ROW  ]│ [ROW  ]│ [ROW ▼]│ [--- ▼]│[—▼] ││
│ │ Samples │ [ROW  ]│ [ROW  ]│ [--- ]│ [---  ]│[—▼] ││
│ │ Disease │[TBL ▼]│ [--- ▼]│ [--- ▼]│ [--- ▼]│[—▼] ││
│ └─────────┴────────┴────────┴────────┴────────┴─────┘│
│                                                       │
│ Italic/grey = inherited from * wildcard               │
│ [Save Changes]  [Discard]                             │
└──────────────────────────────────────────────────────┘
```

### Behavior
- **Role selector**: dropdown listing custom roles (system roles shown as read-only reference)
- **`*` row**: schema-wide default, always first row
- **Per-table rows**: only DATA tables (not ontologies), sorted alphabetically
- **Inherited values**: if a table has no explicit permission but `*` has one, show it greyed/italic
- **Explicit overrides**: bold, user can clear to revert to wildcard
- **SELECT dropdown options**: —, EXISTS, RANGE, AGGREGATOR, COUNT, TABLE, ROW
- **INSERT/UPDATE/DELETE dropdown options**: —, TABLE, ROW
- **GRANT dropdown options**: —, Yes, No
- **Save**: sends `change(roles: [...])` mutation with only changed permissions
- **System roles**: shown in selector but matrix is read-only (no editable dropdowns)

### Components from tailwind-components to use
- `ContentBlock` — section wrapper
- `Table`, `TableHead`, `TableHeadRow`, `TableRow`, `TableCell` — matrix grid
- `Select` or custom dropdown — permission level cells
- `Button` — actions (save, new role, delete)
- `Modal` — confirm delete, new role dialog
- `PageHeader`, `Container`, `BreadCrumbs` — page layout
- `Tab` — navigation between Tables and Roles

## Implementation Steps

### Step 1: roleUtils.ts
- `getRolesAndTables(schemaId)` — fetch roles + tables in one query
- `saveRole(schemaId, roleName, description, permissions[])` — create/update
- `deleteRole(schemaId, roleName)` — delete
- `dropPermission(schemaId, roleName, tableName)` — revoke specific
- Type definitions: `IRoleInfo`, `IPermission`, `IColumnAccess`

### Step 2: PermissionCell.vue
- Props: `modelValue` (current level string or null), `options` (available levels), `inherited` (value from wildcard), `disabled` (for system roles)
- Renders a native `<select>` with options
- Shows inherited value in grey when modelValue is null
- Emits `update:modelValue`

### Step 3: PermissionMatrix.vue
- Props: `role` (IRoleInfo), `tables` (table list), `readonly` (boolean for system roles)
- Computes matrix: for each table, resolve explicit vs inherited permissions
- Tracks dirty state (changed cells)
- Emits `save(permissions[])` with only changed permissions

### Step 4: RoleEditor.vue
- Props: `roles` (IRoleInfo[]), `selectedRole` (string)
- Role selector dropdown, new role button + modal, delete button + confirm
- Emits `select(roleName)`, `create(name, description)`, `delete(roleName)`

### Step 5: roles.vue page
- Fetches data via roleUtils
- Composes RoleEditor + PermissionMatrix
- Handles save/create/delete flows
- Shows success/error messages

### Step 6: Wire navigation
- Add "Roles" tab to schema index page or create schema-level tab navigation
- Route: `/[schema]/roles`
- Only visible to Manager+ users

### Step 7: Tests + Stories
- `roleUtils.test.ts` — test data transformation (inherited resolution, dirty tracking)
- Manual visual testing via dev server

## File List

New files:
- `apps/ui/app/pages/[schema]/roles.vue`
- `apps/ui/app/components/PermissionMatrix.vue`
- `apps/ui/app/components/PermissionCell.vue`
- `apps/ui/app/components/RoleEditor.vue`
- `apps/ui/app/util/roleUtils.ts`
- `apps/ui/tests/vitest/roleUtils.test.ts`

Modified files:
- `apps/ui/app/pages/[schema]/index.vue` — add Roles tab link (or create a schema layout with tabs)
