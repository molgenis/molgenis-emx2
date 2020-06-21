<template>
  <div v-if="schema">
    <router-link to="/">< Back to {{ schema.name }}</router-link>
    <h1>{{ tableName }}</h1>
    <MessageError v-if="error">{{ error }}</MessageError>

    <div class="row flex-nowrap">
      <div class=" col col-mg-4 col-lg-4">
        <FilterSidebar v-if="table" :filters="table.columns" />
      </div>
      <div v-if="loading" class="col">
        <Spinner />
      </div>
      <div v-else class="col">
        <div>
          <label>{{ count }} records found</label>
        </div>
        <FilterWells v-if="table" :filters="table.columns" />
        <DataTable :columns="columns" :rows="rows" class="table-responsive">
          <template v-slot:colheader>
            <RowButtonAdd :table="tableName" @close="reload" />
          </template>
          <template v-slot:rowheader="slotProps">
            <IconBar>
              <RowButtonEdit
                :table="tableName"
                :pkey="slotProps.row[table.pkey]"
                @close="reload"
              />
              <RowButtonDelete
                :table="tableName"
                :pkey="slotProps.row[table.pkey]"
                @close="reload"
              />
            </IconBar>
          </template>
        </DataTable>
      </div>
    </div>

    <br />DEBUG
    <br />
    filter = {{ JSON.stringify(graphqlFilter) }}
    <br />
    columns = {{ JSON.stringify(columns) }}
    <br />
    tableName = {{ tableName }}
    <br />
    molgenis={{ JSON.stringify(molgenis) }}
    <br />
    graphql={{ JSON.stringify(graphql) }}
    <br />
    columnNames = {{ columnNames }}
    <br />
    rows = {{ rows }}
    <br />
    filters = {{ filters }}
    <br />
    <pre>table={{ JSON.stringify(table, null, "\t") }}</pre>

    <br />
    <pre>data={{ JSON.stringify(data, null, "\t") }}</pre>
  </div>
</template>

<script>
import { request } from "graphql-request";
import {
  DataTable,
  FilterSidebar,
  FilterWells,
  IconBar,
  MessageError,
  RowButtonAdd,
  RowButtonDelete,
  RowButtonEdit,
  Spinner
} from "@mswertz/emx2-styleguide";

export default {
  components: {
    Spinner,
    MessageError,
    DataTable,
    FilterSidebar,
    FilterWells,
    RowButtonEdit,
    RowButtonAdd,
    RowButtonDelete,
    IconBar
  },
  props: {
    tableName: String,
    schema: Object,
    molgenis: Object
  },
  data() {
    return {
      loading: false,
      error: null,
      data: [],
      count: 0,
      limit: 100,
      offset: 0,
      select: {},
      filters: null
    };
  },
  computed: {
    graphql() {
      return `query ${this.tableName}($filter:${this.tableName}filter) {${this.tableName}(filter:$filter){data_agg{count},data(limit:${this.limit},offset:${this.offset}){${this.columnNames}}}}`;
    },
    graphqlFilter() {
      let filter = {};
      if (this.table) {
        this.table.columns.forEach(col => {
          let conditions = Array.isArray(col.conditions)
            ? col.conditions.filter(f => f !== "" && f != undefined)
            : [];
          if (conditions.length > 0) {
            if (col.columnType.startsWith("STRING")) {
              filter[col.name] = { like: col.conditions };
            } else if (col.columnType.startsWith("BOOL")) {
              filter[col.name] = { equals: col.conditions };
            } else if (col.columnType.startsWith("REF")) {
              //TODO should instead use REF TYPE!
              filter[col.name] = {};
              filter[col.name][col.refColumn] = { equals: conditions };
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
    table() {
      let table = null;
      if (this.schema != null) {
        this.schema.tables.forEach(element => {
          if (element.name === this.tableName) {
            table = element;
          }
        });
      }
      return table;
    },
    columnNames() {
      let result = "";
      if (this.table != null) {
        this.table.columns.forEach(col => {
          if (["REF", "REF_ARRAY", "REFBACK"].includes(col.columnType)) {
            result = result + " " + col.name + "{" + col.refColumn + "}";
          } else {
            result = result + " " + col.name;
          }
        });
      }
      return result;
    },
    columns() {
      if (this.table && this.table.columns) {
        return this.table.columns.map(col => col.name);
      }
      return null;
    },
    rows() {
      if (this.data != null && this.table != null) {
        return this.data.map(row => {
          let result = { ...row };
          this.table.columns.forEach(col => {
            if (row[col.name]) {
              if (col.columnType === "REF") {
                result[col.name] = row[col.name][col.refColumn];
              } else if (
                col.columnType === "REF_ARRAY" ||
                col.columnType === "REFBACK"
              ) {
                result[col.name] = row[col.name].map(val => val[col.refColumn]);
              }
            }
          });
          return result;
        });
      }
      return this.data;
    }
  },
  methods: {
    reload() {
      console.log(
        JSON.stringify(this.graphqlFilter) + "----" + JSON.stringify(this.table)
      );
      this.data = null;
      if (this.schema == null) return;
      this.loading = true;
      request("graphql", this.graphql, { filter: this.graphqlFilter })
        .then(data => {
          this.error = null;
          this.data = data[this.tableName]["data"];
          this.count = data[this.tableName]["data_agg"]["count"];
        })
        .catch(error => {
          this.error = "internal server error" + error;
        })
        .finally((this.loading = false));
    }
  },
  watch: {
    graphqlFilter() {
      this.reload();
    }
  },
  created() {
    console.log("CREATED");
    this.reload();
  }
};
</script>
