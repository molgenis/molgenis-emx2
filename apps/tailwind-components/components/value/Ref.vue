<script setup lang="ts">
import { computed } from "vue";
import type { IRefColumn, IRow } from "../../../metadata-utils/src/types";
import { rowToString } from "../../utils/rowToString";
import type { RefPayload } from "../../types/types";
const props = defineProps<{
  metadata: IRefColumn;
  data: IRow;
}>();

const emit = defineEmits<{
  (e: "refCellClicked", data: RefPayload): void;
}>();

const handleRefCellClicked = () => {
  emit("refCellClicked", {
    metadata: props.metadata,
    data: props.data,
  });
};

const refColumnLabel = computed(() => {
  const labelTemplate = (
    props.metadata.refLabel
      ? props.metadata.refLabel
      : props.metadata.refLabelDefault
  ) as string;
  return rowToString(props.data, labelTemplate);
});
</script>

<template>
  <span
    class="underline hover:cursor-pointer text-link"
    @click="handleRefCellClicked"
    >{{ refColumnLabel }}</span
  >
</template>
