<template>
  <div>
    <TableExplorer
      :tableName="tableName"
      :graphqlURL="graphqlURL"
      :canEdit="canEdit"
      :canManage="canManage"
      @update:showColumns="updateColumns"
      @update:showFilters="updateFilters"
      @update:conditions="updateConditions"
      @update:showPage="updatePage"
      @update:showLimit="updateLimit"
      @update:showOrderBy="updateOrderBy"
      @update:showOrder="updateOrder"
      :showColumns="setColumns"
      :showFilters="setFilters"
      :conditions="setConditions"
      :showPage="setPage"
      :showLimit="setLimit"
      :showOrderBy="setOrderBy"
      :showOrder="setOrder"
    />
  </div>
</template>

<script>
import TableExplorer from "./TableExplorer.vue";

export default {
  name: "RoutedTableExplorer",
  components: {
    TableExplorer,
  },
  props: {
    tableName: {
      type: String,
      required: true,
    },
    graphqlURL: {
      type: String,
      default: () => "graphql",
    },
    canEdit: {
      type: Boolean,
      default: () => false,
    },
    canManage: {
      type: Boolean,
      default: () => false,
    },
  },
  methods: {
    getOrderBy() {
      if (this.$route.query._orderBy) {
        return this.$route.query._orderBy;
      } else {
        return "";
      }
    },
    getOrder() {
      if (this.$route.query._order) {
        return this.$route.query._order;
      } else {
        return "ASC";
      }
    },
    getColumns() {
      if (this.$route.query._col) {
        if (Array.isArray(this.$route.query._col)) {
          return this.$route.query._col;
        } else {
          return this.$route.query._col.split(",");
        }
      }
      return [];
    },
    getFilters() {
      if (this.$route.query._filter) {
        if (Array.isArray(this.$route.query._filter)) {
          return this.$route.query._filter;
        } else {
          return this.$route.query._filter.split(",");
        }
      }
      return [];
    },
    getPage() {
      if (this.$route.query._page) return parseInt(this.$route.query._page);
      else return 1;
    },
    getLimit() {
      if (this.$route.query._limit) return parseInt(this.$route.query._limit);
      else return 20;
    },
    getConditions() {
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
    },
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
};
</script>

<docs>
<template>
  <div>
    <div class="border p-1 my-1">
      <label>Read only example</label>
      <routed-table-explorer
        id="my-table-explorer"
        tableName="Pet"
        graphqlURL="/pet store/graphql"
        :canEdit="canEdit"
        :canManage="canManage"
      ></routed-table-explorer>
      <div class="border mt-3 p-2">
        <h5>synced props: </h5>
        <div>
          <label for="canEdit" class="pr-1">can edit: </label>
          <input type="checkbox" id="canEdit" v-model="canEdit">
        </div>
        <div>
          <label for="canManage" class="pr-1">canManage: </label>
          <input type="checkbox" id="canManage" v-model="canManage">
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    data() {
      return {
        canEdit: true,
        canManage: true
      }
    },
  }
</script>
</docs>