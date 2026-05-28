<script setup lang="ts">
import Button from "./Button.vue";

defineProps<{
  collapsed: boolean;
  collapsedLabel: string;
}>();

const emit = defineEmits<{ "update:collapsed": [value: boolean] }>();
</script>

<template>
  <div :class="['shrink-0', collapsed ? '' : 'w-80 xl:w-96']">
    <div
      v-if="collapsed"
      class="rounded-t-3px rounded-b-theme pt-5 bg-sidebar-gradient w-16 flex flex-col items-center justify-start pt-4 gap-2 cursor-pointer"
      role="button"
      tabindex="0"
      :aria-label="collapsedLabel"
      @click="emit('update:collapsed', false)"
      @keydown.enter.space.prevent="emit('update:collapsed', false)"
    >
      <Button
        type="secondary"
        :icon-only="true"
        icon="double-arrow-right"
        :label="collapsedLabel"
        size="small"
        @click.stop="emit('update:collapsed', false)"
      />
      <h2
        class="font-display text-heading-2xl [writing-mode:vertical-rl] rotate-180 text-search-filter-title font-bold uppercase mt-4 mb-4"
      >
        {{ collapsedLabel }}
      </h2>
    </div>

    <div
      v-else
      id="filter-sidebar-content"
      class="rounded-t-3px rounded-b-theme bg-sidebar-gradient pb-8"
    >
      <div class="px-5 pt-5 pb-3 flex items-center justify-end">
        <Button
          type="secondary"
          :icon-only="true"
          icon="double-arrow-left"
          label="Hide filters"
          size="small"
          @click="emit('update:collapsed', true)"
        />
      </div>

      <slot />
    </div>
  </div>
</template>
