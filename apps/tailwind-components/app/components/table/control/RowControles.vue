<script setup lang="ts">
import { computed } from "vue";
import Button from "../../Button.vue";
import { Dropdown as VDropdown } from "floating-vue";
import Checkbox from "../../input/Checkbox.vue";
import BaseIcon from "../../BaseIcon.vue";
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
    <VDropdown
      aria-id="drop-it"
      :distance="18"
      :skidding="-4"
      placement="bottom-start"
      :triggers="['hover', 'click']"
      :popper-triggers="['hover', 'click']"
    >
      <div class="flex items-center pl-2 pr-2 gap-2 border-r-2">
        <Checkbox> </Checkbox>
        <BaseIcon name="caret-down" size="medium" />
      </div>

      <template #popper>
        <div class="bg-form p-[10px] w-[180px] border border-form">
          <ul>
            <li class="px-[15px] py-[10px] text-disabled">Select</li>
            <li
              class="px-[15px] py-[10px] text-title hover:bg-button-secondary-hover hover:cursor-pointer"
              @click="emit('rowAction', { action: 'select-all-on-page' })"
            >
              All on this page
            </li>
            <li
              class="px-[15px] py-[10px] text-title hover:bg-button-secondary-hover hover:cursor-pointer"
              @click="emit('rowAction', { action: 'select-all' })"
            >
              All
            </li>
            <li
              class="px-[15px] py-[10px] text-title hover:bg-button-secondary-hover hover:cursor-pointer"
              @click="emit('rowAction', { action: 'select-none' })"
            >
              None
            </li>
            <li
              class="px-[15px] py-[10px] text-title hover:bg-button-secondary-hover hover:cursor-pointer"
              @click="emit('rowAction', { action: 'select-drafts' })"
            >
              Drafts
            </li>
          </ul>
        </div>
      </template>
    </VDropdown>
    <Button
      v-if="canEdit"
      :icon-only="true"
      icon="trash"
      type="inline"
      size="large"
      label="Delete selection"
      :disabled="noRowsSelected"
      @click="emit('rowAction', { action: 'delete-selection' })"
    />
    <Button
      v-if="canEdit"
      :icon-only="true"
      icon="edit"
      type="inline"
      size="large"
      label="Edit"
      :disabled="noRowsSelected || !singleRowSelected"
      @click="emit('rowAction', { action: 'edit-selection' })"
    />
    <Button
      :icon-only="true"
      icon="info"
      type="inline"
      size="large"
      label="View details"
      :disabled="noRowsSelected || !singleRowSelected"
      @click="emit('rowAction', { action: 'view-details' })"
    />
  </div>
</template>
