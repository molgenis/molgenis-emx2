<template>
  <div class="container-fluid mt-3">
    <h1 class="text-capitalize">{{ $route.params.resource }}</h1>
    <div class="d-flex" style="overflow-x: scroll">
      <div class="flex-grow-1 pr-0 pl-0 col-12">
        <table-molgenis
          v-if="tableData"
          :columns="visibleColumns"
          :data="tableData"
          @click="onRowClicked"
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
      tableName: null,
      columns: [],
      tableData: null,
    };
  },
  async fetch() {
    this.tableName = this.$route.params.resource;
    const client = Client.newClient(
      "/" + this.$route.params.schema + "/graphql",
      this.$axios
    );
    const metaData = await client.fetchMetaData();
    const dataResponse = await client.fetchTableData(this.tableName);
    this.tableData = dataResponse[this.tableName];

    this.columns = metaData.tables
      .find((t) => t.name === this.tableName)
      .columns.filter((c) => !c.name.startsWith("mg_"));
  },
  computed: {
    visibleColumns() {
      return this.columns.filter((c) => this.defaultColumns.includes(c.id));
    },
    defaultColumns() {
      return {
        Datasources: ["name", "pid", "type", "recordPrompt", "institution"],
        Databanks: ["name", "pid", "type", "recordPrompt", "institution"],
        Networks: ["name", "pid", "type", "recordPrompt", "institution"],
        Institutions: ["name", "pid", "type", "recordPrompt", "institution"],
        Cohorts: ["pid", "name", "keywords", "noParticipants"],
        Studies: ["pid", "name", "keywords"],
        Contacts: [
          "name",
          "institution",
          "affiliation",
          "email",
          "orcid",
          "homepage",
        ],
        Affiliations: ["name", "homepage", "pid"],
        SourceTables: [
          "dataDictionary",
          "name",
          "label",
          "unitOfObservation",
          "topics",
          "description",
        ],
        TargetTables: [
          "model",
          "name",
          "label",
          "unitOfObservation",
          "topics",
          "description",
        ],
        SourceVariables: [
          "dataDictionary",
          "table",
          "name",
          "label",
          "format",
          "unit",
          "topics",
          "description",
          "mandatory",
        ],
        TargetVariables: [
          "model",
          "table",
          "name",
          "label",
          "format",
          "unit",
          "topics",
          "description",
          "mandatory",
        ],
      }[this.tableName];
    },
  },
  methods: {
    onRowClicked(row) {
      if (row.pid) {
        this.$router.push({
          path: `${this.tableName}/${row.pid}`,
        });
      }
    },
  },
  watch: {
    $route() {
      this.$nuxt.refresh();
    },
  },
};
</script>
