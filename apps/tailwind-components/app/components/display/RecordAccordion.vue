<script setup lang="ts">
import { computed } from "vue";
import type { IColumn, IRow } from "../../../../metadata-utils/src/types";
import { recordTitle } from "../../utils/recordTitle";
import Accordion from "../Accordion.vue";
import Record from "./Record.vue";

const props = withDefaults(
  defineProps<{
    columns: IColumn[];
    row: IRow;
    titleTemplate?: string;
    openByDefault?: boolean;
  }>(),
  {
    openByDefault: false,
  }
);

defineEmits<{
  (e: "expand"): void;
}>();

const displayLabel = computed(
  () =>
    recordTitle(props.columns, props.row, props.titleTemplate) ||
    recordTitle(props.columns, props.row)
);
</script>

<template>
  <Accordion
    :label="displayLabel"
    :open-by-default="openByDefault"
    @click="$emit('expand')"
  >
    <template #toolbar>
      <slot name="toolbar" />
    </template>
    <div @click="$event.stopPropagation()">
      <Record :columns="columns" :row="row" :show-table-of-contents="false" />
    </div>
  </Accordion>
</template>
