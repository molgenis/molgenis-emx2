<template>
  <div>
    <Spinner v-if="loading" class="m-3" />
    <TableStickyHeaders
      v-else
      :columns="columns"
      :rows="rows"
      :data="aggregateData"
    >
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

<script lang="ts">
import { defineComponent } from "vue";
import TableStickyHeaders from "./TableStickyHeaders.vue";
import IAggregateData from "./IAggregateData";
import Client from "../../client/client";

export default defineComponent({
  name: "AggregateTable",
  components: { TableStickyHeaders },
  props: {
    schemaName: {
      type: String,
      required: true,
    },
    /** table to aggregate */
    tableName: {
      type: String,
      required: true,
    },
    /** property of the table to aggregate mref/xref */
    selectedColumnProperty: {
      type: String,
      required: true,
    },
    /** property of the mref/xref to display in the header cell */
    columnNameProperty: {
      type: String,
      required: true,
    },
    /** property of the table to aggregate mref/xref */
    selectedRowProperty: {
      type: String,
      required: true,
    },
    /** property of the mref/xref to display in the header cell */
    rowNameProperty: {
      type: String,
      required: true,
    },
    minimumValue: {
      type: Number,
      default: 1,
    },
    graphqlFilter: {
      type: Object,
      default: {},
    },
  },
  data: function () {
    return {
      selectedColumn: this.selectedColumnProperty,
      selectedRow: this.selectedRowProperty,
      loading: true,
      rows: [] as string[],
      columns: [] as string[],
      aggregateData: {} as IAggregateData,
    };
  },
  methods: {
    addItem(item: any) {
      const column: string = item[this.selectedColumn].name || "not specified";
      const row: string = item[this.selectedRow].name || "not specified";

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
      const client = Client.newClient(this.schemaName);
      const responseData = await client.fetchAggregateData(
        this.tableName,
        {
          name: this.selectedColumnProperty,
          column: this.columnNameProperty,
        },
        {
          name: this.selectedRowProperty,
          column: this.rowNameProperty,
        },
        this.graphqlFilter
      );
      responseData[this.tableName + "_groupBy"].forEach((item: any) =>
        this.addItem(item)
      );
      this.loading = false;
    },
  },
  created() {
    this.fetchData();
  },
  watch: {
    selectedColumnProperty(value) {
      this.selectedColumn = value;
      this.fetchData();
    },
    selectedRowProperty(value) {
      this.selectedRow = value;
      this.fetchData();
    },
  },
});
</script>

<docs>
<template>
  <demo-item>
    <AggregateTable
        :tableName="tableName"
        :schemaName="schemaName"
        :columnProperties="selectableColumns"
        :rowProperties="selectableColumns"
        :selectedColumnProperty="columnName"
        :columnNameProperty="columnNameProperty"
        :selectedRowProperty="rowName"
        :rowNameProperty="columnNameProperty"
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
        schemaName: 'pet store',
        columnName: 'category',
        rowName: 'tags',
        columnNameProperty: 'name',
      };
    },
  };
</script>
</docs>
