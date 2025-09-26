<script lang="ts" setup>
import { useId, ref } from "vue";

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
  <div :id="`accordion__${id}`" class="border">
    <div
      class="group flex justify-between items-center gap-4 text-button-text px-5"
    >
      <button
        :id="`accordion__${id}-toggle`"
        class="group w-full flex justify-start items-center gap-1.5 p-5 pl-0 text-title-contrast cursor-pointer"
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
      <div class="flex justify-center items-center gap-1">
        <slot name="toolbar"></slot>
      </div>
      <Button
        type="inline"
        :id="`accordion__${id}-toggle-icon-only`"
        class="hover:bg-button-secondary-hover"
        :icon="isExpanded ? 'caret-up' : 'caret-down'"
        :icon-only="true"
        :aria-labelledby="`accordion__${id}-toggle`"
        :aria-controls="`accordion__${id}-content`"
        :aria-expanded="isExpanded"
        :aria-haspopup="true"
        size="small"
        :label="isExpanded ? 'Hide details' : 'Show details'"
        @click="isExpanded = !isExpanded"
      />
    </div>
    <div
      :id="`accordion__${id}-content`"
      :aria-labelledby="`accordion__${id}-toggle`"
      class="transition-all overflow-hidden motion-safe:duration-default motion-reduce:duration-0"
      :class="{
        'max-h-96 opacity-100': isExpanded,
        'max-h-0 opacity-0': !isExpanded,
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
