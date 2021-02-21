<template>
  <div>
    <div v-if="columns" style="overflow-x: scroll">
      <MessageError v-if="error">{{ error }}</MessageError>
      <h1>
        {{ table }}
        <ButtonAction v-if="hasSubclass" @click="showSubclass = !showSubclass">
          {{ showSubclass ? "Hide" : "Show" }} subclass rows
        </ButtonAction>
      </h1>
      <div
        class="navbar shadow-none navbar-expand-lg justify-content-between mb-2 bg-white"
      >
        <div class="btn-group">
          <ShowHide
            class="navbar-nav"
            :columns.sync="columns"
            checkAttribute="showFilter"
            label="filters"
            icon="filter"
          />
          <ShowHide
            class="navbar-nav"
            :columns.sync="columns"
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
            <h6>Download:</h6>
            <ButtonAlt :href="'../api/zip/' + table">zip</ButtonAlt>
            <br />
            <ButtonAlt :href="'../api/excel/' + table">excel</ButtonAlt>
            <br />
            <ButtonAlt :href="'../api/jsonld/' + table">jsonld</ButtonAlt>
            <br />
            <ButtonAlt :href="'../api/ttl/' + table">ttl</ButtonAlt>
          </ButtonDropdown>
        </div>
        <InputSearch class="navbar-nav" v-model="searchTerms" />

        <Pagination
          class="navbar-nav"
          v-model="page"
          :limit="limit"
          :count="count"
        />
        <SelectionBox :selection.sync="selectedItems" />
      </div>
      <div class="d-flex">
        <div v-if="countFilters" class="col-2 pl-0">
          <FilterSidebar :filters.sync="columns" />
        </div>
        <div
          class="flex-grow-1 pl-0 pr-0"
          :class="countFilters > 0 ? 'col-10' : 'col-12'"
        >
          <FilterWells v-if="table" :filters.sync="columns" />
          <div v-if="loading">
            <Spinner />
          </div>
          <TableMolgenis
            v-else
            :selection.sync="selectedItems"
            :columns.sync="columns"
            :table-metadata="tableMetadata"
            :data="data"
            :showSelect="true"
          >
            <template v-slot:header>
              <label>{{ count }} records found</label>
            </template>
            <template v-slot:colheader>
              <RowButtonAdd
                v-if="canEdit"
                :table="table"
                @close="reload"
                class="d-inline p-0"
              />
            </template>
            <template v-slot:rowheader="slotProps">
              <RowButtonEdit
                v-if="canEdit"
                :table="table"
                :pkey="getPkey(slotProps.row)"
                @close="reload"
              />
              <RowButtonDelete
                v-if="canEdit"
                :table="table"
                :pkey="getPkey(slotProps.row)"
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

        error = {{ error }}

        schema = {{ schema }}


      columns = {{ JSON.stringify(columns) }}

      table = {{ table }} }

      graphql={{ JSON.stringify(graphql) }}

      columnNames = {{ columnNames }}

      rows = {{ data }}

      tableMetadata={{ JSON.stringify(tableMetadata, null, "\t") }}

    data={{ JSON.stringify(data, null, "\t") }}
    </pre
      >
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
import ButtonAction from "../forms/ButtonAction";
import ButtonDropdown from "../forms/ButtonDropdown";

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
    ShowMore,
    ShowHide,
    InputSearch,
    Pagination,
    ButtonAlt,
    ButtonAction,
    ButtonDropdown,
    SelectionBox,
  },
  props: {
    value: {
      type: Array,
      default: () => [],
    },
  },
  data() {
    return {
      selectedItems: [],
      page: 1,
      showSubclass: false,
      //a copy of column metadata used to show/hide filters and columns
      columns: [],
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
      let filter = {};
      if (this.columns) {
        //filter on subclass, if exists
        if (!this.showSubclass && this.hasSubclass) {
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
      console.log(JSON.stringify(filter));
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
        console.log("bla");
        this.columns.push(...this.tableMetadata.columns);
      }
    },
  },
};
</script>

<docs>
example (graphqlURL is usually not needed because app is served on right path)
```
<Molgenis>
  <TableExplorer table="Variables" graphqlURL="/CohortsCentral/graphql" :showSelect="true"/>
</Molgenis>
```
</docs>
