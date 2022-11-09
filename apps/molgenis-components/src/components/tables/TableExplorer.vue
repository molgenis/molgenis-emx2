<template>
  <div>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <h1 v-if="showHeader">{{ tableName }}</h1>

    <p v-if="showHeader && tableMetadata">
      {{ tableMetadata.description }}
    </p>

    <div class="btn-toolbar mb-3">
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

            <h6>download</h6>
            <div>
              <div>
                <ButtonAlt :href="'../api/zip/' + tableName">zip</ButtonAlt>
              </div>
              <div>
                <ButtonAlt :href="'../api/excel/' + tableName">excel</ButtonAlt>
              </div>
              <div>
                <ButtonAlt :href="'../api/jsonld/' + tableName">
                  jsonld
                </ButtonAlt>
              </div>
              <div>
                <ButtonAlt :href="'../api/ttl/' + tableName">ttl</ButtonAlt>
              </div>
            </div>
          </form>
        </ButtonDropdown>

        <span>
          <button
            type="button"
            class="btn btn-outline-primary"
            @click="toggleView"
          >
            view
            <span class="fas fa-fw" :class="viewIcon"></span>
          </button>
        </span>
      </div>
      <!-- end first btn group -->

      <InputSearch
        class="mx-1 inline-form-group"
        id="explorer-table-search"
        :value="searchTerms"
        @input="setSearchTerms($event)"
      />
      <Pagination
        :value="page"
        @input="setPage($event)"
        :limit="limit"
        :count="count"
      />

      <div class="btn-group m-0" v-if="view !== View.RECORD">
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
      </div>

      <div class="btn-group" v-if="canManage">
        <TableSettings
          :tableMetadata="tableMetadata"
          :graphqlURL="graphqlURL"
          @update:settings="reload"
        />

        <IconDanger icon="bomb" @click="isDeleteAllModalShown = true">
          Delete All
        </IconDanger>
      </div>
    </div>

    <div class="d-flex">
      <div v-if="countFilters" class="col-3 pl-0">
        <FilterSidebar
          :filters.sync="columns"
          @updateFilters="emitConditions"
          :graphqlURL="graphqlURL"
        />
      </div>
      <div
        class="flex-grow-1 pr-0 pl-0"
        :class="countFilters > 0 ? 'col-9' : 'col-12'"
      >
        <FilterWells
          :filters.sync="columns"
          @updateFilters="emitConditions"
          class="border-top pt-3 pb-3"
        />
        <div v-if="loading">
          <Spinner />
        </div>
        <RecordCards
          v-if="!loading && view === View.CARDS"
          class="card-columns"
          id="cards"
          :data="dataRows"
          :columns="columns"
          :table-name="tableName"
          :canEdit="canEdit"
          :template="cardTemplate"
          @click="$emit('click', $event)"
          @reload="reload"
          @edit="handleRowAction('edit', getPrimaryKey($event, tableMetadata))"
          @delete="handleDeleteRowRequest(getPrimaryKey($event, tableMetadata))"
        />
        <RecordCards
          v-if="!loading && view === View.RECORD"
          id="records"
          :data="dataRows"
          :columns="columns"
          :table-name="tableName"
          :canEdit="canEdit"
          :template="recordTemplate"
          @click="$emit('click', $event)"
          @reload="reload"
          @edit="handleRowAction('edit', getPrimaryKey($event, tableMetadata))"
          @delete="handleDeleteRowRequest(getPrimaryKey($event, tableMetadata))"
        />
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
          <template v-slot:rowcolheader>
            <RowButton
              v-if="canEdit"
              type="add"
              :table="tableName"
              :graphqlURL="graphqlURL"
              @add="handleRowAction('add')"
              class="d-inline p-0"
            />
          </template>
          <template v-slot:colheader="slotProps">
            <IconAction
              v-if="slotProps.col && orderByColumn === slotProps.col.id"
              :icon="order === 'ASC' ? 'sort-alpha-down' : 'sort-alpha-up'"
              class="d-inline p-0"
            />
          </template>
          <template v-slot:rowheader="slotProps">
            <RowButton
              v-if="canEdit"
              type="edit"
              @edit="
                handleRowAction(
                  'edit',
                  getPrimaryKey(slotProps.row, tableMetadata)
                )
              "
            />
            <RowButton
              v-if="canEdit"
              type="clone"
              @clone="
                handleRowAction(
                  'clone',
                  getPrimaryKey(slotProps.row, tableMetadata)
                )
              "
            />
            <RowButton
              v-if="canEdit"
              type="delete"
              @delete="
                handleDeleteRowRequest(
                  getPrimaryKey(slotProps.row, tableMetadata)
                )
              "
            />
          </template>
        </TableMolgenis>
      </div>
    </div>

    <EditModal
      v-if="isEditModalShown"
      :isModalShown="true"
      :id="tableName + '-edit-modal'"
      :tableName="tableName"
      :pkey="editRowPrimaryKey"
      :clone="editMode === 'clone'"
      :graphqlURL="graphqlURL"
      @close="handleModalClose"
    />

    <ConfirmModal
      v-if="isDeleteModalShown"
      :title="'Delete from ' + tableName"
      actionLabel="Delete"
      actionType="danger"
      :tableName="tableName"
      :pkey="editRowPrimaryKey"
      @close="isDeleteModalShown = false"
      @confirmed="handleExecuteDelete"
    />

    <ConfirmModal
      v-if="isDeleteAllModalShown"
      :title="'Truncate ' + tableName"
      actionLabel="Truncate"
      actionType="danger"
      :tableName="tableName"
      @close="isDeleteAllModalShown = false"
      @confirmed="handelExecuteDeleteAll"
    >
      <p>
        Truncate <strong>{{ tableName }}</strong>
      </p>
      <p>
        Are you sure that you want to delete ALL rows in table '{{
          tableName
        }}'?
      </p>
    </ConfirmModal>
  </div>
