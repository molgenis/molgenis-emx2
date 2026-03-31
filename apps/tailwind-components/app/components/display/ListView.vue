<script setup lang="ts">
import { computed, type Component } from "vue";
import { NuxtLink } from "#components";
import type { IColumn } from "../../../../metadata-utils/src/types";
import {
  filterDataColumns,
  filterNonEmptyColumns,
  getRowLabel,
} from "../../utils/displayUtils";
import RecordTable from "./RecordTable.vue";
import InlinePagination from "./InlinePagination.vue";
import ListCard from "./ListCard.vue";

const props = withDefaults(
  defineProps<{
    rows: Record<string, any>[];
    columns: IColumn[];
    layout?: "TABLE" | "CARDS" | "LIST";
    visibleColumns?: string[];
    hideColumns?: string[];
    rowLabel?: string;
    getHref?: (col: IColumn, row: Record<string, any>) => string;
    component?: Component;
    totalPages?: number;
    currentPage?: number;
    showPagination?: boolean;
    schemaId?: string;
    tableId?: string;
  }>(),
  {
    layout: "TABLE",
    totalPages: 1,
    currentPage: 1,
    showPagination: false,
  }
);

const emit = defineEmits<{
  "update:page": [page: number];
}>();

const dataColumns = computed(() =>
  filterDataColumns(props.columns, props.hideColumns)
);

const nonEmptyColumns = computed(() =>
  filterNonEmptyColumns(dataColumns.value, props.rows)
);

const tableColumns = computed(() => {
  if (!props.visibleColumns?.length) return nonEmptyColumns.value;
  return props.visibleColumns
    .map((id) => nonEmptyColumns.value.find((c) => c.id === id))
    .filter(Boolean) as IColumn[];
});

function rowKey(row: Record<string, any>): string {
  return row.id || row.name || JSON.stringify(row);
}
</script>

<template>
  <div>
    <template v-if="layout === 'TABLE'">
      <RecordTable
        :columns="tableColumns"
        :rows="rows"
        :schema-id="schemaId"
        :table-id="tableId"
      />
    </template>

    <template v-else-if="layout === 'LIST'">
      <ul class="grid grid-cols-1">
        <component
          v-if="component"
          :is="component"
          v-for="(row, index) in rows"
          :key="rowKey(row) || index"
          :data="row"
        />
        <ListCard
          v-else
          v-for="(row, index) in rows"
          :key="rowKey(row) || index"
          :title="getRowLabel(row, rowLabel)"
          :data="row"
          :columns="tableColumns"
          :href="getHref && columns[0] ? getHref(columns[0], row) : undefined"
        />
      </ul>
      <p
        v-if="rows.length === 0"
        class="text-gray-400 dark:text-gray-500 italic"
      >
        No items
      </p>
    </template>

    <template v-else-if="layout === 'CARDS'">
      <ul class="grid grid-cols-1 lg:grid-cols-2">
        <component
          v-if="component"
          :is="component"
          v-for="(row, index) in rows"
          :key="rowKey(row) || index"
          :data="row"
        />
        <ListCard
          v-else
          v-for="(row, index) in rows"
          :key="rowKey(row) || index"
          :title="getRowLabel(row, rowLabel)"
          :data="row"
          :columns="tableColumns"
          :href="getHref && columns[0] ? getHref(columns[0], row) : undefined"
        />
      </ul>
      <p
        v-if="rows.length === 0"
        class="text-gray-400 dark:text-gray-500 italic"
      >
        No items
      </p>
    </template>

    <template v-else>
      <ul v-if="rows.length" class="grid gap-1 pl-4 list-disc list-outside">
        <li v-for="row in rows" :key="rowKey(row)">
          <NuxtLink
            v-if="getHref && columns[0]"
            :to="getHref(columns[0], row)"
            class="text-link hover:underline"
          >
            {{ getRowLabel(row, rowLabel) }}
          </NuxtLink>
          <span v-else>{{ getRowLabel(row, rowLabel) }}</span>
        </li>
      </ul>
      <p v-else class="text-gray-400 dark:text-gray-500 italic">No items</p>
    </template>

    <InlinePagination
      v-if="showPagination"
      :current-page="currentPage || 1"
      :total-pages="totalPages || 1"
      class="mt-4"
      @update:page="emit('update:page', $event)"
    />
  </div>
</template>
