<template>
  <div>
    <Spinner v-if="loading" class="m-3" />
    <TableStickyHeaders v-else :columns="columns" :rows="rows" :data="aggregateData">
      <template #column="columnProps">
        {{ columnProps.value }}
      </template>
      <template #row="rowProps">
        {{ rowProps.value }}
      </template>
      <template #cell="cell">
        <div v-if="!cell.value" class="text-center text-black-50">-</div>
        <div v-else-if="cell.value < minimumValue">﹤{{ minimumValue }}</div>
        <div v-else>{{ cell.value }}</div>
      </template>
    </TableStickyHeaders>
  </div>
</template>

<script>
import { request } from "../../client/client.js";
import TableStickyHeaders from "./TableStickyHeaders.vue";

export default {
  name: "AggregateTable",
  components: { TableStickyHeaders },
  props: {
    graphQlEndpoint: {
      type: String,
      default: "graphql",
    },
    /** table to aggregate */
    table: {
      type: String,
      required: true,
    },
    /** property of the table to aggregate mref/xref */
    selectedColumnHeaderProperty: {
      type: String,
      required: true,
    },
    /** property of the mref/xref to display in the header cell */
    columnHeaderNameProperty: {
      type: String,
      required: true,
    },
    /** property of the table to aggregate mref/xref */
    selectedRowHeaderProperty: {
      type: String,
      required: true,
    },
    /** property of the mref/xref to display in the header cell */
    rowHeaderNameProperty: {
      type: String,
      required: true,
    },
    minimumValue: {
      type: Number,
      default: 1,
    },
  },
  data: function () {
    return {
      selectedColumnHeader: this.selectedColumnHeaderProperty,
      selectedRowHeader: this.selectedRowHeaderProperty,
      loading: true,
      rows: [],
      columns: [],
      aggregateData: {},
    };
  },
  computed: {
    tableName() {
      return `${this.table}_groupBy`;
    },
    getAggregateQuery() {
      return `{ 
                ${this.tableName} {
                  count,
                  ${this.selectedColumnHeader} {
                    ${this.columnHeaderNameProperty}
                  },
                  ${this.selectedRowHeader} {
                    ${this.rowHeaderNameProperty}
                  }
                }
              }`;
    },
  },
  methods: {
    addItem(item) {
      const column = item[this.selectedColumnHeader].name || "not specified";
      const row = item[this.selectedRowHeader].name || "not specified";

      if (!this.aggregateData[row]) {
        this.aggregateData[row] = { [column]: item.count };
      } else {
        this.aggregateData[row][column] = item.count;
      }

      if (!this.columns.includes(column)) {
        this.columns.push(column);
      }
      if (!this.rows.includes(row)) {
        this.rows.push(row);
      }
    },
    async fetchData() {
      this.loading = true;
      this.rows = [];
      this.columns = [];
      this.aggregateData = {};

      const responseData = await request(
        this.graphQlEndpoint,
        this.getAggregateQuery
      ).catch((reason) => {
        this.$emit("error", reason);
      });

      responseData[this.tableName].forEach((item) => this.addItem(item));
      this.loading = false;
    },
  },
  created() {
    this.fetchData();
  },
  watch: {
    selectedColumnHeaderProperty(value) {
      this.selectedColumnHeader = value;
      this.fetchData()
    },
    selectedRowHeaderProperty(value) {
      this.selectedRowHeader = value;
      this.fetchData();
    }
  }
};
</script>

<docs>
<template>
  <demo-item>
    <AggregateTable
        :table="tableName"
        :graphQlEndpoint="endpoint"
        :columnHeaderProperties="selectableColumns"
        :rowHeaderProperties="selectableColumns"
        :selectedColumnHeaderProperty="columnName"
        :columnHeaderNameProperty="columnNameProperty"
        :selectedRowHeaderProperty="rowName"
        :rowHeaderNameProperty="columnNameProperty"
        :minimumValue="1"
    />
  </demo-item>
</template>

<script>
  export default {
    data() {
      return {
        selectableColumns: [
          'category',
          'tags',
        ],
        tableName: 'Pet',
        endpoint: '/pet store/graphql',
        columnName: 'category',
        rowName: 'tags',
        columnNameProperty: 'name',
      };
    },
  };
</script>
</docs>
