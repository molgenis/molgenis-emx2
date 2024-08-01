<template>
  <ReadMore :text="data" />
</template>

<script setup lang="ts">
import ReadMore from "../../layout/ReadMore.vue";
const { data } = defineProps<{
  data: string | Record<string, any> | number | boolean;
}>();
</script>
