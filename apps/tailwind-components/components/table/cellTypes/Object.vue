<script setup lang="ts">
import type { IColumn } from "../../../../metadata-utils/src/types";

const props = defineProps<{
  metaData: IColumn;
  data: Record<string, any>;
}>();

const hasTemplate = computed(
  () => !!props.metaData.refLabel || !!props.metaData.refLabelDefault
);

const asTemplate = computed(() => {
  const ids = Object.keys(props.data);
  const vals = Object.values(props.data);
  const refLabel = props.metaData.refLabel
    ? props.metaData.refLabel
    : props.metaData.refLabelDefault;
  try {
    return new Function(...ids, "return `" + refLabel + "`;")(...vals);
  } catch (err: any) {
    const idsAsString = JSON.stringify(ids);
    const valsString = JSON.stringify(vals);
    return `${err.message} we got keys: ${idsAsString} vals: ${valsString} and template: ${refLabel}`;
  }
});

const asDotSeparatedString = computed(() => {
  let result = "";
  Object.keys(props.data).forEach((key) => {
    if (props.data[key] === null) {
      //nothing
    } else if (typeof props.data[key] === "object") {
      result += flattenObject(props.data[key]);
    } else {
      result += "." + props.data[key];
    }
  });
  return result.replace(/^\./, "");
});
</script>

<template>
  <td
    class="py-2.5 px-2.5 border-b border-gray-200 first:pl-0 last:pr-0 sm:first:pl-2.5 sm:last:pr-2.5 h-4 overflow-ellipsis whitespace-nowrap overflow-hidden"
  >
    <span v-if="hasTemplate">
      {{ asTemplate }}
    </span>
    <span v-else>
      {{ asDotSeparatedString }}
    </span>
  </td>
</template>
