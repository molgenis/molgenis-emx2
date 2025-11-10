<script lang="ts" setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import Button from "../Button.vue";

const props = defineProps<{
  id: string;
  size: number;
}>();
const isExpanded = ref<boolean>(false);
const filterButtonContainer = ref<HTMLElement | null>(null);
const isOverflowing = ref(false);

const emit = defineEmits(["clear"]);

function checkOverflow() {
  const el = filterButtonContainer.value;
  if (!el) return;
  // Compare scrollHeight (full content height) to visible height (clientHeight)
  isOverflowing.value = el.scrollHeight > el.clientHeight;
}

onMounted(async () => {
  await nextTick();
  checkOverflow();
  // Recheck on window resize or after layout updates
  window.addEventListener("resize", checkOverflow);
});

watch(isExpanded, async () => {
  await nextTick();
  checkOverflow();
});

watch(
  () => props.size,
  async () => {
    await nextTick();
    checkOverflow();
  }
);
</script>

<template>
  <div
    ref="filterWell"
    role="group"
    class="flex justify-between items-start w-full gap-2 w-full"
  >
    <div
      :id="`${id}-collapsible-content`"
      ref="filterButtonContainer"
      class="flex flex-wrap gap-2 flex-1 min-w-0"
      :class="{
        'overflow-y-hidden max-h-12': !isExpanded,
        'overflow-y-visible max-h-auto': isExpanded,
      }"
    >
      <Button
        v-if="size > 1"
        :id="`${id}-button-clear`"
        icon="cross"
        iconPosition="right"
        type="filterWell"
        size="tiny"
        @click="emit('clear', true)"
      >
        clear all
      </Button>
      <slot></slot>
    </div>
    <Button
      v-if="isOverflowing || isExpanded"
      type="inline"
      class="shadow-sm rounded-full shrink-0 text-button-text self-start"
      icon-position="right"
      :id="`accordion__${id}-toggle-icon-only`"
      :icon="isExpanded ? 'caret-up' : 'caret-down'"
      :label="isExpanded ? 'minimize' : 'show all ' + size"
      @click="isExpanded = !isExpanded"
      :aria-labelledby="`accordion__${id}-toggle`"
      :aria-controls="`accordion__${id}-content`"
      :aria-expanded="isExpanded"
      :aria-haspopup="true"
      size="tiny"
    />
  </div>
</template>
