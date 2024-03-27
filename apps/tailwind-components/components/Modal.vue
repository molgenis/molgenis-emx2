<script setup lang="ts">
defineProps<{
  title: string;
  subTitle?: string;
}>();

const dialog = ref<HTMLDialogElement>();
const visible = ref(false);

function showModal() {
  dialog.value?.showModal();
  visible.value = true;
}

const closeModal = (returnVal?: string) => {
  dialog.value?.close(returnVal);
  visible.value = false;
};

function handleClick(e: MouseEvent) {
  if (e.target === dialog.value) {
    closeModal("close from dialog");
  }
}

defineExpose({
  show: showModal,
  close: closeModal,
  visible,
});
</script>
<template>
  <dialog
    ref="dialog"
    class="w-[60vw] rounded-50px backdrop:backdrop-blur-sm"
    @click="handleClick"
  >
    <header class="pt-[36px] px-[50px]">
      <div class="text-gray-900" v-if="subTitle">{{ subTitle }}</div>
      <h2 v-if="title" class="mb-5 uppercase text-heading-4xl font-display">
        {{ title }}
      </h2>

      <button
        @click="closeModal('close from btn')"
        class="absolute top-7 right-8 p-1"
      >
        <BaseIcon class="text-blue-500" name="cross" />
      </button>

      <slot name="header"></slot>
    </header>

    <div class="px-[50px] h-[calc(80vh-232px)] overflow-y-auto py-4">
      <slot></slot>
    </div>

    <footer class="bg-modal-footer px-[50px]">
      <menu class="flex items-center justify-left h-[116px]">
        <slot name="footer">
          <div class="flex flex-wrap gap-5">
            <button
              @click="closeModal('close from btn')"
              class="flex items-center border rounded-full h-10.5 px-5 text-heading-lg gap-3 tracking-widest uppercase font-display bg-button-primary text-button-primary border-button-primary hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover"
            >
              Primary
            </button>
            <button
              @click="closeModal('close from btn')"
              class="flex items-center border rounded-full h-10.5 px-5 text-heading-lg gap-3 tracking-widest uppercase font-display bg-button-secondary text-button-secondary border-button-secondary hover:bg-button-secondary-hover hover:text-button-secondary-hover hover:border-button-secondary-hover"
            >
              Secondary
            </button>
          </div>
        </slot>
      </menu>
    </footer>
  </dialog>
</template>
