<template>
  <div>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <h1 v-if="showHeader">{{ tableName }} Explorer</h1>

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
                <ButtonAlt :href="'../api/jsonld/' + tableName"
                  >jsonld</ButtonAlt
                >
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
        v-model="searchTerms"
      />
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
      </div>

      <div class="btn-group" v-if="canManage">
        <TableSettings
          :tableName="tableName"
          :cardTemplate.sync="cardTemplate"
          :recordTemplate.sync="recordTemplate"
          :graphqlURL="graphqlURL"
        />

        <IconDanger icon="bomb" @click="isDeleteAllModalShown = true"
          >Delete All</IconDanger
        >
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
          v-if="!loading && view == View.CARDS"
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
          v-if="!loading && view == View.RECORD"
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
              v-if="slotProps.col && orderByColumn == slotProps.col.id"
              :icon="order == 'ASC' ? 'sort-alpha-down' : 'sort-alpha-up'"
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

const View = { TABLE: "table", CARDS: "cards", RECORD: "record", EDIT: "edit" };

export default {
  name: "TableExplorer",
  components: {
    ShowHide,
    Pagination,
    ButtonDropdown,
    IconAction,
    IconDanger,
    InputSearch,
    InputSelect,
    SelectionBox,
    Spinner,
    TableMolgenis,
    FilterSidebar,
    FilterWells,
    RecordCards,
    TableSettings,
    EditModal,
    ConfirmModal,
  },
  data() {
    return {
      tableMetadata: null,
      dataRows: [],
      client: null,
      columns: [],
      searchTerms: null,
      count: null,
      page: null,
      limit:
        this.view === View.TABLE || this.view === View.TABLE
          ? this.showLimit
          : 1,
      view: View.TABLE,
      loading: false,
      selectedItems: [],
      orderByColumn: null,
      graphqlError: null,
      cardTemplate: null,
      recordTemplate: null,
      isDeleteModalShown: false,
      isEditModalShown: false,
      editMode: "add", // add, edit, clone
      editRowPrimaryKey: null,
      isDeleteAllModalShown: false,
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
    canEdit: {
      type: Boolean,
      required: false,
      default: () => false,
    },
    canManage: {
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
        return "fa-list-alt";
      } else if (this.view === View.TABLE) {
        return "fa-th";
      } else {
        return "fa-th-list";
      }
    },
    countFilters() {
      return this.columns
        ? this.columns.filter((f) => f.showFilter).length
        : null;
    },
    graphqlFilter() {
      let filter = this.filter ? this.filter : {};
      if (this.columns) {
        this.columns.forEach((col) => {
          let conditions = Array.isArray(col.conditions)
            ? col.conditions.filter((f) => f !== "" && f != undefined)
            : [];
          if (conditions.length > 0) {
            if (
              col.columnType.startsWith("STRING") ||
              col.columnType.startsWith("TEXT")
            ) {
              filter[col.id] = { like: col.conditions };
            } else if (col.columnType.startsWith("BOOL")) {
              filter[col.id] = { equals: col.conditions };
            } else if (
              col.columnType.startsWith("REF") ||
              col.columnType.startsWith("ONTOLOGY")
            ) {
              filter[col.id] = { equals: col.conditions };
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
              alert(
                "filter unsupported for column type '" +
                  col.columnType +
                  "' (please report a bug)"
              );
            }
          }
        });
      }
      return filter;
    },
  },
  methods: {
    getPrimaryKey,
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
        this.handleSuccess(resp)
        this.reload();
      }
    },
    async handelExecuteDeleteAll() {
      this.isDeleteAllModalShown = false;
      const resp = await this.client
        .deleteAllTableData(this.tableName)
        .catch(this.handleError);
      if (resp) {
        this.handleSuccess(resp)
        this.reload();
      }
    },
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
      this.reload();
    },
    setLimit(limit) {
      const limitNumber = parseInt(limit);
      this.limit = limitNumber;
      this.page = 1;
      this.$emit("update:showLimit", limitNumber);
    },
    handleError(error) {
      if (Array.isArray(error?.response?.data?.errors)) {
        this.graphqlError = error.response.data.errors[0].message;
      } else {
        this.graphqlError = error;
      }
      this.loading = false;
    },
    handleSuccess(resp) {
       this.success = resp.data?.data?.delete?.message;
    },
    async reload() {
      this.loading = true;
      this.graphqlError = null;

      if (!this.client) {
        this.client = Client.newClient(this.graphqlURL);
      }
      if (!this.tableMetadata) {
        this.tableMetadata = await this.client
          .fetchTableMetaData(this.tableName)
          .catch(this.handleError);
      }
      const dataResponse = await this.client
        .fetchTableData(this.tableName, {
          filter: this.graphqlFilter,
          orderby: this.orderByColumn
            ? { [this.orderByColumn]: this.order }
            : {},
        })
        .catch(this.handleError);

      this.dataRows = dataResponse[this.tableName];
      this.count = dataResponse[this.tableName + "_agg"]["count"];
      this.loading = false;
    },
  },
  watch: {
    page() {
      this.loading = true;
      this.offset = this.limit * (this.page - 1);
      this.$emit("update:showPage", this.page);
      this.reload();
    },
    showOrderBy() {
      this.orderByColumn = this.showOrderBy;
      this.$emit("update:showOrderBy", this.showOrderBy);
    },
    showOrder() {
      this.order = this.showOrder;
      this.$emit("update:showOrder", this.showOrder);
      this.reload();
    },
    showPage() {
      this.page = this.showPage;
    },
    showLimit() {
      this.limit = this.showLimit;
    },
    tableMetadata() {
      this.page = this.showPage;
      this.limit = this.showLimit;
      this.orderByColumn = this.showOrderBy;
      this.order = this.showOrder;
      if (this.columns.length === 0) {
        this.columns.push(...this.tableMetadata.columns);
        // //init settings
        this.columns.forEach((c) => {
          //show columns
          if (this.showColumns && this.showColumns.length > 0) {
            if (this.showColumns.includes(c.name)) {
              c.showColumn = true;
            } else {
              c.showColumn = false;
            }
          } else {
            //default we show all non mg_ columns
            if (!c.name.startsWith("mg_")) {
              c.showColumn = true;
            } else {
              c.showColumn = false;
            }
          }
          //show filters
          if (this.showFilters && this.showFilters.length > 0) {
            if (this.showFilters.includes(c.name)) {
              c.showFilter = true;
            } else {
              c.showFilter = false;
            }
          } else {
            //default we hide all filters
            c.showFilter = false;
          }
        });
        if (this.showView) {
          this.view = this.showView;
        }
        this.columns.forEach((c) => {
          if (this.conditions[c.name] && this.conditions[c.name].length > 0) {
            this.$set(c, "conditions", this.conditions[c.name]); //haat vue reactivity
          } else {
            c.conditions = [];
          }
        });
        //table settings
        if (this.tableMetadata.settings) {
          this.tableMetadata.settings.forEach((s) => {
            if (s.key == "cardTemplate") this.cardTemplate = s.value;
            if (s.key == "recordTemplate") this.recordTemplate = s.value;
          });
        }
      }
      this.reload();
    },
  },
  mounted: async function () {
    await this.reload();
  },
};
</script>

<style scoped>
/* fix style for use of dropdown btns in within button-group, needed as dropdown component add span due to single route element constraint */
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
        :showColumns.sync="showColumns"
        :showFilters.sync="showFilters"
        :conditions.sync="conditions"
        :showPage.sync="page" 
        :showLimit.sync="limit"
        :showOrderBy.sync="showOrderBy" 
        :showOrder.sync="showOrder"
        :canEdit="canEdit"
        :canManage="canManage"
      ></table-explorer>

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
        <div>showColumns: {{showColumns}}</div>
        <div>showFilters: {{showFilters}}</div>
        <div>conditions: {{conditions}}</div>
        <div>showOrderBy: {{showOrderBy}}</div>
        <div>showOrder: {{showOrder}}</div>
        <div>page: {{page}}</div>
        <div>limit: {{limit}}</div>
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
        conditions: {"name": ["pooky", "spike"]},
        page: 1,
        limit: 10,
        showOrder: 'DESC', 
        showOrderBy: 'name',
        canEdit: true,
        canManage: true
      }
    },
  }
</script>
</docs>