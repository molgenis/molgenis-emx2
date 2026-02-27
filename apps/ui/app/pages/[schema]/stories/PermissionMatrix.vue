<template>
  <Story title="PermissionMatrix" :description="spec">
    <div class="p-4 space-y-8">
      <div>
        <h3 class="text-lg font-semibold mb-2">Editable custom role</h3>
        <PermissionMatrix
          :role="customRole"
          :tables="tables"
          :readonly="false"
          @save="handleSave"
          @dropPermission="handleDrop"
        />
      </div>

      <div>
        <h3 class="text-lg font-semibold mb-2">Read-only system role</h3>
        <PermissionMatrix
          :role="systemRole"
          :tables="tables"
          :readonly="true"
        />
      </div>
    </div>
  </Story>
</template>

<script setup>
import PermissionMatrix from "~/components/PermissionMatrix.vue";

const tables = [
  { id: "Patients", label: "Patients", tableType: "DATA" },
  { id: "Samples", label: "Samples", tableType: "DATA" },
  { id: "Diseases", label: "Diseases", tableType: "DATA" },
];

const customRole = {
  name: "Analysts",
  description: "Can view and analyze data",
  system: false,
  permissions: [
    {
      table: "*",
      select: "ROW",
      insert: "ROW",
      update: null,
      delete: null,
      grant: null,
    },
    {
      table: "Diseases",
      select: "TABLE",
      insert: null,
      update: null,
      delete: null,
      grant: null,
    },
  ],
};

const systemRole = {
  name: "Viewer",
  description: "Can view all data",
  system: true,
  permissions: [
    {
      table: "*",
      select: "TABLE",
      insert: null,
      update: null,
      delete: null,
      grant: null,
    },
  ],
};

function handleSave(permissions) {
  console.log("Save:", permissions);
}

function handleDrop(tableName) {
  console.log("Drop:", tableName);
}

const spec = `
## Features
- Grid showing tables x permission types for one role
- Wildcard (*) row for schema-wide defaults
- Per-table rows override wildcard
- Inherited values shown in grey/italic
- Save/Discard buttons when dirty
- Clear button per row to remove overrides
- Read-only mode for system roles

## Props
| Prop | Type | Default |
|------|------|---------|
| role | IRoleInfo | required |
| tables | ITableInfo[] | required |
| readonly | boolean | false |

## Test Checklist
- [ ] Wildcard row always shown first
- [ ] Per-table rows sorted alphabetically
- [ ] Inherited values displayed correctly
- [ ] Save button appears on change
- [ ] Clear reverts to inherited
- [ ] System role shown read-only
`;
</script>
