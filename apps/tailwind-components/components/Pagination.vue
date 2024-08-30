<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    currentPage: number;
    totalPages: number;
    type?: "gray" | "white";
    preventDefault?: boolean;
  }>(),
  {
    type: "gray",
    preventDefault: false,
  }
);
const emit = defineEmits(["update"]);

onMounted(() => {
  if (window) {
    window.addEventListener("popstate", () => {
      // react to external navigation ( e.g. back button in browser)
      window.location.reload();
    });
  }
});

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

function onPrevClick($event: Event) {
  if (props.preventDefault) {
    $event.preventDefault();
  }
  if (props.currentPage > 1) {
    emit("update", props.currentPage - 1);
  }
}

function onNextClick($event: Event) {
  if (props.preventDefault) {
    $event.preventDefault();
  }
  if (props.currentPage < props.totalPages) {
    emit("update", props.currentPage + 1);
  }
}

function changeCurrentPage(event: Event) {
  const newPage = parseInt((event.target as HTMLInputElement)?.value);
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
    role="navigation"
    aria-label="Pagination navigation"
    class="pt-12.5 flex items-center justify-center font-display text-heading-xl -mx-2.5"
  >
    <a
      role="button"
      :aria-label="'Goto page ' + (currentPage - 1)"
      @click.prevent="onPrevClick"
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
      role="button"
      :aria-label="'Goto page ' + (currentPage + 1)"
      @click.prevent="onNextClick"
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
