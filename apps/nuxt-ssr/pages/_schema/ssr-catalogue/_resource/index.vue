<template>
  <div class="container-fluid mt-3">
    <h1 class="text-capitalize">{{ $route.params.resource }}</h1>
    <div class="row">
      <div class="col-3">
        <FilterSidebar
          :filters="filters"
          @updateFilters="updateFilters"
        ></FilterSidebar>
      </div>
      <div class="col-9">
        <div>
          <FilterWells :filters="filters" @updateFilters="updateFilters" />
        </div>
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
    </div>
  </div>
</template>

<script>
import {
  Client,
  TableMolgenis,
  FilterSidebar,
  FilterWells,
} from "molgenis-components";
export default {
  name: "ResourceList",
  components: { TableMolgenis, FilterSidebar, FilterWells },
  data() {
    return {
      tableName: null,
      tableData: null,
      columns: [],
      filters: [],
    };
  },
  async fetch() {
    this.tableName = this.$route.params.resource;
    const client = Client.newClient(
      this.$axios,
      "/" + this.$route.params.schema + "/graphql"
    );
    const metaData = await client.fetchMetaData();
    const dataResponse = await client.fetchTableData(this.tableName);
    this.tableData = dataResponse[this.tableName];

    this.columns = metaData.tables
      .find((t) => t.name === this.tableName)
      .columns.filter((c) => !c.name.startsWith("mg_"));

    this.filters = this.visibleColumns
      .filter((column) => column.columnType === "STRING")
      .map((column) => {
        column.conditions = [];
        column.showFilter = true;
        return column;
      });
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
    updateFilters(update) {
      console.log('updateFilters')
      this.filters = update;
      this.fetchFiltered();
    },
    async fetchFiltered() {
      const filterQuery = this.filters.reduce((accum, filter) => {
        if (filter.conditions.length) {
          accum[filter.id] = { like: filter.conditions };
        }
        return accum;
      }, {});

      console.log(filterQuery);

      this.client = Client.newClient(
        this.$axios,
        "/" + this.$route.params.schema + "/graphql"
      );
      const dataResponse = await this.client.fetchTableData(this.tableName, {
        filter: filterQuery,
      });
      this.tableData = dataResponse[this.tableName];
    },
  },
  watch: {
    $route() {
      this.$nuxt.refresh();
    },
  },
};
</script>
