<script setup lang="ts">
import { computed } from "vue";
import type { IColumn, IRow } from "../../../../metadata-utils/src/types";
import { rowToString } from "../../../utils/rowToString";
import type { RefPayload } from "../../../types/types";
const props = defineProps<{
  metaData: IColumn;
  data: IRow;
}>();

const emit = defineEmits<{
  (e: "refCellClicked", data: RefPayload): void;
}>();

const handleRefCellClicked = () => {
  emit("refCellClicked", {
    metadata: props.metaData,
    data: props.data,
  });
};

const refColumnLabel = computed(() => {
  const labelTemplate = (
    props.metaData.refLabel
      ? props.metaData.refLabel
      : props.metaData.refLabelDefault
  ) as string;
  return rowToString(props.data, labelTemplate);
});
</script>

<template>
  <span class="underline hover:cursor-pointer" @click="handleRefCellClicked">{{
    refColumnLabel
  }}</span>
</template>
