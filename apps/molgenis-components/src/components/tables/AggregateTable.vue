<template>
  <div>
    <nav class="navbar navbar-expand-lg navbar-light bg-light">
      <a class="navbar-brand" href="#">Aggregate</a>
      <ul class="navbar-nav mr-auto">
        <li>
          <InputSelect
            class="m-0 mr-2"
            id="column-select"
            v-model="selectedColumnHeader"
            :options="columnHeaderProperties"
            @input="fetchData"
          />
        </li>
        <li>
          <InputSelect
            class="m-0"
            id="row-select"
            v-model="selectedRowHeader"
            :options="rowHeaderProperties"
            @input="fetchData"
          />
        </li>
      </ul>
    </nav>
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
        <div v-else-if="cell.value < 10">﹤10</div>
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
    /** list of references(string) to a aggregate on */
    columnHeaderProperties: {
      type: Array,
      required: true,
    },
    /** list of references(string) to a aggregate on */
    rowHeaderProperties: {
      type: Array,
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
      return `${this.table}_agg`;
    },
    getAggregateQuery() {
      return `{ 
                ${this.tableName} {
                  groupBy {
                    count,
                    ${this.selectedColumnHeader} {
                      ${this.columnHeaderNameProperty}
                    },
                    ${this.selectedRowHeader} {
                      ${this.rowHeaderNameProperty}
                    }
                  }
                }
              }`;
    },
  },
  methods: {
    AddItem(item) {
      const column = item[this.selectedColumnHeaderProperty].name;
      const row = item[this.selectedRowHeaderProperty].name;

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
      const responseData = await request(
        this.graphQlEndpoint,
        this.getAggregateQuery
      );
      responseData[this.tableName].groupBy.forEach((item) =>
        this.AddItem(item)
      );
      this.loading = false;
    },
  },
  created() {
    this.fetchData();
  },
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
      :minimumValue="10"
    >
    </AggregateTable>
  </demo-item>
</template>

<script>
export default {
  data() {
    return {
      selectableColumns: [
        "AnatomicalSites",
        "DiagnosisTypes",
        "Diseases",
        "ImagingData",
        "MaterialTypeDetailed",
        "Ontologies",
        "Sex",
      ],
      tableName: "Samples",
      endpoint: "SampleCatalogue/graphql",
      columnName: "MaterialTypeDetailed",
      rowName: "Diseases",
      columnNameProperty: "name",
    };
  },
};
</script>
</docs>