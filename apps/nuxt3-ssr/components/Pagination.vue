<!-- eslint-disable vue/multi-word-component-names -->
<script setup>
import { computed } from "vue";

const props = defineProps({
  currentPage: {
    type: Number,
    required: true,
  },
  totalPages: {
    type: Number,
    required: true,
  },
  type: {
    type: String,
    default: "gray",
    enum: ["gray", "white"],
  },
});
const emit = defineEmits(["update"]);

const TEXT_STYLE_MAPPING = {
  gray: "text-pagination-label-gray",
  white: "text-pagination-label-white",
};

const BORDER_STYLE_MAPPING = {
  gray: "shadow-pagination-gray",
  white: "",
};

const textClasses = computed(() => {
  return TEXT_STYLE_MAPPING[props.type];
});

const borderClasses = computed(() => {
  return BORDER_STYLE_MAPPING[props.type];
});
</script>

<template>
  <nav class="pt-12.5 flex items-center justify-center font-display text-heading-xl -mx-2.5">
    <a href="#" @click="$emit('update', currentPage - 1)"
      class="flex justify-center transition-colors border border-pagination rounded-pagination bg-pagination hover:bg-pagination-hover text-pagination hover:text-pagination-hover h-15 w-15">
      <BaseIcon name="caret-left" :width="24" />
    </a>
    <div class="px-4 tracking-widest sm:px-5" :class="textClasses">Page</div>
    <input
      class="sm:px-12 px-7.5 w-32 text-center border rounded-pagination text-pagination-input h-15 flex items-center tracking-widest bg-white"
      :value="currentPage" :class="borderClasses" />
    <div class="px-4 tracking-widest sm:px-5 whitespace-nowrap" :class="textClasses">
      OF {{ totalPages }}
    </div>
    <a href="#" @click="$emit('update', currentPage + 1)"
      class="flex justify-center transition-colors border border-pagination rounded-pagination bg-pagination hover:bg-pagination-hover text-pagination hover:text-pagination-hover h-15 w-15">
      <BaseIcon name="caret-right" :width="24" />
    </a>
  </nav>
</template>
