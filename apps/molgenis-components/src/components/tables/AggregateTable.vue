<template>
  <div>
    <nav class="navbar navbar-expand-lg navbar-light bg-light">
      <a class="navbar-brand" href="#">Aggregate</a>
      <ul class="navbar-nav mr-auto">
        <li class="nav-item dropdown">
          <a
            class="nav-link dropdown-toggle"
            href="#"
            role="button"
            data-toggle="dropdown"
            aria-expanded="false"
          >
            {{ columnHeaderProperty }}
          </a>
          <div class="dropdown-menu">
            <a class="dropdown-item" href="#">Some item</a>
            <a class="dropdown-item" href="#">Some item</a>
          </div>
        </li>
        <li class="nav-item dropdown">
          <a
            class="nav-link dropdown-toggle"
            href="#"
            role="button"
            data-toggle="dropdown"
            aria-expanded="false"
          >
            {{ rowHeaderProperty }}
          </a>
          <div class="dropdown-menu">
            <a class="dropdown-item" href="#">Some item</a>
            <a class="dropdown-item" href="#">Some item</a>
          </div>
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
    /** property of the table to aggregate mref/xref */
    columnHeaderProperty: {
      type: String,
      required: true,
    },
    /** property of the mref/xref to display in the header cell */
    columnHeaderNameProperty: {
      type: String,
      required: true,
    },
    /** property of the table to aggregate mref/xref */
    rowHeaderProperty: {
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
                    ${this.columnHeaderProperty} {
                      ${this.columnHeaderNameProperty}
                    },
                    ${this.rowHeaderProperty} {
                      ${this.rowHeaderNameProperty}
                    }
                  }
                }
              }`;
    },
  },
  methods: {
    AddItem(item) {
      const column = item[this.columnHeaderProperty].name;
      const row = item[this.rowHeaderProperty].name;

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
      :columnHeaderProperty="columnName" 
      :columnHeaderNameProperty="columnNameProperty" 
      :rowHeaderProperty="rowName"
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
        tableName: 'Samples',
        endpoint: "SampleCatalogue/graphql",
        columnName: "MaterialTypeDetailed",
        rowName: "Diseases",
        columnNameProperty: "name"
      }
    },
  }
</script>
</docs>
