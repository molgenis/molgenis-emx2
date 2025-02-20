<script setup lang="ts">
const label = "Search for patients";
</script>

<template>
  <div
    class="grid grid-cols-2 gap-6 my-5 [&>div]:bg-white [&>div]:p-4 [&_h3]:font-semibold [&_h3]:my-2"
  >
    <div>
      <h3>Input Label: default use</h3>
      <InputLabel for="input-string-default">
        {{ label }}
      </InputLabel>
    </div>
    <div>
      <h3>Input Label: disabled</h3>
      <InputLabel for="input-string-disabled" :disabled="true">
        {{ label }}
      </InputLabel>
    </div>
    <div>
      <h3>Input Text: as a required field</h3>
      <InputLabel for="input-string-required" :required="true">
        {{ label }}
      </InputLabel>
    </div>
    <div>
      <h3>Input Label: positive validation result</h3>
      <InputLabel for="input-string-valid" :valid="true">
        {{ label }}
      </InputLabel>
    </div>
    <div>
      <h3>Input Label: negative validation result</h3>
      <InputLabel for="input-string-error" :has-error="true">
        {{ label }}
      </InputLabel>
    </div>
    <div>
      <h3>Input Label: visually hidden label</h3>
      <InputLabel for="input-string-hidden-label" :hide-label="true">
        {{ label }}
      </InputLabel>
    </div>
  </div>
</template>
