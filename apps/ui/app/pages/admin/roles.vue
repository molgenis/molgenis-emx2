<script setup lang="ts">
import { computed, ref } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import constants from "../../../../tailwind-components/app/utils/constants.ts";
import type {
  ITableSettings,
  SchemaRole,
} from "../../../../tailwind-components/types/types.ts";
import { getSchemaPermissions } from "~/util/adminUtils.ts";

const schemaRoles = ref<SchemaRole[]>([]);
schemaRoles.value = filterSchemaRoles(await getSchemaPermissions());

const COLUMNS: IColumn[] = [
  { label: "Schema", id: "schemaId", columnType: "STRING" },
  { label: "Role Name", id: "roleName", columnType: "STRING" },
  { label: "Tables", id: "tables", columnType: "STRING_ARRAY" },
];

const settings = defineModel<ITableSettings>("settings", {
  required: false,
  default: () => ({
    page: 1,
    pageSize: constants.PAGE_SIZE_DEFAULT,
    orderby: { column: "", direction: "ASC" },
    search: "",
    orderedColumnsIds: [],
  }),
});

const count = computed(() => schemaRoles.value.length);

function handlePagingRequest(page: number) {
  settings.value.page = page;
}

function handlePageSizeChange(pageSize: string) {
  settings.value.pageSize = Number.parseInt(pageSize);
  settings.value.page = 1;
}

function filterSchemaRoles(schemaRoles: SchemaRole[]) {
  return schemaRoles.filter((schemaRole) => schemaRole.permissions.length);
}

function tableNames(schemaRole: SchemaRole) {
  return schemaRole.permissions
    .map((permission) => permission.table)
    .join(", ");
}
</script>

<template>
  <InputSearch
    class="w-3/5 xl:w-2/5 2xl:w-1/5"
    size="medium"
    v-model="settings.search"
    placeholder="Search roles"
    id="search-input"
  />
  <Table>
    <template #head>
      <TableHeadRow>
        <TableHead v-for="column in COLUMNS" :key="column.id">
          {{ column.label }}
        </TableHead>
      </TableHeadRow>
    </template>
    <template #body>
      <TableRow
        v-for="schemaRole in schemaRoles"
        :key="`${schemaRole.schemaId}/${schemaRole.roleName}`"
      >
        <TableCell>{{ schemaRole.schemaId }}</TableCell>
        <TableCell>{{ schemaRole.roleName }}</TableCell>
        <TableCell>{{ tableNames(schemaRole) }}</TableCell>
      </TableRow>
    </template>
    <template #foot> </template>
  </Table>
  <Pagination
    class="pt-0 pb-[30px]"
    :current-page="settings.page"
    :totalPages="Math.ceil(count / settings.pageSize)"
    :jump-to-edge="true"
    :page-size="settings.pageSize"
    :show-page-size-selector="true"
    @update="handlePagingRequest($event)"
    @update:pageSize="handlePageSizeChange($event)"
  />
</template>
