<template>
  <InputTestContainer
    show-placeholder
    show-state
    v-slot="{ placeholder, valid, invalid, disabled }"
  >
    <label for="input-date" class="text-title"> Enter a date </label>
    <InputDateTime
      id="input-date-time"
      :placeholder="placeholder"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
    />
  </InputTestContainer>
</template>
