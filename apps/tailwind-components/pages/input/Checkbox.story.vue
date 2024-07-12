<script setup lang="ts">
const selections = ref<string>('');
</script>

<template>
  <h2>Radio component</h2>
  <div class="flex">
    <div>
      <InputCheckbox id="radio-default" />
      <InputLabel for="radio-default" label="Check this box" @change="selections = event" />
      <p>Is the box ticked? Answer: {{ selections }}</p>
    </div>
  </div>
</template>