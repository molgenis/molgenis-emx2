<script setup lang="ts">
import { computed, ref } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import TableInteractive from "../../../../tailwind-components/app/components/table/TableInteractive.vue";
import constants from "../../../../tailwind-components/app/utils/constants.ts";
import type {
  ITableSettings,
  SchemaRole,
} from "../../../../tailwind-components/types/types.ts";
import { getSchemaPermissions } from "../../util/adminUtils.ts";

const COLUMNS: IColumn[] = [
  { label: "Schema", id: "schemaId", columnType: "STRING" },
  { label: "Role Name", id: "roleName", columnType: "STRING" },
  { label: "Tables", id: "tables", columnType: "STRING_ARRAY" },
  { label: "Users", id: "users", columnType: "STRING_ARRAY" },
];

const settings = ref<ITableSettings>({
  page: 1,
  pageSize: constants.PAGE_SIZE_DEFAULT,
  orderby: { column: "", direction: "ASC" },
  search: "",
  orderedColumnsIds: [],
});

const schemaRoles = ref<SchemaRole[]>([]);
const schemaRoleCount = ref(0);

const rows = computed(() =>
  schemaRoles.value.map((schemaRole) => ({
    schemaId: schemaRole.schemaId,
    roleName: schemaRole.roleName,
    tables: getTableNames(schemaRole),
    users: getUserNames(schemaRole),
  }))
);

let latestRequest = 0;

async function loadSchemaRoles() {
  const request = ++latestRequest;
  const { page, pageSize, search } = settings.value;
  const loaded = await getSchemaPermissions(
    pageSize,
    (page - 1) * pageSize,
    search
  );
  if (request === latestRequest) {
    schemaRoles.value = loaded.schemaRoles;
    schemaRoleCount.value = loaded.schemaRoleCount;
  }
}

await loadSchemaRoles();

async function handleSettingsChange(updated: ITableSettings) {
  settings.value = updated;
  await loadSchemaRoles();
}

function getTableNames(schemaRole: SchemaRole) {
  return schemaRole.permissions
    .map((permission) => permission.table)
    .join(", ");
}

function getUserNames(schemaRole: SchemaRole) {
  return schemaRole.users.join(", ");
}
</script>

<template>
  <TableInteractive
    :columns="COLUMNS"
    :rows="rows"
    :count="schemaRoleCount"
    :settings="settings"
    @update:settings="handleSettingsChange"
    search-placeholder="Search roles"
  />
</template>
