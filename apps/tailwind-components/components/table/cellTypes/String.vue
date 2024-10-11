<script setup lang="ts">
import type { IColumn } from "../../../../metadata-utils/src/types";
defineProps<{
  metaData: IColumn;
  data: string;
}>();

const divRef = useTemplateRef("overflow-ref");

const isOverFlow = computed(() => {
  return (divRef.value?.scrollWidth ?? 0) > (divRef.value?.offsetWidth ?? 0);
});

function onMoreClick() {
  alert("More clicked");
}
</script>

<template>
  <td
    class="py-2.5 px-2.5 border-b border-gray-200 first:pl-0 last:pr-0 sm:first:pl-2.5 sm:last:pr-2.5 h-4"
  >
    <div style="position: relative; padding-right: 40px">
      <div
        ref="overflow-ref"
        class="overflow-ellipsis whitespace-nowrap overflow-hidden"
      >
        {{ data }}
      </div>
      <button
        v-if="isOverFlow"
        @click="onMoreClick"
        style="position: absolute; right: 0px; top: 0px"
        class="text-blue-500 underline"
      >
        More
      </button>
    </div>
  </td>
</template>
