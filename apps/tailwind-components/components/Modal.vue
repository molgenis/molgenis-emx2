<script setup lang="ts">
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

const visible = ref(false);

function showModal() {
  visible.value = true;
}

const closeModal = () => {
  visible.value = false;
};

defineExpose({
  show: showModal,
  close: closeModal,
  visible,
});
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
      @click="closeModal()"
      class="w-full h-full absolute left-0 bg-black/60 overscroll-behavior: contain"
      href="#"
      tabindex="-1"
    />

    <div
      class="bg-modal w-3/4 relative m-auto rounded-t-none rounded-b-theme"
      :class="maxWidth"
    >
      <slot name="header">
        <header
          class="pt-[36px] px-[50px] overflow-y-auto border-b border-divider"
        >
          <div class="text-gray-900" v-if="subtitle">{{ subtitle }}</div>
          <h2 v-if="title" class="mb-5 uppercase text-heading-4xl font-display">
            {{ title }}
          </h2>

          <button
            @click="closeModal()"
            aria-label="Close modal"
            class="absolute top-7 right-8 p-1"
          >
            <BaseIcon class="text-link" name="cross" />
          </button>
        </header>
      </slot>

      <div class="overflow-y-auto max-h-[calc(95vh-232px)]">
        <slot />
      </div>

      <footer
        class="bg-modal-footer px-[30px] rounded-b-theme border-t border-divider"
      >
        <slot name="footer" />
      </footer>
    </div>
  </section>
</template>
