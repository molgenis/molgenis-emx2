<template>
  <span>
    <RowButton type="add" @add="isModalShown = true" />
    <EditModal
      v-if="isModalShown"
      :id="id + 'add-modal'"
      :tableName="tableName"
      :isModalShown="isModalShown"
      :schemaName="schemaName"
      :defaultValue="defaultValue"
      :visibleColumns="visibleColumns"
      :applyDefaultValues="true"
      @close="handleClose"
    />
  </span>
</template>

<script>
import RowButton from "./RowButton.vue";

export default {
  name: "RowButtonAdd",
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
          tableName="Pet"
          schemaName="pet store"
      />
    </div>
  </div>
</template>
</docs>
