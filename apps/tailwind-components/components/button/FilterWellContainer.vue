<script lang="ts" setup>
import { ref, useTemplateRef, watch, onMounted, onBeforeUnmount } from "vue";

const buttonContainer = useTemplateRef<HTMLDivElement>("filterButtonContainer");
const widthObserver = ref();
const showButtons = ref<boolean>(false);
const hasFilterWellButtons = ref<boolean>(false);
const filterButtons = ref<HTMLCollection>();
const focusCounter = ref<number>(0);
const minFocusCounter = ref<number>(0);
const maxFocusCounter = ref<number>(0);

const scrollConfig = {
  behavior: "smooth",
  block: "nearest",
  inline: "center",
} as ScrollOptions;

const observerConfig = {
  childList: true,
  subtree: true,
  attributes: false,
  characterData: false,
};

const emit = defineEmits(["clear"]);

function observeScrollWidth() {
  if (
    buttonContainer.value &&
    buttonContainer.value.scrollWidth > buttonContainer.value.offsetWidth
  ) {
    showButtons.value = true;
  } else {
    showButtons.value = false;
  }

  updateFilterButtons();
}

function updateFilterButtons() {
  filterButtons.value = buttonContainer.value?.children as HTMLCollection;
  maxFocusCounter.value = filterButtons.value.length;

  if (buttonContainer.value) {
    hasFilterWellButtons.value = buttonContainer.value.children.length > 1;
  }
}

function addFilterButtonClass(button: HTMLButtonElement) {
  button.classList.add("bg-button-filter-hover");
  button.classList.add("border-button-filter-hover");
}

function removeFilterButtonClass(button: HTMLButtonElement) {
  button.classList.remove("bg-button-filter-hover");
  button.classList.remove("border-button-filter-hover");
}

function showPreviousItem() {
  if (filterButtons.value) {
    // const currentButton = filterButtons.value[(focusCounter.value as number)];
    // removeFilterButtonClass(currentButton as HTMLButtonElement);

    focusCounter.value -= 1;
    if (focusCounter.value <= minFocusCounter.value) {
      focusCounter.value = minFocusCounter.value;
    }

    const previousButton = filterButtons.value[focusCounter.value as number];
    // addFilterButtonClass(previousButton as HTMLButtonElement);
    previousButton.scrollIntoView(scrollConfig);
  }
}

function showNextItem() {
  if (filterButtons.value) {
    // const currentButton = filterButtons.value[(focusCounter.value as number)];
    // removeFilterButtonClass(currentButton as HTMLButtonElement);

    focusCounter.value += 1;
    if (focusCounter.value >= maxFocusCounter.value - 1) {
      focusCounter.value = maxFocusCounter.value - 1;
    }

    const nextButton = filterButtons.value[focusCounter.value as number];
    // addFilterButtonClass(nextButton as HTMLButtonElement);
    nextButton.scrollIntoView(scrollConfig);
  }
}

onMounted(() => {
  widthObserver.value = new MutationObserver(observeScrollWidth);
  widthObserver.value.observe(buttonContainer.value, observerConfig);
});

onBeforeUnmount(() => widthObserver.value.disconnect());

watch(
  () => buttonContainer.value,
  () => {
    if (buttonContainer.value) {
      updateFilterButtons();
    }
  }
);
</script>

<template>
  <div
    ref="filterWell"
    role="group"
    class="grid flex-nowrap justify-start items-center gap-2 mb-2 py-3 pr-2"
    :class="{
      'grid-cols-1': !hasFilterWellButtons,
      'grid-cols-[100px_1fr]': hasFilterWellButtons && !showButtons,
      'grid-cols-[100px_35px_1fr_35px]': hasFilterWellButtons && showButtons,
    }"
  >
    <div v-if="hasFilterWellButtons">
      <Button
        @click="emit('clear', true)"
        type="filterWell"
        size="tiny"
        icon="cross"
        iconPosition="right"
      >
        Clear all
      </Button>
    </div>
    <div v-if="showButtons">
      <Button
        :icon-only="true"
        type="filterWell"
        icon="CaretLeft"
        label="Show previous term"
        size="tiny"
        :disabled="focusCounter === minFocusCounter"
        @click="showPreviousItem"
      />
    </div>
    <div
      ref="filterButtonContainer"
      class="flex flex-row flex-nowrap gap-2 justify-start items-center overflow-x-auto"
    >
      <slot></slot>
    </div>
    <div v-if="showButtons">
      <Button
        :icon-only="true"
        type="filterWell"
        icon="CaretRight"
        label="Show next term"
        size="tiny"
        :disabled="focusCounter === maxFocusCounter"
        @click="showNextItem"
      />
    </div>
  </div>
</template>
