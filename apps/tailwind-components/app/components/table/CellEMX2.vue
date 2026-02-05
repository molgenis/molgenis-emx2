<template>
  <td class="p-2.5 border-b border-gray-200 min-h-8 text-left truncate">
    <slot name="row-actions"></slot>
    <slot>
      <ValueEMX2
        v-if="metadata && data !== undefined && data !== null"
        :metadata="metadata"
        :data="data"
        @valueClick="$emit('cellClicked', $event)"
      />
    </slot>
  </td>
</template>

<script setup lang="ts">
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { RefPayload } from "../../../types/types";
import ValueEMX2 from "../value/EMX2.vue";

defineProps<{
  metadata?: IColumn;
  data?: any;
}>();

defineEmits<{
  (e: "cellClicked", payload: RefPayload): void;
}>();
</script>
