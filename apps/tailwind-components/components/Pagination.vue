<script setup lang="ts">
import { onMounted, useId } from "vue";

const pageInputId = useId();

const props = withDefaults(
  defineProps<{
    currentPage: number;
    totalPages: number;
    preventDefault?: boolean;
    inverted?: boolean;
  }>(),
  {
    preventDefault: false,
    inverted: false,
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
    class="pt-12.5 font-display text-heading-xl -mx-2.5"
    :aria-labelledby="`${pageInputId}Label`"
  >
    <span :id="`${pageInputId}Label`" class="sr-only">
      pagination navigation
    </span>
    <ul class="flex items-center justify-center list-none">
      <li>
        <a
          href="#"
          @click.prevent="onPrevClick"
          class="flex justify-center border border-pagination rounded-theme bg-pagination text-pagination-button h-15 w-15"
          :class="{
            'cursor-pointer hover:bg-pagination-hover hover:text-pagination-hover focus:bg-pagination-hover focus:text-pagination-hover':
              currentPage > 1,
          }"
        >
          <span class="sr-only">Go to page {{ currentPage - 1 }}</span>
          <BaseIcon name="caret-left" :width="24" />
        </a>
      </li>
      <li class="flex justify-center items-center">
        <div class="px-4 tracking-widest sm:px-5">
          <label :for="pageInputId" class="sr-only">go to specific page</label>
          <span
            class="text-pagination"
            :class="{
              'text-pagination-inverted': inverted,
            }"
          >
            Page
          </span>
        </div>
        <input
          :id="pageInputId"
          class="sm:px-12 px-7.5 w-32 text-center border border-input rounded-theme bg-input text-pagination-input h-15 flex items-center tracking-widest"
          :value="currentPage"
          @change="changeCurrentPage"
        />
        <div class="px-4 tracking-widest sm:px-5 whitespace-nowrap">
          <span
            class="text-pagination"
            :class="{
              'text-pagination-inverted': inverted,
            }"
          >
            OF {{ totalPages }}
          </span>
        </div>
      </li>
      <li>
        <a
          href="#"
          @click.prevent="onNextClick"
          class="flex justify-center border border-pagination rounded-theme bg-pagination text-pagination-button h-15 w-15"
          :class="{
            'cursor-pointer hover:bg-pagination-hover hover:text-pagination-hover focus:bg-pagination-hover focus:text-pagination-hover':
              currentPage < totalPages,
          }"
        >
          <span class="sr-only">Go to page {{ currentPage + 1 }}</span>
          <BaseIcon name="caret-right" :width="24" />
        </a>
      </li>
    </ul>
  </nav>
</template>
