<script setup lang="ts">
import type { IColumn } from "../../../../metadata-utils/src/types";
import ValueEMX2 from "../value/EMX2.vue";
import {
  getRowLabel,
  getDetailColumns,
  getDescriptionColumn,
  getLogoColumn,
} from "../../utils/displayUtils";
import { useRecordNavigation } from "../../composables/useRecordNavigation";

const props = withDefaults(
  defineProps<{
    rows: Record<string, any>[];
    columns?: IColumn[];
    gridColumns?: 1 | 2;
    rowLabelTemplate?: string;
    schemaId?: string;
    tableId?: string;
  }>(),
  {
    gridColumns: 2,
  }
);

function rowKey(row: Record<string, any>): string {
  return row.id || row.name || JSON.stringify(row);
}

const { navigateToRecord } = useRecordNavigation();

function cardTitle(row: Record<string, any>): string {
  return getRowLabel(row, props.rowLabelTemplate);
}

function cardDescription(row: Record<string, any>): string | undefined {
  const col = getDescriptionColumn(props.columns ?? [], row);
  return col ? row[col.id] : undefined;
}

function cardDetailColumns(row: Record<string, any>): IColumn[] {
  return getDetailColumns(props.columns ?? [], row);
}

function cardLogoUrl(row: Record<string, any>): string | undefined {
  const col = getLogoColumn(props.columns ?? [], row);
  if (!col) return undefined;
  return row[col.id]?.url;
}
</script>

<template>
  <ul class="grid grid-cols-1" :class="{ 'lg:grid-cols-2': gridColumns === 2 }">
    <li
      v-for="(row, index) in rows"
      :key="rowKey(row) || index"
      class="border lg:even:border-l-0 p-11 relative -mb-[1px]"
    >
      <div class="flex items-start flex-col h-full">
        <img
          v-if="cardLogoUrl(row)"
          :src="cardLogoUrl(row)"
          :alt="cardTitle(row)"
          class="max-h-16 max-w-full mb-2 object-contain"
        />
        <span class="block">
          <a
            v-if="schemaId && tableId"
            href="#"
            class="font-bold text-link hover:underline"
            @click.prevent="navigateToRecord(schemaId, tableId, row)"
          >
            {{ cardTitle(row) }}
          </a>
          <span v-else class="font-bold">{{ cardTitle(row) }}</span>
        </span>
        <p v-if="cardDescription(row)" class="mt-1 line-clamp-2">
          {{ cardDescription(row) }}
        </p>
        <dl v-if="cardDetailColumns(row).length" class="mt-3 grid gap-1">
          <div
            v-for="col in cardDetailColumns(row)"
            :key="col.id"
            class="flex gap-2"
          >
            <dt class="text-sm font-bold capitalize">
              {{ col.label || col.id }}
            </dt>
            <dd class="text-sm">
              <ValueEMX2 :metadata="col" :data="row[col.id]" />
            </dd>
          </div>
        </dl>
      </div>
    </li>
  </ul>
  <p v-if="rows.length === 0" class="text-gray-400 dark:text-gray-500 italic">
    No items
  </p>
</template>
