<script setup>
import { computed, ref } from "vue";

const props = defineProps({
  addFade: {
    type: Boolean,
  },
  addScrollButton: {
    type: Boolean,
  },
});

let scrollAtStart = ref(true);
let scrollAtEnd = ref(false);
let scrollElement = ref(null);

function scroll(event) {
  scrollAtStart.value = event.target.scrollLeft === 0;
  scrollAtEnd.value =
    event.target.scrollLeft + event.target.offsetWidth ===
    event.target.scrollWidth;
}

function scrollLeft(event) {
  scrollElement.value.scrollLeft -= 200;
}

function scrollRight(event) {
  scrollElement.value.scrollLeft += 200;
}
</script>

<template>
  <div class="relative">
    <div
      :class="addFade && scrollAtStart ? 'opacity-0' : 'opacity-100'"
      class="flex items-center justify-items-start absolute z-10 left-0 inset-y-0 w-10 bg-gradient-to-r from-white to-transparent pointer-events-none transition-opacity"
    >
      <div
        class="relative right-6 flex items-center justify-center pointer-events-auto bg-white hover:bg-gray-100 rounded-full border w-10 h-10"
        @click="scrollLeft"
      >
        <BaseIcon name="caret-left" :width="26" />
      </div>
    </div>

    <div
      :class="addFade && scrollAtEnd ? 'opacity-0' : 'opacity-100'"
      class="flex items-center justify-items-end absolute z-10 right-0 inset-y-0 w-10 bg-gradient-to-r from-transparent to-white pointer-events-none transition-opacity"
    >
      <div
        class="relative left-6 flex items-center justify-center pointer-events-auto bg-white hover:bg-gray-100 rounded-full border w-10 h-10"
        @click="scrollRight"
      >
        <BaseIcon name="caret-right" :width="26" />
      </div>
    </div>
    <div class="overflow-x-auto" @scroll.passive="scroll" ref="scrollElement">
      <slot></slot>
    </div>
  </div>
</template>
