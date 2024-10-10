<template>
  <span>
    <a :href="`/${lastUpdate.schema}/settings/#/changelog`">
      {{ timeStamp }} ({{ lastUpdate.tableName }})
    </a>
  </span>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{
  lastUpdate: {
    stamp: number;
    tableName: string;
    schema: string;
  };
}>();

const timeStamp = computed(() => {
  return new Date(props.lastUpdate.stamp).toLocaleDateString();
});
</script>
