<script setup lang="ts">
const label = "Search for patients";
</script>

<template>
  <h2>Label component</h2>  
  <p>The <code>InputLabel</code>component is a wrapper around the <code>label</code> element. There are a few useful options when used in forms and for form validation. These are displayed below. There is also a <code>hideLabel</code> option that visually hides the label. Every input should also have a label, but sometimes you may wish to hide it. In this case, the label is still rendered in the document and accessible, but visually hidden using css classes using the <code>sr-only</code> class. Label would appear below.</p>
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
      <InputLabel for="input-string-valid" :valid="true" >
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
      <InputLabel for="input-string-hidden-label" :hide-label="true" >
        {{ label }}
      </InputLabel>
    </div>
  </div>
</template>
