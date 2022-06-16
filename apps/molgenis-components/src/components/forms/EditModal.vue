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
        :clone="clone"
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
      const action =
        this.pkey && !this.clone ? "updateDataRow" : "insertDataRow";
      const result = await this.client[action](
        formData,
        this.tableName,
        this.graphqlURL
      ).catch(this.handleSaveError);
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
    async fetchRowData() {
      // build id filter
      const filter = this.tableMetaData.columns
        .filter((column) => column.key === 1)
        .reduce((accum, column) => {
          accum[column.id] = { equals: this.pkey[column.id] };
          return accum;
        }, {});

      const resultArray = await this.client.fetchTableDataValues(
        this.tableName,
        { filter }
      );

      if (!resultArray.length || resultArray.length !== 1) {
        this.errorMessage = `Error, unable to fetch data for this row (${this.pkey})`;
      }

      return resultArray[0];
    },
    handleClose() {
      this.errorMessage = null;
      this.$emit("close");
    },
  },
  async mounted() {
    this.client = Client.newClient(this.graphqlURL);
    this.tableMetaData = await this.client.fetchTableMetaData(this.tableName);

    if (this.pkey) {
      this.rowData = await this.fetchRowData();

      if (this.clone) {
        // in case of clone, remove the key columns from the row data
        const keyColumnsNames = this.tableMetaData.columns
          .filter((column) => column.key === 1)
          .map((column) => column.name);
          
        this.rowData = Object.keys(this.rowData)
          .filter((key) => !keyColumnsNames.includes(key))
          .reduce((obj, key) => {
            obj[key] = this.rowData[key];
            return obj;
          }, {});
      }
    }
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
          Show {{demoMode}} {{tableName}}
      </button>

      <label for="table-selector" class="ml-5 pr-1">table</label>
      <select id="table-selector"  v-model="tableName">
        <option>Pet</option>
        <option>Order</option>
        <option>Category</option>
        <option>User</option>
      </select>

      <input type="radio" id="insert" value="insert" v-model="demoMode" class="ml-5">
      <label for="insert" class="pl-1">Insert</label>
    
      <input type="radio" id="update" value="update" v-model="demoMode" class="ml-1 pr-1">
      <label for="update" class="pl-1">Update</label>
   
      <input type="radio" id="clone" value="clone" v-model="demoMode" class="ml-1 pr-1">
      <label for="clone" class="pl-1">Clone</label>

      <EditModal 
        :key="tableName + demoKey + demoMode"
        id="edit-modal" 
        :tableName="tableName" 
        :pkey="demoKey"
        :clone="demoMode === 'clone'"
        :isModalShown="isModalShown" 
        :graphqlURL="graphqlURL"
        @close="isModalShown = false"
      />

      <div v-if="log.length" class="py-3">
        <label>showCase log:</label>
        <ul>
          <li v-for="(msg, idx) in log" :key="idx">{{msg}}</li>
        </ul>
      </div>
    </demo-item>
  </template>
  <script>
  export default {
    data: function () {
      return {
        tableName: "Pet",
        demoMode: "insert", // one of [insert, update, clone] 
        demoKey: null, // empty in case of insert 
        isModalShown: false,
        graphqlURL: "/pet store/graphql",
        log: []
      };
    },
    methods: {
      async reload () {
        const client = this.$Client.newClient(this.graphqlURL);
        const tableMetaData = await client.fetchTableMetaData(this.tableName);
        const rowData = await client.fetchTableDataValues(this.tableName);
        this.demoKey = this.$utils.getPrimaryKey(rowData[0], tableMetaData)
        this.log.unshift(`reloaded ${this.tableName} for ${this.demoMode}`);
      },
      onModeChange () {
        if(this.demoMode !== 'insert') {
          this.reload();
        } else {
          this.log.unshift(`cleared  ${this.tableName} pKey for insert`);
          this.demoKey = null;
        }
      }
    },
    watch: {
      demoMode () {
        this.onModeChange()
      }
    }
  };
  </script>
</docs>