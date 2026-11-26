<script setup lang="ts">
import {
  isFileValue,
  type IColumn,
  type IRow,
} from "../../../../../metadata-utils/src/types";
import { plainValue, titleSlots } from "../../../utils/displayUtils";
import Pairs from "./Pairs.vue";
import ValueEMX2 from "../../value/EMX2.vue";

const DESCRIPTION_LINES = 4;

const props = defineProps<{
  rows: IRow[];
  titleColumn?: IColumn;
  subtitleColumn?: IColumn;
  descriptionColumn?: IColumn;
  detailColumns?: IColumn[];
  logoColumn?: IColumn;
  linkTo?: (row: IRow) => string;
}>();

function slots(row: IRow) {
  return titleSlots(row, props.titleColumn, props.subtitleColumn);
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
  <div class="@container">
    <ul class="grid grid-cols-1 gap-5 @2xl:grid-cols-2" role="list">
      <li v-for="(row, rowIndex) in rows" :key="rowIndex" class="border p-5">
        <img
          v-if="logoUrl(row)"
          :src="logoUrl(row)"
          :alt="plainValue(row, slots(row).title)"
          class="mb-3 block h-16 w-auto max-w-full object-contain object-left"
        />
        <h2 class="font-extrabold text-record-heading">
          <NuxtLink
            v-if="linkTo"
            :to="linkTo(row)"
            class="text-link hover:underline hover:bg-link-hover"
          >
            <ValueEMX2
              v-if="slots(row).title"
              :metadata="slots(row).title!"
              :data="row[slots(row).title!.id]"
            />
          </NuxtLink>
          <ValueEMX2
            v-else-if="slots(row).title"
            :metadata="slots(row).title!"
            :data="row[slots(row).title!.id]"
          />
        </h2>
        <span
          v-if="slots(row).subtitle"
          class="mt-1.5 block md:inline text-record-value"
        >
          <ValueEMX2
            :metadata="slots(row).subtitle!"
            :data="row[slots(row).subtitle!.id]"
          />
        </span>
        <div v-if="descriptionColumn" class="mt-1 text-record-value">
          <ValueEMX2
            :metadata="descriptionColumn"
            :data="row[descriptionColumn.id]"
            :maxLines="DESCRIPTION_LINES"
          />
        </div>
        <Pairs
          v-if="detailColumns?.length"
          :columns="detailColumns"
          :row="row"
        />
      </li>
    </ul>
  </div>
</template>
