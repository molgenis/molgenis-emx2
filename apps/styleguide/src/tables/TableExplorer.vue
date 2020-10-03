<template>
  <div>
    <div v-if="tableMetadata">
      <div class="navbar navbar-expand-lg">
        <h1>
          {{ table }}
        </h1>
        <InputSearch class="navbar-nav ml-auto" v-model="searchTerms" />
        <ShowHide
          class="navbar-nav "
          v-model="tableMetadata.columns"
          checkAttribute="showColumn"
          @input="updateTimestamp"
          icon="columns"
        />
        <ShowHide
          class="navbar-nav "
          v-model="tableMetadata.columns"
          checkAttribute="showFilter"
          @input="updateTimestamp"
          icon="filter"
        />
        <Pagination
          class="navbar-nav"
          v-model="page"
          :limit="limit"
          :count="count"
          :key="timestamp"
          @change="updateTimestamp"
        />
      </div>
      <MessageError v-if="error">{{ error }}</MessageError>
      <div class="row flex-nowrap" :key="timestamp">
        <div class="col-3" v-if="showFilters">
          <FilterSidebar :filters="tableMetadata.columns" />
        </div>
        <div v-if="loading" class="col-9">
          <Spinner />
        </div>
        <div v-else :class="{ 'col-9': showFilters, 'col-12': !showFilters }">
          <FilterWells v-if="table" :filters="tableMetadata.columns" />
          <TableMolgenis
            :metadata="tableMetadata"
            :data="data"
            class="table-responsive"
            :key="JSON.stringify(tableMetadata.columns)"
          >
            <template v-slot:header
              ><label>{{ count }} records found</label></template
            >
            <template v-slot:colheader>
              <RowButtonAdd v-if="canEdit" :table="table" @close="reload" />
            </template>
            <template v-slot:rowheader="slotProps">
              <RowButtonEdit
                v-if="canEdit"
                :table="table"
                :pkey="pkey(slotProps.row)"
                @close="reload"
              />
              <RowButtonDelete
                v-if="canEdit"
                :table="table"
                :pkey="pkey(slotProps.row)"
                @close="reload"
              />
            </template>
          </TableMolgenis>
        </div>
      </div>
    </div>
    <ShowMore title="debug">
      <pre>
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
    Pagination
  },
  data() {
    return {
      timestamp: 0,
      page: 1
    };
  },
  computed: {
    showFilters() {
      return (
        this.tableMetadata &&
        this.tableMetadata.columns.filter(c => c.showFilter === true).length > 0
      );
    },
    //overrides from TableMixin
    graphqlFilter() {
      let filter = {};
      if (this.tableMetadata) {
        this.tableMetadata.columns.forEach(col => {
          let conditions = Array.isArray(col.conditions)
            ? col.conditions.filter(f => f !== "" && f != undefined)
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
                "DATE_ARRAY"
              ].includes(col.columnType)
            ) {
              filter[col.name] = {
                between: conditions.flat()
              };
            }
          }
        });
      }
      return filter;
    },
    columns() {
      if (this.tableMetadata && this.tableMetadata.columns) {
        return this.tableMetadata.columns.map(col => col.name);
      }
      return null;
    },
    canEdit() {
      return (
        this.session.email == "admin" ||
        (this.session.roles &&
          (this.session.roles.includes("Editor") ||
            this.session.roles.includes("Manager")))
      );
    }
  },
  methods: {
    pkey(row) {
      let result = {};
      if (this.tableMetadata != null) {
        this.tableMetadata.columns.forEach(col => {
          if (col.key == 1) {
            result[col.name] = row[col.name];
          }
        });
      }
      return result;
    },
    updateTimestamp() {
      this.timestamp = new Date().getTime();
    }
  },
  watch: {
    page() {
      this.loading = true;
      this.offset = this.limit * (this.page - 1);
      this.reload();
    }
  }
};
</script>

<docs>
example (graphqlURL is usually not needed because app is served on right path)
```
<TableExplorer table="biobanks" graphqlURL="/testImportLegacyFormat/graphql"/>
```
</docs>
