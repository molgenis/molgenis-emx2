<template>
  <div v-if="role">
    <Table>
      <template #head>
        <TableHeadRow>
          <TableHead>Table</TableHead>
          <TableHead>Select</TableHead>
          <TableHead>Insert</TableHead>
          <TableHead>Update</TableHead>
          <TableHead>Delete</TableHead>
          <TableHead>Grant</TableHead>
          <TableHead v-if="!readonly"></TableHead>
        </TableHeadRow>
      </template>
      <template #body>
        <TableRow>
          <TableCell class="font-semibold">* (all tables)</TableCell>
          <PermissionCell
            :modelValue="wildcardPerm.select"
            :options="selectOptions"
            :disabled="readonly"
            @update:modelValue="updateWildcard('select', $event)"
          />
          <PermissionCell
            :modelValue="wildcardPerm.insert"
            :options="modifyOptions"
            :disabled="readonly"
            @update:modelValue="updateWildcard('insert', $event)"
          />
          <PermissionCell
            :modelValue="wildcardPerm.update"
            :options="modifyOptions"
            :disabled="readonly"
            @update:modelValue="updateWildcard('update', $event)"
          />
          <PermissionCell
            :modelValue="wildcardPerm.delete"
            :options="modifyOptions"
            :disabled="readonly"
            @update:modelValue="updateWildcard('delete', $event)"
          />
          <PermissionCell
            :modelValue="wildcardPerm.grant"
            :options="grantStringOptions"
            :disabled="readonly"
            isGrant
            @update:modelValue="updateWildcard('grant', $event)"
          />
          <TableCell v-if="!readonly"></TableCell>
        </TableRow>
        <TableRow v-for="table in dataTables" :key="table.id">
          <TableCell>{{ table.label }}</TableCell>
          <PermissionCell
            :modelValue="getTablePerm(table.id)?.select ?? null"
            :options="selectOptions"
            :inherited="wildcardPerm.select"
            :disabled="readonly"
            @update:modelValue="updateTablePerm(table.id, 'select', $event)"
          />
          <PermissionCell
            :modelValue="getTablePerm(table.id)?.insert ?? null"
            :options="modifyOptions"
            :inherited="wildcardPerm.insert"
            :disabled="readonly"
            @update:modelValue="updateTablePerm(table.id, 'insert', $event)"
          />
          <PermissionCell
            :modelValue="getTablePerm(table.id)?.update ?? null"
            :options="modifyOptions"
            :inherited="wildcardPerm.update"
            :disabled="readonly"
            @update:modelValue="updateTablePerm(table.id, 'update', $event)"
          />
          <PermissionCell
            :modelValue="getTablePerm(table.id)?.delete ?? null"
            :options="modifyOptions"
            :inherited="wildcardPerm.delete"
            :disabled="readonly"
            @update:modelValue="updateTablePerm(table.id, 'delete', $event)"
          />
          <PermissionCell
            :modelValue="getTablePerm(table.id)?.grant ?? null"
            :options="grantStringOptions"
            :inherited="wildcardPerm.grant"
            :disabled="readonly"
            isGrant
            @update:modelValue="updateTablePerm(table.id, 'grant', $event)"
          />
          <TableCell v-if="!readonly">
            <Button
              v-if="hasExplicitPerm(table.id)"
              iconOnly
              icon="cross"
              type="secondary"
              size="tiny"
              label="Clear table permission"
              @click="emit('dropPermission', role.name, table.id)"
            />
          </TableCell>
        </TableRow>
      </template>
    </Table>
    <div v-if="!readonly" class="flex gap-2 mt-3">
      <Button
        type="primary"
        size="small"
        :disabled="!isDirty"
        @click="handleSave"
      >
        Save Changes
      </Button>
      <Button
        type="secondary"
        size="small"
        :disabled="!isDirty"
        @click="handleDiscard"
      >
        Discard
      </Button>
    </div>
    <p v-if="readonly" class="text-sm text-gray-500 mt-2">
      System roles are read-only
    </p>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from "vue";
import {
  type IRoleInfo,
  type IPermission,
  type ITableInfo,
  SELECT_OPTIONS,
  MODIFY_OPTIONS,
  getDataTables,
} from "~/util/roleUtils";
import Table from "../../../tailwind-components/app/components/Table.vue";
import TableHead from "../../../tailwind-components/app/components/TableHead.vue";
import TableRow from "../../../tailwind-components/app/components/TableRow.vue";
import TableCell from "../../../tailwind-components/app/components/TableCell.vue";
import TableHeadRow from "../../../tailwind-components/app/components/TableHeadRow.vue";
import Button from "../../../tailwind-components/app/components/Button.vue";
import PermissionCell from "./PermissionCell.vue";

const props = defineProps<{
  role: IRoleInfo;
  tables: ITableInfo[];
  readonly: boolean;
}>();

const emit = defineEmits<{
  save: [permissions: IPermission[]];
  dropPermission: [roleName: string, tableName: string];
}>();

const selectOptions = SELECT_OPTIONS;
const modifyOptions = MODIFY_OPTIONS;
const grantStringOptions = ["true", "false"];

const dataTables = computed(() => getDataTables(props.tables));

const emptyPerm = (): IPermission => ({
  table: "*",
  select: null,
  insert: null,
  update: null,
  delete: null,
  grant: null,
  columns: null,
});

const editedPerms = ref<Map<string, IPermission>>(new Map());

function initPerms() {
  const permsMap = new Map<string, IPermission>();
  for (const perm of props.role.permissions) {
    permsMap.set(perm.table, { ...perm });
  }
  editedPerms.value = permsMap;
}

watch(() => props.role, initPerms, { immediate: true });

const wildcardPerm = computed(() => {
  return editedPerms.value.get("*") || emptyPerm();
});

function getTablePerm(tableId: string): IPermission | null {
  return editedPerms.value.get(tableId) || null;
}

function hasExplicitPerm(tableId: string): boolean {
  return editedPerms.value.has(tableId);
}

const isDirty = computed(() => {
  const originalMap = new Map<string, IPermission>();
  for (const perm of props.role.permissions) {
    originalMap.set(perm.table, perm);
  }
  if (editedPerms.value.size !== originalMap.size) return true;
  for (const [table, edited] of editedPerms.value) {
    const original = originalMap.get(table);
    if (!original) return true;
    if (
      edited.select !== original.select ||
      edited.insert !== original.insert ||
      edited.update !== original.update ||
      edited.delete !== original.delete ||
      edited.grant !== original.grant
    ) {
      return true;
    }
  }
  return false;
});

function updateWildcard(field: string, value: string | boolean | null) {
  const current = editedPerms.value.get("*") || emptyPerm();
  const updated = { ...current, table: "*", [field]: value };
  editedPerms.value = new Map(editedPerms.value).set("*", updated);
}

function updateTablePerm(
  tableId: string,
  field: string,
  value: string | boolean | null
) {
  const current = editedPerms.value.get(tableId) || {
    table: tableId,
    select: null,
    insert: null,
    update: null,
    delete: null,
    grant: null,
    columns: null,
  };
  const updated = { ...current, [field]: value };
  const allNull =
    updated.select === null &&
    updated.insert === null &&
    updated.update === null &&
    updated.delete === null &&
    updated.grant === null;
  const newMap = new Map(editedPerms.value);
  if (allNull) {
    newMap.delete(tableId);
  } else {
    newMap.set(tableId, updated);
  }
  editedPerms.value = newMap;
}

function handleSave() {
  const permissions = Array.from(editedPerms.value.values());
  emit("save", permissions);
}

function handleDiscard() {
  initPerms();
}
</script>
