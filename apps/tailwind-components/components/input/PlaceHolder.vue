<template>
  <div class="border border-dotted p-2">
    <pre>place holder for field type {{ type }}</pre>
  </div>
</template>

<script setup lang="ts">
import type { CellValueType } from "../../../metadata-utils/src/types";
defineProps<{
  type: CellValueType;
}>();
defineExpose({ validate });

function validate() {
  // do nothing, its just a place holder for unkown types
}
</script>
