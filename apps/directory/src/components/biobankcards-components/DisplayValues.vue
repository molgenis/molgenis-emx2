<template>
  <div>
    <div v-for="item in meta" :key="item.id">
      <div v-if="typeof item.column === 'string'">
        {{ item.label }} {{ values[item.column] }}
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  meta: Object,
  values: Object,
});
</script>
