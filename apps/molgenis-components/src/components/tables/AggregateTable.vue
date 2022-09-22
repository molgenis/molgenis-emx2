<template>
  <div>
    <nav class="navbar navbar-expand-lg navbar-light bg-light">
      <a class="navbar-brand" href="#">Aggragate</a>
      <ul class="navbar-nav mr-auto">
        <li class="nav-item dropdown">
          <a
            class="nav-link dropdown-toggle"
            href="#"
            role="button"
            data-toggle="dropdown"
            aria-expanded="false"
          >
            Thing on rows
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
            Thing on columns
          </a>
          <div class="dropdown-menu">
            <a class="dropdown-item" href="#">Some item</a>
            <a class="dropdown-item" href="#">Some item</a>
          </div>
        </li>
      </ul>
    </nav>
    <Spinner v-if="loading" />
    <div v-else>lets place our cool table here</div>
    <table class="table table-striped">
      <thead ref="tablehead">
        <tr>
          <th></th>
          <th v-for="(column, index) of columns" :key="`head-${index}`">
            {{ column }}
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
            {{ aggregateData[row][column] || 0 }}
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import { request } from "../../client/client.js";
import requestData from "./request.json";

export default {
  name: "AggregateTable",
  props: {},
  data: function () {
    return {
      loading: true,
      rows: [],
      columns: [],
      aggregateData: {},
      columnName: "MaterialTypeDetailed",
      rowName: "Diseases",
    };
  },
  computed: {},
  methods: {
    AddItem(item) {
      const column = item[this.columnName].name;
      const row = item[this.rowName].name;
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
      /*
      const responseData = await request(
        "SampleCatalogue/graphql",
        `
        {
          Samples_agg {
            groupBy {
              count,
              Diseases {
                code,
                name
              },
              MaterialTypeDetailed {
                code,
                name
              }
            }
          }
        }     
        `
      );
      */
      requestData.data.Samples_agg.groupBy.forEach(this.AddItem);
      this.loading = false;
    },
  },
  created() {
    this.fetchData();
  },
};
</script>

<style scoped>
</style>

<docs>
<template>
  <demo-item>
    <AggregateTable></AggregateTable>
  </demo-item>
</template>

<script>
  export default {
    data() {
      return {
      }
    },
  }
</script>
</docs>
