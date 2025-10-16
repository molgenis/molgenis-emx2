<script lang="ts" setup>
import { useId, ref } from "vue";
import Button from "./Button.vue";

const props = withDefaults(
  defineProps<{
    label: string;
    openByDefault?: boolean;
    contentIsFullWidth?: boolean;
  }>(),
  {
    openByDefault: true,
    contentIsFullWidth: false,
  }
);

const id = useId();
const isExpanded = ref<boolean>(props.openByDefault);
</script>

<template>
  <div
    :id="`accordion__${id}`"
    class="border border-input hover:border-input-focused focus-within:border-input-focused"
  >
    <div
      class="group flex justify-between items-center gap-5 text-button-text p-5 px-7.5"
    >
      <div class="w-full">
        <button
          :id="`accordion__${id}-toggle`"
          class="flex justify-start items-center gap-1.5 pl-0 text-title-contrast cursor-pointer"
          @click="isExpanded = !isExpanded"
          :aria-controls="`accordion__${id}-content`"
          :aria-expanded="isExpanded"
          :aria-haspopup="true"
        >
          <span
            class="group-hover:underline group-focus:underline text-left capitalize font-bold text-clip"
          >
            {{ label }}
          </span>
        </button>
      </div>
      <div class="flex justify-start items-center">
        <slot name="toolbar"></slot>
      </div>
      <div class="flex justify-center items-center">
        <Button
          type="inline"
          class="hover:bg-button-secondary-hover"
          :id="`accordion__${id}-toggle-icon-only`"
          :icon-only="true"
          :icon="isExpanded ? 'caret-up' : 'caret-down'"
          :label="isExpanded ? 'Hide details' : 'Show details'"
          @click="isExpanded = !isExpanded"
          :aria-labelledby="`accordion__${id}-toggle`"
          :aria-controls="`accordion__${id}-content`"
          :aria-expanded="isExpanded"
          :aria-haspopup="true"
        />
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
        <div :class="{ 'px-7.5 pb-7.5': !contentIsFullWidth }">
          <slot></slot>
        </div>
      </div>
    </div>
  </div>
</template>
