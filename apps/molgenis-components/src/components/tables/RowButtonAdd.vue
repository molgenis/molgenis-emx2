<template>
  <span>
    <RowButton type="add" @add="isModalShown = true" />
    <EditModal
      v-if="isModalShown"
      :id="id + 'add-modal'"
      :tableName="tableName"
      :isModalShown="isModalShown"
      :schemaName="schemaName"
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
