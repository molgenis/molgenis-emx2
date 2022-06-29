<template>
  <div class="h-100">
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <div class="bg-white">
      <h1 v-if="showHeader" class="pl-2">
        {{ tableName }}
      </h1>
      <p class="pl-2" v-if="showHeader && tableMetadata">
        {{ tableMetadata.description }}
      </p>
      <div
        class="
          navbar
          pl-0
          ml-0
          shadow-none
          navbar-expand-lg
          justify-content-between
          mb-3
          pt-3
          bg-white
        "
      >
        <div class="btn-group">
          <ShowHide
            class="navbar-nav"
            :columns.sync="columns"
            @update:columns="emitFilters"
            checkAttribute="showFilter"
            :exclude="['HEADING', 'FILE']"
            label="filters"
            icon="filter"
          />
          <ShowHide
            class="navbar-nav"
            :columns.sync="columns"
            @update:columns="emitColumns"
            checkAttribute="showColumn"
            label="columns"
            icon="columns"
            id="showColumn"
            :defaultValue="true"
          />
          <ButtonDropdown label="download" icon="download" v-slot="scope">
            <IconAction
              icon="times"
              class="float-right"
              style="margin-top: -10px; margin-right: -10px"
              @click="scope.close"
            />
            <h6>Download</h6>
            <ButtonAlt :href="'../api/zip/' + table">zip</ButtonAlt>
            <br />
            <ButtonAlt :href="'../api/excel/' + table">excel</ButtonAlt>
            <br />
            <ButtonAlt :href="'../api/jsonld/' + table">jsonld</ButtonAlt>
            <br />
            <ButtonAlt :href="'../api/ttl/' + table">ttl</ButtonAlt>
          </ButtonDropdown>
          <IconAction
            class="ml-2"
            label="view"
            :icon="viewIcon"
            @click="toggleView"
          />
        </div>
        <InputSearch class="navbar-nav" v-model="searchTerms" />
        <Pagination
          class="navbar-nav"
          v-model="page"
          :limit="limit"
          :count="count"
        />
        <div class="btn-group m-0" v-if="view != View.RECORD">
          <span class="btn">Rows per page:</span>
          <InputSelect
            :value="limit"
            :options="[10, 20, 50, 100]"
            :clear="false"
            @input="setlimit($event)"
            class="mb-0"
          />
          <SelectionBox v-if="showSelect" :selection.sync="selectedItems" />
          <TableSettings
            v-if="canManage"
            :tableName="table"
            :cardTemplate.sync="cardTemplate"
            :recordTemplate.sync="recordTemplate"
            :graphqlURL="graphqlURL"
          />
        </div>
      </div>
    </div>
    <TableMolgenis
      v-if="!loading && view == viewOptions.TABLE"
      :selection.sync="selectedItems"
      :columns="columns"
      :table-metadata="tableMetadata"
      :data="tableData"
      :showSelect="showSelect"
      @column-click="onColumnClick"
      @click="$emit('click', $event)"
    >
    </TableMolgenis>
  </div>
</template>

<script>
import Client from "../../client/client.js";
import TableMolgenis from "./TableMolgenis.vue";
import MessageError from "../forms/MessageError.vue";

const viewOptions = {
  TABLE: "table",
  CARDS: "cards",
  RECORD: "record",
  EDIT: "edit",
};

export default {
  name: "ExplorerTable",
  components: { TableMolgenis, MessageError },
  props: {
    tableName: {
      type: String,
      required: true,
    },
    graphqlURL: {
      type: String,
      required: false,
      default: "graphql",
    },
    showHeader: {
      type: Boolean,
      required: false,
      default: () => true,
    },
    showSelect: {
      type: Boolean,
      required: false,
      default: () => false,
    },
  },
  data() {
    return {
      tableMetadata: null,
      tableData: null,
      loading: false,
      cardTemplate: null,
      recordTemplate: null,
      selectedItems: [],
      showSubclass: false,
      //a copy of column metadata used to show/hide filters and columns
      viewOptions,
      view: viewOptions.TABLE,
      page: 1,
      order: "ASC",
      orderByColumn: null,
      graphqlError: "",
    };
  },
  computed: {
    columns() {
      return !this.tableMetadata ? [] : this.tableMetadata.columns;
    },
  },
  methods: {
    onColumnClick() {
      alert("onColumnClick");
    },
  },
  async mounted() {
    const client = Client.newClient(this.graphqlURL);
    this.loading = true;
    this.tableMetadata = await client
      .fetchTableMetaData(this.tableName)
      .catch((e) => (this.graphqlError = e));
    this.tableData = await client
      .fetchTableDataValues(this.tableName)
      .catch((e) => (this.graphqlError = e));
    this.loading = false;
  },
};
</script>

<docs>
<template>
  <demo-item>
    <explorer-table 
    tableName="Pet"
    graphqlURL="/pet store/graphql"
    ></explorer-table>
  </demo-item>
</template>
</docs>
