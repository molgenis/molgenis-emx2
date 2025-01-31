<template>
  <span>
    <a :href="`/${lastUpdate.schemaName}/settings/#/changelog`">
      {{ timeStamp }} ({{ lastUpdate.tableName }})
    </a>
  </span>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{
  lastUpdate: {
    stamp: string;
    tableName: string;
    schemaName: string;
    userId: string;
    operation: string;
  };
}>();

const timeStamp = computed(() => {
  return new Date(props.lastUpdate.stamp).toLocaleDateString();
});
</script>
