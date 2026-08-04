<script setup lang="ts">
import type { ISections } from "../../../types/cms";

const props = withDefaults(defineProps<ISections>(), {
  enableFullScreenWidth: false,
  isEditable: false,
});
</script>

<template>
  <div class="w-full flex" >
    <div v-if="isEditable" class="flex items-center justify-center py-4 pr-8 w-0 relative">
      <div class="border border-dashed w-0 absolute top-3 bottom-3"></div>

      <span class="text-button-disabled rotate-90 inline-block pb-6">Section</span> 
    </div>
    <div :id="id" class="w-full py-8 justify-center items-center">
      <div
        class="m-auto"
        :class="{
          'w-pg-section': !enableFullScreenWidth,
          'w-full': enableFullScreenWidth,
        }"
      >
        <slot></slot>
      </div>
    </div>
  </div>
</template>
