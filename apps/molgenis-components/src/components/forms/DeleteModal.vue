<template>
  <LayoutModal :title="title" @close="$emit('close')">
    <template v-slot:body>
      <div>
        Delete
        <strong>{{ tableName }}({{ pkeyAsString }})</strong>
        <br />Are you sure?
        <br />
      </div>
    </template>
    <template v-slot:footer>
      <ButtonAlt @click="$emit('close')">Close</ButtonAlt>
      <ButtonAction @click="$emit('executeDelete')"> Delete </ButtonAction>
    </template>
  </LayoutModal>
</template>

<script>
import LayoutModal from "../layout/LayoutModal.vue";
import ButtonAlt from "./ButtonAlt.vue";
import ButtonAction from "./ButtonAction.vue";
import { flattenObject } from "../utils";

export default {
  name: "DeleteModal",
  components: {
    LayoutModal,
    ButtonAlt,
    ButtonAction,
  },
  props: {
    tableName: {
      type: String,
      required: true,
    },
    pkey: Object,
  },
  computed: {
    title() {
      return `Delete from ${this.tableName}`;
    },
    pkeyAsString() {
      return flattenObject(this.pkey);
    },
    tableId() {
      return this.table.replaceAll(" ", "_");
    },
  },
};
</script>