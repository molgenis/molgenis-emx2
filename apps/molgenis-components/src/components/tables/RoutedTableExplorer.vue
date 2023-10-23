<template>
  <div>
    <TableExplorer
      :tableId="tableId"
      :schemaId="schemaId"
      :canView="canView"
      :canEdit="canEdit"
      :canManage="canManage"
      @updateConditions="updateConditions"
      @updateShowColumns="updateColumns"
      @updateShowFilters="updateFilters"
      @updateShowPage="updatePage"
      @updateShowLimit="updateLimit"
      @updateShowOrder="updateOrder"
      @updateShowView="updateView"
      :showView="getView()"
      :showColumns="getColumns()"
      :showFilters="getFilters()"
      :urlConditions="getConditions()"
      :showPage="getPage()"
      :showLimit="getLimit()"
      :showOrderBy="getOrderBy()"
      :showOrder="getOrder()"
      :locale="locale"
      @rowClick="$emit('rowClick', $event)"
    >
      <template v-slot:rowcolheader>
        <slot name="rowcolheader" />
      </template>
      <template v-slot:rowheader="slotProps">
        <slot
          name="rowheader"
          :row="slotProps.row"
          :metadata="slotProps.metadata"
          :rowKey="slotProps.rowKey"
        />
      </template>
    </TableExplorer>
  </div>
</template>

<script>
import TableExplorer from "./TableExplorer.vue";
import { deepClone } from "../utils";

export default {
  name: "RoutedTableExplorer",
  components: {
    TableExplorer,
  },
  data() {
    return {
      allColumns: [],
    };
  },
  props: {
    tableId: {
      type: String,
      required: true,
    },
    schemaId: {
      type: String,
      required: false,
    },
    canView: {
      type: Boolean,
      default: () => true,
    },
    canEdit: {
      type: Boolean,
      default: () => false,
    },
    canManage: {
      type: Boolean,
      default: () => false,
    },
    locale: {
      type: String,
      default: () => "en",
    },
    showFilters: {
      type: Array,
      default: () => [],
    },
    showColumns: {
      type: Array,
      default: () => [],
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
      } else {
        return deepClone(this.showColumns);
      }
    },
    getFilters() {
      if (this.$route.query._filter) {
        if (Array.isArray(this.$route.query._filter)) {
          return this.$route.query._filter;
        } else {
          return this.$route.query._filter.split(",");
        }
      } else {
        return deepClone(this.showFilters);
      }
    },
    getPage() {
      if (this.$route.query._page) {
        return parseInt(this.$route.query._page);
      } else {
        return 1;
      }
    },
    getLimit() {
      if (this.$route.query._limit) {
        return parseInt(this.$route.query._limit);
      } else {
        return 20;
      }
    },
    getView() {
      if (this.$route.query._view) {
        return this.$route.query._view;
      } else {
        return "table";
      }
    },
    getConditions() {
      let result = {};
      Object.keys(this.$route.query).forEach((key) => {
        if (!key.startsWith("_")) {
          result[key] = this.$route.query[key];
        }
      });
      return result;
    },
    updatePage(page) {
      const query = Object.assign({}, this.$route.query);
      query._page = page;
      this.updateRoute(query);
    },
    updateOrder(order) {
      const query = Object.assign({}, this.$route.query);
      query._order = order.direction;
      query._orderBy = order.column;
      this.updateRoute(query);
    },
    updateView(view, limit) {
      const query = Object.assign({}, this.$route.query);
      query._view = view;
      query._limit = limit;
      delete query._page;
      this.updateRoute(query);
    },
    updateLimit(limit) {
      const query = Object.assign({}, this.$route.query);
      query._limit = limit;
      delete query._page;
      this.updateRoute(query);
    },
    updateColumns(showColumns) {
      const query = Object.assign({}, this.$route.query);
      if (showColumns.length) {
        query._col = showColumns.join(",");
      } else {
        delete query._col;
      }
      this.updateRoute(query);
    },
    updateAllColumns(allColumns) {
      this.allColumns = allColumns;
    },
    updateFilters(showFilters) {
      const query = Object.assign({}, this.$route.query);
      if (showFilters.length > 0) {
        query._filter = showFilters.join(",");
      } else {
        delete query._filter;
      }
      this.updateRoute(query);
    },
    updateConditions(columns) {
      let query = Object.assign({}, this.$route.query);
      delete query._page;
      columns.forEach((column) => {
        const conditions = column.conditions;
        if (conditions?.length) {
          switch (column.columnType) {
            case "REF":
            case "REF_ARRAY":
            case "REFBACK":
            case "ONTOLOGY":
            case "ONTOLOGY_ARRAY":
              query[column.name] = JSON.stringify(conditions);
              break;
            case "DATE":
            case "DATETIME":
            case "INT":
            case "LONG":
            case "DECIMAL":
              const result = conditions.map((v) => v.join("..")).join(",");
              if (result !== "..") {
                query[column.name] = result;
              } else {
                delete query[column.name];
              }
              break;
            default:
              query[column.name] = conditions.join(",");
          }
        } else {
          delete query[column.name];
        }
      });
      this.updateRoute(query);
    },
    updateRoute(query) {
      if (JSON.stringify(query) !== JSON.stringify(this.$route.query)) {
        this.$router.push({ query: query });
      }
    },
  },
  emits: ["rowClick"],
};
</script>

<docs>
<template>
  <div>
    <div class="border p-1 my-1">
    <div>
     <label for="routed-table-explorer-can-edit">Can edit</label>
     <input class="ml-1" id='routed-table-explorer-can-edit' type="checkbox" v-model="canEdit" />
    </div>
    <div>
      <label for="routed-table-explorer-can-edit">Can view</label>
      <input class="ml-1" id='routed-table-explorer-can-view' type="checkbox" v-model="canView" />
    </div>
    <div>
     <label for="routed-table-explorer-schema">Schema</label>
     <input class="ml-1" id='routed-table-explorer-schema' v-model="schema" />
    </div>

    <div>
     <label for="routed-table-explorer-table">Table</label>
     <input class="ml-1" id='routed-table-explorer-table' v-model="table" />
    </div>

    {{ schema }}  
     
      <routed-table-explorer
        id="my-table-explorer"
        :key="dynamicKey"
        :tableId="table"
        :schemaId="schema"
        :canEdit="canEdit"
        :canView="canView"
      />
    </div>
  </div>
</template>

<script>
export default {
    data() {
      return {
        canEdit: false,
        canView: false,
        schema: "pet store",
        table: "Pet"
      }
    },
    computed: {
      dynamicKey() {
        return "my-table-explorer" + this.schema + "_" + this.table +"_"+this.canView
      },
     
    },
  }
</script>

</docs>
