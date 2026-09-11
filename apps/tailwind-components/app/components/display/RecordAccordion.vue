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
    showDetails?: boolean;
    openByDefault?: boolean;
  }>(),
  {
    showDetails: true,
    openByDefault: false,
  }
);

defineEmits<{
  (e: "expand"): void;
}>();

// A blank accordion header is unusable, so a template that yields nothing falls back to the primary key.
const displayLabel = computed(
  () =>
    recordTitle(props.columns, props.row, props.titleTemplate) ||
    recordTitle(props.columns, props.row)
);
</script>

<template>
  <span v-if="!showDetails">{{ displayLabel }}</span>
  <Accordion
    v-else
    :label="displayLabel"
    :open-by-default="openByDefault"
    @click="$emit('expand')"
  >
    <template #toolbar>
      <slot name="toolbar" />
    </template>
    <div @click="$event.stopPropagation()">
      <Record :columns="columns" :row="row" :show-legend="false" />
    </div>
  </Accordion>
</template>
