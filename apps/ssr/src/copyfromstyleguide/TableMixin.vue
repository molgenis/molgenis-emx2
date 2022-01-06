<script>
//this has been changed compared to styleguide to use fetch api directly, because library seems to be not ssr compatible
import TableMetadataMixin from "./TableMetadataMixin";
// //ponyfill
// import fetch from "../util/fetch";

export default {
  extends: TableMetadataMixin,
  props: {
    /** Name of the table within graphql endpoint */
    table: String,
    /** pass filters conform TableMixin */
    filter: {},
    /** pass orderBy as Object {field1: 'ASC', field2 {field3:'ASC'}}*/
    orderBy: {},
    initialSearchTerms: {
      type: String,
      default: () => null,
    },
  },
  data: function () {
    return {
      data: [],
      count: 0,
      offset: 0,
      limit: 20,
      searchTerms: this.initialSearchTerms,
    };
  },
  computed: {
    tableId() {
      return this.tableMetadata.id;
    },
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
          ? ',search:"' + this.searchTerms.trim() + '"'
          : "";
      return `query ${this.tableId}($filter:${this.tableId}Filter, $orderby:${this.tableId}orderby){
              ${this.tableId}(filter:$filter,limit:${this.limit},offset:${this.offset}${search},orderby:$orderby){${this.columnNames}}
              ${this.tableId}_agg(filter:$filter${search}){count}}`;
    },
    tableMetadata() {
      return this.getTable(this.table);
    },
    //this method allows overrides
    orderByObject() {
      if (this.orderBy) {
        return this.orderBy;
      } else {
        return {};
      }
    },
    columnNames() {
      let result = "";
      if (this.tableMetadata != null) {
        this.tableMetadata.columns.forEach((col) => {
          if (
            [
              "REF",
              "REF_ARRAY",
              "REFBACK",
              "ONTOLOGY",
              "ONTOLOGY_ARRAY",
            ].includes(col.columnType) > 0
          ) {
            result = result + " " + col.id + "{" + this.refGraphql(col) + "}";
          } else if (col.columnType == "FILE") {
            result = result + " " + col.id + "{id,size,extension,url}";
          } else if (col.columnType != "HEADING") {
            result = result + " " + col.id;
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
        this.graphqlError = null;
        //using fetch https://www.netlify.com/blog/2020/12/21/send-graphql-queries-with-the-fetch-api-without-using-apollo-urql-or-other-graphql-clients/
        fetch(this.graphqlURL, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            query: this.graphql,
            variables: {
              filter: this.graphqlFilter,
              orderby: this.orderByObject,
            },
          }),
        })
          .then((res) => {
            res.json().then((data) => {
              console.log(data);
              this.data = data.data[this.tableId];
              this.count = data.data[this.tableId + "_agg"]["count"];
              this.loading = false;
            });
          })
          .catch((error) => {
            if (Array.isArray(error.response.errors)) {
              this.graphqlError = error.response.errors[0].message;
            } else {
              this.graphqlError = error;
            }
            this.loading = false;
          });
      }
    },
    refGraphql(column) {
      let graphqlString = "";
      this.getTable(column.refTable).columns.forEach((c) => {
        if (c.key == 1) {
          graphqlString += c.id + " ";
          if (
            [
              "REF",
              "REF_ARRAY",
              "REFBACK",
              "ONTOLOGY",
              "ONTOLOGY_ARRAY",
            ].includes(c.columnType) > 0
          ) {
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
          this.graphqlError = "Table " + table + " not found";
        }
      }
      if (result) return result;
    },
    getPkey(row) {
      //we only have pkey when the record has been saved
      if (!row["mg_insertedOn"]) return null;
      let result = {};
      if (this.tableMetadata != null) {
        this.tableMetadata.columns.forEach((col) => {
          if (col.key == 1 && row[col.id]) {
            result[col.id] = row[col.id];
          }
        });
      }
      return result;
    },
  },
  watch: {
    searchTerms: {
      handler(newValue) {
        this.$emit("searchTerms", newValue);
        this.reload();
      },
    },
    graphqlFilter: {
      deep: true,
      handler() {
        this.reload();
      },
    },
    table: "reload",
    schema: "reload",
    limit: "reload",
  },
};
</script>

<docs>
```
<!-- normally you don't need provide graphqlURL because default 'graphql' just works -->
<TableMixin table="Pet" graphqlURL="/pet store/graphql"/>
```
</docs>
