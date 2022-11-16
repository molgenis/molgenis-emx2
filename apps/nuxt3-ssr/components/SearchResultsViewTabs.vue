<script setup>
import { computed } from "vue";
import BaseIcon from "./BaseIcon.vue";

const props = defineProps({
  buttonLeftLabel: {
    type: String,
  },
  buttonLeftName: {
    type: String,
  },
  buttonLeftIcon: {
    type: String,
  },
  buttonRightLabel: {
    type: String,
  },
  buttonRightName: {
    type: String,
  },
  buttonRightIcon: {
    type: String,
  },
  activeName: {
    type: String,
  },
});

const ACTIVE = "bg-white text-blue-500";
const IDLE = "bg-blue-800 text-white";

const buttonClassesLeft = computed(() => {
  return props.activeName === props.buttonLeftName ? ACTIVE : IDLE;
});

const buttonClassesRight = computed(() => {
  return props.activeName === props.buttonRightName ? ACTIVE : IDLE;
});
</script>

<template>
  <div class="flex self-center">
    <div>
      <button :class="buttonClassesLeft"
        class="h-[50px] pl-7 pr-5 font-display text-heading-xl uppercase flex items-center tracking-widest rounded-l-full">
        <BaseIcon :name="buttonLeftIcon" class="mr-3" />
        {{ buttonLeftLabel }}
      </button>
    </div>
    <div>
      <button :class="buttonClassesRight"
        class="h-[50px] pl-5 pr-7 font-display text-heading-xl uppercase flex items-center tracking-widest rounded-r-full">
        <BaseIcon :name="buttonRightIcon" class="mr-3" />
        {{ buttonRightLabel }}
      </button>
    </div>
  </div>
</template>
