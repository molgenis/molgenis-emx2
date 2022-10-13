<template>
  <span>
    <RowButton type="add" @add="isModalShown = true" />
    <EditModal
      :id="id + 'add-modal'"
      :tableName="tableName"
      :isModalShown="isModalShown"
      :graphqlURL="graphqlURL"
      :defaultValue="defaultValue"
      @close="handleClose"
    />
  </span>
</template>

<script>
import RowButton from "./RowButton.vue";
import EditModal from "../forms/EditModal.vue";

export default {
  name: "RowButtonAdd",
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
    graphqlURL: {
      type: String,
      required: false,
      default: () => "graphql",
    },
    defaultValue: {
      type: Object,
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
          graphqlURL="/pet store/graphql"
      />
    </div>
  </div>
</template>
</docs>
