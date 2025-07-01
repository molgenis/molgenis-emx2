<script setup lang="ts">
import { computed } from "vue";
import type { IRefColumn, IRow } from "../../../metadata-utils/src/types";
import { rowToString } from "../../utils/rowToString";
import type { RefPayload } from "../../types/types";

const props = defineProps<{
  metadata: IRefColumn;
  data: IRow[];
}>();

const emit = defineEmits<{
  (e: "refBackCellClicked", payload: RefPayload): void;
}>();

const handleRefBackCellClicked = () => {
  emit("refBackCellClicked", {
    metadata: props.metadata,
    data: props.data[0], // todo think about how to handle multiple rows, separate for each row or joined as one?
  });
};

const refBackColumnLabel = computed(() => {
  // we know that in case of refback, either refLabel or refLabelDefault is defined, although this can not easily be expressed in the typescript
  const labelTemplate = (
    props.metadata.refLabel
      ? props.metadata.refLabel
      : props.metadata.refLabelDefault
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
