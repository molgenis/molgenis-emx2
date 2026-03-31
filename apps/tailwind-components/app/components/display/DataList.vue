<script setup lang="ts">
import { computed } from "vue";
import { NuxtLink } from "#components";
import type { IColumn } from "../../../../metadata-utils/src/types";
import { getListColumns, getRowLabel } from "../../utils/displayUtils";
import DataTable from "./DataTable.vue";
import DataCard from "./DataCard.vue";

const props = withDefaults(
  defineProps<{
    rows: Record<string, any>[];
    columns: IColumn[];
    layout?: "TABLE" | "CARDS" | "LIST" | "LINKS";
    rowLabel?: string;
    getHref?: (row: Record<string, any>) => string;
    schemaId?: string;
    tableId?: string;
  }>(),
  {
    layout: "TABLE",
  }
);

const tableColumns = computed(() =>
  getListColumns(props.columns, {
    rows: props.rows,
  })
);

function rowKey(row: Record<string, any>): string {
  return row.id || row.name || JSON.stringify(row);
}
</script>

<template>
  <div>
    <template v-if="layout === 'TABLE'">
      <DataTable
        :columns="tableColumns"
        :rows="rows"
        :schema-id="schemaId"
        :table-id="tableId"
      />
    </template>

    <template v-else-if="layout === 'CARDS'">
      <ul class="grid grid-cols-1 lg:grid-cols-2">
        <DataCard
          v-for="(row, index) in rows"
          :key="rowKey(row) || index"
          :title="getRowLabel(row, rowLabel)"
          :data="row"
          :columns="tableColumns"
          :href="getHref?.(row)"
        />
      </ul>
      <p
        v-if="rows.length === 0"
        class="text-gray-400 dark:text-gray-500 italic"
      >
        No items
      </p>
    </template>

    <template v-else-if="layout === 'LIST'">
      <ul class="grid grid-cols-1">
        <DataCard
          v-for="(row, index) in rows"
          :key="rowKey(row) || index"
          :title="getRowLabel(row, rowLabel)"
          :data="row"
          :columns="tableColumns"
          :href="getHref?.(row)"
        />
      </ul>
      <p
        v-if="rows.length === 0"
        class="text-gray-400 dark:text-gray-500 italic"
      >
        No items
      </p>
    </template>

    <template v-else-if="layout === 'LINKS'">
      <ul v-if="rows.length" class="grid gap-1 pl-4 list-disc list-outside">
        <li v-for="row in rows" :key="rowKey(row)">
          <NuxtLink
            v-if="getHref"
            :to="getHref(row)"
            class="text-link hover:underline"
          >
            {{ getRowLabel(row, rowLabel) }}
          </NuxtLink>
          <span v-else>{{ getRowLabel(row, rowLabel) }}</span>
        </li>
      </ul>
      <p v-else class="text-gray-400 dark:text-gray-500 italic">No items</p>
    </template>
  </div>
</template>
