<template>
  <span>
    <RowButton type="edit" @edit="isModalShown = true" />
    <EditModal
      v-if="isModalShown"
      :id="id + 'edit-modal'"
      :tableName="tableName"
      :pkey="pkey"
      :isModalShown="isModalShown"
      :schemaName="schemaName"
      @close="handleClose"
    />
  </span>
</template>

<script>
import RowButton from "./RowButton.vue";
export default {
  name: "RowButtonEdit",
  components: { RowButton },
  props: {
    id: {
      type: String,
      required: true,
    },
    tableName: {
      type: String,
      required: true,
    },
    schemaName: {
      type: String,
      required: false,
    },
    pkey: {
      type: Object,
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
          tableName="Pet"
          :pkey="{name: 'pooky'}"
          schemaName="pet store"
        />
      </div>
    </div>
  </template>
</docs>
