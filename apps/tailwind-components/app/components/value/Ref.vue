<template>
  <span
    class="underline hover:cursor-pointer text-link"
    @click="handleRefCellClicked"
    >{{ refColumnLabel }}</span
  >
</template>

<script setup lang="ts">
import { computed } from "vue";
import type {
  columnValue,
  IRefColumn,
  IRow,
} from "../../../../metadata-utils/src/types";
import { columnValueToString } from "../../utils/columnValueToString";
import type { RefPayload } from "../../../types/types";
const props = defineProps<{
  metadata: IRefColumn;
  data?: columnValue | null;
}>();

const emit = defineEmits<{
  (e: "refCellClicked", data: RefPayload): void;
}>();

function handleRefCellClicked() {
  if (!props.data) {
    return;
  }
  emit("refCellClicked", {
    metadata: props.metadata,
    data: props.data,
  });
}

const refColumnLabel = computed(() => {
  if (!props.data) {
    return "";
  }
  const labelTemplate = (
    props.metadata.refLabel
      ? props.metadata.refLabel
      : props.metadata.refLabelDefault
  ) as string;
  return columnValueToString(props.data, labelTemplate);
});
</script>
