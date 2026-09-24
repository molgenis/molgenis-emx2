<script setup lang="ts">
import { computed, ref } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import TableInteractive from "../../../../tailwind-components/app/components/table/TableInteractive.vue";
import constants from "../../../../tailwind-components/app/utils/constants.ts";
import type {
  ITableSettings,
  CustomRole,
} from "../../../../tailwind-components/types/types.ts";
import { getCustomRoles } from "../../util/adminUtils.ts";

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

const customRoles = ref<CustomRole[]>([]);

const rows = computed(() =>
  customRoles.value.map((customRole) => ({
    schemaId: customRole.schemaId,
    roleName: customRole.roleName,
    tables: getTableNames(customRole),
    users: getUserNames(customRole),
  }))
);

let latestRequest = 0;

async function loadCustomRoles() {
  const request = ++latestRequest;
  const loaded = await getCustomRoles();
  if (request === latestRequest) {
    customRoles.value = loaded.customRoles;
  }
}

await loadCustomRoles();

async function handleSettingsChange(updated: ITableSettings) {
  settings.value = updated;
  await loadCustomRoles();
}

function getTableNames(customRole: CustomRole) {
  return customRole.permissions
    .map((permission) => permission.table)
    .join(", ");
}

function getUserNames(customRole: CustomRole) {
  return customRole.users.join(", ");
}
</script>

<template>
  <TableInteractive
    :columns="COLUMNS"
    :rows="rows"
    :count="customRoles.length"
    :settings="settings"
    @update:settings="handleSettingsChange"
    search-placeholder="Search roles"
  />
</template>
