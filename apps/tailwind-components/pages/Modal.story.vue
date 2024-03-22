<script setup lang="ts">
const isShown = ref(false);
</script>

<template>
  <div class="flex h-screen">
    <h1>Modal</h1>
    <div class="m-auto">
      <button
        @click="isShown = true"
        class="flex items-center border rounded-full h-10.5 px-5 text-heading-lg gap-3 tracking-widest uppercase font-display bg-button-primary text-button-primary border-button-primary hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover"
      >
        Show modal
      </button>
    </div>

    <Modal :shown="isShown" @close="isShown = false">
      <div class="p-6">This is a modal</div>
    </Modal>
  </div>
</template>
