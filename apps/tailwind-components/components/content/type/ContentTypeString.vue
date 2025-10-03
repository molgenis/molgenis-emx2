<script setup lang="ts">
import type { ISectionField } from "../../../types/types";

defineProps<{
  field: ISectionField;
}>();
</script>

<template>
  <p>{{ field.value }}</p>
</template>
