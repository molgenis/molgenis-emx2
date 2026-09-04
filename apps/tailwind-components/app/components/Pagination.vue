<script setup lang="ts">
import { onMounted, ref, useId, computed } from "vue";
import Button from "./Button.vue";
import InputSelect from "./input/Select.vue";
import constants from "../utils/constants";

const pageInputId = useId();

const props = withDefaults(
  defineProps<{
    currentPage: number;
    totalPages: number;
    pageSize?: number;
    preventDefault?: boolean;
    inverted?: boolean;
    jumpToEdge?: boolean;
    showPageSelector?: boolean;
    showPageSizeSelector?: boolean;
  }>(),
  {
    preventDefault: false,
    inverted: false,
    jumpToEdge: false,
    showPageSelector: true,
    showPageSizeSelector: false,
    pageSize: constants.PAGE_SIZE_DEFAULT,
  }
);

const isAtStart = computed(() => props.currentPage <= 1);
const isAtEnd = computed(() => props.currentPage >= props.totalPages);

// A page number outside [1, totalPages] does not exist; a screen reader
// must never be told to go there.
const prevPageLabel = computed(() => Math.max(props.currentPage - 1, 1));
const nextPageLabel = computed(() =>
  Math.min(props.currentPage + 1, props.totalPages)
);

const emit = defineEmits(["update", "update:pageSize"]);

onMounted(() => {
  if (window) {
    window.addEventListener("popstate", () => {
      // react to external navigation ( e.g. back button in browser)
      window.location.reload();
    });
  }
});

function onFirstClick($event: Event) {
  if (props.preventDefault) {
    $event.preventDefault();
  }
  if (props.currentPage > 1) {
    emit("update", 1);
  }
}

function onLastClick($event: Event) {
  if (props.preventDefault) {
    $event.preventDefault();
  }
  if (props.currentPage < props.totalPages) {
    emit("update", props.totalPages);
  }
}

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
    <ul class="flex items-center justify-center list-none gap-2.5">
      <li v-if="showPageSelector && jumpToEdge">
        <Button
          icon-only
          type="tertiary"
          size="small"
          icon="double-arrow-left"
          label="Go to page first"
          :disabled="isAtStart"
          @click="onFirstClick"
        />
      </li>
      <li>
        <Button
          icon-only
          type="tertiary"
          size="small"
          icon="caret-left"
          :label="`Go to page ${prevPageLabel}`"
          :disabled="isAtStart"
          @click="onPrevClick"
        />
      </li>
      <li v-if="$slots.info" class="flex justify-center items-center">
        <div class="px-4 tracking-widest sm:px-5">
          <slot name="info" />
        </div>
      </li>
      <li v-if="showPageSelector" class="flex justify-center items-center">
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
          class="sm:px-12 px-7.5 w-32 text-center border border-input rounded-alt bg-input text-pagination-input hover:text-pagination-hover hover:border-pagination-hover hover:bg-pagination-hover h-15 flex items-center tracking-widest"
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
        <Button
          icon-only
          type="tertiary"
          size="small"
          icon="caret-right"
          :label="`Go to page ${nextPageLabel}`"
          :disabled="isAtEnd"
          @click="onNextClick"
        />
      </li>
      <li v-if="showPageSelector && jumpToEdge">
        <Button
          icon-only
          type="tertiary"
          size="small"
          icon="double-arrow-right"
          label="Go to last page"
          :disabled="isAtEnd"
          @click="onLastClick"
        />
      </li>

      <li class="flex justify-center items-center" v-if="showPageSizeSelector">
        <div class="px-4 tracking-widest sm:px-5 whitespace-nowrap">
          <span
            class="text-pagination"
            :class="{
              'text-pagination-inverted': inverted,
            }"
          >
            {{ showPageSelector ? "with size" : "Results per page" }}
          </span>
        </div>
      </li>

      <li class="flex justify-center items-center" v-if="showPageSizeSelector">
        <div class="tracking-widest whitespace-nowrap w-32">
          <span
            class="text-pagination"
            :class="{
              'text-pagination-inverted': inverted,
            }"
          >
            <InputSelect
              id="page-size-select"
              :options="constants.PAGE_SIZE_OPTIONS"
              @update:modelValue="(value) => emit('update:pageSize', value)"
              :modelValue="pageSize"
              class="!p-0 text-center border border-input rounded-alt !bg-input text-pagination-input !hover:text-pagination-hover hover:outline-pagination-hover hover:bg-pagination-hover h-15 flex items-center tracking-widest"
              :required="true"
            />
          </span>
        </div>
      </li>
    </ul>
  </nav>
</template>
