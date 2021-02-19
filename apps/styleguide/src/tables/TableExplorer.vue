<template>
  <div>
    <div v-if="tableMetadata">
      <MessageError v-if="error">{{ error }}</MessageError>
      <h1>
        {{ table }}
        <ButtonAction v-if="hasSubclass" @click="showSubclass = !showSubclass">
          {{ showSubclass ? "Hide" : "Show" }} subclass rows
        </ButtonAction>
      </h1>
      <div class="navbar shadow-none navbar-expand-lg justify-content-between">
        <InputSearch class="navbar-nav" v-model="searchTerms" />
        <div class="btn-group">
          <ShowHide
            class="navbar-nav"
            v-model="tableMetadata.columns"
            checkAttribute="showFilter"
            @input="updateTimestamp"
            label="filters"
            icon="filter"
          />
          <ShowHide
            class="navbar-nav"
            v-model="tableMetadata.columns"
            checkAttribute="showColumn"
            @input="updateTimestamp"
            label="columns"
            icon="columns"
          />
        </div>
        <Pagination
          class="navbar-nav"
          v-model="page"
          :limit="limit"
          :count="count"
          :key="timestamp"
          @change="updateTimestamp"
        />
        <SelectionBox v-model="selectedItems" />
      </div>
      <div>
        Download:
        <ButtonAlt :href="'../api/zip/' + table">zip</ButtonAlt>
        |
        <ButtonAlt :href="'../api/excel/' + table">excel</ButtonAlt>
        |
        <ButtonAlt :href="'../api/jsonld/' + table">jsonld</ButtonAlt>
        |
        <ButtonAlt :href="'../api/ttl/' + table">ttl</ButtonAlt>
      </div>
      <div class="row" :key="timestamp">
        <div v-if="showFilters" class="col col-3">
          <FilterSidebar v-model="tableMetadata.columns" />
        </div>
        <div
          class="col"
          :class="{ 'col-9': showFilters, 'col-12': !showFilters }"
        >
          <FilterWells v-if="table" v-model="tableMetadata.columns" />
          <div v-if="loading">
            <Spinner />
          </div>
          <TableMolgenis
            v-else
            v-model="selectedItems"
            :metadata="tableMetadata"
            :data="data"
            :key="JSON.stringify(tableMetadata.columns)"
            :showSelect="true"
          >
            <template v-slot:header>
              <label>{{ count }} records found</label>
            </template>
            <template v-slot:colheader>
              <RowButtonAdd v-if="canEdit" :table="table" @close="reload" />
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
      timestamp: 0,
      page: 1,
      showFilters: false,
      showSubclass: false,
    };
  },
  computed: {
    hasSubclass() {
      if (this.tableMetadata && this.columnNames.includes("mg_tableclass")) {
        return true;
      }
      return false;
    },
    //overrides from TableMixin
    graphqlFilter() {
      let filter = {};
      if (this.tableMetadata) {
        //filter on subclass, if exists
        if (!this.showSubclass && this.hasSubclass) {
          filter.mg_tableclass = {
            equals: this.schema.name + "." + this.tableMetadata.name,
          };
        }
        this.tableMetadata.columns.forEach((col) => {
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
    columns() {
      if (this.tableMetadata && this.tableMetadata.columns) {
        return this.tableMetadata.columns.map((col) => col.name);
      }
      return null;
    },
  },
  methods: {
    updateTimestamp() {
      this.timestamp = new Date().getTime();
    },
  },
  watch: {
    page() {
      this.loading = true;
      this.offset = this.limit * (this.page - 1);
      this.reload();
    },
    value() {
      this.selectedItems = this.value;
    },
    timestamp: {
      deep: true,
      handler() {
        this.showFilters = false;
        if (this.tableMetadata) {
          this.tableMetadata.columns.forEach((f) => {
            if (f.showFilter) {
              this.showFilters = true;
            }
          });
        }
      },
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
