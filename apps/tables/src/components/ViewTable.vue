<template>
  <div>
    <router-link to="/">< Back to '{{ schema.name }}'</router-link>
    <h1>{{ tableName }}</h1>
    <Spinner v-if="loading" />
    <MessageError v-else-if="error">{{ error }}</MessageError>
    <div v-else class="row flex-nowrap">
      <div
        class=" col col-mg-4 col-lg-4
        "
      >
        <FilterSidebar v-if="table" :filters="table.columns" />
      </div>
      <div class="col">
        <div>
          <label>{{ count }} records found</label>
        </div>
        <FilterWells v-if="table" :filters="table.columns" />
        <DataTable :columns="columns" :rows="rows" class="table-responsive">
          <template v-slot:colheader>
            <RowButtonAdd :schema="schema" :table="tableName" @close="reload" />
          </template>
          <template v-slot:rowheader="slotProps">
            <IconBar>
              <RowButtonEdit
                :schema="schema"
                :table="tableName"
                :pkey="slotProps.row[table.pkey]"
                @close="reload"
              />
              <RowButtonDelete
                :schema="schema"
                :table="tableName"
                :pkey="slotProps.row[table.pkey]"
                @close="reload"
              />
            </IconBar>
          </template>
        </DataTable>
      </div>
    </div>

    <br />DEBUG
    <br />
    tableName = {{ tableName }}
    <br />
    molgenis={{ JSON.stringify(molgenis) }}
    <br />
    graphql={{ JSON.stringify(graphql) }}
    <br />
    columnNames = {{ columnNames }}
    <br />
    rows = {{ rows }}
    <br />
    filters = {{ filters }}
    <br />
    <pre>table={{ JSON.stringify(table, null, "\t") }}</pre>

    <br />
    <pre>data={{ JSON.stringify(data, null, "\t") }}</pre>
  </div>
</template>

<script>
import { request } from "graphql-request";
import {
  Spinner,
  MessageError,
  DataTable,
  FilterSidebar,
  FilterWells,
  IconBar,
  RowButtonEdit,
  RowButtonAdd,
  RowButtonDelete
} from "@mswertz/emx2-styleguide";

export default {
  components: {
    Spinner,
    MessageError,
    DataTable,
    FilterSidebar,
    FilterWells,
    RowButtonEdit,
    RowButtonAdd,
    RowButtonDelete
  },
  props: {
    tableName: String,
    schema: Object,
    molgenis: Object
  },
  data() {
    return {
      loading: false,
      error: null,
      data: [],
      count: 0,
      limit: 100,
      offset: 0,
      select: {},
      filters: null
    };
  },
  computed: {
    graphql() {
      return `{${this.tableName}{data_agg{count},data(limit:${this.limit},offset:${this.offset}){${this.columnNames}}}}`;
    },
    table() {
      let table = null;
      if (this.schema != null) {
        this.schema.tables.forEach(element => {
          if (element.name === this.tableName) {
            table = element;
          }
        });
      }
      return table;
    },
    columnNames() {
      let result = "";
      if (this.table != null) {
        this.table.columns.forEach(element => {
          if (["REF", "REF_ARRAY", "REFBACK"].includes(element.columnType)) {
            result =
              result + " " + element.name + "{" + element.refColumn + "}";
          } else {
            result = result + " " + element.name;
          }
        });
      }
      return result;
    },
    columns() {
      if (this.table && this.table.columns) {
        return this.table.columns.map(col => col.name);
      }
      return null;
    },
    rows() {
      if (this.data != null && this.table != null) {
        return this.data.map(row => {
          let result = { ...row };
          this.table.columns.forEach(col => {
            if (row[col.name]) {
              if (col.columnType === "REF") {
                result[col.name] = row[col.name][col.refColumn];
              } else if (
                col.columnType === "REF_ARRAY" ||
                col.columnType === "REFBACK"
              ) {
                result[col.name] = row[col.name].map(val => val[col.refColumn]);
              }
            }
          });
          return result;
        });
      }
      return this.data;
    }
  },
  methods: {
    reload() {
      this.data = null;
      if (this.schema == null) return;
      this.loading = true;
      request("graphql", this.graphql)
        .then(data => {
          this.error = null;
          this.data = data[this.tableName]["data"];
          this.count = data[this.tableName]["data_agg"]["count"];
        })
        .catch(error => {
          this.error = "internal server error" + error;
        })
        .finally((this.loading = false));
    }
  },
  watch: {
    graphql() {
      this.reload();
    }
  },
  created() {
    this.reload();
  }
};
</script>
