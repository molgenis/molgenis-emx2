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
      <h3>Input Hyperlink: default use (model value: {{ demoValue }})</h3>
      <InputLabel for="input-hyperlink">
        {{ label }}
      </InputLabel>
      <InputHyperlink
        id="input-hyperlink"
        :placeholder="placeholder"
        v-model="demoValue"
        ref="inputHyperlink"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { InputHyperlink } from "#build/components";

const inputHyperlink = ref<InstanceType<typeof InputHyperlink>>();

const label = "Input a hyperlink";
const placeholder = "https://example.com";
const demoValue = ref("");
</script>
