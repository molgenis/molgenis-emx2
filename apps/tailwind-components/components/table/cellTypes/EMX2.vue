<script setup lang="ts">
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { RefPayload } from "../../../types/types";
import ColumnData from "./ColumnData.vue";
defineProps<{
  metaData: IColumn;
  data: any;
}>();

defineEmits<{
  (e: "cellClicked", payload: RefPayload): void;
}>();
</script>
<template>
  <td
    class="h-4 px-2.5 border-b border-gray-200 first:pl-0 last:pr-0 sm:first:pl-2.5 sm:last:pr-2.5 overflow-ellipsis whitespace-nowrap overflow-hidden"
  >
    <div class="flex justify-between">
      <span
        class="py-2.5 text-body-base flex items-center shrink overflow-ellipsis whitespace-nowrap overflow-hidden flex-0"
      >
        <ColumnData
          :data="data"
          :meta-data="metaData"
          @cellClicked="$emit('cellClicked', $event)"
        />
      </span>
      <slot />
    </div>
  </td>
</template>
