<script setup lang="ts">
import { computed } from "vue";
import type { IColumn, IRow } from "../../../../metadata-utils/src/types";
import type { DisplayConfig } from "../../types/display";
import { columnValueToString } from "../../utils/columnValueToString";
import { resolveDisplay } from "../../utils/displayUtils";
import ValueEMX2 from "../value/EMX2.vue";

const props = defineProps<{
  rows: IRow[];
  columns?: IColumn[];
  displayConfig?: DisplayConfig;
  linkTo?: (row: IRow) => string;
  hideListSeparator?: boolean;
  maxLines?: number;
  renderLimit?: number;
  truncate?: boolean;
}>();

const resolved = computed(() =>
  resolveDisplay(props.columns ?? [], props.displayConfig)
);

function titleText(row: IRow): string {
  if (!resolved.value.titleTemplate) {
    return "";
  }
  return columnValueToString(row, resolved.value.titleTemplate) ?? "";
}
</script>

<template>
  <div class="overflow-x-auto">
    <table class="w-full table-auto">
      <thead>
        <tr>
          <th
            scope="col"
            class="py-2.5 px-2.5 text-left text-table-column-header"
          >
            Title
          </th>
          <th
            v-for="column in resolved.detailColumns"
            :key="column.id"
            scope="col"
            class="py-2.5 px-2.5 text-left text-table-column-header"
          >
            {{ column.label || column.id }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="(row, rowIndex) in rows"
          :key="rowIndex"
          class="border-b border-theme"
        >
          <td class="py-2.5 px-2.5 font-bold text-table-row">
            <a v-if="linkTo" :href="linkTo(row)" class="text-link underline">
              {{ titleText(row) }}
            </a>
            <span v-else>{{ titleText(row) }}</span>
          </td>
          <td
            v-for="column in resolved.detailColumns"
            :key="column.id"
            class="py-2.5 px-2.5 text-table-row"
          >
            <ValueEMX2
              :metadata="column"
              :data="row[column.id]"
              :hide-list-separator="hideListSeparator"
              :max-lines="maxLines"
              :render-limit="renderLimit"
              :truncate="truncate"
            />
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
