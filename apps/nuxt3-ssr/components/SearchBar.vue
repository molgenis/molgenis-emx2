<script setup>
import { computed } from "vue";

const props = defineProps({
  borderRadius: {
    type: String,
    default: "full",
    enum: ["full", "rounded", "none"],
  },
});

defineEmits(["submitSearch"]);

function submitSearch(event) {
  alert(`Zoeken naar ${event.target.elements[0].value}`);
}

const INPUT = {
  full: "rounded-full",
  rounded: "rounded",
  "rounded-none": "",
};

const BUTTON = {
  full: "rounded-full",
  rounded: "rounded",
  "rounded-none": "",
};

const inputClasses = computed(() => {
  return INPUT[props.borderRadius];
});

const buttonClasses = computed(() => {
  return BUTTON[props.borderRadius];
});
</script>

<template>
  <form class="relative" @submit.prevent="submitSearch">
    <input
      type="search"
      class="
        w-full
        pr-32
        font-sans
        text-gray-900
        bg-white
        outline-none
        h-15
        ring-red-500
        pl-7
        border-white
        focus:border-white
        shadow-primary
        focus:bg-gray-100 focus:shadow-2xl
        hover:shadow-2xl
      "
      :class="inputClasses"
      placeholder="Search variable. cohort etc"
    />
    <button
      class="
        absolute
        top-0
        right-0
        flex
        items-center
        pl-8
        pr-6
        tracking-wider
        text-blue-500
        transition-colors
        bg-blue-50
        h-15
        font-display
        text-heading-2xl
        hover:text-blue-800
      "
      :class="buttonClasses"
    >
      Search
    </button>
  </form>
</template>
