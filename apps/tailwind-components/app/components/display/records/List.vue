<script setup lang="ts">
import {
  isFileValue,
  type IColumn,
  type IRow,
} from "../../../../../metadata-utils/src/types";
import { resolveTitleAndSubtitle } from "../../../utils/displayUtils";
import Pairs from "./Pairs.vue";
import CardList from "../../CardList.vue";
import CardListItem from "../../CardListItem.vue";
import ValueEMX2 from "../../value/EMX2.vue";

const props = withDefaults(
  defineProps<{
    rows: IRow[];
    titleTemplate: string;
    subtitleTemplate?: string;
    descriptionColumn?: IColumn;
    detailColumns?: IColumn[];
    logoColumn?: IColumn;
    linkTo?: (row: IRow) => string;
    hideEmpty?: boolean;
  }>(),
  {
    // Vue casts an unset boolean prop to false unless a default is given.
    // hideEmpty is Pairs' default (true) to make, not List's, so this
    // forwards a genuine undefined rather than silently overriding it.
    hideEmpty: undefined,
  }
);

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
  <CardList role="list">
    <CardListItem v-for="(row, rowIndex) in rows" :key="rowIndex">
      <div class="grid grid-cols-12 gap-6 py-12.5 px-5 lg:px-12.5">
        <div v-if="logoUrl(row)" class="col-span-3">
          <div class="flex items-center justify-center h-full w-full">
            <img
              :src="logoUrl(row)"
              :alt="title(row)"
              class="max-h-16 max-w-full object-contain"
            />
          </div>
        </div>
        <div :class="logoUrl(row) ? 'col-span-9' : 'col-span-12'">
          <h2 class="font-extrabold text-record-heading">
            <NuxtLink
              v-if="linkTo"
              :to="linkTo(row)"
              class="text-link hover:underline hover:bg-link-hover"
            >
              {{ title(row) }}
            </NuxtLink>
            <span v-else>{{ title(row) }}</span>
          </h2>
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
          <Pairs
            v-if="detailColumns?.length"
            wide
            :columns="detailColumns"
            :row="row"
            :hide-empty="hideEmpty"
          />
        </div>
      </div>
    </CardListItem>
  </CardList>
</template>
