<template>
    <div class="h-100">
        <div v-if="columns" clas="overflow-auto">
            <MessageError v-if="graphqlError">
                {{ graphqlError }}
            </MessageError>
            <div class="bg-white">
                <h1 v-if="showHeader" class="pl-2">
                    {{ table }}
                </h1>
                <p v-if="showHeader && tableMetadata" class="pl-2">
                    {{ tableMetadata.description }}
                </p>
                <div
                    class="navbar pl-0 ml-0 shadow-none navbar-expand-lg justify-content-between mb-3 pt-3 bg-white"
                >
                    <div class="btn-group">
                        <ShowHide
                            v-model:columns="columns"
                            check-attribute="showFilter"
                            class="navbar-nav"
                            icon="filter"
                            label="filters"
                        />
                        <ShowHide
                            id="showColumn"
                            v-model:columns="columns"
                            check-attribute="showColumn"
                            class="navbar-nav"
                            :default-value="true"
                            icon="columns"
                            label="columns"
                        />
                        <ButtonDropdown icon="download" label="download" v-slot="scope">
                            <IconAction
                                class="float-right"
                                icon="times"
                                style="margin-top: -10px; margin-right: -10px"
                                @click="scope.close"
                            />
                            <h6>Download:</h6>
                            <ButtonAlt :href="'../api/zip/' + table">
                                zip
                            </ButtonAlt>
                            <br>
                            <ButtonAlt :href="'../api/excel/' + table">
                                excel
                            </ButtonAlt>
                            <br>
                            <ButtonAlt :href="'../api/jsonld/' + table">
                                jsonld
                            </ButtonAlt>
                            <br>
                            <ButtonAlt :href="'../api/ttl/' + table">
                                ttl
                            </ButtonAlt>
                        </ButtonDropdown>
                        <IconAction
                            class="ml-2"
                            :icon="layoutTable ? 'th' : 'table'"
                            :label="'layout'"
                            @click="layoutTable = !layoutTable"
                        />
                    </div>
                    <InputSearch v-model="searchTerms" class="navbar-nav" />
                    <Pagination
                        v-model="page"
                        class="navbar-nav"
                        :count="count"
                        :limit="limit"
                    />
                    <div class="btn-group">
                        <span class="btn">Rows per page:</span>
                        <InputSelect
                            :clear="false"
                            :options="[10, 20, 50, 100]"
                            :value="limit"
                            @input="setlimit($event)"
                        />
                    </div>
                    <SelectionBox v-if="showSelect" v-model:selection="selectedItems" />
                </div>
            </div>
            <div class="d-flex">
                <div v-if="countFilters" class="col-3 pl-0">
                    <FilterSidebar v-model:filters="columns" />
                </div>
                <div
                    class="flex-grow-1 pr-0 pl-0"
                    :class="countFilters > 0 ? 'col-9' : 'col-12'"
                >
                    <FilterWells
                        v-if="table"
                        v-model:filters="columns"
                        class="border-top pt-3 pb-3"
                    />
                    <div v-if="loading">
                        <Spinner />
                    </div>
                    <TableCards
                        v-if="!loading && !layoutTable"
                        :can-edit="canEdit"
                        :columns="columns"
                        :data="data"
                        :table-name="table"
                        @click="$emit('click', $event)"
                        @reload="reload"
                    />
                    <TableMolgenis
                        v-if="!loading && layoutTable"
                        v-model:columns="columns"
                        v-model:selection="selectedItems"
                        :data="data"
                        :show-select="showSelect"
                        :table-metadata="tableMetadata"
                        @click="$emit('click', $event)"
                    >
                        <template #header>
                            <label>{{ count }} records found</label>
                        </template>
                        <template #colheader>
                            <RowButtonAdd
                                v-if="canEdit"
                                class="d-inline p-0"
                                :table="table"
                                @close="reload"
                            />
                        </template>
                        <template #rowheader="slotProps">
                            <RowButtonEdit
                                v-if="canEdit"
                                :pkey="getPkey(slotProps.row)"
                                :table="table"
                                @close="reload"
                            />
                            <RowButtonDelete
                                v-if="canEdit"
                                :pkey="getPkey(slotProps.row)"
                                :table="table"
                                @close="reload"
                            />
                        </template>
                    </TableMolgenis>
                </div>
            </div>
        </div>
        <ShowMore title="debug">
            <pre>
        columns = {{ columns }}

        selection = {{ selectedItems }}

      graphqlFilter = {{ JSON.stringify(graphqlFilter) }}

      session = {{ session }}

        graphqlError = {{ graphqlError }}

        schema = {{ schema }}


      columns = {{ JSON.stringify(columns) }}

      table = {{ table }} }

      graphql={{ JSON.stringify(graphql) }}

      columnNames = {{ columnNames }}

      rows = {{ data }}

      tableMetadata={{ JSON.stringify(tableMetadata, null, "\t") }}

    data={{ JSON.stringify(data, null, "\t") }}
    </pre>
        </ShowMore>
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
import ShowMore from "../layout/ShowMore";
import ShowHide from "./ShowHide";
import InputSearch from "../forms/InputSearch";
import Pagination from "./Pagination";
import ButtonAlt from "../forms/ButtonAlt";
import SelectionBox from "./SelectionBox";
import ButtonDropdown from "../forms/ButtonDropdown";
import InputSelect from "../forms/InputSelect";
import TableCards from "./TableCards";
import IconAction from "../forms/IconAction";

export default {
  components: {
    Spinner,
    MessageError,
    TableMolgenis,
    FilterSidebar,
    FilterWells,
    RowButtonEdit,
    RowButtonAdd,
    RowButtonDelete,
    ShowMore,
    ShowHide,
    InputSearch,
    Pagination,
    ButtonAlt,
    ButtonDropdown,
    SelectionBox,
    InputSelect,
    TableCards,
    IconAction,
  },
  extends: TableMixin,
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
    showCards: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      selectedItems: [],
      page: 1,
      showSubclass: false,
      //a copy of column metadata used to show/hide filters and columns
      columns: [],
      layoutTable: true,
    };
  },
  computed: {
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
        //filter on subclass, if exists
        if (this.hasSubclass) {
          filter.mg_tableclass = {
            equals: this.schema.name + "." + this.tableMetadata.name,
          };
        }
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
      this.reload();
    },
    tableMetadata() {
      if (this.columns.length == 0) {
        this.columns.push(
          ...this.tableMetadata.columns.filter((c) => c.name != "mg_tableclass")
        );
        // //init settings
        this.columns.forEach((c) => {
          if (this.showColumns.length > 0 && !this.showColumns.includes(c.name))
            c.showColumn = false;
          else c.showColumn = true;
          if (this.showColumns.length > 0 && this.showFilters.includes(c.name))
            c.showFilter = true;
          else c.showFilter = false;
        });
        if (this.showCards) {
          this.layoutTable = false;
        }
      }
    },
  },
  methods: {
    setlimit(limit) {
      console.log("resetpage");
      this.limit = limit;
      this.page = 1;
    },
  },
};
</script>
