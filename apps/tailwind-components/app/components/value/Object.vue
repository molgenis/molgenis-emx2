<script setup lang="ts">
import { flattenObject } from "../../utils/flattenObject";
import { computed } from "vue";
import type { IColumn, IRow } from "../../../../metadata-utils/src/types";
import type { ColumnPayload } from "../../../types/types";

const props = defineProps<{
  metadata: IColumn;
  data?: IRow | null;
}>();

const emit = defineEmits<{
  (e: "refCellClicked", data: ColumnPayload): void;
}>();

const hasTemplate = computed(
  () => !!props.metadata.refLabel || !!props.metadata.refLabelDefault
);

const asTemplate = computed(() => {
  if (!props.data) {
    return "";
  }
  const ids = Object.keys(props.data);
  const vals = Object.values(props.data);
  const refLabel = props.metadata.refLabel
    ? props.metadata.refLabel
    : props.metadata.refLabelDefault;
  try {
    return new Function(...ids, "return `" + refLabel + "`;")(...vals);
  } catch (err: any) {
    const idsAsString = JSON.stringify(ids);
    const valsString = JSON.stringify(vals);
    return `${err.message} we got keys: ${idsAsString} vals: ${valsString} and template: ${refLabel}`;
  }
});

const asDotSeparatedString = computed(() => {
  if (!props.data) {
    return "";
  }
  let result = "";
  Object.keys(props.data).forEach((key) => {
    const value = props.data ? props.data[key] : null;
    if (value === null) {
      //nothing
    } else if (typeof value === "object") {
      result += flattenObject(value);
    } else {
      result += "." + value;
    }
  });
  return result.replace(/^\./, "");
});

function handleRefCellClicked() {
  if (!props.data) {
    return;
  }
  emit("refCellClicked", {
    metadata: props.metadata,
    data: props.data,
  });
}
</script>

<template>
  <span class="inline">
    <span v-if="hasTemplate">
      {{ asTemplate }}
    </span>
    <span v-else>
      {{ asDotSeparatedString }}
    </span>
  </span>
</template>
