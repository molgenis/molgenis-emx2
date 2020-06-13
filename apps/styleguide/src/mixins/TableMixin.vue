<template>
  <div>nothing</div>
</template>

<script>
import { request } from "graphql-request";
import TableMetadataMixin from "./TableMetadataMixin";

export default {
  mixins: [TableMetadataMixin],
  data: function() {
    return {
      data: [],
      count: 0,
      offset: 0,
      limit: 100,
      searchTerms: null
    };
  },
  methods: {
    reload() {
      this.loading = true;
      request("graphql", this.graphql)
        .then(data => {
          this.error = null;
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
  computed: {
    graphql() {
      let search =
        this.searchTerms != null && this.searchTerms !== ""
          ? '(search:"' + this.searchTerms + '")'
          : "";
      return `{${this.table}${search}{data_agg{count},data(limit:${this.limit},offset:${this.offset}){${this.columnNames}
        }}}`;
    },
    columnNames() {
      let result = "";
      this.metadata.columns.forEach(element => {
        if (["REF", "REF_ARRAY", "REFBACK"].includes(element.columnType)) {
          result = result + " " + element.name + "{" + element.refColumn + "}";
        } else {
          result = result + " " + element.name;
        }
      });
      return result;
    }
  },
  watch: {
    searchTerms: "reload",
    metadata: "reload"
  }
};
</script>
