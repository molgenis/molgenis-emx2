<template>
  <demo-item id="table-search" label="Table Search">
    <label class="font-italic">Local sample data</label>
    <table-search
      id="my-search-table"
      :selection.sync="selected"
      :columns.sync="columns"
      :lookupTableName="'Pet'"
      :showSelect="false"
      :graphqlURL="'/pet store/graphql'"
      :canEdit="true"
      @select="click"
      @deselect="click"
      @click="click"
    >
    </table-search>
  </demo-item>
</template>

<script>
import Client from "../../client/client.js";
export default {
  data() {
    return {
      selected: [],
      columns: [
        { id: "col1", name: "col1", columnType: "STRING", key: 1 },
        {
          id: "ref1",
          name: "ref1",
          columnType: "REF",
          refColumns: ["firstName", "lastName"],
        },
        {
          id: "ref_arr1",
          name: "ref_arr1",
          columnType: "REF_ARRAY",
          refColumns: ["firstName", "lastName"],
        },
      ],
      remoteSelected: [],
      remoteColumns: [],
      remoteTableData: null,
    };
  },
  methods: {
    click(value) {
      alert("click " + JSON.stringify(value));
    },
  },
  async mounted() {
    const client = Client.newClient("/pet store/graphql", this.$axios);
    const remoteMetaData = await client.fetchMetaData();
    const petColumns = remoteMetaData.tables.find(
      (t) => t.name === "Pet"
    ).columns;
    this.remoteColumns = petColumns.filter((c) => !c.name.startsWith("mg_"));
    this.remoteTableData = (await client.fetchTableData("Pet")).Pet;
  },
};
</script>