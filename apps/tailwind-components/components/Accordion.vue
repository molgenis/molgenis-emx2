<script lang="ts" setup>
import { useId, ref } from "vue";

const props = withDefaults(
  defineProps<{
    label: string;
    openByDefault?: boolean;
  }>(),
  {
    openByDefault: true,
  }
);

const isExpanded = ref<boolean>(props.openByDefault);
const elemId = ref<string>(`accordion__${useId()}`);
</script>

<template>
  <div :id="elemId" class="border rounded">
    <button
      :id="`${elemId}-toggle`"
      class="group w-full flex justify-start items-center gap-1.5 p-5 text-title-contrast cursor-pointer"
      @click="isExpanded = !isExpanded"
      :aria-controls="`${elemId}-content`"
      :aria-expanded="isExpanded"
    >
      <span
        class="w-full group-hover:underline text-left capitalize font-bold text-clip"
      >
        {{ label }}
      </span>
      <div
        class="flex items-center justify-center h-6 w-6 group-hover:bg-button-secondary-hover group-hover:text-button-secondary-hover origin-center rounded-full"
      >
        <BaseIcon
          name="CaretDown"
          :width="26"
          :class="{
            'rotate-180 -mt-0.5': isExpanded,
            'rotate-0 mt-0.5': !isExpanded,
          }"
        />
      </div>
    </button>
    <div
      :id="`${elemId}-content`"
      :aria-labelledby="`${elemId}-toggle`"
      class="px-5 pb-5"
      :class="{
        block: isExpanded,
        hidden: !isExpanded,
      }"
    >
      <slot></slot>
    </div>
  </div>
</template>
