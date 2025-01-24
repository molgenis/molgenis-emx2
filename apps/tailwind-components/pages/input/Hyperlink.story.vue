<template>
  <h2>Hyperlink component</h2>
  <p>
    The <code>Hyperlink Component</code> enables you to use the String component
    with the added validation of the input being a hyperlink.
  </p>

  <div
    class="grid grid-cols-2 gap-6 my-5 [&>div]:bg-white [&>div]:p-4 [&_h3]:font-semibold [&_h3]:my-2"
  >
    <div>
      <h3>Input Hyperlink: (model value: {{ demoValue }})</h3>
      <InputLabel for="input-hyperlink">
        {{ label }}
      </InputLabel>
      <InputHyperlink
        id="input-hyperlink"
        v-model="demoValue"
        ref="input-hyperlink"
        :placeholder="placeholder"
        :hasError="error.length > 0"
        @error="handleError"
      />
      <div>Error: {{ error[0]?.message }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { InputHyperlink } from "#build/components";
import type { IFieldError } from "../../../metadata-utils/src/types";

const label = "Input a hyperlink";
const placeholder = "https://example.com";
const demoValue = ref("");
const error = ref<IFieldError[]>([]);

function handleError(errors: IFieldError[]) {
  error.value = errors;
}
</script>
