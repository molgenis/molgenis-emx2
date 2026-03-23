<script setup lang="ts">
import { computed } from "vue";
import type { IRefColumn, IRow } from "../../../../metadata-utils/src/types";
import { columnValueToString } from "../../utils/columnValueToString";
import type { RefPayload } from "../../../types/types";

const props = defineProps<{
  metadata: IRefColumn;
  data?: IRow[] | null;
}>();

const emit = defineEmits<{
  (e: "refBackCellClicked", payload: RefPayload): void;
}>();

const handleRefBackCellClicked = () => {
  if (props.data === null || props.data === undefined || !props.data[0]) {
    return;
  }
  emit("refBackCellClicked", {
    metadata: props.metadata,
    data: props.data[0], // todo think about how to handle multiple rows, separate for each row or joined as one?
  });
};

const refBackColumnLabel = computed(() => {
  if (!props.data) {
    return "";
  }
  const labelTemplate = (
    props.metadata.refLabel
      ? props.metadata.refLabel
      : props.metadata.refLabelDefault
  ) as string;
  return props.data
    .map((refRow) => columnValueToString(refRow, labelTemplate))
    .join(", ");
});
</script>

<template>
  <span
    class="underline hover:cursor-pointer text-link"
    @click="handleRefBackCellClicked"
    >{{ refBackColumnLabel }}</span
  >
</template>
