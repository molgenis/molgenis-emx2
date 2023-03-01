<template>
  <SideModal :label="label" :isVisible="isVisible" @onClose="emit('onClose')">
    {{ tableId }}
    {{ label }}
    {{ row }}
  </SideModal>
</template>

<script lang="ts" setup>
import { ref } from "vue";

const props = defineProps({
  tableId: {
    type: String,
    required: true,
  },
  label: {
    type: String,
    required: true,
  },
  row: {
    type: String,
    required: true,
  },
  isVisible: {
    type: Boolean,
    default: true,
  },
});

const emit = defineEmits(["onClose"]);
</script>

<style scoped></style>

<docs>
      <template>
          <RefSideModal label="Label" tableId="Pet" :row="{}" />
      </template>
  </docs>
