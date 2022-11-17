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
  gray: "text-gray-400",
  white: "text-white",
};

const BORDER_STYLE_MAPPING = {
  gray: "shadow-primary",
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
  <nav
    class="
      pt-12.5
      flex
      items-center
      justify-center
      font-display
      text-heading-xl
      -mx-2.5
    "
  >
    <a
      href="#"
      @click="$emit('update', currentPage - 1)"
      class="
        rounded-full
        bg-button-secondary
        hover:bg-button-secondary-hover
        text-white
        h-15
        w-15
        flex
        justify-center
      "
    >
      <BaseIcon name="caret-left" width="24" />
    </a>
    <div class="sm:px-5 px-4 tracking-widest" :class="textClasses">Page</div>
    <div
      class="
        sm:px-12
        px-7.5
        border
        rounded-full
        text-blue-800
        h-15
        flex
        items-center
        tracking-widest
        bg-white
      "
      :class="borderClasses"
    >
      {{ currentPage }}
    </div>
    <div
      class="sm:px-5 px-4 whitespace-nowrap tracking-widest"
      :class="textClasses"
    >
      OF {{ totalPages }}
    </div>
    <a
      href="#"
      @click="$emit('update', currentPage + 1)"
      class="
        rounded-full
        bg-button-secondary
        hover:bg-button-secondary-hover
        text-white
        h-15
        w-15
        flex
        justify-center
      "
    >
      <BaseIcon name="caret-right" width="24" />
    </a>
  </nav>
</template>
