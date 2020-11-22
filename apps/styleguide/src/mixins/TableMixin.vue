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
    filter: {},
  },
  data: function () {
    return {
      data: [],
      count: 0,
      offset: 0,
      limit: 20,
      searchTerms: null,
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
          ? ',search:"' + this.searchTerms + '"'
          : "";
      return `query ${this.table}($filter:${this.table}Filter){
              ${this.table}(filter:$filter,limit:${this.limit},offset:${this.offset}${search}){${this.columnNames}}
              ${this.table}_agg(filter:$filter${search}){count}}`;
    },
    tableMetadata() {
      return this.getTable(this.table);
    },
    columnNames() {
      let result = "";
      if (this.tableMetadata != null) {
        this.tableMetadata.columns.forEach((col) => {
          if (
            ["REF", "REF_ARRAY", "REFBACK", "MREF"].includes(col.columnType)
          ) {
            result = result + " " + col.name + "{" + this.refGraphql(col) + "}";
          } else if (col.columnType == "FILE") {
            result = result + " " + col.name + "{id,size,extension}";
          } else {
            result = result + " " + col.name;
          }
        });
      }
      return result;
    },
  },
  methods: {
    reload() {
      if (this.tableMetadata != undefined) {
        this.loading = true;
        this.error = null;
        request(this.graphqlURL, this.graphql, { filter: this.graphqlFilter })
          .then((data) => {
            this.data = data[this.table];
            this.count = data[this.table + "_agg"]["count"];
            this.loading = false;
          })
          .catch((error) => {
            this.error = "internal server error" + error;
            this.loading = false;
          });
      }
    },
    refGraphql(column) {
      let graphqlString = "";
      this.getTable(column.refTable).columns.forEach((c) => {
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
        this.schema.tables.forEach((t) => {
          if (t.name == table) {
            result = t;
          }
        });
        if (!result) {
          this.error = "Table " + table + " not found";
        }
      }
      if (result) return result;
    },
  },
  watch: {
    searchTerms: "reload",
    graphqlFilter: {
      deep: true,
      handler() {
        this.reload();
      },
    },
    table: "reload",
    schema: "reload",
  },
};
</script>

<docs>
```
<!-- normally you don't need provide graphqlURL because default 'graphql' just works -->
<TableMixin table="Code" graphqlURL="/TestCohortCatalogue/graphql"/>
```
</docs>
