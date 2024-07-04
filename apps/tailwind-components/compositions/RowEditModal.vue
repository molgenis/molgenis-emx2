<script setup lang="ts">
import { ref } from "vue";
import Modal from "../components/Modal.vue";

const modal = ref<InstanceType<typeof Modal>>();

const { operation, table } = defineProps<{
  operation: string;
  table: string;
}>();

function show() {
  modal.value?.show();
}

function close() {
  modal.value?.close();
}

defineExpose({
  show,
  close,
});
</script>

<template>
  <Modal
    ref="modal"
    :title="`${operation} into ${table}`"
    subtitle="Subtitle"
    maxWidth="max-w-7xl"
    minHeight="min-h-full"
  >
    <div class="flex">
      <div>
        <p class="pb-2">Forminput here</p>
      </div>
      <div>
        <p>Side bar</p>
      </div>
    </div>

    <div class="flex flex-wrap gap-5">
      <button
        @click="close"
        class="flex items-center border rounded-full h-10.5 px-5 text-heading-lg gap-3 tracking-widest uppercase font-display bg-button-tertiary text-button-tertiary border-button-tertiary hover:bg-button-tertiary-hover hover:text-button-tertiary-hover hover:border-button-tertiary-hover"
      >
        previous
      </button>
      <button
        @click="close"
        class="flex items-center border rounded-full h-10.5 px-5 text-heading-lg gap-3 tracking-widest uppercase font-display bg-button-tertiary text-button-tertiary border-button-tertiary hover:bg-button-tertiary-hover hover:text-button-tertiary-hover hover:border-button-tertiary-hover"
      >
        next
      </button>
      <button
        @click="close"
        class="flex items-center border rounded-full h-10.5 px-5 text-heading-lg gap-3 tracking-widest uppercase font-display bg-button-tertiary text-button-tertiary border-button-tertiary hover:bg-button-tertiary-hover hover:text-button-tertiary-hover hover:border-button-tertiary-hover"
      >
        save
      </button>
      <button
        @click="close"
        class="flex items-center border rounded-full h-10.5 px-5 text-heading-lg gap-3 tracking-widest uppercase font-display bg-button-tertiary text-button-tertiary border-button-tertiary hover:bg-button-tertiary-hover hover:text-button-tertiary-hover hover:border-button-tertiary-hover"
      >
        save draft
      </button>
      <button
        @click="close"
        class="flex items-center border rounded-full h-10.5 px-5 text-heading-lg gap-3 tracking-widest uppercase font-display bg-button-tertiary text-button-tertiary border-button-tertiary hover:bg-button-tertiary-hover hover:text-button-tertiary-hover hover:border-button-tertiary-hover"
      >
        cancel
      </button>
    </div>
  </Modal>
</template>
