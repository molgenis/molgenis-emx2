<template>
  <span v-if="subclasses">
    <IconAction icon="plus" @click="isModalShown = true">add</IconAction>
    <LayoutModal
      v-if="isModalShown && !subclassSelected"
      @close="isModalShown = false"
    >
      <template v-slot:body>
        <h5>Select subtype</h5>
        <p>
          {{ subclasses[0].label }} has several variant subtypes that allow for
          diffferent properties. Please select below the subtype to create.
        </p>
        <table class="table-bordered">
          <tr v-for="subclass in subclasses">
            <td>
              <a href="#" @click.prevent="subclassSelected = subclass">{{
                subclass.label
              }}</a>
            </td>
            <td>{{ subclass.description }}</td>
          </tr>
        </table>
      </template>
    </LayoutModal>
    <EditModal
      v-else-if="isModalShown"
      :id="id + 'add-modal'"
      :tableId="subclassSelected.id"
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
import LayoutModal from "../layout/LayoutModal.vue";
import IconAction from "../forms/IconAction.vue";
import Client from "../../client/client";

import { ref, defineAsyncComponent } from "vue";
import { ITableMetaData } from "meta-data-utils";
const EditModal = defineAsyncComponent(() => import("../forms/EditModal.vue"));

withDefaults(
  defineProps<{
    id: string;
    tableId: string;
    schemaId: string;
    label?: string;
    defaultValue?: Record<string, any>;
    visibleColumns?: any[] | null;
  }>(),
  { label: "", visibleColumns: null }
);

let isModalShown = ref(false);
let subclassSelected: ITableMetaData = ref(null);

const emit = defineEmits(["close", "update:newRow"]);
const client = Client.newClient(this.schemaId);
const subclasses = [
  await client.fetchTableMetaData(tableId.value),
  ...(await client.fetchSubclassTables(tableId.value)),
];
if (subclasses.length == 1) {
  subclassSelected.value = subclasses.value[0];
}

function handleClose() {
  isModalShown.value = false;
  emit("close");
}
</script>

<docs>
<template>
  <div>
    <label for="row-add-btn-sample"
      >composition of RowButton and EditModal configured for row
      add/insert</label
    >
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
