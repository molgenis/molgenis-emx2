<script setup lang="ts">
import type { ISectionField } from "../../../../types/types";
import ContentClamp from "../../ContentClamp.vue";

defineProps<{
  field: ISectionField;
}>();
</script>

<template>
  <ContentClamp :maxLines="8">{{ field.value }}</ContentClamp>
</template>
