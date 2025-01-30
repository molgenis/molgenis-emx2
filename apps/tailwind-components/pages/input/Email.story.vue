<template>
  <h2>Email component</h2>
  <p>
    The <code>Email Component</code> enables you to use the String component
    with the added validation of the input being a email.
  </p>

  <div
    class="grid grid-cols-2 gap-6 my-5 [&>div]:bg-white [&>div]:p-4 [&_h3]:font-semibold [&_h3]:my-2"
  >
    <div>
      <h3>Input email: (model value: {{ demoValue }})</h3>
      <InputLabel for="input-email">
        {{ label }}
      </InputLabel>
      <InputEmail
        id="input-email"
        v-model="demoValue"
        ref="input-email"
        :placeholder="placeholder"
        :hasError="error.length > 0"
        @error="handleError"
      />
      <div>Error: {{ error[0]?.message }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { IFieldError } from "../../../metadata-utils/src/types";

const label = "Input an email address";
const placeholder = "example@molgenis.net";
const demoValue = ref("");
const error = ref<IFieldError[]>([]);

function handleError(errors: IFieldError[]) {
  error.value = errors;
}
</script>
