<template>
  <div v-if="role">
    <p v-if="readonly" class="text-sm text-gray-500 mb-3">
      System role — permissions are built-in and cannot be edited. Use custom
      roles for fine-grained permissions.
    </p>
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
          <TableCell v-if="readonly">{{ wildcardPerm.select || "—" }}</TableCell>
          <PermissionCell
            v-else
            :modelValue="wildcardPerm.select"
            :options="selectOptions"
            @update:modelValue="updateWildcard('select', $event)"
          />
          <TableCell v-if="readonly">{{ wildcardPerm.insert || "—" }}</TableCell>
          <TableCell
            v-else-if="isModifyDisabled(wildcardPerm.select)"
            class="bg-gray-50 text-gray-400 text-center"
            title="Requires SELECT = TABLE or ROW"
          >
            —
          </TableCell>
          <PermissionCell
            v-else
            :modelValue="wildcardPerm.insert"
            :options="getModifyOptions(wildcardPerm.select)"
            @update:modelValue="updateWildcard('insert', $event)"
          />
          <TableCell v-if="readonly">{{ wildcardPerm.update || "—" }}</TableCell>
          <TableCell
            v-else-if="isModifyDisabled(wildcardPerm.select)"
            class="bg-gray-50 text-gray-400 text-center"
            title="Requires SELECT = TABLE or ROW"
          >
            —
          </TableCell>
          <PermissionCell
            v-else
            :modelValue="wildcardPerm.update"
            :options="getModifyOptions(wildcardPerm.select)"
            @update:modelValue="updateWildcard('update', $event)"
          />
          <TableCell v-if="readonly">{{ wildcardPerm.delete || "—" }}</TableCell>
          <TableCell
            v-else-if="isModifyDisabled(wildcardPerm.select)"
            class="bg-gray-50 text-gray-400 text-center"
            title="Requires SELECT = TABLE or ROW"
          >
            —
          </TableCell>
          <PermissionCell
            v-else
            :modelValue="wildcardPerm.delete"
            :options="getModifyOptions(wildcardPerm.select)"
            @update:modelValue="updateWildcard('delete', $event)"
          />
          <TableCell v-if="readonly">{{ wildcardPerm.grant ? "Yes" : "—" }}</TableCell>
          <TableCell
            v-else-if="isGrantDisabled(wildcardPerm.select)"
            class="bg-gray-50 text-gray-400 text-center"
            title="Requires SELECT = ROW or TABLE"
          >
            —
          </TableCell>
          <PermissionCell
            v-else
            :modelValue="wildcardPerm.grant"
            :options="grantStringOptions"
            isGrant
            @update:modelValue="updateWildcard('grant', $event)"
          />
          <TableCell v-if="!readonly"></TableCell>
        </TableRow>
        <TableRow v-for="table in dataTables" :key="table.id">
          <TableCell>{{ table.label }}</TableCell>
          <TableCell v-if="readonly">{{
            getTablePerm(table.id)?.select ?? wildcardPerm.select ?? "—"
          }}</TableCell>
          <PermissionCell
            v-else
            :modelValue="getTablePerm(table.id)?.select ?? null"
            :options="selectOptions"
            :inherited="wildcardPerm.select"
            @update:modelValue="updateTablePerm(table.id, 'select', $event)"
          />
          <TableCell v-if="readonly">{{
            getTablePerm(table.id)?.insert ?? wildcardPerm.insert ?? "—"
          }}</TableCell>
          <TableCell
            v-else-if="isModifyDisabled(getEffectiveSelect(table.id))"
            class="bg-gray-50 text-gray-400 text-center"
            title="Requires SELECT = TABLE or ROW"
          >
            —
          </TableCell>
          <PermissionCell
            v-else
            :modelValue="getTablePerm(table.id)?.insert ?? null"
            :options="getModifyOptions(getEffectiveSelect(table.id))"
            :inherited="wildcardPerm.insert"
            @update:modelValue="updateTablePerm(table.id, 'insert', $event)"
          />
          <TableCell v-if="readonly">{{
            getTablePerm(table.id)?.update ?? wildcardPerm.update ?? "—"
          }}</TableCell>
          <TableCell
            v-else-if="isModifyDisabled(getEffectiveSelect(table.id))"
            class="bg-gray-50 text-gray-400 text-center"
            title="Requires SELECT = TABLE or ROW"
          >
            —
          </TableCell>
          <PermissionCell
            v-else
            :modelValue="getTablePerm(table.id)?.update ?? null"
            :options="getModifyOptions(getEffectiveSelect(table.id))"
            :inherited="wildcardPerm.update"
            @update:modelValue="updateTablePerm(table.id, 'update', $event)"
          />
          <TableCell v-if="readonly">{{
            getTablePerm(table.id)?.delete ?? wildcardPerm.delete ?? "—"
          }}</TableCell>
          <TableCell
            v-else-if="isModifyDisabled(getEffectiveSelect(table.id))"
            class="bg-gray-50 text-gray-400 text-center"
            title="Requires SELECT = TABLE or ROW"
          >
            —
          </TableCell>
          <PermissionCell
            v-else
            :modelValue="getTablePerm(table.id)?.delete ?? null"
            :options="getModifyOptions(getEffectiveSelect(table.id))"
            :inherited="wildcardPerm.delete"
            @update:modelValue="updateTablePerm(table.id, 'delete', $event)"
          />
          <TableCell v-if="readonly">{{
            getTablePerm(table.id)?.grant ? "Yes" : "—"
          }}</TableCell>
          <TableCell
            v-else-if="isGrantDisabled(getEffectiveSelect(table.id))"
            class="bg-gray-50 text-gray-400 text-center"
            title="Requires SELECT = ROW or TABLE"
          >
            —
          </TableCell>
          <PermissionCell
            v-else
            :modelValue="getTablePerm(table.id)?.grant ?? null"
            :options="grantStringOptions"
            :inherited="wildcardPerm.grant"
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
const grantStringOptions = ["true"];

