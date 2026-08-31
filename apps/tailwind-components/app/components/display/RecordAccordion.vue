<script setup lang="ts">
import { computed } from "vue";
import type {
  IRow,
  ITableMetaData,
} from "../../../../metadata-utils/src/types";
import { recordTitle } from "../../utils/recordTitle";
import Accordion from "../Accordion.vue";
import Record from "./Record.vue";

const props = withDefaults(
  defineProps<{
    metadata: ITableMetaData;
    rowData: IRow | null;
    label?: string;
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

// A blank accordion header is unusable, so fall back to the table label.
const displayLabel = computed(
  () =>
    props.label ??
    (recordTitle(props.metadata, props.rowData) || props.metadata.label)
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
      <Record
        :metadata="metadata"
        :row-data="rowData"
        :show-legend="false"
        :show-filter="false"
      />
    </div>
  </Accordion>
</template>
