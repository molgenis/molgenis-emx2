<template>
  <div class="border border-dotted p-2">
    <pre>place holder for field type {{ type }}</pre>
  </div>
</template>

<script setup lang="ts">
import type { CellValueType } from "../../../metadata-utils/src/types";
defineProps<{
  type: string;
}>();
</script>
