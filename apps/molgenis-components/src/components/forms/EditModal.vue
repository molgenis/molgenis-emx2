<template>
  <LayoutModal :title="title" :show="isModalShown" @close="handleClose">
    <template #body>
      <RowEdit
        :id="id"
        v-model="rowData"
        :pkey="pkey"
        :tableName="tableName"
        :tableMetaData="tableMetaData"
        :graphqlURL="graphqlURL"
      >
      </RowEdit>
    </template>
    <template #footer>
      <RowEditFooter
        class="modal-footer"
        :id="id + '-footer'"
        :tableName="tableName"
        :errorMessage="errorMessage"
        @cancel="handleClose"
        @saveDraft="handleSaveDraftRequest"
        @save="handleSaveRequest"
      ></RowEditFooter>
    </template>
  </LayoutModal>
</template>

<script>
import Client from "../../client/client.js";
import LayoutModal from "../layout/LayoutModal.vue";
import RowEditFooter from "./RowEditFooter.vue";
import RowEdit from "./RowEdit.vue";

export default {
  name: "EditModal",
  components: { LayoutModal, RowEditFooter, RowEdit },
  data() {
    return {
      rowData: {},
      tableMetaData: {},
      client: null,
      errorMessage: null,
    };
  },
  props: {
    id: {
      type: String,
      required: true,
    },
    tableName: {
      type: String,
      required: true,
    },
    isModalShown: {
      type: Boolean,
      required: true,
    },
    graphqlURL: {
      type: String,
      required: false,
      default: () => "graphql",
    },
    pkey: {
      type: Object,
      required: false,
      default: () => null,
    },
    clone: {
      type: Boolean,
      required: false,
      default: () => false,
    },
    visibleColumns: {
      type: Array,
      required: false,
      default: () => [],
    },
    defaultValue: {
      type: Object,
      required: false,
      default: () => null,
    },
  },
  computed: {
    title() {
      return this.titlePrefix + " " + this.tableName;
    },
    titlePrefix() {
      return this.pkey && this.clone ? "copy" : this.pkey ? "update" : "insert";
    },
  },
  methods: {
    handleSaveRequest() {
      this.save({ ...this.rowData, mg_draft: false });
    },
    handleSaveDraftRequest() {
      this.save({ ...this.rowData, mg_draft: true });
    },
    async save(formData) {
      this.errorMessage = null;
      const result = await this.client
        .saveRowData(formData, this.tableName, this.graphqlURL)
        .catch(this.handleSaveError);
      if (result) {
        this.$emit("close");
      }
    },
    handleSaveError(error) {
      this.errorMessage =
        error.response && error.response.status === 403
          ? "Schema doesn't exist or permission denied. Do you need to Sign In?"
          : error.response.data.errors[0].message;
    },
    handleClose() {
      this.errorMessage = null;
      this.$emit("close");
    },
  },
  async mounted() {
    this.client = Client.newClient(this.graphqlURL);
    this.tableMetaData = await this.client.fetchTableMetaData(this.tableName);
    // todo handle update
    // this.rowData = await this.client.fetchTableDataValues(this.tableName);
  },
};
</script>

<style>
</style>

<docs>
  <template>
    <demo-item label="Edit Modal">
      <button 
        class="btn btn-primary" 
        @click="isModalShown = !isModalShown">
          Show edit {{tableName}}
      </button>
       <select class="ml-5" v-model="tableName">
          <option>Pet</option>
          <option>Order</option>
          <option>Category</option>
          <option>User</option>
        </select>
      <EditModal 
        :key="tableName"
        id="edit-modal" 
        :tableName="tableName" 
        :isModalShown="isModalShown" 
        :graphqlURL="graphqlURL"
        @close="isModalShown = false"
      />
    </demo-item>
  </template>
  <script>
  export default {
    data: function () {
      return {
        tableName: "Pet",
        isModalShown: false,
        graphqlURL: "/pet store/graphql"
      };
    },
    methods: {
      
    }
  };
  </script>
</docs>