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
                <span class="fixed-width">zip</span>
                <ButtonAlt :href="'/' + schemaName + '/api/zip/' + tableId"
                  >all rows</ButtonAlt
                >
              </div>
              <div>
                <span class="fixed-width">csv</span>
                <ButtonAlt :href="'/' + schemaName + '/api/csv/' + tableId"
                  >all rows</ButtonAlt
                >
                <span v-if="Object.keys(graphqlFilter).length > 0">
                  |
                  <ButtonAlt
                    :href="
                      '/' +
                      schemaName +
                      '/api/csv/' +
                      tableId +
                      '?filter=' +
                      JSON.stringify(graphqlFilter)
                    "
                  >
                    filtered rows
                  </ButtonAlt>
                </span>
              </div>
              <div>
                <span class="fixed-width">excel</span>
                <ButtonAlt :href="'/' + schemaName + '/api/excel/' + tableId"
                  >all rows</ButtonAlt
                >
                <span v-if="Object.keys(graphqlFilter).length > 0">
                  |
                  <ButtonAlt
                    :href="
                      '/' +
                      schemaName +
                      '/api/excel/' +
                      tableId +
                      '?filter=' +
                      JSON.stringify(graphqlFilter)
                    "
                  >
                    filtered rows
                  </ButtonAlt></span
                >
              </div>
              <div>
                <span class="fixed-width">jsonld</span>
                <ButtonAlt :href="'/' + schemaName + '/api/jsonld/' + tableId">
                  all rows
                </ButtonAlt>
              </div>
              <div>
                <span class="fixed-width">ttl</span>
                <ButtonAlt :href="'/' + schemaName + '/api/ttl/' + tableId"
                  >all rows</ButtonAlt
                >
              </div>
            </div>
          </form>
        </ButtonDropdown>
        <span>
          <ButtonDropdown
            :closeOnClick="true"
            :label="ViewButtons[view].label"
            :icon="ViewButtons[view].icon"
          >
            <div
              v-for="button in ViewButtons"
              class="dropdown-item"
              @click="setView(button)"
              role="button"
            >
              <i class="fas fa-fw" :class="'fa-' + button.icon" />
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
          :filters="columns"
          @updateFilters="emitConditions"
          class="border-top pt-3 pb-3"
        />
        <div v-if="loading">
          <Spinner />
        </div>
        <div v-if="!loading">
          <AggregateTable
            v-if="view === View.AGGREGATE"
            :allColumns="columns"
            :tableName="tableName"
            :schemaName="schemaName"
            :minimumValue="1"
            :graphqlFilter="graphqlFilter"
          />
          <RecordCards
            v-if="view === View.CARDS"
            class="card-columns"
            id="cards"
            :data="dataRows"
            :columns="columns"
            :table-name="tableName"
            :canEdit="canEdit"
            :template="cardTemplate"
            @click="$emit('rowClick', $event)"
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
            @click="$emit('rowClick', $event)"
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
            @rowClick="$emit('rowClick', $event)"
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

<style scoped>
.fixed-width {
  width: 3em;
  display: inline-block;
}
</style>

<script setup>
import Client from "../../client/client.ts";
import {
  deepClone,
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
import { ref, computed } from "vue";

const emit = defineEmits([
  "searchTerms",
  "updateShowView",
  "updateShowOrder",
  "updateShowColumns",
  "updateShowFilters",
  "updateConditions",
  "updateShowPage",
  "updateShowLimit",
  "rowClick",
]);

const View = {
  TABLE: "table",
  CARDS: "cards",
  RECORD: "record",
  AGGREGATE: "aggregate",
};

const ViewButtons = {
  table: { id: View.TABLE, label: "Table", icon: "th" },
  cards: { id: View.CARDS, label: "Card", icon: "list-alt" },
  record: {
    id: View.RECORD,
    label: "Record",
    icon: "th-list",
    limitOverride: 1,
  },
  aggregate: {
    id: View.AGGREGATE,
    label: "Aggregate",
    icon: "object-group",
  },
};

const {
  tableName,
  schemaName,
  showSelect,
  showHeader,
  showFilters,
  showColumns,
  showView,
  showPage,
  showLimit,
  urlConditions,
  filter,
  showOrderBy,
  showOrder,
  canEdit,
  canManage,
  locale,
} = defineProps({
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
    default: false,
  },
  showHeader: {
    type: Boolean,
    default: true,
  },
  showFilters: {
    type: Array,
    default: [],
  },
  showColumns: {
    type: Array,
    default: [],
  },
  showView: {
    type: String,
    default: "table",
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
    default: {},
  },
  filter: {
    type: Object,
    default: {},
  },
  showOrderBy: {
    type: String,
    required: false,
  },
  showOrder: {
    type: String,
    default: "ASC",
  },
  canEdit: {
    type: Boolean,
    default: false,
  },
  canManage: {
    type: Boolean,
    default: false,
  },
  locale: {
    type: String,
    default: "en",
  },
});

//data
let client = null;
const cardTemplate = ref(null);
const columns = ref([]);
const count = ref(0);
const dataRows = ref([]);
const editMode = ref("add"); // add, edit, clone
const editRowPrimaryKey = ref(null);
const graphqlError = ref(null);
const isDeleteAllModalShown = ref(false);
const isDeleteModalShown = ref(false);
const isEditModalShown = ref(false);
const limit = ref(showLimit);
const loading = ref(true);
const order = ref(showOrder);
const orderByColumn = ref(showOrderBy);
const page = ref(showPage);
const recordTemplate = ref(null);
const searchTerms = ref("");
const selectedItems = ref([]);
const tableMetadata = ref(null);
const view = ref(showView);

