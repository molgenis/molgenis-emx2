<template>
  <ShowMore>
    <pre>error = {{ error }}</pre>
    <pre>graphql = {{ graphql }}</pre>
    <pre>data = {{ data }}</pre>
  </ShowMore>
</template>

<script>
import { request } from "graphql-request";
import TableMetadataMixin from "./TableMetadataMixin";

export default {
  extends: TableMetadataMixin,
  props: {
    table: String,
    filter: {}
  },
  data: function() {
    return {
      data: [],
      count: 0,
      offset: 0,
      limit: 100,
      searchTerms: null
    };
  },
  computed: {
    //filter can be passed as prop or overridden in subclass
    graphqlFilter() {
      if (this.filter) {
        return this.filter;
      } else return {};
    },
    graphql() {
      if (this.tableMetadata == undefined) {
        return "";
      }
      let search =
        this.searchTerms != null && this.searchTerms !== ""
          ? '(filter:$filter,search:"' + this.searchTerms + '")'
          : "(filter:$filter)";
      return `query ${this.table}($filter:${this.table}Filter){${this.table}${search}{data_agg{count},data(limit:${this.limit},offset:${this.offset}){${this.columnNames}}}}`;
    },
    tableMetadata() {
      return this.getTable(this.table);
    },
    columnNames() {
      let result = "";
      if (this.tableMetadata != null) {
        this.tableMetadata.columns.forEach(col => {
          if (
            ["REF", "REF_ARRAY", "REFBACK", "MREF"].includes(col.columnType)
          ) {
            result = result + " " + col.name + "{" + this.refGraphql(col) + "}";
          } else {
            result = result + " " + col.name;
          }
        });
      }
      return result;
    }
  },
  methods: {
    reload() {
      if (this.tableMetadata != undefined) {
        this.loading = true;
        this.error = null;
        request("graphql", this.graphql, { filter: this.graphqlFilter })
          .then(data => {
            this.data = data[this.table]["data"];
            this.count = data[this.table]["data_agg"]["count"];
            this.loading = false;
          })
          .catch(error => {
            this.error = "internal server error" + error;
            this.loading = false;
          });
      }
    },
    refGraphql(column) {
      let graphqlString = "";
      this.getTable(column.refTable).columns.forEach(c => {
        if (c.key == 1) {
          graphqlString += c.name + " ";
          if (["REF", "REF_ARRAY", "REFBACK", "MREF"].includes(c.columnType)) {
            graphqlString += "{" + this.refGraphql(c) + "}";
          }
        }
      });
      return graphqlString;
    },
    getTable(table) {
      let result = undefined;
      if (this.schema != null && this.schema.tables != null) {
        this.schema.tables.forEach(t => {
          if (t.name == table) {
            result = t;
          }
        });
        if (!result) {
          this.error = "Table " + table + " not found";
        }
      }
      if (result) return result;
    }
  },
  watch: {
    searchTerms: "reload",
    graphqlFilter: {
      deep: true,
      handler() {
        this.reload();
        console.log(this.graphqlFilter);
      }
    },
    table: "reload",
    schema: "reload"
  }
};
</script>

<docs>
    ```
    <TableMixin table="Code"/>
    ```
</docs>
