<template>
  <div>
    <RowButton type="clone" @clone="isModalShown = true" />
    <EditModal
      :id="id + 'edit-modal'"
      :tableName="tableName"
      :pkey="pkey"
      :clone="true"
      :isModalShown="isModalShown"
      :graphqlURL="graphqlURL"
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
    <label for="row-clone-btn-sample">composition of RowButton and EditModal configured for row clone</label>
    <div>
      <RowButtonClone
          id="row-clone-btn-sample"
          tableName="Pet"
          :pkey="{name: 'pooky'}"
          graphqlURL="/pet store/graphql"
      />
    </div>
  </div>
</template>
</docs>
