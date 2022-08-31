<template>
  <div>
    <router-link v-if="schema" to="/">
      &gt; Back to {{ schema.name }}
    </router-link>
    <TableExplorer
      :tableName="table"
      :showColumns="showColumns"
      :showFilters="showFilters"
      :conditions="conditions"
      :showPage="showPage"
      :showLimit="showLimit"
      :showOrderBy="showOrderBy"
      :showOrder="showOrder"
      :key="timestamp"
    />
  </div>
</template>

<script>
import { TableExplorer } from "molgenis-components";

export default {
  name: "ViewTable",
  props: {
    table: { type: String, required: true },
    schema: { type: Object, default: null },
  },
  data() {
    return { timestamp: Date.now(), query: {} };
  },
  components: {
    TableExplorer,
  },
  methods: {
    updateOrderBy(showOrderBy) {
      this.query = Object.assign({}, this.$route.query);
      this.query._orderBy = showOrderBy;
      if (JSON.stringify(this.query) != JSON.stringify(this.$route.query)) {
        this.$router.push({ query: this.query });
      }
    },
    updateOrder(showOrder) {
      this.query = Object.assign({}, this.$route.query);
      if (showOrder) {
        this.query._order = showOrder;
      }
      if (JSON.stringify(this.query) != JSON.stringify(this.$route.query)) {
        this.$router.push({ query: this.query });
      }
    },
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
    showOrderBy() {
      if (this.$route.query._orderBy) {
        return this.$route.query._orderBy;
      } else {
        return null;
      }
    },
    showOrder() {
      if (this.$route.query._order) {
        return this.$route.query._order;
      } else {
        return null;
      }
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
      return [];
    },
  },
  watch: {
    $route(to) {
      //this is to prevent updates if changes come from outside vs inside
      if (JSON.stringify(to.query) != JSON.stringify(this.query)) {
        this.timestamp = Date.now();
        this.query = to.query;
      }
    },
  },
};
</script>
