<script setup lang="ts">
import { computed } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import ValueEMX2 from "../value/EMX2.vue";
import {
  getRowLabel,
  isEmptyValue,
  classifyCardColumns,
} from "../../utils/displayUtils";
import { useRecordNavigation } from "../../composables/useRecordNavigation";

const props = withDefaults(
  defineProps<{
    rows: Record<string, any>[];
    columns?: IColumn[];
    maxColumns?: 1 | 2;
    rowLabelTemplate?: string;
    schemaId?: string;
    tableId?: string;
  }>(),
  {
    maxColumns: 2,
  }
);

const cardClassification = computed(() =>
  classifyCardColumns(props.columns ?? [])
);

function rowKey(row: Record<string, any>): string {
  return row.id || row.name || JSON.stringify(row);
}

const { navigateToRecord } = useRecordNavigation();

function cardTitle(row: Record<string, any>): string {
  return getRowLabel(row, props.rowLabelTemplate);
}

function visibleDescription(row: Record<string, any>): string | undefined {
  const col = cardClassification.value.descriptionColumn;
  if (!col) return undefined;
  const value = row[col.id];
  return !isEmptyValue(value) ? value : undefined;
}

function visibleDetailColumns(row: Record<string, any>): IColumn[] {
  return cardClassification.value.detailColumns.filter(
    (col) => !isEmptyValue(row[col.id])
  );
}

function visibleLogoUrl(row: Record<string, any>): string | undefined {
  const col = cardClassification.value.logoColumn;
  if (!col) return undefined;
  const value = row[col.id];
  return !isEmptyValue(value) ? value?.url : undefined;
}
</script>

<template>
  <ul class="grid grid-cols-1" :class="{ 'lg:grid-cols-2': maxColumns === 2 }">
    <li
      v-for="(row, index) in rows"
      :key="rowKey(row) || index"
      class="border lg:even:border-l-0 p-11 relative -mb-[1px]"
    >
      <div class="flex items-start flex-col h-full">
        <img
          v-if="visibleLogoUrl(row)"
          :src="visibleLogoUrl(row)"
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
        <p
          v-if="visibleDescription(row)"
          class="mt-1 line-clamp-2"
          :title="visibleDescription(row)"
        >
          {{ visibleDescription(row) }}
        </p>
        <dl v-if="visibleDetailColumns(row).length" class="mt-3 grid gap-1">
          <div
            v-for="col in visibleDetailColumns(row)"
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
  <p v-if="rows.length === 0" class="text-disabled italic">No items</p>
</template>
