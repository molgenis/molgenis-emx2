<template>
  <SideModal :label="label" :isVisible="visible" @onClose="visible = false">
    {{ tableId }}
    {{ label }}
    {{ row }}
  </SideModal>
  <button @click="visible != visible">test</button>
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
});

let visible = ref(true);
</script>

<style scoped></style>

<docs>
      <template>
          <RefSideModal label="Label" tableId="Pet" :row="{}" />
      </template>
  </docs>
