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
    <div>
      <table class="table table-striped">
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
              {{ aggregateData[row][column] || 0 }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>
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
.table td {
  text-align: center;
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
  position: relative;
}

thead {
  background: linear-gradient(
    180deg,
    rgba(255, 255, 255, 1) 0%,
    rgba(255, 255, 255, 0.9) 75%,
    rgba(255, 255, 255, 0.5) 100%
  );
  border-bottom: 1px solid black;
  pointer-events: none;
  position: sticky;
  top: 0; /* Don't forget this, required for the stickiness */
}
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
