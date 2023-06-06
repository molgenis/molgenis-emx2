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
  preventDefault: {
    type: Boolean,
    default: false,
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

function onPrevClick($event) {
  if (props.preventDefault) {
    $event.preventDefault();
  }
  if (props.currentPage > 1) {
    emit("update", props.currentPage - 1);
  }
}

function onNextClick($event) {
  if (props.preventDefault) {
    $event.preventDefault();
  }
  if (props.currentPage < props.totalPages) {
    emit("update", props.currentPage + 1);
  }
}

function changeCurrentPage(event) {
  const newPage = parseInt(event.target.value);
  const clampedPage = Math.min(Math.max(newPage, 1), props.totalPages);
  if (isNaN(clampedPage)) {
    emit("update", 1);
  } else {
    emit("update", clampedPage);
  }
}
</script>

<template>
  <nav
    class="pt-12.5 flex items-center justify-center font-display text-heading-xl -mx-2.5"
  >
    <a
      :href="currentPage > 1 ? '#' : undefined"
      role="button"
      @click="onPrevClick"
      class="flex justify-center transition-colors border border-pagination rounded-pagination bg-pagination text-pagination h-15 w-15"
      :class="{
        'hover:bg-pagination-hover hover:text-pagination-hover':
          currentPage > 1,
      }"
    >
      <BaseIcon name="caret-left" :width="24" />
    </a>
    <div class="px-4 tracking-widest sm:px-5" :class="textClasses">Page</div>
    <input
      class="sm:px-12 px-7.5 w-32 text-center border rounded-pagination text-pagination-input h-15 flex items-center tracking-widest bg-white"
      :value="currentPage"
      @change="changeCurrentPage"
      :class="borderClasses"
    />
    <div
      class="px-4 tracking-widest sm:px-5 whitespace-nowrap"
      :class="textClasses"
    >
      OF {{ totalPages }}
    </div>
    <a
      :href="currentPage < totalPages ? '#' : undefined"
      role="button"
      @click="onNextClick"
      class="flex justify-center transition-colors border border-pagination rounded-pagination bg-pagination text-pagination h-15 w-15"
      :class="{
        'hover:bg-pagination-hover hover:text-pagination-hover':
          currentPage < totalPages,
      }"
    >
      <BaseIcon name="caret-right" :width="24" />
    </a>
  </nav>
</template>
