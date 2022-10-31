<template>
  <div>
    <RowButton type="add" @add="isModalShown = true" />
    <EditModal
      :id="id + 'add-modal'"
      :tableName="tableName"
      :isModalShown="isModalShown"
      :graphqlURL="graphqlURL"
      @close="handleClose"
    />
  </div>
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
    graphqlURL: {
      type: String,
      required: false,
      default: () => "graphql",
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
          graphqlURL="/pet store/graphql"
        />
      </div>
    </div>
  </template>
</docs>
