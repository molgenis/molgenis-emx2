<template>
  <div>
    <RowButton type="edit" @edit="isModalShown = true" />
    <EditModal
      :id="id + 'edit-modal'"
      :tableName="tableName"
      :pkey="pkey"
      :isModalShown="isModalShown"
      :graphqlURL="graphqlURL"
      @close="isModalShown = false"
    />
  </div>
</template>

<script>
import RowButton from "./RowButton.vue";
import EditModal from "../forms/EditModal.vue";

export default {
  name: "RowButtonEdit",
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
    pkey: {
      type: Object,
    },
  },
  data() {
    return {
      isModalShown: false,
    };
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
          graphqlURL="/pet store/graphql"
      />
    </div>
  </div>
</template>
</docs>
