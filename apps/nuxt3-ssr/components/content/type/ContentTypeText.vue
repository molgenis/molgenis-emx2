<script setup lang="ts">
import { ISectionField } from "interfaces/types";
defineProps<{
  field: ISectionField;
}>();
</script>

<template>
  <p>{{ field.value }}</p>
</template>
