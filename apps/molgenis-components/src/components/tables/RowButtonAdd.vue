<template>
  <span>
    <RowButton type="add" @add="isModalShown = true" />
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
      @update:newRow="(event) => $emit('update:newRow', event)"
    />
  </span>
</template>

<script>
import RowButton from "./RowButton.vue";
import { defineAsyncComponent } from "vue";

export default {
  name: "RowButtonAdd",
  components: {
    RowButton,
    EditModal: defineAsyncComponent(() => import("../forms/EditModal.vue")),
  },
  props: {
    id: {
      type: String,
      required: true,
    },
    tableId: {
      type: String,
      required: true,
    },
    schemaId: {
      type: String,
      required: false,
    },
    defaultValue: {
      type: Object,
      required: false,
    },
    visibleColumns: {
      type: Array,
      required: false,
      default: () => null,
    },
  },
  data() {
    return {
      isModalShown: false,
    };
  },
  methods: {
    handleClose() {
      this.isModalShown = false;
      this.$emit("close");
    },
  },
};
</script>

<docs>
<template>
  <div>
    <label for="row-add-btn-sample">composition of RowButton and EditModal configured for row add/insert</label>
    <div>
      <RowButtonAdd
          id="row-add-btn-sample"
          tableId="Pet"
          schemaId="pet store"
      />
    </div>
  </div>
</template>
</docs>
