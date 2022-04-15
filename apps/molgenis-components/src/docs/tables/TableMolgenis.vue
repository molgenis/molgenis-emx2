<template>
  <demo-item id="table-molgenis" label="Table Molgenis">
    <label class="font-italic">Local sample data</label>
    <table-molgenis
      :selection.sync="selected"
      :columns.sync="columns"
      :data="data"
      @select="click"
      @deselect="click"
      @click="click"
    >
      <template v-slot:header>columns</template>
      <template v-slot:rowheader="slotProps"> some row content </template>
    </table-molgenis>

    <label class="font-italic">Remote Pet data</label>
    <table-molgenis v-if="remoteTableData"
      :selection.sync="remoteSelected"
      :columns.sync="remoteColumns"
      :data="remoteTableData"
      @click="click"
    >
    </table-molgenis>
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
          id: "ref1", name: "ref1",
          columnType: "REF",
          refColumns: ["firstName", "lastName"],
        },
        {
          id: "ref_arr1", name: "ref_arr1",
          columnType: "REF_ARRAY",
          refColumns: ["firstName", "lastName"],
        },
      ],
      data: [
        {
          col1: "row1",
          ref1: { firstName: "katrien", lastName: "duck" },
          ref_arr1: [
            { firstName: "kwik", lastName: "duck" },
            {
              firstName: "kwek",
              lastName: "duck",
            },
            { firstName: "kwak", lastName: "duck" },
          ],
        },
        {
          col1: "row2",
        },
      ],
      remoteSelected: [],
      remoteColumns: [],
      remoteTableData: null
    };
  },
  methods: {
    click(value) {
      alert("click " + JSON.stringify(value));
    },
  },
  async mounted () {
    const client = Client.newClient("/pet store/graphql", this.$axios);
    const remoteMetaData = await client.fetchMetaData();
    const petColumns = remoteMetaData.tables.find(t => t.name === "Pet").columns
    this.remoteColumns = petColumns.filter(c => !c.name.startsWith("mg_"))
    this.remoteTableData = (await client.fetchTableData("Pet")).Pet;
  }
};
</script>
