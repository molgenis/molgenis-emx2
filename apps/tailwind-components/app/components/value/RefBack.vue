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

const handleRefBackCellClicked = (index: number) => {
  if (props.data === null || props.data === undefined || !props.data[0]) {
    return;
  }

  emit("refBackCellClicked", {
    metadata: props.metadata,
    data: props.data[index],
  });
};

const refBackColumnLabels = computed(() => {
  if (!props.data) {
    return [];
  }
  const labelTemplate = (
    props.metadata.refLabel
      ? props.metadata.refLabel
      : props.metadata.refLabelDefault
  ) as string;
  return props.data.map((refRow) => columnValueToString(refRow, labelTemplate));
});
</script>

<template>
  <span
    v-for="(refBackColumnLabel, index) in refBackColumnLabels"
    class="underline hover:cursor-pointer text-link"
    @click="handleRefBackCellClicked(index)"
  >
    {{ refBackColumnLabel
    }}<span class="no-underline" v-if="index < refBackColumnLabels.length - 1"
      >,
    </span>
  </span>
</template>
