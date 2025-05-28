<script setup lang="ts">
import { onBeforeUnmount, onMounted, onUnmounted, watchEffect } from "vue";

withDefaults(
  defineProps<{
    title?: string;
    subtitle?: string;
    maxWidth?: string;
  }>(),
  {
    maxWidth: "max-w-xl",
  }
);

const visible = defineModel("visible", {
  required: true,
});

const emit = defineEmits(["closed"]);

// needed for case where modal is show/hidden but not added /removed from DOM
watchEffect(() => {
  if (typeof document === "undefined" || !document.body) {
    // short circuit if document is not available (e.g. during SSR)
    return;
  }
  if (visible.value) {
    document.body.style.overflow = "hidden";
  } else {
    document.body.style.overflow = "";
    emit("closed");
  }
});

// needed for case where modal is added /removed ( versus show/hide)
onBeforeUnmount(() => {
  document.body.style.overflow = "";
});

function handleKeydown(e: KeyboardEvent) {
  if (e.key === "Escape") {
    visible.value = false;
  }
}

onMounted(() => {
  if (window) {
    window.addEventListener("keydown", handleKeydown);
  }
});

onUnmounted(() => {
  emit("closed");
  if (window) {
    window.removeEventListener("keydown", handleKeydown);
  }
});

function hide() {
  visible.value = false;
}
</script>

<template>
  <section
    v-show="visible"
    role="dialog"
    :aria-labelledby="title"
    ref="dialog"
    class="fixed min-h-lvh w-full top-0 left-0 flex z-20 overscroll-behavior: contain"
  >
    <a
      id="backdrop"
      @click="visible = false"
      class="w-full h-full absolute left-0 bg-black/60 overscroll-behavior: contain"
      tabindex="-1"
    />

    <div
      class="bg-modal w-3/4 relative m-auto rounded-t-none rounded-b-theme"
      :class="maxWidth"
    >
      <slot name="header">
        <header
          class="pt-[36px] px-[30px] overflow-y-auto border-b border-divider"
        >
          <div v-if="subtitle" class="text-title-contrast">{{ subtitle }}</div>
          <h2
            v-if="title"
            class="mb-5 uppercase text-heading-4xl font-display text-title-contrast"
          >
            {{ title }}
          </h2>

          <button
            @click="visible = false"
            aria-label="Close modal"
            class="absolute top-7 right-8 p-1"
          >
            <BaseIcon class="text-button-input-toggle" name="cross" />
          </button>
        </header>
      </slot>

      <div class="overflow-y-auto max-h-[calc(95vh-232px)]">
        <slot />
      </div>

      <footer
        class="bg-modal-footer px-[30px] rounded-b-theme border-t border-divider"
      >
        <slot name="footer" :hide="hide" />
      </footer>
    </div>
  </section>
</template>
