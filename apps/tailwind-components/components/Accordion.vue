<script lang="ts" setup>
import { useId, ref } from "vue";

const props = withDefaults(
  defineProps<{
    label: string;
    openByDefault?: boolean;
    contentIsFullWidth?: boolean;
    toggleIconSize?: "tiny" | "small" | "medium" | "large";
  }>(),
  {
    openByDefault: true,
    contentIsFullWidth: false,
    toggleIconSize: "medium",
  }
);

const id = useId();
const isExpanded = ref<boolean>(props.openByDefault);
</script>

<template>
  <div :id="`accordion__${id}`" class="border">
    <div
      class="group flex justify-between items-center gap-5 text-button-text px-5"
    >
      <div class="w-full">
        <button
          :id="`accordion__${id}-toggle`"
          class="group flex justify-start items-center gap-1.5 p-5 pl-0 text-title-contrast cursor-pointer"
          @click="isExpanded = !isExpanded"
          :aria-controls="`accordion__${id}-content`"
          :aria-expanded="isExpanded"
          :aria-haspopup="true"
        >
          <span
            class="hover:underline focus:underline text-left capitalize font-bold text-clip"
          >
            {{ label }}
          </span>
        </button>
      </div>
      <div class="flex justify-start items-center">
        <slot name="toolbar"></slot>
      </div>
      <div class="flex justify-center items-center">
        <button
          :v-tooltip.bottom="isExpanded ? 'Hide details' : 'Show details'"
          :id="`accordion__${id}-toggle-icon-only`"
          class="rounded-full hover:bg-button-secondary-hover flex-1"
          :aria-labelledby="`accordion__${id}-toggle`"
          :aria-controls="`accordion__${id}-content`"
          :aria-expanded="isExpanded"
          :aria-haspopup="true"
          @click="isExpanded = !isExpanded"
        >
          <BaseIcon
            :name="isExpanded ? 'caret-up' : 'caret-down'"
            :class="{
              'p-[8px] h-8 w-8': toggleIconSize === 'tiny',
              'p-[5px] h-10 w-10': toggleIconSize === 'small',
              'p-[8px] h-14 w-14': toggleIconSize === 'medium',
              'p-[8px] h-18 w-18': toggleIconSize === 'large',
            }"
          />
          <span class="sr-only">{{
            isExpanded ? "Hide details" : "Show details"
          }}</span>
        </button>
      </div>
    </div>
    <div
      :id="`accordion__${id}-content`"
      :aria-labelledby="`accordion__${id}-toggle`"
      class="grid transition-all ease-in-out motion-safe:duration-default motion-reduce:duration-0"
      :class="{
        'grid-rows-[0]': !isExpanded,
        'grid-rows-1': isExpanded,
      }"
    >
      <div class="overflow-hidden">
        <div :class="{ 'px-5 pb-5': !contentIsFullWidth }">
          <slot></slot>
        </div>
      </div>
    </div>
  </div>
</template>
