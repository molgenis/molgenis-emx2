<template>
  <div>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <h1 v-if="showHeader && tableMetadata">{{ localizedLabel }}</h1>
    <p v-if="showHeader && tableMetadata">
      {{ localizedDescription }}
    </p>
    <div class="btn-toolbar mb-3">
      <div class="btn-group">
        <ShowHide
          :columns="columns"
          @update:columns="emitFilters"
          checkAttribute="showFilter"
          :exclude="['HEADING', 'FILE']"
          label="filters"
          icon="filter"
        />

        <ShowHide
          v-if="view !== View.AGGREGATE"
          :columns="columns"
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
                <ButtonAlt :href="'../api/zip/' + tableId">zip</ButtonAlt>
              </div>
              <div>
                <ButtonAlt :href="'../api/excel/' + tableId">excel</ButtonAlt>
              </div>
              <div>
                <ButtonAlt :href="'../api/jsonld/' + tableId">
                  jsonld
                </ButtonAlt>
              </div>
              <div>
                <ButtonAlt :href="'../api/ttl/' + tableId">ttl</ButtonAlt>
              </div>
            </div>
          </form>
        </ButtonDropdown>
        <span>
          <ButtonDropdown>
            <template v-slot:label>
              {{ ViewButtons[view].label }}
              <i class="fas fa-fw" :class="ViewButtons[view].icon" />
            </template>
            <div
              v-for="button in ViewButtons"
              class="dropdown-item"
              @click="setView(button)"
              role="button"
            >
              <i class="fas fa-fw" :class="button.icon" />
              {{ button.label }}
            </div>
          </ButtonDropdown>
        </span>
      </div>
      <!-- end first btn group -->

      <InputSearch
        class="mx-1 inline-form-group"
        :id="'explorer-table-search' + Date.now()"
        :modelValue="searchTerms"
        @update:modelValue="setSearchTerms($event)"
      />
      <Pagination
        v-if="view !== View.AGGREGATE"
        :modelValue="page"
        @update:modelValue="setPage($event)"
        :limit="limit"
        :count="count"
      />

      <div
        class="btn-group m-0"
        v-if="view !== View.RECORD && view !== View.AGGREGATE"
      >
        <span class="btn">Rows per page:</span>
        <InputSelect
          id="explorer-table-page-limit-select"
          :modelValue="limit"
          :options="[10, 20, 50, 100]"
          :clear="false"
          @update:modelValue="setLimit($event)"
          class="mb-0"
        />
        <SelectionBox
          v-if="showSelect"
          :selection="selectedItems"
          @update:selection="selectedItems = $event"
        />
      </div>

      <div class="btn-group" v-if="canManage">
        <TableSettings
          v-if="tableMetadata"
          :tableMetadata="tableMetadata"
          :schemaName="schemaName"
          @update:settings="reloadMetadata"
        />

        <IconDanger icon="bomb" @click="isDeleteAllModalShown = true">
          Delete All
        </IconDanger>
      </div>
    </div>

    <div class="d-flex">
      <div v-if="countFilters" class="col-3 pl-0">
        <FilterSidebar
          :filters="columns"
          @updateFilters="emitConditions"
          :schemaName="schemaName"
        />
      </div>
      <div
        class="flex-grow-1 pr-0 pl-0"
        :class="countFilters > 0 ? 'col-9' : 'col-12'"
      >
        <FilterWells
          v-if="view !== View.AGGREGATE"
          :filters="columns"
          @updateFilters="emitConditions"
          class="border-top pt-3 pb-3"
        />
        <div v-if="loading">
          <Spinner />
        </div>
        <div v-if="!loading">
          <div v-if="view === View.AGGREGATE">
            <AggregateOptions
              :columns="columns"
              @setAggregateColumns="aggregateColumns = $event"
              v-model:selectedColumn="aggregateSelectedColumn"
              v-model:selectedRow="aggregateSelectedRow"
            />
            <AggregateTable
              v-if="aggregateColumns?.length > 0"
              :tableName="tableName"
              :schemaName="schemaName"
              :minimumValue="1"
              :columnProperties="aggregateColumns"
              :rowProperties="aggregateColumns"
              :selectedColumnProperty="aggregateSelectedColumn"
              columnNameProperty="name"
              :selectedRowProperty="aggregateSelectedRow"
              rowNameProperty="name"
            />
          </div>
          <RecordCards
            v-if="view === View.CARDS"
            class="card-columns"
            id="cards"
            :data="dataRows"
            :columns="columns"
            :table-name="tableName"
            :canEdit="canEdit"
            :template="cardTemplate"
            @click="$emit('click', $event)"
            @reload="reload"
            @edit="
              handleRowAction('edit', getPrimaryKey($event, tableMetadata))
            "
            @delete="
              handleDeleteRowRequest(getPrimaryKey($event, tableMetadata))
            "
          />
          <RecordCards
            v-if="view === View.RECORD"
            id="records"
            :data="dataRows"
            :columns="columns"
            :table-name="tableName"
            :canEdit="canEdit"
            :template="recordTemplate"
            @click="$emit('click', $event)"
            @reload="reload"
            @edit="
              handleRowAction('edit', getPrimaryKey($event, tableMetadata))
            "
            @delete="
              handleDeleteRowRequest(getPrimaryKey($event, tableMetadata))
            "
          />
          <TableMolgenis
            v-if="view == View.TABLE"
            :selection="selectedItems"
            @update:selection="selectedItems = $event"
            :columns="columns"
            @update:colums="columns = $event"
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
                :schemaName="schemaName"
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
    </div>

    <EditModal
      v-if="isEditModalShown"
      :isModalShown="true"
      :id="tableName + '-edit-modal'"
      :tableName="tableName"
      :pkey="editRowPrimaryKey"
      :clone="editMode === 'clone'"
      :schemaName="schemaName"
      @close="handleModalClose"
      :locale="locale"
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
import Client from "../../client/client.ts";
import {
  getPrimaryKey,
  convertToPascalCase,
  getLocalizedDescription,
  getLocalizedLabel,
} from "../utils";
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
import AggregateTable from "./AggregateTable.vue";
import AggregateOptions from "./AggregateOptions.vue";

