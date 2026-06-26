<script setup lang="ts">
import type { MaybeRef } from "vue";
import { unref } from "vue";
import FormLegendErrorCounter from "./ErrorCounter.vue";
import BaseIcon from "../../BaseIcon.vue";

withDefaults(
  defineProps<{
    id: string;
    label: string;
    isActive?: boolean;
    errorCount?: MaybeRef<number>;
    collapsible?: boolean;
    hasChildren?: boolean;
    expanded?: boolean;
  }>(),
  {
    isActive: false,
    errorCount: 0,
    collapsible: false,
    hasChildren: false,
    expanded: false,
  }
);

const emit = defineEmits<{
  (e: "goToSection", id: string): void;
  (e: "toggle"): void;
}>();
</script>
<template>
  <div class="flex">
    <div
      class="bg-button-primary w-[0.3125rem] min-w-[0.3125rem] self-stretch transition-opacity"
      :class="{ 'opacity-0': !isActive }"
    />
    <div class="flex gap-2 items-center flex-1">
      <button
        v-if="collapsible && hasChildren"
        type="button"
        :id="`form-legend-header-${id}`"
        :aria-describedby="`form-legend-header-${id}-error-count`"
        :aria-expanded="expanded"
        class="pl-7 bg-form-legend cursor-pointer flex-1 text-left flex items-center gap-2"
        @click="emit('toggle')"
      >
        <span
          class="text-title-contrast capitalize py-1 truncate hover:overflow-visible"
          :class="{ 'font-bold': isActive }"
        >
          {{ label }}
        </span>
        <FormLegendErrorCounter
          v-if="(unref(errorCount) ?? 0) > 0"
          :label="label"
          :errorCount="errorCount"
        />
        <BaseIcon
          name="CaretRight"
          class="w-4 h-4 transition-transform ml-auto shrink-0"
          :class="{ 'rotate-90': expanded }"
        />
      </button>
      <template v-else>
        <a
          :id="`form-legend-header-${id}`"
          :aria-describedby="`form-legend-header-${id}-error-count`"
          class="pl-7 truncate hover:overflow-visible bg-form-legend cursor-pointer"
          href="#"
          :aria-current="isActive"
          @click.prevent="emit('goToSection', id)"
        >
          <span
            class="text-title-contrast capitalize py-1"
            :class="{ 'font-bold': isActive }"
          >
            {{ label }}
          </span>
        </a>
        <FormLegendErrorCounter
          v-if="(unref(errorCount) ?? 0) > 0"
          :label="label"
          :errorCount="errorCount"
        />
      </template>
    </div>
  </div>
</template>
