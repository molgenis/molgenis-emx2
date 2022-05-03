<template>
  <div>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <div v-else style="text-align: center">
      <form
        v-if="showHeaderIfNeeded"
        class="form-inline justify-content-between mb-2 bg-white"
      >
        <InputSearch
          id="input-search"
          v-if="lookupTableName"
          v-model="searchTerms"
        />
        <Pagination class="ml-2" v-model="page" :limit="limit" :count="count" />
      </form>
      <Spinner v-if="loading" />
      <div v-else>
        <TableMolgenis
          :selection="selection"
          @update:selection="$emit('update:selection', $event)"
          :metadata="tableMetadata"
          :columns="columnsVisible"
          :data="data"
          :showSelect="showSelect"
          @select="select"
          @deselect="deselect"
        >
          <template v-slot:header>
            <slot name="colheader" v-bind="$props" />
            <label>{{ count }} records found</label>
          </template>
          <template v-slot:colheader="slotProps">
            <slot
              name="colheader"
              v-bind="$props"
              :canEdit="canEdit"
              :reload="loadData"
              :grapqlURL="graphqlURL"
            />
          </template>
          <template v-slot:rowheader="slotProps">
            <slot
              name="rowheader"
              :row="slotProps.row"
              :metadata="tableMetadata"
              :rowkey="slotProps.rowkey"
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
import Client from "../../client/client.js";

export default {
  components: {
    TableMolgenis,
    MessageError,
    InputSearch,
    Pagination,
    Spinner,
  },
  props: {
    lookupTableName: {
      type: String,
      required: true,
    },
    graphqlURL: {
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
          (this.showColumns == null && !column.name.startsWith("mg_")) ||
          (this.showColumns != null && this.showColumns.includes(column.name))
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
      };

      const client = Client.newClient(this.graphqlURL);
      const remoteMetaData = await client
        .fetchMetaData()
        .catch(() => (this.graphqlError = "Failed to load meta data"));
      const gqlResponse = await client
        .fetchTableData(this.lookupTableName, queryOptions)
        .catch(() => (this.graphqlError = "Failed to load data"));

      this.tableMetadata = remoteMetaData.tables.find(
        (table) => table.name === this.lookupTableName
      );
      this.data = gqlResponse[this.lookupTableName];
      this.count = gqlResponse[`${this.lookupTableName}_agg`].count;
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
    <table-search
        id="my-search-table"
        :selection.sync="selected"
        :columns.sync="columns"
        :lookupTableName="'Pet'"
        :showSelect="false"
        :graphqlURL="'/pet store/graphql'"
        :canEdit="true"
        @select="click"
        @deselect="click"
        @click="click"
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
          {id: "col1", name: "col1", columnType: "STRING", key: 1},
          {
            id: "ref1",
            name: "ref1",
            columnType: "REF",
            refColumns: ["firstName", "lastName"],
          },
          {
            id: "ref_arr1",
            name: "ref_arr1",
            columnType: "REF_ARRAY",
            refColumns: ["firstName", "lastName"],
          },
        ],
        remoteSelected: [],
        remoteColumns: [],
        remoteTableData: null,
      };
    },
    methods: {
      click(value) {
        alert("click " + JSON.stringify(value));
      },
    },
  };
</script>
</docs>
