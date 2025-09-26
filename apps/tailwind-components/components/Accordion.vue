<script lang="ts" setup>
import { ref, computed } from "vue";

const props = withDefaults(
  defineProps<{
    id: string;
    label: string;
    openByDefault?: boolean;
  }>(),
  {
    openByDefault: true,
  }
);

const isExpanded = ref<boolean>(props.openByDefault);
const elemId = computed<string>(() => {
  return "accordion__" + props.id.toLowerCase().replaceAll(" ", "-");
});
</script>

<template>
  <div :id="elemId" class="border rounded">
    <button
      :id="`${elemId}-toggle`"
      class="w-full flex justify-start items-center gap-5 p-5 text-title-contrast"
      @click="isExpanded = !isExpanded"
      :aria-controls="`${elemId}-content`"
      :aria-expanded="isExpanded"
    >
      <span class="w-full text-left capitalize font-bold text-clip">
        {{ label }}
      </span>
      <BaseIcon
        name="CaretDown"
        class="origin-center"
        :class="{
          'rotate-180': isExpanded,
          'rotate-0': !isExpanded,
        }"
      />
    </button>
    <div
      :id="`${elemId}-content`"
      class="p-5"
      :class="{
        static: isExpanded,
        hidden: !isExpanded,
      }"
    >
      <slot></slot>
    </div>
  </div>
</template>
