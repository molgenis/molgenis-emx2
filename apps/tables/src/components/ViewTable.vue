<template>
  <div>
    <router-link v-if="schema" to="/">< Back to {{ schema.name }}</router-link>
    <TableExplorer
      :table="table"
      :showColumns="showColumns"
      @update:showColumns="updateColumns"
      :showFilters="showFilters"
      @update:showFilters="updateFilters"
      :conditions="conditions"
      @update:conditions="updateConditions"
      :showPage="showPage"
      @update:showPage="updatePage"
      :showLimit="showLimit"
      @update:showLimit="updateLimit"
      :key="timestamp"
    />
  </div>
</template>

<script>
import { TableExplorer } from "@mswertz/emx2-styleguide";

export default {
  props: {
    table: String,
    schema: Object,
  },
  data() {
    return { timestamp: Date.now(), query: {} };
  },
  components: {
    TableExplorer,
  },
  methods: {
    updatePage(showPage) {
      this.query = Object.assign({}, this.$route.query);
      this.query._page = showPage;
      if (JSON.stringify(this.query) != JSON.stringify(this.$route.query)) {
        this.$router.push({ query: this.query });
      }
    },
    updateLimit(showLimit) {
      this.query = Object.assign({}, this.$route.query);
      this.query._limit = showLimit;
      if (JSON.stringify(this.query) != JSON.stringify(this.$route.query)) {
        this.$router.push({ query: this.query });
      }
    },
    updateColumns(showColumns) {
      this.query = Object.assign({}, this.$route.query);
      if (showColumns.length > 0) this.query._col = showColumns.join(",");
      else delete this.query._col;
      if (JSON.stringify(this.query) != JSON.stringify(this.$route.query)) {
        this.$router.push({ query: this.query });
      }
    },
    updateFilters(showFilters) {
      this.query = Object.assign({}, this.$route.query);
      if (showFilters.length > 0) this.query._filter = showFilters.join(",");
      else delete this.query._filter;
      if (JSON.stringify(this.query) != JSON.stringify(this.$route.query)) {
        this.$router.push({ query: this.query });
      }
    },
    updateConditions(conditions) {
      this.query = Object.assign({}, this.$route.query);
      this.activeTable.columns.forEach((c) => {
        if (conditions[c.name]) {
          if (["REF", "REF_ARRAY", "REFBACK"].includes(c.columnType)) {
            //todo try to make this human readible too
            this.query[c.name] = JSON.stringify(conditions[c.name]);
          } else if (
            ["DATE", "DATETIME", "INT", "DECIMAL"].includes(c.columnType)
          ) {
            this.query[c.name] = conditions[c.name]
              .map((v) => v.join(".."))
              .join(",");
          } else {
            this.query[c.name] = conditions[c.name].join(",");
          }
        } else {
          delete this.query[c.name];
        }
      });
      if (JSON.stringify(this.query) != JSON.stringify(this.$route.query)) {
        this.$router.push({ query: this.query });
      }
    },
  },
  computed: {
    activeTable() {
      if (this.schema) {
        return this.schema.tables.filter((t) => t.name == this.table)[0];
      }
      return null;
    },
    showColumns() {
      if (this.$route.query._col) {
        if (Array.isArray(this.$route.query._col)) {
          return this.$route.query._col;
        } else {
          return this.$route.query._col.split(",");
        }
      }
      return null;
    },
    showFilters() {
      if (this.$route.query._filter) {
        if (Array.isArray(this.$route.query._filter)) {
          return this.$route.query._filter;
        } else {
          return this.$route.query._filter.split(",");
        }
      }
      return null;
    },
    showPage() {
      if (this.$route.query._page) return parseInt(this.$route.query._page);
      else return 1;
    },
    showLimit() {
      if (this.$route.query._limit) return parseInt(this.$route.query._limit);
      else return 20;
    },
    conditions() {
      if (this.schema) {
        let result = {};
        //find the table and then iterate the colums
        this.activeTable.columns.forEach((c) => {
          if (this.$route.query[c.name]) {
            if (["DATE", "DATETIME", "INT", "DECIMAL"].includes(c.columnType)) {
              result[c.name] = this.$route.query[c.name]
                .split(",")
                .map((v) => v.split(".."));
            } else if (["REF", "REF_ARRAY", "REFBACK"].includes(c.columnType)) {
              result[c.name] = JSON.parse(this.$route.query[c.name]);
            } else {
              result[c.name] = this.$route.query[c.name].split(",");
            }
          }
        });
        return result;
      }
      return null;
    },
  },
  watch: {
    $route(to, from) {
      //this is to prevent updates if changes come from outside vs inside
      if (JSON.stringify(to.query) != JSON.stringify(this.query)) {
        this.timestamp = Date.now();
        this.query = to.query;
      }
    },
  },
};
</script>
