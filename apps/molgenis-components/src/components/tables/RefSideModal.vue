<template>
  <SideModal>
    {{ tableId }}
    {{ label }}
    {{ row }}
  </SideModal>
</template>

<script lang="ts" setup>
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
</script>

<style scoped></style>

<docs>
      <template>
          <RefSideModal/>
      </template>
  </docs>
