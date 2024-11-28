<script setup lang="ts">
const label = "Search for patients";
const placeholder = "'A-001' or 'A-001,A-002'";
const demoValue = ref("");
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
      <h3>Input String: default use (model value: {{ demoValue }})</h3>
      <InputLabel for="input-string-default">
        {{ label }}
      </InputLabel>
      <InputString
        id="input-string-default"
        :placeholder="placeholder"
        v-model="demoValue"
      />
    </div>
    <div>
      <h3>Input String: disabled (model value: {{ demoValue }})</h3>
      <InputLabel for="input-string-disabled">
        {{ label }}
      </InputLabel>
      <InputString
        id="input-string-disabled"
        :placeholder="placeholder"
        :disabled="true"
        v-model="demoValue"
      />
    </div>
    <div>
      <h3>Input Text: as a required field (model value: {{ demoValue }})</h3>
      <InputLabel for="input-string-required" :required="true">
        {{ label }}
      </InputLabel>
      <InputString
        id="input-string-required"
        :label="label"
        :placeholder="placeholder"
        v-model="demoValue"
      />
    </div>
    <div>
      <h3>
        Input String: positive validation result (model value: {{ demoValue }})
      </h3>
      <InputLabel for="input-string-valid" :valid="true">
        {{ label }}
      </InputLabel>
      <InputString
        id="input-string-valid"
        :placeholder="placeholder"
        v-model="demoValue"
      />
    </div>
    <div>
      <h3>
        Input String: negative validation result (model value: {{ demoValue }})
      </h3>
      <InputLabel for="input-string-error" :has-error="true">
        {{ label }}
      </InputLabel>
      <InputString
        id="input-string-error"
        :placeholder="placeholder"
        v-model="demoValue"
      />
    </div>
    <div>
      <h3>
        Input String: visually hidden label (model value: {{ demoValue }})
      </h3>
      <InputLabel for="input-string-hidden-label" :hide-label="true">
        {{ label }}
      </InputLabel>
      <InputString
        id="input-string-hidden-label"
        :placeholder="placeholder"
        v-model="demoValue"
      />
    </div>
    <div>
      <h3>Input String: password (model value: {{ demoValue }})</h3>
      <InputLabel for="input-string-default">
        {{ label }}
      </InputLabel>
      <InputString
        id="input-string-default"
        :placeholder="placeholder"
        v-model="demoValue"
        type="password"
      />
    </div>
  </div>
</template>
