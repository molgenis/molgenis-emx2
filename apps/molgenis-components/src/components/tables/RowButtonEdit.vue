<template>
  <span>
    <RowButton type="edit" @edit="isModalShown = true" />
    <EditModal
      v-if="isModalShown"
      :id="id + 'edit-modal'"
      :tableId="tableId"
      :pkey="pkey"
      :isModalShown="isModalShown"
      :schemaId="schemaId"
      :visibleColumns="visibleColumns"
      @close="handleClose"
    />
  </span>
</template>

<script>
import RowButton from "./RowButton.vue";
import { defineAsyncComponent } from "vue";

export default {
  name: "RowButtonEdit",
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
    pkey: {
      type: Object,
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
    <label for="row-edit-btn-sample">composition of RowButton and EditModal configured for row edit/update</label>
    <div>
      <RowButtonEdit
          id="row-edit-btn-sample"
          tableId="Pet"
          :pkey="{name: 'pooky'}"
          schemaId="pet store"
      />
    </div>
  </div>
  <p>With only few columns visible</p>
  <div>
    <label for="row-edit-btn-sample">composition of RowButton and EditModal configured for row edit/update</label>
    <div>
      <RowButtonEdit
          id="row-edit-btn-sample"
          tableId="Pet"
          :pkey="{name: 'pooky'}"
          schemaId="pet store"
          :visibleColumns="['name']"
      />
    </div>
  </div>
</template>
</docs>
