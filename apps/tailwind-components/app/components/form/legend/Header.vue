<script setup lang="ts">
import type { MaybeRef } from "vue";
import { computed, unref, useId } from "vue";
import FormLegendErrorCounter from "./ErrorCounter.vue";
const props = withDefaults(
  defineProps<{
    id: string;
    label: string;
    isActive?: boolean;
    errorCount?: MaybeRef<number>;
  }>(),
  {
    isActive: false,
    errorCount: 0,
  }
);

const anchorId = `form-legend-header-${useId()}-${props.id}`;
const errorCountId = computed(() =>
  (unref(props.errorCount) ?? 0) > 0 ? `${anchorId}-error-count` : undefined
);

const emit = defineEmits<{
  (e: "goToSection", id: string): void;
}>();
</script>
<template>
  <div class="flex my-2">
    <div
      class="bg-button-primary w-[0.3125rem] min-w-[0.3125rem] h-7 min-h-7 transition-opacity"
      :class="{ 'opacity-0': !isActive }"
    />
    <div class="flex gap-2 grow min-w-0">
      <a
        :id="anchorId"
        :aria-describedby="errorCountId"
        class="pl-7 grow truncate hover:overflow-visible bg-form-legend cursor-pointer"
        href="#"
        :aria-current="isActive"
        @click.prevent="emit('goToSection', id)"
      >
        <span
          class="text-title-contrast capitalize"
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
    </div>
  </div>
</template>
