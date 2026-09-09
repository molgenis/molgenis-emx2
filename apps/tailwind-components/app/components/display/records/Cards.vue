<script setup lang="ts">
import {
  isFileValue,
  type IColumn,
  type IRow,
} from "../../../../../metadata-utils/src/types";
import { resolveTitleAndSubtitle } from "../../../utils/displayUtils";
import Pairs from "./Pairs.vue";
import ValueEMX2 from "../../value/EMX2.vue";

const props = defineProps<{
  rows: IRow[];
  titleTemplate: string;
  subtitleTemplate?: string;
  descriptionColumn?: IColumn;
  detailColumns?: IColumn[];
  logoColumn?: IColumn;
  linkTo?: (row: IRow) => string;
}>();

function title(row: IRow): string {
  return resolveTitleAndSubtitle(
    row,
    props.titleTemplate,
    props.subtitleTemplate
  ).title;
}

function subtitle(row: IRow): string | undefined {
  return resolveTitleAndSubtitle(
    row,
    props.titleTemplate,
    props.subtitleTemplate
  ).subtitle;
}

function logoUrl(row: IRow): string | undefined {
  if (!props.logoColumn) {
    return undefined;
  }
  const value = row[props.logoColumn.id];
  return isFileValue(value) ? value.url : undefined;
}
</script>

<template>
  <ul class="grid grid-cols-1 lg:grid-cols-2" role="list">
    <li
      v-for="(row, rowIndex) in rows"
      :key="rowIndex"
      class="border p-11 relative -mb-[1px] lg:even:border-l-0"
    >
      <img
        v-if="logoUrl(row)"
        :src="logoUrl(row)"
        :alt="title(row)"
        class="float-right ml-4 mb-2 max-h-16 max-w-[40%] object-contain"
      />
      <div class="font-bold text-record-heading">
        <NuxtLink v-if="linkTo" :to="linkTo(row)" class="text-link underline">
          {{ title(row) }}
        </NuxtLink>
        <span v-else>{{ title(row) }}</span>
      </div>
      <span
        v-if="subtitle(row)"
        class="mt-1.5 block md:inline text-record-value"
      >
        {{ subtitle(row) }}
      </span>
      <div v-if="descriptionColumn" class="mt-1 text-record-value">
        <ValueEMX2
          :metadata="descriptionColumn"
          :data="row[descriptionColumn.id]"
        />
      </div>
      <Pairs v-if="detailColumns?.length" :columns="detailColumns" :row="row" />
    </li>
  </ul>
</template>
