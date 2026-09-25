<script setup lang="ts">
import { definePageMeta } from "#imports";
import { computed, ref } from "vue";
import type { IColumn, IRow } from "../../../../metadata-utils/src/types.ts";
import TableInteractive from "../../../../tailwind-components/app/components/table/TableInteractive.vue";
import constants from "../../../../tailwind-components/app/utils/constants.ts";
import type {
  CustomRole,
  ITableSettings,
} from "../../../../tailwind-components/types/types.ts";
import { getCustomRoles } from "../../util/adminUtils.ts";

definePageMeta({
  middleware: "admin-only",
});

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

const rows = computed<IRow[]>(() => {
  const offset = (settings.value.page - 1) * settings.value.pageSize;

  const transformedRoles = customRoles.value.map((customRole) => ({
    schemaId: customRole.schemaId,
    roleName: customRole.roleName,
    tables: getTableNames(customRole),
    users: getUserNames(customRole),
  }));

  const filteredRoles = transformedRoles.filter((role) => {
    const search = settings.value?.search?.toLowerCase() || "";
    return (
      role.schemaId.toLowerCase().includes(search) ||
      role.roleName.toLowerCase().includes(search) ||
      role.tables.toLowerCase().includes(search) ||
      role.users.toLowerCase().includes(search)
    );
  });

  const sortedRoles = filteredRoles.sort(
    (a: Record<string, string>, b: Record<string, string>) => {
      const { column, direction } = settings.value.orderby;
      if (!column) return 0;

      const aValue = a[column]?.toLowerCase() ?? "";
      const bValue = b[column]?.toLowerCase() ?? "";

      if (aValue < bValue) return direction === "ASC" ? -1 : 1;
      if (aValue > bValue) return direction === "ASC" ? 1 : -1;
      return 0;
    }
  );

  return sortedRoles.slice(offset, offset + settings.value.pageSize);
});

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
    :rowCount="customRoles.length"
    :settings="settings"
    @update:settings="handleSettingsChange"
    search-placeholder="Search roles"
  />
</template>
