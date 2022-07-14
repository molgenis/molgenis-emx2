<template>
  <div>
    <h1 v-if="showHeader">{{ tableName }} Explorer</h1>

    <p v-if="showHeader && tableMetadata">
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
      <InputSearch id="explorer-table-search" class="navbar-nav" v-model="searchTerms" />
      <Pagination
        class="navbar-nav"
        v-model="page"
        :limit="limit"
        :count="count"
      />
      <div class="btn-group m-0" v-if="view != View.RECORD">
        <span class="btn">Rows per page:</span>
        <InputSelect
          id="explorer-table-page-limit-select"
          :value="limit"
          :options="[10, 20, 50, 100]"
          :clear="false"
          @input="setlimit($event)"
          class="mb-0"
        />
        <SelectionBox v-if="showSelect" :selection.sync="selectedItems" />
        <!-- <TableSettings
          v-if="canManage"
          :tableName="table"
          :cardTemplate.sync="cardTemplate"
          :recordTemplate.sync="recordTemplate"
          :graphqlURL="graphqlURL"
        /> -->
      </div>
    </div>
  </div>
</template>


<script>
import Client from "../../client/client.js";
import ShowHide from "./ShowHide.vue";
import Pagination from "./Pagination.vue";
import ButtonDropdown from "../forms/ButtonDropdown.vue";
import IconAction from "../forms/IconAction.vue";
import InputSearch from "../forms/InputSearch.vue";
import InputSelect from "../forms/InputSelect.vue";
import SelectionBox from "./SelectionBox.vue";

const View = { TABLE: "table", CARDS: "cards", RECORD: "record", EDIT: "edit" };

export default {
  name: "ExplorerTable",
  components: {
    ShowHide,
    Pagination,
    ButtonDropdown,
    IconAction,
    InputSearch,
    InputSelect,
    SelectionBox,
  },
  data() {
    return {
      tableMetadata: null,
      client: null,
      columns: [],
      searchTerms: null,
      count: null,
      page: null,
      view: View.TABLE
    };
  },
  props: {
    tableName: {
      type: String,
      required: true,
    },
    graphqlURL: {
      type: String,
      default: "graphql",
    },
    value: {
      type: Array,
      default: () => [],
    },
    showSelect: {
      type: Boolean,
      default: () => false,
    },
    showHeader: {
      type: Boolean,
      default: () => true,
    },
    showFilters: {
      type: Array,
      default: () => [],
    },
    showColumns: {
      type: Array,
      default: () => [],
    },
    showView: {
      type: String,
      default: View.TABLE,
    },
    showPage: {
      type: Number,
      default: 1,
    },
    showLimit: {
      type: Number,
      default: 20,
    },
    conditions: {
      type: Object,
      default: () => ({}),
    },
    showOrderBy: {
      type: String,
    },
    showOrder: {
      type: String,
      default: () => "ASC",
    },
  },
  computed: {
    View () { return View},
    viewIcon() {
      if (this.view === View.CARDS) {
        return "list-alt";
      } else if (this.view === View.TABLE) {
        return "th";
      } else {
        return "th-list";
      }
    },
    limit() {
      return this.view === View.TABLE || this.view === View.TABLE
        ? this.showLimit
        : 1;
    },
  },
  methods: {
    toggleView() {
      if (this.view === View.TABLE) {
        this.view = View.CARDS;
      } else if (this.view === View.CARDS) {
         this.view = View.RECORD;
      } else {
         this.view = View.TABLE;
      }
    },
    emitColumns() {
      let columns = this.columns
        .filter((c) => c.showColumn && c.columnType !== "HEADING")
        .map((c) => c.name);
      this.$emit("update:showColumns", columns);
    },
    emitFilters() {
      this.$emit(
        "update:showFilters",
        this.columns
          .filter((c) => c.showFilter && c.columnType !== "HEADING")
          .map((c) => c.name)
      );
    },
  },
  mounted: async function () {
    this.client = Client.newClient(this.graphqlURL);
    this.tableMetadata = await this.client.fetchTableMetaData(this.tableName);
    // this.data = await this.client.fetchTableDataValues(this.tableName, { filter: this.graphqlFilter });
  },
};
</script>

<docs>
<template>
  <demo-item>
    <explorer-table 
      id="my-explorer-table"
      tableName="Pet"
      graphqlURL="/pet store/graphql"
    ></explorer-table>
  </demo-item>
</template>
</docs>