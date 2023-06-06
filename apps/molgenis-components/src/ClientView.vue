<template>
  <div class="container">
    <h1>Table data client module</h1>

    <h5>Table data for: {{ tableName }}</h5>
    <div class="form-group">
      <label for="filterInput">filter</label>
      <input
        v-model="filter"
        id="filterInput"
        class="form-control"
        type="text" />
      <label for="filterInput">orderby</label>
      <input
        v-model="orderby"
        id="filterInput"
        class="form-control"
        type="text" />
    </div>
    <p>{{ tableData }}</p>

    <h5>Schema data</h5>
    <p>{{ metaData }}</p>
  </div>
</template>

<script>
import Client from "./client/client.ts";
export default {
  name: "ClientView",
  data() {
    return {
      client: null,
      tableName: "Pet",
      filter: '{"name": {"like": ["pooky"]}}',
      orderby: '{"status": "ASC"}',
      metaData: {},
      tableData: [],
    };
  },
  methods: {
    async fetchData() {
      const filter = this.filter ? JSON.parse(this.filter) : {};
      const orderby = this.filter ? JSON.parse(this.orderby) : {};
      this.metaData = await this.client.fetchSchemaMetaData();
      this.tableData = await this.client.fetchTableData("Pet", {
        filter,
        orderby,
      });
    },
  },
  async mounted() {
    this.client = Client.newClient("pet store", this.$axios);
    this.fetchData();
  },
  watch: {
    filter() {
      this.fetchData();
    },
    orderby() {
      this.fetchData();
    },
  },
};
</script>
