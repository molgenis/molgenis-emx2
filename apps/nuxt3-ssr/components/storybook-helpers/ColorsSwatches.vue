<script>
import resolveConfig from "tailwindcss/resolveConfig";
import tailwindConfig from "../../../tailwind.config";
import ColorSwatch from "./ColorSwatch.vue";

const fullConfig = resolveConfig(tailwindConfig);

import { defineComponent } from "vue";
export default defineComponent({
  name: "ColorsSwatches",
  components: {
    ColorSwatch,
  },
  props: {},
  setup: () => {
    return { fullConfig: fullConfig };
  },
});
</script>

<template>
  <div v-for="(steps, colorName) in fullConfig.theme.colors" :key="colorName">
    <div
      v-if="
        colorName !== 'transparent' &&
        colorName !== 'current' &&
        colorName !== 'black' &&
        colorName !== 'white'
      "
      class="max-w-lg m-auto mb-10">
      <div class="mb-4 text-base font-bold uppercase">{{ colorName }}</div>
      <div class="flex">
        <div v-for="(hex, key) in steps" :key="key" class="justify-center">
          <div class="w-20 h-32">
            <ColorSwatch :color="hex" class="mr-4 -ml-2 w-28 h-28" />
            <div
              class="p-4 ml-2 font-mono text-xs text-center whitespace-nowrap">
              {{ colorName + "-" + key }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
