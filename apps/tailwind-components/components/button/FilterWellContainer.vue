<script lang="ts" setup>
import {
  ref,
  useTemplateRef,
  computed,
  onMounted,
  onUpdated,
  onBeforeUnmount,
} from "vue";

const clearButton = useTemplateRef<HTMLDivElement>("clearButtonColumn");
const btnContainer = useTemplateRef<HTMLDivElement>("filterButtonContainer");
const widthObserver = ref();
const buttonObserver = ref();
const showButtons = ref<boolean>(false);
const hasClearButton = ref<boolean>(false);
// const filterButtons = computed<HTMLButtonElement[]>(() => {
//   if (btnContainer.value) {
//     return btnContainer.value.children as HTMLCollection;
//   }
// })

// console.log(filterButtons.value);

function observeClearButton() {
  if (clearButton.value) {
    hasClearButton.value = clearButton.value.children.length > 0;
  }
}

function observeScrollWidth() {
  if (
    btnContainer.value &&
    btnContainer.value.scrollWidth > btnContainer.value.offsetWidth
  ) {
    showButtons.value = true;
  } else {
    showButtons.value = false;
  }
}

function showPreviousItem() {}

function showNextItem() {}

onMounted(() => {
  widthObserver.value = new MutationObserver(observeScrollWidth);
  buttonObserver.value = new MutationObserver(observeClearButton);

  const config = {
    childList: true,
    subtree: true,
    attributes: false,
    characterData: false,
  };
  widthObserver.value.observe(btnContainer.value, config);
  buttonObserver.value.observe(clearButton.value, config);
});

onBeforeUnmount(() => {
  widthObserver.value.disconnect();
  buttonObserver.value.disconnect();
});
</script>

<template>
  <div
    ref="filterWell"
    role="group"
    class="grid flex-nowrap justify-start items-center gap-2 mb-2 py-3 pr-2"
    :class="{
      'grid-cols-1': !hasClearButton,
      'grid-cols-[100px_1fr]': hasClearButton && !showButtons,
      'grid-cols-[100px_35px_1fr_35px]': hasClearButton && showButtons,
    }"
  >
    <div ref="clearButtonColumn">
      <slot name="column-clear-button"></slot>
    </div>
    <div v-if="showButtons">
      <Button
        :icon-only="true"
        type="filterWell"
        icon="CaretLeft"
        label="Show previous"
        size="tiny"
      />
    </div>
    <div
      ref="filterButtonContainer"
      class="flex flex-row flex-nowrap gap-2 justify-start items-center overflow-x-auto"
    >
      <slot name="filterButtons"></slot>
    </div>
    <div v-if="showButtons">
      <Button
        :icon-only="true"
        type="filterWell"
        icon="CaretRight"
        label="Show next"
        size="tiny"
      />
    </div>
  </div>
</template>
