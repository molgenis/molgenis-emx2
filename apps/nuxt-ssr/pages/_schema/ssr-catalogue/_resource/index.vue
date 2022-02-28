<template>
  <div class="container-fluid mt-3">
    <h1 class="text-capitalize">{{ $route.params.resource }}</h1>
    <div class="d-flex" style="overflow-x: scroll;">
      <div class="flex-grow-1 pr-0 pl-0 col-12">
        <table-molgenis
          v-if="tableData"
          :selection.sync="selected"
          :columns.sync="columns"
          :data="tableData"
        >
        </table-molgenis>
      </div>
    </div>
  </div>
</template>

<script>
import { Client, TableMolgenis } from "molgenis-components";
export default {
  name: "ResourceList",
  components: { TableMolgenis },
  data() {
    return {
      selected: [],
      columns: [],
      tableData: null,
    };
  },
  async fetch() {
    const tableName = this.$route.params.resource
    const client = Client.newClient(
      this.$axios,
      "/" + this.$route.params.schema + "/graphql"
    );
    const metaData = await client.fetchMetaData();
    this.tableData = (await client.fetchTableData(tableName)).Networks;
    this.columns = metaData.tables
      .find((t) => t.name === tableName)
      .columns.filter((c) => !c.name.startsWith("mg_"));
  },
  watch: {
    $route() {
      this.$nuxt.refresh();
    },
  },
};
</script>
