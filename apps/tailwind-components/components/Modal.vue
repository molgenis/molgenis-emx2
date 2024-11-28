<script setup lang="ts">
defineProps<{
  title: string;
  subtitle?: string;
}>();

const visible = ref(false);

function showModal() {
  visible.value = true;
}

const closeModal = (_returnVal?: string) => {
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
    class="fixed min-h-lvh w-full top-0 left-0 flex z-20"
  >
    <a
      @click="closeModal('close from backdrop')"
      class="w-full h-full absolute left-0 bg-black/60"
      href="#"
      tabindex="-1"
    ></a>

    <div class="bg-white w-3/4 relative m-auto h-3/4 rounded-50px max-w-xl">
      <header class="pt-[36px] px-[50px] overflow-y-auto">
        <div class="text-gray-900" v-if="subtitle">{{ subtitle }}</div>
        <h2 v-if="title" class="mb-5 uppercase text-heading-4xl font-display">
          {{ title }}
        </h2>

        <button
          @click="closeModal('close from btn')"
          aria-label="Close modal"
          class="absolute top-7 right-8 p-1"
        >
          <BaseIcon class="text-blue-500" name="cross" />
        </button>

        <slot name="header"></slot>
      </header>

      <div class="px-[50px] overflow-y-auto py-4 max-h-[calc(80vh-232px)]">
        <slot></slot>
      </div>

      <footer class="bg-modal-footer px-[50px] rounded-b-50px">
        <menu class="flex items-center justify-left h-[116px]">
          <slot name="footer" />
        </menu>
      </footer>
    </div>
  </section>
</template>
