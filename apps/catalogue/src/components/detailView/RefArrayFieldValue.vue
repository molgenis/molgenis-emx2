<template>
  <div>
    <span v-for="(ref, idx) in data" :key="idx">
      <RefFieldValue :data="ref" :metadata="metadata" />&nbsp;
    </span>
  </div>
</template>

<script setup lang="ts">
import RefFieldValue from "./RefFieldValue.vue";

const { data } = defineProps<{
  data: Record<string, any> | any[];
  metadata: Record<string, any>;
}>();
</script>
