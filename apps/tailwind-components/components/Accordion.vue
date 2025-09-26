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
      class="group w-full flex justify-start items-center gap-1.5 p-5 text-title-contrast"
      @click="isExpanded = !isExpanded"
      :aria-controls="`${elemId}-content`"
      :aria-expanded="isExpanded"
    >
      <span
        class="w-full group-hover:underline text-left capitalize font-bold text-clip"
      >
        {{ label }}
      </span>
      <BaseIcon
        name="CaretDown"
        :width="26"
        class="group-hover:bg-button-secondary-hover group-hover:text-button-secondary-hover origin-center rounded-full"
        :class="{
          'rotate-180': isExpanded,
          'rotate-0': !isExpanded,
        }"
      />
    </button>
    <div
      :id="`${elemId}-content`"
      class="px-5 pb-5"
      :class="{
        static: isExpanded,
        hidden: !isExpanded,
      }"
    >
      <slot></slot>
    </div>
  </div>
</template>