const View = {
  TABLE: "table",
  CARDS: "cards",
  RECORD: "record",
  AGGREGATE: "aggregate",
};

const ViewButtons = {
  table: { id: View.TABLE, label: "Table", icon: "fa-th" },
  cards: { id: View.CARDS, label: "Card", icon: "fa-list-alt" },
  record: {
    id: View.RECORD,
    label: "Record",
    icon: "fa-th-list",
    limitOverride: 1,
  },
  aggregate: {
    id: View.AGGREGATE,
    label: "Aggregate",
    icon: "fa-object-group",
  },
};

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
    AggregateTable,
    AggregateOptions,
  },
  data() {
    return {
      cardTemplate: null,
      client: null,
      columns: [],
      count: 0,
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
      aggregateColumns: [],
      aggregateSelectedColumn: "",
      aggregateSelectedRow: "",
    };
  },
  props: {
    tableName: {
      type: String,
      required: true,
    },
    schemaName: {
      type: String,
      required: false,
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
    locale: {
      type: String,
      default: () => "en",
    },
  },
  computed: {
    tableId() {
      return convertToPascalCase(this.tableName);
    },
    localizedLabel() {
      return getLocalizedLabel(this.tableMetadata, this.locale);
    },
    localizedDescription() {
      return getLocalizedDescription(this.tableMetadata, this.locale);
    },
    View() {
      return View;
    },
    ViewButtons() {
      return ViewButtons;
    },
    countFilters() {
      return this.columns
        ? this.columns.filter((filter) => filter.showFilter).length
        : null;
    },
    graphqlFilter() {
      const errorCallback = (msg) => {
        this.graphqlError = msg;
      };
      return graphqlFilter(this.columns, errorCallback);
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
        .deleteRow(this.editRowPrimaryKey, this.tableId)
        .catch(this.handleError);
      if (resp) {
        this.reload();
      }
    },
    async handelExecuteDeleteAll() {
      this.isDeleteAllModalShown = false;
      const resp = await this.client
        .deleteAllTableData(this.tableId)
        .catch(this.handleError);
      if (resp) {
        this.reload();
      }
    },
    setView(button) {
      this.view = button.id;
      if (button.limitOverride) {
        this.limit = button.limitOverride;
      } else {
        this.limit = this.showLimit;
      }
      this.page = 1;
      this.$emit("updateShowView", button.id, this.limit);
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
    emitColumns(event) {
      this.columns = event;
      this.$emit(
        "updateShowColumns",
        getColumnNames(this.columns, "showColumn")
      );
    },
    emitFilters(event) {
      this.columns = event;
      this.$emit("updateShowFilters", getColumnNames(event, "showFilter"));
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
    async reloadMetadata() {
      this.client = Client.newClient(this.schemaName);
      const newTableMetadata = await this.client
        .fetchTableMetaData(this.tableName)
        .catch(this.handleError);
      this.setTableMetadata(newTableMetadata);
      this.reload();
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
      this.dataRows = dataResponse[this.tableId];
      this.count = dataResponse[this.tableId + "_agg"]["count"];
      this.loading = false;
    },
  },
  mounted: async function () {
    await this.reloadMetadata();
  },
  emits: [
    "updateShowFilters",
    "click",
    "updateShowLimit",
    "updateShowPage",
    "updateConditions",
    "updateShowColumns",
    "updateShowOrder",
    "updateShowView",
    "searchTerms",
  ],
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
      case "LONG":
      case "DECIMAL":
        return condition.split(",").map((v) => v.split(".."));
      default:
        return condition.split(",");
    }
  } else {
    return [];
  }
}

function graphqlFilter(columns, errorCallback) {
  let filter = {};
  if (columns) {
    columns.forEach((col) => {
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
            "LONG",
            "LONG_ARRAY",
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
          errorCallback(
            `filter unsupported for column type ${col.columnType} (please report a bug)`
          );
        }
      }
    });
  }
  return filter;
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
        schemaName="pet store"
        :showColumns="showColumns"
        :showFilters="showFilters"
        :urlConditions="urlConditions"
        :showPage="page"
        :showLimit="limit"
        :showOrderBy="showOrderBy"
        :showOrder="showOrder"
        :canEdit="canEdit"
        :canManage="canManage"
        :locale="locale"
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
        canManage: false,
        locale: 'en'
      }
    },
  }
</script>
</docs>
