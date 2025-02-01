<script setup lang="ts">
const demoValue = ref("test");
</script>

<template>
    <InputTestContainer
        v-slot="{placeholder, valid, error, disabled}">
      <InputString
          id="input-string-default"
          v-model="demoValue"
          :placeholder="placeholder"
          :disabled="disabled"
          :error="error"
          :valid="valid"
      />
    </InputTestContainer>
</template>
