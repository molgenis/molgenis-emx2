<script setup lang="ts">
import type { MaybeRef } from "vue";
import { computed, unref } from "vue";
import FormLegendErrorCounter from "./ErrorCounter.vue";
const props = withDefaults(
  defineProps<{
    id: string;
    /** Keeps the ids unique when two legends render on one page. */
    idPrefix: string;
    label: string;
    /**
     * A record's sections sit at real URL fragments, so the record menu passes one and the entry
     * becomes a link the browser navigates itself, which a reader can copy or open in a new tab.
     * A form's fields sit in a modal with no address of their own, so the form legend passes none.
     */
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

/** With no href the browser has nothing to navigate to, so the click asks the caller to scroll. */
function goTo(event: MouseEvent) {
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
        @click="goTo"
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
