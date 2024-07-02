<script setup lang="ts">
import RowEditModal from "../compositions/RowEditModal.vue";
import { ref } from "vue";
const rowEditModal = ref<InstanceType<typeof RowEditModal>>();

function showModal() {
  rowEditModal.value?.show();
}
</script>

<template>
  <button
    @click="showModal"
    class="flex items-center border rounded-full h-10.5 px-5 text-heading-lg gap-3 tracking-widest uppercase font-display bg-button-primary text-button-primary border-button-primary hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover"
  >
    Open modal 🙈
  </button>
  <RowEditModal ref="rowEditModal" operation="Add"> </RowEditModal>
</template>
