<template>
  <div class="container">
    <h1>Table data client module</h1>

    <h5>Table data for: {{tableName}}</h5>
    <p>{{ tableData }}</p>

    <h5>Schema data</h5>
    <p>{{ metaData }}</p>
   
  </div>
</template>

<script>
import Client from "./client/client.js";
export default {
  name: "ClientView",
  data() {
    return {
      tableName: "Pet",
      metaData: {},
      tableData: [],
    };
  },
  async mounted() {
    const client = Client.newClient(this.$axios, "/pet store/graphql");
    this.metaData = await client.fetchMetaData();
    this.tableData = await client.fetchTableData("Pet");
  }
};
</script>
