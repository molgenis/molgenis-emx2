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
      :visibleColumns="visibleColumns"
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
          tableName="Pet"
          :pkey="{name: 'pooky'}"
          schemaName="pet store"
      />
    </div>
  </div>
  <p>With only few columns visible</p>
  <div>
    <label for="row-edit-btn-sample">composition of RowButton and EditModal configured for row edit/update</label>
    <div>
      <RowButtonEdit
          id="row-edit-btn-sample"
          tableName="Pet"
          :pkey="{name: 'pooky'}"
          schemaName="pet store"
          :visibleColumns="['name']"
      />
    </div>
  </div>
</template>
</docs>
