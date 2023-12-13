<template>
  <div>
    <RowButton type="clone" @clone="isModalShown = true" />
    <EditModal
      v-if="isModalShown"
      :id="id + 'edit-modal'"
      :tableId="tableId"
      :pkey="pkey"
      :clone="true"
      :isModalShown="isModalShown"
      :schemaId="schemaId"
      @close="handleClose"
    />
  </div>
</template>

<script>
import RowButton from "./RowButton.vue";
import EditModal from "../forms/EditModal.vue";

export default {
  name: "RowButtonClone",
  components: { RowButton, EditModal },
  props: {
    id: {
      type: String,
      required: true,
    },
    tableId: {
      type: String,
      required: true,
    },
    pkey: {
      type: Object,
      required: true,
    },
    schemaId: {
      type: String,
      required: false,
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
    <label for="row-clone-btn-sample">composition of RowButton and EditModal configured for row clone</label>
    <div>
      <RowButtonClone
          id="row-clone-btn-sample"
          tableId="Pet"
          :pkey="{name: 'pooky'}"
          schemaId="pet store"
      />
    </div>
  </div>
</template>
</docs>
