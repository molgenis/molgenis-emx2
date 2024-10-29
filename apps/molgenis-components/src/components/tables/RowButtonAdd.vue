<template>
  <span>
    <ButtonOutline v-if="label !== ''" @click="isModalShown = true">
      {{ label }}
    </ButtonOutline>
    <RowButton v-else type="add" @add="isModalShown = true" />
    <EditModal
      v-if="isModalShown"
      :id="id + 'add-modal'"
      :tableId="tableId"
      :isModalShown="isModalShown"
      :schemaId="schemaId"
      :defaultValue="defaultValue"
      :visibleColumns="visibleColumns"
      :applyDefaultValues="true"
      @close="handleClose"
      @update:newRow="(event:any) => $emit('update:newRow', event)"
    />
  </span>
</template>

<script setup lang="ts">
import RowButton from "./RowButton.vue";
import ButtonOutline from "../forms/ButtonOutline.vue";
import { defineAsyncComponent, ref } from "vue";

const EditModal = defineAsyncComponent({
  loader: () => import("../forms/EditModal.vue"),
});

withDefaults(
  defineProps<{
    id: string;
    tableId: string;
    schemaId: string;
    label?: string;
    defaultValue?: Record<string, any>;
    visibleColumns?: string[];
  }>(),
  { label: "" }
);

let isModalShown = ref(false);

const emit = defineEmits(["close", "update:newRow"]);

function handleClose() {
  isModalShown.value = false;
  emit("close");
}
</script>

<docs>
<template>
  <div>
    <label for="row-add-btn-sample">
      composition of RowButton and EditModal configured for row add/insert
    </label>
    <div>
      <RowButtonAdd
        id="row-add-btn-sample"
        tableId="Pet"
        schemaId="pet store"
      />
      <br />
      <RowButtonAdd
        id="row-add-btn-sample"
        tableId="Pet"
        label="Add a new pet"
        schemaId="pet store"
      />
    </div>
  </div>
</template>
</docs>
