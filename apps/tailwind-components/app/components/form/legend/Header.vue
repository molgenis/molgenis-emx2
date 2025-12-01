<script setup lang="ts">
import FormLegendErrorCounter from "./ErrorCounter.vue";
withDefaults(
  defineProps<{
    id: string;
    label: string;
    isActive?: boolean;
    errorCount?: number;
  }>(),
  {
    isActive: false,
    errorCount: 0,
  }
);

const emit = defineEmits<{
  (e: "goToSection", id: string): void;
}>();
</script>
<template>
  <div class="flex">
    <div
      class="bg-button-primary w-1 h-7 visible"
      :class="{ invisible: !isActive }"
    />
    <div class="flex gap-2">
      <a
        :id="`form-legend-header-${id})`"
        :aria-describedby="`form-legend-header-${id})-error-count`"
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
        v-if="(errorCount ?? 0) > 0"
        :label="label"
        :errorCount="errorCount"
      />
    </div>
  </div>
</template>