// computed:
const tableId = computed(() => convertToPascalCase(tableName.value));
const localizedLabel = computed(() =>
  getLocalizedLabel(tableMetadata.value, locale.value)
);
const localizedDescription = computed(() =>
  getLocalizedDescription(tableMetadata.value, locale.value)
);
const countFilters = computed(() =>
  columns.value
    ? columns.value.filter((filter) => filter.showFilter).length
    : null
);
const graphqlFilter = computed(getGraphqlFilter);

// execute
reloadMetadata();

// methods
function setSearchTerms(newSearchValue) {
  searchTerms.value = newSearchValue;
  emit("searchTerms", newSearchValue);
  reload();
}

function handleRowAction(type, key) {
  editMode.value = type;
  editRowPrimaryKey.value = key;
  isEditModalShown.value = true;
}
function handleModalClose() {
  isEditModalShown.value = false;
  reload();
}

function handleDeleteRowRequest(key) {
  editRowPrimaryKey.value = key;
  isDeleteModalShown.value = true;
}

async function handleExecuteDelete() {
  isDeleteModalShown.value = false;
  const response = await client
    .deleteRow(editRowPrimaryKey.value, tableId)
    .catch(handleError);
  if (response) {
    reload();
  }
}

async function handelExecuteDeleteAll() {
  isDeleteAllModalShown.value = false;
  const response = await client.deleteAllTableData(tableId).catch(handleError);
  if (response) {
    reload();
  }
}

function setView(button) {
  view.value = button.id;
  if (button.limitOverride) {
    limit.value = button.limitOverride;
  } else {
    limit.value = showLimit;
  }
  page.value = 1;
  emit("updateShowView", button.id, limit.value);
  reload();
}

function onColumnClick(column) {
  order.value = getNewOrderValue();
  orderByColumn.value = column.id;
  emit("updateShowOrder", {
    direction: order.value,
    column: column.id,
  });
  reload();
}

function getNewOrderValue() {
  if (oldOrderByColumn.value !== column.id) {
    return "ASC";
  } else if (order.value === "ASC") {
    return "DESC";
  } else {
    return "ASC";
  }
}

function emitColumns(event) {
  columns.value = event;
  emit("updateShowColumns", getColumnNames(columns.value, "showColumn"));
}

function emitFilters(event) {
  columns.value = event;
  emit("updateShowFilters", getColumnNames(event, "showFilter"));
}

function emitConditions() {
  page.value = 1;
  emit("updateConditions", columns.value);
  reload();
}

function setPage(page) {
  page.value = page;
  emit("updateShowPage", page);
  reload();
}

function setLimit(limit) {
  limit.value = parseInt(limit);
  if (!Number.isInteger(limit.value) || limit.value < 1) {
    limit.value = 20;
  }
  page.value = 1;
  emit("updateShowLimit", limit);
  reload();
}

function handleError(error) {
  if (Array.isArray(error?.response?.data?.errors)) {
    graphqlError.value = error.response.data.errors[0].message;
  } else {
    graphqlError.value = error;
  }
  loading.value = false;
}

function setTableMetadata(newTableMetadata) {
  columns.value = newTableMetadata.columns.map((column) => {
    const showColumn = showColumns.length
      ? showColumns.includes(column.name)
      : !column.name.startsWith("mg_");
    const conditions = getCondition(
      column.columnType,
      urlConditions[column.name]
    );
    return {
      ...column,
      showColumn,
      showFilter: showFilters.includes(column.name),
      conditions,
    };
  });
  //table settings
  newTableMetadata.settings?.forEach((setting) => {
    if (setting.key === "cardTemplate") {
      cardTemplate.value = setting.value;
    } else if (setting.key === "recordTemplate") {
      recordTemplate.value = setting.value;
    }
  });
  tableMetadata.value = newTableMetadata;
}

async function reloadMetadata() {
  client = Client.newClient(schemaName);
  const newTableMetadata = await client
    .fetchTableMetaData(tableName)
    .catch(handleError);
  setTableMetadata(newTableMetadata);
  reload();
}

async function reload() {
  loading.value = true;
  graphqlError.value = null;
  const offset = limit.value * (page.value - 1);
  const orderBy = orderByColumn.value
    ? { [orderByColumn.value]: order.value }
    : {};
  const dataResponse = await client
    .fetchTableData(tableName, {
      limit: limit.value,
      offset: offset,
      filter: deepClone(graphqlFilter),
      searchTerms: searchTerms.value,
      orderby: orderBy,
    })
    .catch(handleError);
  dataRows.value = dataResponse[tableId];
  count.value = dataResponse[tableId + "_agg"]["count"];
  loading.value = false;
}

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

function getGraphqlFilter() {
  console.log(JSON.stringify(filter));
  let newFilter = deepClone(filter);
  if (columns.value) {
    columns.value.forEach((col) => {
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
          newFilter[col.id] = { like: conditions };
        } else if (
          col.columnType.startsWith("BOOL") ||
          col.columnType.startsWith("REF") ||
          col.columnType.startsWith("ONTOLOGY")
        ) {
          newFilter[col.id] = { equals: conditions };
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
          newFilter[col.id] = {
            between: conditions.flat(),
          };
        } else {
          graphqlError.value = `filter unsupported for column type ${col.columnType} (please report a bug)`;
        }
      }
    });
  }
  return newFilter;
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
