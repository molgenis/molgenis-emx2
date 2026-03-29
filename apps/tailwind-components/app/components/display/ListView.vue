<script setup lang="ts">
import { computed } from "vue";
import { NuxtLink } from "#components";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { IListConfig } from "../../../types/types";
import {
  filterDataColumns,
  filterNonEmptyColumns,
  getRowLabel,
} from "../../utils/displayUtils";
import RecordTable from "./RecordTable.vue";
import InlinePagination from "./InlinePagination.vue";

const props = withDefaults(
  defineProps<{
    rows: Record<string, any>[];
    columns: IColumn[];
    config?: IListConfig;
    totalPages?: number;
    currentPage?: number;
    showPagination?: boolean;
    schemaId?: string;
    tableId?: string;
  }>(),
  {
    totalPages: 1,
    currentPage: 1,
    showPagination: false,
  }
);

const emit = defineEmits<{
  "update:page": [page: number];
}>();

const dataColumns = computed(() =>
  filterDataColumns(props.columns, props.config?.hideColumns)
);

const nonEmptyColumns = computed(() =>
  filterNonEmptyColumns(dataColumns.value, props.rows)
);

const tableColumns = computed(() => {
  if (!props.config?.visibleColumns?.length) return nonEmptyColumns.value;
  return props.config.visibleColumns
    .map((id) => nonEmptyColumns.value.find((c) => c.id === id))
    .filter(Boolean) as IColumn[];
});

function rowKey(row: Record<string, any>): string {
  return row.id || row.name || JSON.stringify(row);
}
</script>

<template>
  <div>
    <template v-if="config?.layout === 'table'">
      <RecordTable
        :columns="tableColumns"
        :rows="rows"
        :schema-id="schemaId"
        :table-id="tableId"
      />
    </template>

    <template v-else-if="config?.layout === 'cards' && config?.component">
      <div class="grid gap-4">
        <component
          :is="config.component"
          v-for="(row, index) in rows"
          :key="rowKey(row) || index"
          :data="row"
        />
      </div>
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
            v-if="config?.getHref && columns[0]"
            :to="config.getHref(columns[0], row)"
            class="text-link hover:underline"
          >
            {{ getRowLabel(row, config?.rowLabel) }}
          </NuxtLink>
          <span v-else>{{ getRowLabel(row, config?.rowLabel) }}</span>
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
