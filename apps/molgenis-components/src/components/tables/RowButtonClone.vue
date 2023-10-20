<template>
  <div>
    <RowButton type="clone" @clone="isModalShown = true" />
    <EditModal
      v-if="isModalShown"
      :id="id + 'edit-modal'"
      :tableName="tableName"
      :pkey="pkey"
      :clone="true"
      :isModalShown="isModalShown"
      :schemaName="schemaName"
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
    tableName: {
      type: String,
      required: true,
    },
    pkey: {
      type: Object,
      required: true,
    },
    schemaName: {
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
          tableName="Pet"
          :pkey="{name: 'pooky'}"
          schemaName="petStore"
      />
    </div>
  </div>
</template>
</docs>
