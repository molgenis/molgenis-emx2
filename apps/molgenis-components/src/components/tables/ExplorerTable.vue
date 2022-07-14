<template>
  <div>
    <h1 v-if="showHeader">{{ tableName }} Explorer</h1>

    <p v-if="showHeader && tableMetadata">
      {{ tableMetadata.description }}
    </p>

    <div class="btn-toolbar">
      <div class="btn-group">
        <ShowHide
          :columns.sync="columns"
          @update:columns="emitFilters"
          checkAttribute="showFilter"
          :exclude="['HEADING', 'FILE']"
          label="filters"
          icon="filter"
        />

        <ShowHide
          :columns.sync="columns"
          @update:columns="emitColumns"
          checkAttribute="showColumn"
          label="columns"
          icon="columns"
          id="showColumn"
          :defaultValue="true"
        />

        <ButtonDropdown label="download" icon="download" v-slot="scope">
          <form class="px-4 py-3" style="min-width: 15rem">
            <IconAction icon="times" @click="scope.close" class="float-right" />

            <h6>Download</h6>
            <ul>
              <li><ButtonAlt :href="'../api/zip/' + table">zip</ButtonAlt></li>
              <li>
                <ButtonAlt :href="'../api/excel/' + table">excel</ButtonAlt>
              </li>
              <li>
                <ButtonAlt :href="'../api/jsonld/' + table">jsonld</ButtonAlt>
              </li>
              <li><ButtonAlt :href="'../api/ttl/' + table">ttl</ButtonAlt></li>
            </ul>
          </form>
        </ButtonDropdown>

        <!-- <IconAction
          class="ml-2"
          label="view"
          :icon="viewIcon"
          @click="toggleView"
        /> -->
      </div>
      <InputSearch id="explorer-table-search" v-model="searchTerms" />
      <Pagination v-model="page" :limit="limit" :count="count" />
      <div class="btn-group m-0" v-if="view != View.RECORD">
        <span class="btn">Rows per page:</span>
        <InputSelect
          id="explorer-table-page-limit-select"
          :value="limit"
          :options="[10, 20, 50, 100]"
          :clear="false"
          @input="setLimit($event)"
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

    <div class="d-flex">
      <div v-if="countFilters" class="col-3 pl-0"></div>
      <div
        class="flex-grow-1 pr-0 pl-0"
        :class="countFilters > 0 ? 'col-9' : 'col-12'"
      >
        <FilterWells
          :filters.sync="columns"
          @update:filters="emitConditions"
          class="border-top pt-3 pb-3"
        />
        <div v-if="loading">
          <Spinner />
        </div>
        <!-- <TableCards
            v-if="!loading && view == View.CARDS"
            :data="dataRows"
            :columns="columns"
            :table-name="table"
            @reload="reload"
            :canEdit="canEdit"
            @click="$emit('click', $event)"
            :template="cardTemplate"
          /> -->
        <!-- <RecordCard
            v-if="!loading && view == View.RECORD"
            :data="dataRows"
            :table-name="table"
            :columns="columns"
            :canEdit="canEdit"
            @click="$emit('click', $event)"
            :template="recordTemplate"
          /> -->
        <TableMolgenis
          v-if="!loading && view == View.TABLE"
          :selection.sync="selectedItems"
          :columns.sync="columns"
          :table-metadata="tableMetadata"
          :data="dataRows"
          :showSelect="showSelect"
          @column-click="onColumnClick"
          @click="$emit('click', $event)"
        >
          <template v-slot:header>
            <label>{{ count }} records found</label>
          </template>
          <template v-slot:colheader="slotProps">
            <RowButton
              v-if="canEdit && !slotProps.col"
              type="add"
              :table="table"
              :graphqlURL="graphqlURL"
              @close="reload"
              class="d-inline p-0"
            />
            <IconAction
              v-if="slotProps.col && orderByColumn == slotProps.col.id"
              :icon="order == 'ASC' ? 'sort-alpha-down' : 'sort-alpha-up'"
              class="d-inline p-0"
            />
          </template>
          <template v-slot:rowheader="slotProps">
            <RowButton
              v-if="canEdit"
              type="add"
              :table="table"
              :graphqlURL="graphqlURL"
              :pkey="getPkey(slotProps.row)"
              @close="reload"
            />
            <RowButton
              v-if="canEdit"
              type="edit"
              :table="table"
              :graphqlURL="graphqlURL"
              :pkey="getPkey(slotProps.row)"
              @close="reload"
            />
            <RowButton
              v-if="canEdit"
              type="delete"
              :table="table"
              :graphqlURL="graphqlURL"
              :pkey="getPkey(slotProps.row)"
              @close="reload"
            />
          </template>
        </TableMolgenis>
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
import Spinner from "../layout/Spinner.vue";
import TableMolgenis from "./TableMolgenis.vue";

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
    Spinner,
    TableMolgenis,
  },
  data() {
    return {
      tableMetadata: null,
      dataRows: [],
      client: null,
      columns: null,
      searchTerms: null,
      count: null,
      page: null,
      view: View.TABLE,
      loading: false,
      selectedItems: [],
      orderByColumn: null,
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
    /**
     * if table (that has a column that is referred to by this table) can be edited
     *  */
    canEdit: {
      type: Boolean,
      required: false,
      default: () => false,
    },
  },
  computed: {
    View() {
      return View;
    },
    viewIcon() {
      if (this.view === View.CARDS) {
        return "list-alt";
      } else if (this.view === View.TABLE) {
        return "th";
      } else {
        return "th-list";
      }
    },
    countFilters() {
      return this.columns
        ? this.columns.filter((f) => f.showFilter).length
        : null;
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
    onColumnClick(column) {
      let orderByColumn = this.orderByColumn;
      let order = this.order;
      if (orderByColumn != column.id) {
        orderByColumn = column.id;
        order = "ASC";
      } else if (order == "ASC") {
        order = "DESC";
      } else {
        order = "ASC";
      }
      this.$emit("update:showOrderBy", orderByColumn);
      this.$emit("update:showOrder", order);
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
    emitConditions() {
      const result = this.columns.reduce((accum, c) => {
        if (c.conditions && c.conditions.length > 0) {
          accum[c.id] = c.conditions;
        }
        return accum;
      }, {});
      this.$emit("update:conditions", result);
    },
    setLimit(limit) {
      this.limit = limit;
      this.page = 1;
      this.$emit("update:showLimit", limit);
    },
  },
  mounted: async function () {
    this.loading = true;
    this.client = Client.newClient(this.graphqlURL);
    this.tableMetadata = await this.client.fetchTableMetaData(this.tableName);
    this.columns = this.tableMetadata.columns;
    this.dataRows = await this.client.fetchTableDataValues(this.tableName, {
      filter: this.graphqlFilter,
    });
    this.loading = false;
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