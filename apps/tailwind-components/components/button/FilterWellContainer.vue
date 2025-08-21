<script lang="ts" setup>
import { ref, useTemplateRef, watch, onMounted, onBeforeUnmount } from "vue";

defineProps<{
  id: string;
}>();

const buttonContainer = useTemplateRef<HTMLDivElement>("filterButtonContainer");
const widthObserver = ref();
const showExpandButton = ref<boolean>(false);
const hasFilterWellButtons = ref<boolean>(false);
const filterButtons = ref<HTMLCollection>();
const isExpanded = ref<boolean>(false);

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
    buttonContainer.value.scrollHeight > buttonContainer.value.offsetHeight
  ) {
    showExpandButton.value = true;
  } else {
    showExpandButton.value = false;
    isExpanded.value = false;
  }

  updateFilterButtons();
}

function updateFilterButtons() {
  filterButtons.value = buttonContainer.value?.children as HTMLCollection;

  if (buttonContainer.value) {
    hasFilterWellButtons.value = buttonContainer.value.children.length > 1;
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
    class="grid flex-nowrap justify-start items-center gap-2"
  >
    <div class="flex gap-4" v-if="hasFilterWellButtons">
      <ButtonText
        :id="`${id}-button-clear`"
        @click="emit('clear', true)"
        type="filterWell"
        size="tiny"
      >
        Clear all
      </ButtonText>
      <ButtonText
        :id="`${id}-button-expand`"
        @click="isExpanded = !isExpanded"
        v-if="showExpandButton"
        :aria-expanded="isExpanded"
        :aria-controls="`${id}-collapsible-content`"
        :aria-haspopup="true"
      >
        {{ isExpanded ? "Show less" : "Show more" }}
      </ButtonText>
    </div>
    <div
      :id="`${id}-collapsible-content`"
      ref="filterButtonContainer"
      class="flex flex-wrap gap-2 justify-start items-center"
      :class="{
        'overflow-y-hidden max-h-8': !isExpanded,
        'overflow-y-visible max-h-auto': isExpanded,
      }"
    >
      <slot></slot>
    </div>
  </div>
</template>
