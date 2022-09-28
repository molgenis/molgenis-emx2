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
    <div v-else role="region" class="border border-light">
      <table>
        <thead ref="tablehead">
          <tr>
            <th></th>
            <th v-for="(column, index) of columns" :key="`head-${index}`">
              <div class="rotated-title">
                <span>{{ column }}</span>
              </div>
            </th>
          </tr>
        </thead>
        <tbody ref="tablebody">
          <tr v-for="(row, rowIndex) of rows" :key="`tr-${rowIndex}`">
            <th>{{ row }}</th>
            <td
              v-for="(column, columnIndex) of columns"
              :key="`td-${columnIndex}`"
            >
              {{ displayValue(aggregateData[row][column] || 0) }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
import { request } from "../../client/client.js";

export default {
  name: "AggregateTable",
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
    displayValue(value) {
      if (value == 0) {
        return "-";
      } else if (value < this.minimumValue) {
        return "<" + this.minimumValue;
      } else {
        return value;
      }
    },
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

<style scoped>
/* 
  Based on: 
  https://css-tricks.com/a-table-with-both-a-sticky-header-and-a-sticky-first-column/
*/

table td {
  text-align: center;
  border: 1px solid var(--light);
}
.rotated-title {
  width: 2em;
  height: 10em;
  vertical-align: bottom;
  position: relative;
}
.rotated-title > span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  width: 12em;
  height: 2em;
  transform-origin: 0 0;
  transform: rotate(-55deg) translate(-7.5em, 5.25em);
  display: inline-block;
  z-index: 1;
  position: relative;
}
table {
  border-spacing: 0;
}
table thead th {
  padding-bottom: 1em;
}
table thead tr:after {
  display: inline-block;
  content: "";
  width: 100%;
  height: 15px;
  background: linear-gradient(
    180deg,
    rgba(0, 0, 0, 0.05) 0%,
    rgba(0, 0, 0, 0) 100%
  );
  position: absolute;
  left: 0;
  bottom: -15px;
  pointer-events: none;
}
table thead th:first-child {
  background: white;
  z-index: 1;
  border-bottom: 0px;
}
table thead {
  position: sticky;
  top: 0;
  z-index: 1;
  background: white;
}
table tbody {
  position: relative;
}
table tbody tr:hover {
  background-color: var(--light);
}
table tbody tr:hover th {
  background-color: var(--light);
}
table tbody td,
table tbody th {
  position: relative;
}
table tbody td:hover::before {
  content: "";
  position: absolute;
  display: inline-block;
  background-color: var(--light);
  left: 0;
  right: 0;
  top: -100vh;
  bottom: -100vh;
  z-index: -1;
}
table thead th:first-child {
  position: sticky;
  left: 0;
  z-index: 2;
}
table thead th:first-child::after {
  display: inline-block;
  content: "";
  width: 15px;
  height: 100vh;
  background: linear-gradient(
    90deg,
    rgba(0, 0, 0, 0.05) 0%,
    rgba(0, 0, 0, 0) 100%
  );
  position: absolute;
  right: -15px;
  top: 0;
  pointer-events: none;
}
table tbody th {
  position: sticky;
  left: 0;
  background: white;
  z-index: 1;
  padding-right: 1em;
}
[role="region"] {
  width: 100%;
  max-height: 98vh;
  overflow: auto;
}
</style>

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
