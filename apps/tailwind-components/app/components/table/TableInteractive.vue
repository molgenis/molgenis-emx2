<script setup lang="ts">
import Table from "../Table.vue";
const props = defineProps<{
  columns: { id: string; label: string }[];
  rows: Record<string, unknown>[];
  settings: {
    search: string | undefined;
    page: number;
    pageSize: number;
    totalRowCount: number;
  };
}>();

const emit = defineEmits<{
  (event: "update:settings", value: typeof props.settings): void;
}>();

function handlePagingRequest(page: number) {
  emit("update:settings", { ...props.settings, page });
}

function handlePageSizeChange(pageSize: string) {
  emit("update:settings", {
    ...props.settings,
    pageSize: Number.parseInt(pageSize),
    page: 1,
  });
}

function handleSearchChange(search?: string) {
  emit("update:settings", { ...props.settings, search, page: 1 });
}
</script>

<template>
  <InputSearch
    class="w-3/5 xl:w-2/5 2xl:w-1/5"
    size="medium"
    modelValue="settings.search"
    @update:model-value="handleSearchChange($event)"
    placeholder="Search roles"
    id="search-input"
  />
  <Table>
    <template #head>
      <TableHeadRow>
        <TableHead v-for="column in columns" :key="column.id">
          {{ column.label }}
        </TableHead>
      </TableHeadRow>
    </template>
    <template #body>
      <TableRow v-for="row in rows" :key="`${row.schemaId}/${row.roleName}`">
        <TableCell v-for="column in columns" :key="column.id">
          {{ row[column.id] }}
        </TableCell>
      </TableRow>
    </template>
    <template #foot> </template>
  </Table>
  <Pagination
    class="pt-0 pb-[30px]"
    :current-page="settings.page"
    :totalPages="Math.ceil(settings.totalRowCount / settings.pageSize)"
    :jumpToEdge="true"
    :pageSize="settings.pageSize"
    :showPageSizeSelector="true"
    @update="handlePagingRequest($event)"
    @update:pageSize="handlePageSizeChange($event)"
  />
</template>
