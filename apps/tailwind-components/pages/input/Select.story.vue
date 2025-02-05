<script setup lang="ts">
const fieldValue = ref("");
const options = ["option 1", "option 2", "option 3", "option 4"];
</script>

<template>
  <InputTestContainer
    show-required
    show-placeholder
    show-state
    v-slot="{ placeholder, state, required }"
  >
    <InputSelect
      id="story-input-1"
      :options="options"
      v-model="fieldValue"
      :required="required"
      :state="state"
      :placeholder="placeholder"
    />
  </InputTestContainer>
</template>
