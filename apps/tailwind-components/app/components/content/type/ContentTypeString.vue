<script setup lang="ts">
import type { ISectionColumn } from "../../../../types/types";

defineProps<{
  field: ISectionColumn;
}>();
</script>

<template>
  <p>{{ field.value }}</p>
</template>
