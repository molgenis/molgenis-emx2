<script setup lang="ts">
import { computed } from "vue";
import { useDebounceFn } from "@vueuse/core";
import type { ITableSettings } from "../../../types/types.ts";
import { FILTER_DEBOUNCE } from "../../composables/useFilters";
import Table from "../Table.vue";

const props = withDefaults(
  defineProps<{
    columns: { id: string; label: string }[];
    rows: Record<string, unknown>[];
    count: number;
    settings: ITableSettings;
    searchPlaceholder?: string;
  }>(),
  {
    searchPlaceholder: "Search",
  }
);

const emit = defineEmits<{
  (event: "update:settings", value: ITableSettings): void;
}>();

const totalPages = computed(() =>
  Math.max(1, Math.ceil(props.count / props.settings.pageSize))
);

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

const handleSearchChange = useDebounceFn((search?: string) => {
  emit("update:settings", { ...props.settings, search, page: 1 });
}, FILTER_DEBOUNCE);
</script>

<template>
  <InputSearch
    class="w-3/5 xl:w-2/5 2xl:w-1/5"
    size="medium"
    :model-value="settings.search"
    @update:model-value="handleSearchChange($event)"
    :placeholder="searchPlaceholder"
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
      <TableRow v-for="(row, index) in rows" :key="index">
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
    :totalPages="totalPages"
    :jumpToEdge="true"
    :pageSize="settings.pageSize"
    :showPageSizeSelector="true"
    @update="handlePagingRequest($event)"
    @update:pageSize="handlePageSizeChange($event)"
  />
</template>
