<!-- eslint-disable vue/multi-word-component-names -->
<script>
import resolveConfig from "tailwindcss/resolveConfig";
import tailwindConfig from "./../../../tailwind.config";

const fullConfig = resolveConfig(tailwindConfig);

export default {
  setup: () => {
    return { fullConfig: fullConfig };
  },
};
</script>

<template>
  <div class="grid gap-4">
    <div class="grid grid-cols-3 gap-6 p-4 bg-black bg-opacity-5">
      <h2>Font Family</h2>
      <div class="grid col-span-2 gap-4">
        <div
          v-for="(styles, key) in fullConfig.theme.fontFamily"
          :key="key"
          class=""
        >
          <p class="text-heading-3xl" :class="'font-' + key">{{ key }}</p>
          <span class="font-mono text-gray-100"
            >Font-size: {{ styles[0] }}, Line-height: {{ styles[1] }}</span
          >
        </div>
      </div>
    </div>
    <div class="grid grid-cols-3 gap-6 p-4 bg-black bg-opacity-5">
      <h2>Font Sizes</h2>
      <div class="grid col-span-2 gap-4">
        <div
          v-for="(styles, key) in fullConfig.theme.fontSize"
          :key="key"
          class=""
        >
          <p class="font-sans" :class="'text-' + key">{{ key }}</p>
          <span class="font-mono text-gray-100"
            >Font-size: {{ styles[0] }}, Line-height: {{ styles[1] }}</span
          >
        </div>
      </div>
    </div>
  </div>
</template>
