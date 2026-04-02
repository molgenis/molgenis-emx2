<script setup lang="ts">
import { computed } from "vue";
import Button from "../../Button.vue";
const props = defineProps<{
  numberOfSelectedRows: number;
  canEdit: boolean;
}>();

const emit = defineEmits<{
  (e: "rowAction", payload: { action: string }): void;
}>();

const noRowsSelected = computed(() => props.numberOfSelectedRows === 0);

const singleRowSelected = computed(() => props.numberOfSelectedRows === 1);
</script>

<template>
  <div
    class="flex flex-row items-center gap-2 px-2 group border border-theme rounded-theme"
  >
    <Button
      v-if="canEdit"
      :icon-only="true"
      icon="trash"
      type="inline"
      size="small"
      label="Delete selection"
      :disabled="noRowsSelected"
      @click="emit('rowAction', { action: 'delete-selection' })"
    />
    <Button
      v-if="canEdit"
      :icon-only="true"
      icon="edit"
      type="inline"
      size="small"
      label="Edit"
      :disabled="noRowsSelected || !singleRowSelected"
      @click="emit('rowAction', { action: 'edit-selection' })"
    />
    <Button
      :icon-only="true"
      icon="info"
      type="inline"
      size="small"
      label="View details"
      :disabled="noRowsSelected || !singleRowSelected"
      @click="emit('rowAction', { action: 'view-details' })"
    />
  </div>
</template>
