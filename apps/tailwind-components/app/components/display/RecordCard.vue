<script setup lang="ts">
import { computed } from "vue";
import type {
  IColumn,
  IRow,
  IDisplayConfig,
} from "../../../../metadata-utils/src/types";
import ValueEMX2 from "../value/EMX2.vue";

const props = defineProps<{
  row: IRow;
  columns: IColumn[];
  columnConfig?: Record<string, IDisplayConfig>;
}>();

defineSlots<{
  actions?: (props: { row: IRow }) => any;
  default?: (props: { row: IRow; columns: IColumn[] }) => any;
}>();

function getColumnConfig(colId: string): IDisplayConfig | undefined {
  return props.columnConfig?.[colId];
}

const firstColumn = computed(() => props.columns[0]);
const firstColumnConfig = computed(() =>
  firstColumn.value ? getColumnConfig(firstColumn.value.id) : undefined
);
</script>

<template>
  <div class="flex gap-2">
    <div v-if="$slots.actions" class="flex flex-col gap-1 shrink-0">
      <slot name="actions" :row="row" />
    </div>
    <div class="flex-1">
      <slot :row="row" :columns="columns">
        <div
          v-for="(col, colIndex) in columns"
          :key="col.id"
          class="flex justify-between gap-2 py-1 border-b border-black/5 last:border-0"
        >
          <span
            class="text-body-sm font-semibold text-table-column-header shrink-0"
          >
            {{ col.label || col.id }}
          </span>
          <span class="text-body-sm text-table-row text-right">
            <NuxtLink
              v-if="colIndex === 0 && firstColumnConfig?.getHref"
              :to="firstColumnConfig.getHref(col, row)"
              class="text-link hover:underline"
            >
              <ValueEMX2 :metadata="col" :data="row[col.id]" />
            </NuxtLink>
            <span
              v-else-if="colIndex === 0 && firstColumnConfig?.clickAction"
              class="text-link hover:underline cursor-pointer"
              @click="firstColumnConfig.clickAction(col, row)"
            >
              <ValueEMX2 :metadata="col" :data="row[col.id]" />
            </span>
            <ValueEMX2 v-else :metadata="col" :data="row[col.id]" />
          </span>
        </div>
      </slot>
    </div>
  </div>
</template>
