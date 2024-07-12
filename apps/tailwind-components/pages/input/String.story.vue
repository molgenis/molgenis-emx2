<script setup lang="ts">
const label = "Search for patients";
const placeholder = "'A-001' or 'A-001,A-002'";
</script>

<template>
  <h2>String component</h2>
  <p>
    The <code>String Component</code> enables you to create an input element
    that takes user input via the keyboard or other device. The available input
    types are not only text, but date, email, number, password, search, etc.
    However, checkbox and radio types are in a separate component.
  </p>
  <div
    class="grid grid-cols-2 gap-6 my-5 [&>div]:bg-white [&>div]:p-4 [&_h3]:font-semibold [&_h3]:my-2"
  >
    <div>
      <h3>Input String: default use</h3>
      <InputLabel for="input-string-default" :label="label" />
      <InputString id="input-string-default" :placeholder="placeholder" />
    </div>
    <div>
      <h3>Input String: disabled</h3>
      <InputLabel for="input-string-disabled" :label="label" />
      <InputString
        id="input-string-disabled"
        :placeholder="placeholder"
        :disabled="true"
      />
    </div>
    <div>
      <h3>Input Text: as a required field</h3>
      <InputLabel for="input-string-required" :label="label" />
      <InputString
        id="input-string-required"
        :label="label"
        :placeholder="placeholder"
      />
    </div>
    <div>
      <h3>Input String: positive validation result</h3>
      <InputLabel for="input-string-valid" :label="label" :valid="true" />
      <InputString id="input-string-valid" :placeholder="placeholder" />
    </div>
    <div>
      <h3>Input String: negative validation result</h3>
      <InputLabel for="input-string-error" :label="label" :has-error="true" />
      <InputString id="input-string-error" :placeholder="placeholder" />
    </div>
    <div>
      <h3>Input String: visually hidden label</h3>
      <InputLabel
        for="input-string-hidden-label"
        :label="label"
        :hide-label="true"
      />
      <InputString id="input-string-hidden-label" :placeholder="placeholder" />
    </div>
  </div>
</template>
