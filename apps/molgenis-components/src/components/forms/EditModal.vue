<template>
  <LayoutModal :title="title" :show="isModalShown" @close="$emit('close')">
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
        :tableName="tableName"
        :id="id + '-footer'"
        class="modal-footer"
        @cancel="$emit('close')"
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
      return this.tableName;
    },
  },
  methods: {
    async handleSaveRequest() {
      await this.client
        .saveRowData(
          { ...this.rowData, mg_draft: false },
          this.tableName,
          this.graphqlURL
        )
        .catch(this.handleSaveError);
      this.$emit("close");
    },
    async handleSaveDraftRequest() {
      await this.client
        .saveRowData(
          { ...this.rowData, mg_draft: true },
          this.tableName,
          this.graphqlURL
        )
        .catch(this.handleSaveError);
      this.$emit("close");
    },
    handleSaveError(error) {
      if (error.status === 403) {
        this.graphqlError =
          "Schema doesn't exist or permission denied. Do you need to Sign In?";
      } else {
        this.graphqlError = error.errors[0].message;
      }
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
          Show edit modal
      </button>
      <EditModal 
        id="edit-modal" 
        tableName="Pet" 
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
        value: null,
        isModalShown: false,
        graphqlURL: "/pet store/graphql"
      };
    },
    methods: {
      
    }
  };
  </script>
</docs>