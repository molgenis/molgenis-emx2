<template>
  <div>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <div v-else style="text-align: center">
      <form
        v-if="showHeaderIfNeeded"
        class="form-inline justify-content-between mb-2 bg-white"
      >
        <InputSearch id="input-search" v-if="lookupTableName" v-model="searchTerms" />
        <Pagination class="ml-2" v-model="page" :limit="limit" :count="count" />
        <span>Selection box</span>
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
              :reload="reload"
              :grapqlURL="graphqlURL"
            />
            <span>add</span>
          </template>
          <template v-slot:rowheader="slotProps">
            <slot
              name="rowheader"
              :row="slotProps.row"
              :metadata="tableMetadata"
              :rowkey="slotProps.rowkey"
            />
            <span>edit</span>
            <span>delete</span>
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
      required: true
    },
    graphqlURL: {
      type: String,
      required: true
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
  },
  data: function () {
    return {
      page: 1,
      limit: 20,
      count: 0,
      loading: true,
      graphqlError: null,
      searchTerms: ''
    };
  },
  computed: {
    showHeaderIfNeeded() {
      return this.showHeader || this.count > this.limit;
    },
    columnsVisible() {
      return this.tableMetadata.columns.filter(
        (c) =>
          (this.showColumns == null && !c.name.startsWith("mg_")) ||
          (this.showColumns != null && this.showColumns.includes(c.name))
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
  },
  watch: {
    page() {
      this.loading = true;
      this.offset = this.limit * (this.page - 1);
      this.reload();
    },
  },
  async mounted () {
    const client = Client.newClient(this.graphqlURL);
    const remoteMetaData = await client.fetchMetaData();
    this.tableMetadata = remoteMetaData.tables.find(t => t.name === this.lookupTableName)
    const gqlResponse = await client.fetchTableData(this.lookupTableName)
    this.data = gqlResponse[this.lookupTableName];
    this.loading = false;
  }
};
</script>