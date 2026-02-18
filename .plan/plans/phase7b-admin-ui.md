# Phase 7b: Admin UI Permission Matrix — Implementation Plan

## Goal

Add a "Roles" tab to the per-schema page in `/apps/ui` that lets Managers view and edit custom roles and their permission matrix. The UI shows a grid where rows are tables and columns are permission types (SELECT, INSERT, UPDATE, DELETE, GRANT).

## Where It Lives

- **Route**: `/[schema]/roles` (new page under the schema section)
- **Navigation**: "Roles" link in top nav for Manager+ users
- **Access**: Manager+ only (same restriction as `_schema { roles }` GraphQL query)

## Architecture

### Pages
- `apps/ui/app/pages/[schema]/roles.vue` — main page with role selector + permission matrix

### Components (in `apps/ui/app/components/`)
- `PermissionMatrix.vue` — the grid table showing tables x permissions for one role
- `PermissionCell.vue` — individual cell: dropdown for SELECT (6 levels) or INSERT/UPDATE/DELETE (TABLE/ROW/—) or GRANT (yes/—)
- `RoleEditor.vue` — role CRUD panel: create/delete role, role selector dropdown

### Utilities (in `apps/ui/app/util/`)
- `roleUtils.ts` — GraphQL queries/mutations for roles + permissions

### Tests (in `apps/ui/tests/vitest/`)
- `roleUtils.test.ts` — unit tests for data transformation logic (TODO)

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
│ Role: [▼ Analysts    ] [+ New Role] [Delete]          │
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
- **Role selector**: dropdown listing all roles; system roles shown with "(system)" suffix
- **`*` row**: schema-wide default, always first row
- **Per-table rows**: only DATA tables (not ontologies), sorted alphabetically
- **Inherited values**: if a table has no explicit permission but `*` has one, show it greyed/italic
- **Explicit overrides**: bold, user can clear via "x" button to revert to wildcard
- **SELECT dropdown options**: —, EXISTS, RANGE, AGGREGATOR, COUNT, TABLE, ROW
- **INSERT/UPDATE/DELETE dropdown options**: —, TABLE, ROW
- **GRANT dropdown options**: —, Yes
- **Save**: sends `change(roles: [...])` mutation with current permissions
- **System roles**: matrix is read-only (disabled dropdowns), message above matrix explains this
- **System role permissions**: returned as wildcard `*` entry (e.g. Viewer: SELECT=TABLE on `*`)

## Implementation Status

### Step 1: roleUtils.ts — DONE
- `getRolesAndTables(schemaId)` — fetch roles + tables in one query
- `saveRole(schemaId, roleName, description, permissions[])` — create/update
- `createRole(schemaId, roleName, description)` — create new role
- `deleteRole(schemaId, roleName)` — delete
- `dropPermission(schemaId, roleName, tableName)` — revoke specific
- `getDataTables(tables)` — filter to DATA tables only
- Type definitions: `IRoleInfo`, `IPermission`, `ITableInfo`, `IColumnAccess`

### Step 2: PermissionCell.vue — DONE
- Props: `modelValue`, `options`, `inherited`, `disabled`, `isGrant`
- Native `<select>` with options
- Shows inherited value in grey/italic when modelValue is null
- Grant column: only "—" and "Yes" (no redundant "No")

### Step 3: PermissionMatrix.vue — DONE
- Props: `role` (IRoleInfo), `tables` (table list), `readonly` (boolean)
- System role message shown above matrix when readonly
- Tracks dirty state, save/discard buttons
- Clear button per table row to remove explicit overrides

### Step 4: RoleEditor.vue — DONE
- Role selector with "(system)" suffix for system roles
- New Role modal, Delete confirmation modal
- Delete hidden for system roles

### Step 5: roles.vue page — DONE
- Fetches data via roleUtils, composes RoleEditor + PermissionMatrix
- Handles save/create/delete flows with data refresh
- Preserves description on save

### Step 6: Wire navigation — DONE
- "Roles" link added to `default.vue` layout nav for Manager+ users

### Step 7: Backend fix — DONE
- `SqlRoleManager.getPermissions()`: system roles return wildcard `*` permission
- Manager/Owner include `grant=true`

### Step 8: Tests + Stories — TODO
- `roleUtils.test.ts` — test data transformation

## Files

New files:
- `apps/ui/app/pages/[schema]/roles.vue`
- `apps/ui/app/components/PermissionMatrix.vue`
- `apps/ui/app/components/PermissionCell.vue`
- `apps/ui/app/components/RoleEditor.vue`
- `apps/ui/app/util/roleUtils.ts`

Modified files:
- `apps/ui/app/layouts/default.vue` — added Roles nav link for Manager+
- `backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/SqlRoleManager.java` — system role permissions as wildcard
