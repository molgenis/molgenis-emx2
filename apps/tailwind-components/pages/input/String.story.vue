<script setup lang="ts">
const demoValue = ref("test");
</script>

<template>
  <InputTestContainer
    show-placeholder
    show-state
    v-slot="{ placeholder, state }"
  >
    <InputString
      id="input-string-default"
      v-model="demoValue"
      :placeholder="placeholder"
      :state="state"
    />
  </InputTestContainer>
</template>
