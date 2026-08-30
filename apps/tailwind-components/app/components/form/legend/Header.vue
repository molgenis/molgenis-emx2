<script setup lang="ts">
import type { MaybeRef } from "vue";
import { computed, unref } from "vue";
import FormLegendErrorCounter from "./ErrorCounter.vue";
const props = withDefaults(
  defineProps<{
    id: string;
    idPrefix: string;
    label: string;
    href?: string;
    isActive?: boolean;
    errorCount?: MaybeRef<number>;
  }>(),
  {
    isActive: false,
    errorCount: 0,
  }
);

const errorCountId = computed(() =>
  (unref(props.errorCount) ?? 0) > 0
    ? `${props.idPrefix}-${props.id}-error-count`
    : undefined
);

const emit = defineEmits<{
  (e: "goToSection", id: string): void;
}>();

function scrollIfNotALink(event: MouseEvent) {
  if (!props.href) {
    event.preventDefault();
    emit("goToSection", props.id);
  }
}
</script>
<template>
  <div class="flex my-2">
    <div
      class="bg-button-primary w-[0.3125rem] min-w-[0.3125rem] h-7 min-h-7 transition-opacity"
      :class="{ 'opacity-0': !isActive }"
    />
    <div class="flex gap-2 grow min-w-0">
      <a
        :id="`${idPrefix}-${id}`"
        :aria-describedby="errorCountId"
        class="pl-7 grow truncate hover:overflow-visible bg-form-legend cursor-pointer"
        :href="href ?? '#'"
        :aria-current="isActive"
        @click="scrollIfNotALink"
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
