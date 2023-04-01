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

const emit = defineEmits(["update:activeName"]);

const IDLE =
  "bg-white text-search-results-view-tabs hover:text-search-results-view-tabs-hover";
const ACTIVE = "bg-search-results-view-tabs text-white hover:cursor-default";

const buttonClassesLeft = computed(() => {
  return props.activeName === props.buttonLeftName ? ACTIVE : IDLE;
});

const buttonClassesRight = computed(() => {
  return props.activeName === props.buttonRightName ? ACTIVE : IDLE;
});

function setLeft() {
  emit("update:activeName", props.buttonLeftName);
}

function setRight() {
  emit("update:activeName", props.buttonRightName);
}
</script>

<template>
  <div class="flex self-center">
    <button
      :class="buttonClassesLeft"
      @click="setLeft"
      class="flex items-center pr-5 tracking-widest uppercase rounded-l-full h-50px pl-7 font-display text-heading-xl"
    >
      <BaseIcon :name="buttonLeftIcon" class="mr-3" />
      {{ buttonLeftLabel }}
    </button>

    <button
      :class="buttonClassesRight"
      @click="setRight"
      class="flex items-center pl-5 tracking-widest uppercase rounded-r-full h-50px pr-7 font-display text-heading-xl"
    >
      <BaseIcon :name="buttonRightIcon" class="mr-3" />
      {{ buttonRightLabel }}
    </button>
  </div>
</template>