</template>


<script>
import Client from "../../client/client.js";
import { getPrimaryKey } from "../utils";
import ShowHide from "./ShowHide.vue";
import Pagination from "./Pagination.vue";
import ButtonAlt from "../forms/ButtonAlt.vue";
import ButtonDropdown from "../forms/ButtonDropdown.vue";
import IconAction from "../forms/IconAction.vue";
import IconDanger from "../forms/IconDanger.vue";
import InputSearch from "../forms/InputSearch.vue";
import InputSelect from "../forms/InputSelect.vue";
import SelectionBox from "./SelectionBox.vue";
import Spinner from "../layout/Spinner.vue";
import TableMolgenis from "./TableMolgenis.vue";
import FilterSidebar from "../filters/FilterSidebar.vue";
import FilterWells from "../filters/FilterWells.vue";
import RecordCards from "./RecordCards.vue";
import TableSettings from "./TableSettings.vue";
import EditModal from "../forms/EditModal.vue";
import ConfirmModal from "../forms/ConfirmModal.vue";
import RowButton from "../tables/RowButton.vue";
import MessageError from "../forms/MessageError.vue";

const View = { TABLE: "table", CARDS: "cards", RECORD: "record", EDIT: "edit" };

export default {
  name: "TableExplorer",
  components: {
    ShowHide,
    Pagination,
    ButtonAlt,
    ButtonDropdown,
    IconAction,
    IconDanger,
    InputSearch,
    InputSelect,
    MessageError,
    SelectionBox,
    Spinner,
    TableMolgenis,
    FilterSidebar,
    FilterWells,
    RecordCards,
    RowButton,
    TableSettings,
    EditModal,
    ConfirmModal,
  },
  data() {
    return {
      cardTemplate: null,
      client: null,
      columns: [],
      count: null,
      dataRows: [],
      editMode: "add", // add, edit, clone
      editRowPrimaryKey: null,
      graphqlError: null,
      isDeleteAllModalShown: false,
      isDeleteModalShown: false,
      isEditModalShown: false,
      limit: this.showLimit,
      loading: false,
      order: this.showOrder,
      orderByColumn: this.showOrderBy,
      page: this.showPage,
      recordTemplate: null,
      searchTerms: "",
      selectedItems: [],
      tableMetadata: null,
      view: this.showView,
    };
  },
  props: {
    tableName: {
      type: String,
      required: true,
    },
    graphqlURL: {
      type: String,
      default: () => "graphql",
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
    urlConditions: {
      type: Object,
      default: () => ({}),
    },
    showOrderBy: {
      type: String,
      required: false,
    },
    showOrder: {
      type: String,
      default: () => "ASC",
    },
    canEdit: {
      type: Boolean,
      default: () => false,
    },
    canManage: {
      type: Boolean,
      default: () => false,
    },
  },
  computed: {
    View() {
      return View;
    },
    viewIcon() {
      if (this.view === View.CARDS) {
        return "fa-list-alt";
      } else if (this.view === View.TABLE) {
        return "fa-th";
      } else {
        return "fa-th-list";
      }
    },
    countFilters() {
      return this.columns
        ? this.columns.filter((filter) => filter.showFilter).length
        : null;
    },
    graphqlFilter() {
      let filter = {};
      if (this.columns) {
        this.columns.forEach((col) => {
          const conditions = col.conditions
            ? col.conditions.filter(
                (condition) => condition !== "" && condition !== undefined
              )
            : [];
          if (conditions.length) {
            if (
              col.columnType.startsWith("STRING") ||
              col.columnType.startsWith("TEXT")
            ) {
              filter[col.id] = { like: conditions };
            } else if (col.columnType.startsWith("BOOL")) {
              filter[col.id] = { equals: conditions };
            } else if (
              col.columnType.startsWith("REF") ||
              col.columnType.startsWith("ONTOLOGY")
            ) {
              filter[col.id] = { equals: conditions };
            } else if (
              [
                "DECIMAL",
                "DECIMAL_ARRAY",
                "INT",
                "INT_ARRAY",
                "DATE",
                "DATE_ARRAY",
              ].includes(col.columnType)
            ) {
              filter[col.id] = {
                between: conditions.flat(),
              };
            } else {
              const msg =
                "filter unsupported for column type '" +
                col.columnType +
                "' (please report a bug)";
              this.graphqlError = msg;
            }
          }
        });
      }
      return filter;
    },
  },
  methods: {
    getPrimaryKey,
    setSearchTerms(newSearchValue) {
      this.searchTerms = newSearchValue;
      this.$emit("searchTerms", newSearchValue);
      this.reload();
    },
    handleRowAction(type, key) {
      this.editMode = type;
      this.editRowPrimaryKey = key;
      this.isEditModalShown = true;
    },
    handleModalClose() {
      this.isEditModalShown = false;
      this.reload();
    },
    handleDeleteRowRequest(key) {
      this.editRowPrimaryKey = key;
      this.isDeleteModalShown = true;
    },
    async handleExecuteDelete() {
      this.isDeleteModalShown = false;
      const resp = await this.client
        .deleteRow(this.editRowPrimaryKey, this.tableName)
        .catch(this.handleError);
      if (resp) {
        this.reload();
      }
    },
    async handelExecuteDeleteAll() {
      this.isDeleteAllModalShown = false;
      const resp = await this.client
        .deleteAllTableData(this.tableName)
        .catch(this.handleError);
      if (resp) {
        this.reload();
      }
    },
    toggleView() {
      if (this.view === View.TABLE) {
        this.view = View.CARDS;
        this.limit = this.showLimit;
      } else if (this.view === View.CARDS) {
        this.view = View.RECORD;
        this.limit = 1;
      } else {
        this.view = View.TABLE;
        this.limit = 20;
      }
      this.page = 1;
      this.$emit("updateShowView", this.view, this.limit);
      this.reload();
    },
    onColumnClick(column) {
      const oldOrderByColumn = this.orderByColumn;
      let order = this.order;
      if (oldOrderByColumn !== column.id) {
        order = "ASC";
      } else if (order === "ASC") {
        order = "DESC";
      } else {
        order = "ASC";
      }
      this.order = order;
      this.orderByColumn = column.id;
      this.$emit("updateShowOrder", {
        direction: order,
        column: column.id,
      });
      this.reload();
    },
    emitColumns() {
      this.$emit(
        "updateShowColumns",
        getColumnNames(this.columns, "showColumn")
      );
    },
    emitFilters() {
      this.$emit(
        "updateShowFilters",
        getColumnNames(this.columns, "showFilter")
      );
    },
    emitConditions() {
      this.page = 1;
      this.$emit("updateConditions", this.columns);
      this.reload();
    },
    setPage(page) {
      this.page = page;
      this.$emit("updateShowPage", page);
      this.reload();
    },
    setLimit(limit) {
      this.limit = parseInt(limit);
      if (!Number.isInteger(this.limit) || this.limit < 1) {
        this.limit = 20;
      }
      this.page = 1;
      this.$emit("updateShowLimit", limit);
      this.reload();
    },
    handleError(error) {
      if (Array.isArray(error?.response?.data?.errors)) {
        this.graphqlError = error.response.data.errors[0].message;
      } else {
        this.graphqlError = error;
      }
      this.loading = false;
    },
    setTableMetadata(newTableMetadata) {
      this.columns = newTableMetadata.columns.map((column) => {
        const showColumn = this.showColumns.length
          ? this.showColumns.includes(column.name)
          : !column.name.startsWith("mg_");
        const conditions = getCondition(
          column.columnType,
          this.urlConditions[column.name]
        );
        return {
          ...column,
          showColumn,
          showFilter: this.showFilters.includes(column.name),
          conditions,
        };
      });
      //table settings
      newTableMetadata.settings?.forEach((setting) => {
        if (setting.key === "cardTemplate") {
          this.cardTemplate = setting.value;
        } else if (setting.key === "recordTemplate") {
          this.recordTemplate = setting.value;
        }
      });
      this.tableMetadata = newTableMetadata;
    },
    async reload() {
      this.loading = true;
      this.graphqlError = null;
      const offset = this.limit * (this.page - 1);
      const orderBy = this.orderByColumn
        ? { [this.orderByColumn]: this.order }
        : {};
      const dataResponse = await this.client
        .fetchTableData(this.tableName, {
          limit: this.limit,
          offset: offset,
          filter: this.graphqlFilter,
          searchTerms: this.searchTerms,
          orderby: orderBy,
        })
        .catch(this.handleError);

      this.dataRows = dataResponse[this.tableName];
      this.count = dataResponse[this.tableName + "_agg"]["count"];
      this.loading = false;
    },
  },
  mounted: async function () {
    this.client = Client.newClient(this.graphqlURL);
    const newTableMetadata = await this.client
      .fetchTableMetaData(this.tableName)
      .catch(this.handleError);
    this.setTableMetadata(newTableMetadata);
    await this.reload();
  },
};

function getColumnNames(columns, property) {
  return columns
    .filter((column) => column[property] && column.columnType !== "HEADING")
    .map((column) => column.name);
}

function getCondition(columnType, condition) {
  if (condition) {
    switch (columnType) {
      case "REF":
      case "REF_ARRAY":
      case "REFBACK":
      case "ONTOLOGY":
      case "ONTOLOGY_ARRAY":
        return JSON.parse(condition);
      case "DATE":
      case "DATETIME":
      case "INT":
      case "DECIMAL":
        return condition.split(",").map((v) => v.split(".."));
      default:
        return condition.split(",");
    }
  } else {
    return [];
  }
}
</script>

<style scoped>
/* fix style for use of dropdown btns in within button-group, needed as dropdown component add span due `to` single route element constraint */
.btn-group >>> span:not(:first-child) .btn {
  margin-left: 0;
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
  border-left: 0;
}

.btn-group >>> span:not(:last-child) .btn {
  margin-left: 0;
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
}

.inline-form-group {
  margin-bottom: 0;
}
</style>

<docs>
<template>
  <div>
    <div class="border p-1 my-1">
      <label>Read only example</label>
      <table-explorer
        id="my-table-explorer"
        tableName="Pet"
        graphqlURL="/pet store/graphql"
        :showColumns="showColumns"
        :showFilters="showFilters"
        :urlConditions="urlConditions"
        :showPage="page" 
        :showLimit="limit"
        :showOrderBy="showOrderBy" 
        :showOrder="showOrder"
        :canEdit="canEdit"
        :canManage="canManage"
      />
      <div class="border mt-3 p-2">
        <h5>synced props: </h5>
        <div>
          <label for="canEdit" class="pr-1">can edit: </label>
          <input type="checkbox" id="canEdit" v-model="canEdit">
        </div>
        <div>
          <label for="canManage" class="pr-1">canManage: </label>
          <input type="checkbox" id="canManage" v-model="canManage">
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    data() {
      return {
        showColumns: ['name'],
        showFilters: ['name'],
        urlConditions: {"name": "pooky,spike"},
        page: 1,
        limit: 10,
        showOrder: 'DESC', 
        showOrderBy: 'name',
        canEdit: false,
        canManage: false
      }
    },
  }
</script>
</docs>
