<template>
  <p>{{ field.value }}</p>
</template>

<script setup lang="ts">
import type { ISectionField } from "~/interfaces/types";
defineProps<{
  field: ISectionField;
}>();
</script>
