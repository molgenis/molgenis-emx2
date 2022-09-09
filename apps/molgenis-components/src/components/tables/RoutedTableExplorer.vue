<template>
  <div>
    <TableExplorer
      :tableName="tableName"
      :graphqlURL="graphqlURL"
      :canEdit="canEdit"
      :canManage="canManage"
      @update:showColumns="updateColumns"
      @update:showAllColumns="updateAllColumns"
      @update:showFilters="updateFilters"
      @update:conditions="updateConditions"
      @update:showPage="updatePage"
      @update:showLimit="updateLimit"
      @update:showOrderBy="updateOrderBy"
      @update:showOrder="updateOrder"
      :showColumns="getColumns()"
      :showFilters="getFilters()"
      :conditions="getConditions()"
      :showPage="getPage()"
      :showLimit="getLimit()"
      :showOrderBy="getOrderBy()"
      :showOrder="getOrder()"
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
  data() {
    return {
      columnsData: [],
    };
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
      this.columnsData.forEach((c) => {
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
      const query = Object.assign({}, this.$route.query);
      this.query._orderBy = showOrderBy;
      this.queryRoute(query);
    },
    updateOrder(showOrder) {
      const query = Object.assign({}, this.$route.query);
      if (showOrder) {
        query._order = showOrder;
      }
      this.queryRoute(query);
    },
    updatePage(showPage) {
      const query = Object.assign({}, this.$route.query);
      query._page = showPage.toString();
      this.queryRoute(query);
    },
    updateLimit(showLimit) {
      const query = Object.assign({}, this.$route.query);
      query._limit = showLimit;
      this.queryRoute(query);
    },
    updateColumns(showColumns) {
      const query = Object.assign({}, this.$route.query);
      if (showColumns.length > 0) query._col = showColumns.join(",");
      else delete query._col;
      this.queryRoute(query);
    },
    updateAllColumns(showAllColumns) {
      this.columnsData = showAllColumns;
    },
    updateFilters(showFilters) {
      const query = Object.assign({}, this.$route.query);
      if (showFilters.length > 0) query._filter = showFilters.join(",");
      else delete query._filter;
      this.queryRoute(query);
    },
    updateConditions(conditions) {
      const query = Object.assign({}, this.$route.query);
      this.columnsData.forEach((c) => {
        if (conditions[c.name]) {
          if (["REF", "REF_ARRAY", "REFBACK"].includes(c.columnType)) {
            //todo try to make this human readible too
            query[c.name] = JSON.stringify(conditions[c.name]);
          } else if (
            ["DATE", "DATETIME", "INT", "DECIMAL"].includes(c.columnType)
          ) {
            query[c.name] = conditions[c.name]
              .map((v) => v.join(".."))
              .join(",");
          } else {
            query[c.name] = conditions[c.name].join(",");
          }
        } else {
          delete query[c.name];
        }
      });
      this.queryRoute(query);
    },
    queryRoute(query) {
      if (JSON.stringify(query) !== JSON.stringify(this.$route.query)) {
        this.$router.push({ query: query });
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