function getModifyOptions(selectLevel: string | null): string[] {
  if (!selectLevel) return [];
  const selectIndex = SELECT_OPTIONS.indexOf(selectLevel);
  const tableIndex = SELECT_OPTIONS.indexOf("TABLE");
  const rowIndex = SELECT_OPTIONS.indexOf("ROW");

  if (selectIndex < tableIndex) return [];
  if (selectIndex === tableIndex) return ["TABLE", "ROW"];
  if (selectIndex === rowIndex) return ["ROW"];
  return [];
}

function isModifyDisabled(selectLevel: string | null): boolean {
  return getModifyOptions(selectLevel).length === 0;
}

function isGrantDisabled(selectLevel: string | null): boolean {
  if (!selectLevel) return true;
  const selectIndex = SELECT_OPTIONS.indexOf(selectLevel);
  const tableIndex = SELECT_OPTIONS.indexOf("TABLE");
  return selectIndex < tableIndex;
}

function correctModifyValue(
  value: string | null,
  selectLevel: string | null
): string | null {
  if (!value) return null;
  const allowed = getModifyOptions(selectLevel);
  if (allowed.length === 0) return null;
  if (allowed.includes(value)) return value;
  if (value === "TABLE" && allowed.includes("ROW")) return "ROW";
  return null;
}

function correctGrantValue(
  value: string | boolean | null,
  selectLevel: string | null
): string | boolean | null {
  if (!value) return null;
  if (isGrantDisabled(selectLevel)) return null;
  return value;
}

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

function getEffectiveSelect(tableId: string): string | null {
  const tablePerm = getTablePerm(tableId);
  return tablePerm?.select ?? wildcardPerm.value.select;
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
  let updated = { ...current, table: "*", [field]: value };

  if (field === "select") {
    const selectValue = value as string | null;
    updated.insert = correctModifyValue(updated.insert, selectValue);
    updated.update = correctModifyValue(updated.update, selectValue);
    updated.delete = correctModifyValue(updated.delete, selectValue);
    updated.grant = correctGrantValue(updated.grant, selectValue);
  }

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
  let updated = { ...current, [field]: value };

  if (field === "select") {
    const selectValue = value as string | null;
    updated.insert = correctModifyValue(updated.insert, selectValue);
    updated.update = correctModifyValue(updated.update, selectValue);
    updated.delete = correctModifyValue(updated.delete, selectValue);
    updated.grant = correctGrantValue(updated.grant, selectValue);
  }

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
