<script setup lang="ts">
import { computed } from "vue";
import type { IRefColumn, IRow } from "../../../metadata-utils/src/types";
import { rowToString } from "../../utils/rowToString";
import type { RefBackPayload } from "../../types/types";

const props = defineProps<{
  metaData: IRefColumn;
  data: IRow[];
}>();

const emit = defineEmits<{
  (e: "refBackCellClicked", payload: RefBackPayload): void;
}>();

const handleRefBackCellClicked = () => {
  emit("refBackCellClicked", {
    metadata: props.metaData,
    data: props.data,
  });
};

const refBackColumnLabel = computed(() => {
  // we know that in case of refback, either refLabel or refLabelDefault is defined, although this can not easily be expressed in the typescript
  const labelTemplate = (
    props.metaData.refLabel
      ? props.metaData.refLabel
      : props.metaData.refLabelDefault
  ) as string;
  return props.data
    .map((refRow) => rowToString(refRow, labelTemplate))
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
