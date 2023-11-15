<template>
  <div>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <div v-else style="text-align: center">
      <form
        v-if="showHeaderIfNeeded"
        class="form-inline justify-content-between mb-2 bg-white"
      >
        <InputSearch id="input-search" v-if="tableId" v-model="searchTerms" />
        <Pagination class="ml-2" v-model="page" :limit="limit" :count="count" />
      </form>
      <Spinner v-if="loading" />
      <div v-else>
        <TableMolgenis
          :schemaId="schemaId"
          :selection="selection"
          :tableMetadata="tableMetadata"
          :columns="columnsVisible"
          :data="data"
          :showSelect="showSelect"
          @update:selection="$emit('update:selection', $event)"
          @select="select"
          @deselect="deselect"
        >
          <template v-slot:header>
            <slot name="colheader" v-bind="$props" />
            <label>{{ count }} records found</label>
          </template>
          <template v-slot:rowcolheader>
            <RowButtonAdd
              v-if="canEdit"
              :id="'row-button-add-' + tableId"
              :tableId="tableId"
              :schemaId="schemaId"
              @close="loadData"
              class="d-inline p-0"
            />
          </template>
          <template v-slot:colheader>
            <slot
              name="colheader"
              v-bind="$props"
              :canEdit="canEdit"
              :reload="loadData"
              :schemaId="schemaId"
            />
          </template>
          <template v-slot:rowheader="slotProps">
            <slot
              name="rowheader"
              :row="slotProps.row"
              :metadata="tableMetadata"
              :rowKey="slotProps.rowKey"
            />
            <RowButtonEdit
              v-if="canEdit"
              :id="'row-button-edit-' + tableId"
              :tableId="tableId"
              :schemaId="schemaId"
              :pkey="slotProps.rowKey"
              @close="loadData"
              class="text-left"
            />
            <RowButtonDelete
              v-if="canEdit"
              :id="'row-button-del-' + tableId"
              :tableId="tableId"
              :schemaId="schemaId"
              :pkey="slotProps.rowKey"
              @close="loadData"
            />
          </template>
        </TableMolgenis>
      </div>
    </div>
  </div>
</template>

<script>
import TableMolgenis from "./TableMolgenis.vue";
import MessageError from "../forms/MessageError.vue";
import InputSearch from "../forms/InputSearch.vue";
import Pagination from "./Pagination.vue";
import Spinner from "../layout/Spinner.vue";
import Client from "../../client/client.ts";
import RowButtonAdd from "./RowButtonAdd.vue";
import RowButtonEdit from "./RowButtonEdit.vue";
import RowButtonDelete from "./RowButtonDelete.vue";

export default {
  name: "TableSearch",
  components: {
    TableMolgenis,
    MessageError,
    InputSearch,
    Pagination,
    Spinner,
    RowButtonAdd,
    RowButtonEdit,
    RowButtonDelete,
  },
  props: {
    tableId: {
      type: String,
      required: true,
    },
    schemaId: {
      type: String,
      required: true,
    },
    /** two-way binding of the selection */
    selection: { type: Array, default: () => [] },
    /** enables checkbox to select rows */
    showSelect: {
      type: Boolean,
      default: false,
    },
    showHeader: {
      type: Boolean,
      default: true,
    },
    showColumns: {
      type: Array,
    },
    canEdit: {
      type: Boolean,
      default: false,
    },
    filter: {
      type: Object,
      required: false,
    },
  },
  data: function () {
    return {
      page: 1,
      limit: 20,
      count: 0,
      loading: true,
      graphqlError: null,
      searchTerms: "",
    };
  },
  computed: {
    showHeaderIfNeeded() {
      return this.showHeader || this.count > this.limit;
    },
    columnsVisible() {
      return this.tableMetadata.columns.filter(
        (column) =>
          (this.showColumns == null && !column.id.startsWith("mg_")) ||
          (this.showColumns != null && this.showColumns.includes(column.id))
      );
    },
  },
  methods: {
    select(value) {
      this.$emit("select", value);
    },
    deselect(value) {
      this.$emit("deselect", value);
    },
    async loadData() {
      this.loading = true;
      const queryOptions = {
        limit: this.limit,
        offset: this.limit * (this.page - 1),
        searchTerms: this.searchTerms,
        filter: this.filter,
      };

      const client = Client.newClient(this.schemaId);
      const gqlResponse = await client
        .fetchTableData(this.tableId, queryOptions)
        .catch(() => (this.graphqlError = "Failed to load data"));
      this.tableMetadata = await client.fetchTableMetaData(this.tableId);
      this.data = gqlResponse[this.tableId];
      this.count = gqlResponse[`${this.tableId}_agg`].count;
      this.loading = false;
    },
  },
  watch: {
    page() {
      this.loadData();
    },
    searchTerms() {
      this.loadData();
    },
  },
  async mounted() {
    this.loadData();
  },
};
</script>

<docs>
<template>
  <demo-item>
    <div class="border-bottom mb-3 p-2">
      <h5>synced demo props: </h5>
      <div>
        <label for="canEdit" class="pr-1">can edit: </label>
        <input type="checkbox" id="canEdit" v-model="canEdit">
      </div>
      <div>
        <label for="canSelect" class="pr-1">can select: </label>
        <input type="checkbox" id="canSelect" v-model="canSelect">
      </div>
      <div v-show="canSelect">
      {{ selected }}
      </div>
    </div>
    <table-search
        id="my-search-table"
        v-model:selection="selected"
        v-model:columns="columns"
        :tableId="'Pet'"
        schemaId="pet store"
        :canEdit="canEdit"
        :showSelect="canSelect"
    >
    </table-search>
  </demo-item>
</template>

<script>
  export default {
    data() {
      return {
        selected: [],
        columns: [
          {id: 'col1', name: 'col1', columnType: 'STRING', key: 1},
          {
            id: 'ref1',
            name: 'ref1',
            columnType: 'REF',
            refColumns: ['firstName', 'lastName'],
          },
          {
            id: 'ref_arr1',
            name: 'ref_arr1',
            columnType: 'REF_ARRAY',
            refColumns: ['firstName', 'lastName'],
          },
        ],
        remoteSelected: [],
        remoteColumns: [],
        remoteTableData: null,
        canEdit: false,
        canSelect: false,
      };
    },
  };
</script>
</docs>
