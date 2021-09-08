<template>
  <div class="h-100">
    <div v-if="columns" class="overflow-auto">
      <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
      <div class="bg-white">
        <h1 v-if="showHeader" class="pl-2">
          {{ table }}
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
      <div class="d-flex">
        <div v-if="countFilters" class="col-3 pl-0">
          <FilterSidebar
            :filters.sync="columns"
            @update:filters="emitConditions"
            :graphqlURL="graphqlURL"
          />
        </div>
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
          <TableCards
            v-if="!loading && view == View.CARDS"
            :data="data"
            :columns="columns"
            :table-name="table"
            @reload="reload"
            :canEdit="canEdit"
            @click="$emit('click', $event)"
            :template="cardTemplate"
          />
          <RecordCard
            v-if="!loading && view == View.RECORD"
            :data="data"
            :table-name="table"
            :columns="columns"
            :canEdit="canEdit"
            @click="$emit('click', $event)"
            :template="recordTemplate"
          />
          <TableMolgenis
            v-if="!loading && view == View.TABLE"
            :selection.sync="selectedItems"
            :columns.sync="columns"
            :table-metadata="tableMetadata"
            :data="data"
            :showSelect="showSelect"
            @click="$emit('click', $event)"
          >
            <template v-slot:header>
              <label>{{ count }} records found</label>
            </template>
            <template v-slot:colheader>
              <RowButtonAdd
                v-if="canEdit"
                :table="table"
                :graphqlURL="graphqlURL"
                @close="reload"
                class="d-inline p-0"
              />
            </template>
            <template v-slot:rowheader="slotProps">
              <RowButtonEdit
                v-if="canEdit"
                :table="table"
                :graphqlURL="graphqlURL"
                :pkey="getPkey(slotProps.row)"
                @close="reload"
              />
              <RowButtonDelete
                v-if="canEdit"
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
  </div>
</template>

<script>
import TableMolgenis from "./TableMolgenis";
import FilterSidebar from "./FilterSidebar";
import FilterWells from "./FilterWells";
import MessageError from "../forms/MessageError";
import RowButtonAdd from "./RowButtonAdd";
import RowButtonDelete from "./RowButtonDelete";
import RowButtonEdit from "./RowButtonEdit";
import Spinner from "../layout/Spinner";
import TableMixin from "../mixins/TableMixin";
import ShowHide from "./ShowHide";
import InputSearch from "../forms/InputSearch";
import Pagination from "./Pagination";
import ButtonAlt from "../forms/ButtonAlt";
import SelectionBox from "./SelectionBox";
import ButtonDropdown from "../forms/ButtonDropdown";
import InputSelect from "../forms/InputSelect";
import TableCards from "./TableCards";
import IconAction from "../forms/IconAction";
import RecordCard from "./RecordCard";
import TableSettings from "./TableSettings";

const View = { TABLE: "table", CARDS: "cards", RECORD: "record", EDIT: "edit" };

export default {
  extends: TableMixin,
  components: {
    Spinner,
    MessageError,
    TableMolgenis,
    FilterSidebar,
    FilterWells,
    RowButtonEdit,
    RowButtonAdd,
    RowButtonDelete,
    ShowHide,
    InputSearch,
    Pagination,
    ButtonAlt,
    ButtonDropdown,
    SelectionBox,
    InputSelect,
    TableCards,
    IconAction,
    RecordCard,
    TableSettings,
  },
  props: {
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
  },
  data() {
    return {
      View,
      cardTemplate: null,
      recordTemplate: null,
      selectedItems: [],
      page: 1,
      showSubclass: false,
      //a copy of column metadata used to show/hide filters and columns
      columns: [],
      view: View.TABLE,
    };
  },
  methods: {
    toggleView() {
      if (this.view == View.TABLE) {
        this.view = View.CARDS;
        this.limit = this.showLimit;
      } else if (this.view == View.CARDS) {
        this.limit = 1;
        this.view = View.RECORD;
      } else {
        this.view = View.TABLE;
        this.limit = this.showLimit;
      }
    },
    emitColumns() {
      let columns = this.columns
        .filter((c) => c.showColumn && c.columnType != "HEADING")
        .map((c) => c.name);
      this.$emit("update:showColumns", columns);
    },
    emitFilters() {
      this.$emit(
        "update:showFilters",
        this.columns
          .filter((c) => c.showFilter && c.columnType != "HEADING")
          .map((c) => c.name)
      );
    },
    emitConditions() {
      let result = {};
      this.columns.forEach((c) => {
        if (c.conditions && c.conditions.length > 0)
          result[c.name] = c.conditions;
      });
      this.$emit("update:conditions", result);
    },
    setlimit(limit) {
      this.limit = limit;
      this.page = 1;
      this.$emit("update:showLimit", limit);
    },
  },
  computed: {
    viewIcon() {
      //icon should be next
      if (this.view == View.CARDS) return "list-alt";
      else if (this.view == View.TABLE) return "th";
      else return "th-list";
    },
    tableMetadataMerged() {
      let tm = this.tableMetadata;
      tm.columns = this.columns;
      return tm;
    },
    countFilters() {
      if (this.columns) {
        return this.columns.filter((f) => f.showFilter).length;
      }
      return null;
    },
    countColumns() {
      if (this.columns) {
        return this.columns.filter((f) => f.showColumn).length;
      }
      return null;
    },
    hasSubclass() {
      if (
        this.columns &&
        this.columns.filter((c) => c.name == "mg_tableclass").length > 0
      ) {
        return true;
      }
      return false;
    },
    //overrides from TableMixin
    graphqlFilter() {
      let filter = this.filter ? this.filter : {};
      if (this.columns) {
        this.columns.forEach((col) => {
          let conditions = Array.isArray(col.conditions)
            ? col.conditions.filter((f) => f !== "" && f != undefined)
            : [];
          if (conditions.length > 0) {
            if (col.columnType.startsWith("STRING")) {
              filter[col.name] = { like: col.conditions };
            } else if (col.columnType.startsWith("BOOL")) {
              filter[col.name] = { equals: col.conditions };
            } else if (col.columnType.startsWith("REF")) {
              filter[col.name] = { equals: col.conditions };
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
              filter[col.name] = {
                between: conditions.flat(),
              };
            }
          }
        });
      }
      return filter;
    },
  },
  watch: {
    page() {
      this.loading = true;
      this.offset = this.limit * (this.page - 1);
      this.$emit("update:showPage", this.page);
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
      if (this.columns.length == 0) {
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
            delete c.conditions;
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
};
</script>

<docs>
example (graphqlURL is usually not needed because app is served on right path)
```
<template>
  <div>
    <TableExplorer
        table="Pet"
        graphqlURL="/pet store/graphql"
        :showSelect="false" @click="click" :showColumns.sync="showColumns" :showFilters.sync="showFilters"
        :showPage.sync="page" :showLimit.sync="limit"
        :conditions.sync="conditions"/>
    showColumns: {{ showColumns }}<br/>
    showFilters: {{ showFilters }}<br/>
    conditions: {{ conditions }} <br/>
    page: {{ page }}<br/>
    limit: {{ limit }}

  </div>
</template>
<script>
  export default {
    data() {
      return {
        showColumns: ['name'], showFilters: ['name'], conditions: {"name": ["pooky", "spike"]}, page: 1, limit: 10
      }
    },
    methods: {
      click(event) {
        alert(JSON.stringify(event));
      }
    }
  }
</script>
```
</docs>
