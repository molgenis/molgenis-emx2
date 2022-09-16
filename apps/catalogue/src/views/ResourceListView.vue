<template>
  <div>
    <TableExplorer
      :showColumns="defaultColumns"
      :showFilters="defaultFilters"
      :tableName="tableName"
      :showCards="defaultCards"
      :initialSearchTerms="searchTerm"
      @click="openDetailView"
      @searchTerms="onSearchTermUpdate"
    />
  </div>
</template>

<script>
import { TableExplorer } from "molgenis-components";

const css = {
  Institutions: "bg-dark text-white",
  Datasources: "bg-secondary text-white",
  Databanks: "bg-info text-white",
  Cohorts: "bg-primary text-white",
  Models: "bg-warning text-dark",
  Networks: "bg-danger text-white",
  Studies: "bg-success text-white",
  Contacts: "bg-info text-white",
  Affiliations: "bg-info text-white",
  Releases: "bg-dark text-white",
  Tables: "bg-dark text-white",
  Variables: "bg-dark text-white",
  TableMappings: "bg-dark text-white",
  VariableMappings: "bg-dark text-white",
};

export default {
  components: {
    TableExplorer,
  },
  props: {
    tableName: String,
    searchTerm: String,
  },
  computed: {
    headerCss() {
      return css[this.tableName];
    },
    detailRouteName() {
      //detailRoute is name of table minus trailing 's'
      return this.tableName + "-details";
    },
    defaultCards() {
      if (this.tableName == "Institutions") {
        return true;
      }
      return false;
    },
    defaultColumns() {
      if (this.tableName == "Institutions") {
        return ["name", "pid", "type", "country"];
      } else if (
        ["Datasources", "Databanks", "Networks", "Models"].includes(
          this.tableName
        )
      ) {
        return ["name", "pid", "type", "recordPrompt", "institution"];
      } else if (this.tableName == "Cohorts") {
        return ["pid", "name", "keywords", "noParticipants"];
      } else if (this.tableName == "Studies") {
        return ["pid", "name", "keywords"];
      } else if (this.tableName == "Contacts") {
        return [
          "name",
          "institution",
          "affiliation",
          "email",
          "orcid",
          "homepage",
        ];
      } else if (this.tableName == "Affiliations") {
        return ["name", "homepage", "pid"];
      } else if (this.tableName == "SourceTables") {
        return [
          "dataDictionary",
          "name",
          "label",
          "unitOfObservation",
          "topics",
          "description",
        ];
      } else if (this.tableName == "TargetTables") {
        return [
          "model",
          "name",
          "label",
          "unitOfObservation",
          "topics",
          "description",
        ];
      } else if (this.tableName == "SourceVariables") {
        return [
          "dataDictionary",
          "table",
          "name",
          "label",
          "format",
          "unit",
          "topics",
          "description",
          "mandatory",
        ];
      } else if (this.tableName == "TargetVariables") {
        return [
          "model",
          "table",
          "name",
          "label",
          "format",
          "unit",
          "topics",
          "description",
          "mandatory",
        ];
      }
      return [];
    },
    defaultFilters() {
      if (this.tableName == "Institutions") {
        return ["type", "country"];
      }
      if (this.tableName == "Studies") {
        return ["keywords", "networks", "startYear", "endYear"];
      }
      if (this.tableName == "Databanks") {
        return ["keywords", "recordPrompt"];
      }
      if (this.tableName == "Cohorts") {
        return [
          "sampleCategories",
          "dataCategories",
          "noParticipants",
          "ageCategories",
        ];
      }
      return [];
    },
  },
  methods: {
    onSearchTermUpdate(searchTerm) {
      let newQuery = { ...this.$route.query };
      if (searchTerm) {
        newQuery.q = searchTerm;
      } else {
        delete newQuery.q;
      }
      this.$router.replace({
        ...this.$route,
        query: newQuery,
      });
    },
    openDetailView(row) {
      // in case of table
      if (this.tableName == "SourceTables") {
        this.$router.push({
          name: this.detailRouteName,
          params: {
            pid: row.dataDictionary.resource.pid,
            version: row.dataDictionary.version,
            name: row.name,
          },
        });
      } else if (this.tableName == "TargetTables") {
        this.$router.push({
          name: this.detailRouteName,
          params: {
            pid: row.dataDictionary.resource.pid,
            version: row.dataDictionary.version,
            name: row.name,
          },
        });
      } else if (this.tableName == "TableMappings") {
        this.$router.push({
          name: "tablemapping",
          params: {
            fromPid: row.fromDataDictionary.resource.pid,
            fromVersion: row.fromDataDictionary.version,
            fromTable: row.fromTable.name,
            toPid: row.toDataDictionary.resource.pid,
            toVersion: row.toDataDictionary.version,
            toTable: row.toTable.name,
          },
        });
      } else if (this.tableName == "SourceVariables") {
        this.$router.push({
          name: this.detailRouteName,
          params: {
            pid: row.dataDictionary.resource.pid,
            version: row.dataDictionary.version,
            table: row.table.name,
            name: row.name,
          },
        });
      } else if (this.tableName == "TargetVariables") {
        this.$router.push({
          name: this.detailRouteName,
          params: {
            pid: row.dataDictionary.resource.pid,
            version: row.dataDictionary.version,
            table: row.table.name,
            name: row.name,
          },
        });
      } else if (this.tableName == "VariableMappings") {
        this.$router.push({
          name: this.detailRouteName,
          params: {
            toResource: row.toDataDictionary.resource.pid,
            toVersion: row.toDataDictionary.version,
            toTable: row.toTable.name,
            toVariable: row.toVariable.name,
            fromResource: row.fromDataDictionary.resource.pid,
            fromVersion: row.fromDataDictionary.version,
            fromTable: row.fromTable.name,
          },
        });
      } else if (
        this.tableName == "SourceDataDictionaries" ||
        this.tableName == "TargetDataDictionaries"
      ) {
        this.$router.push({
          name: this.detailRouteName,
          params: {
            resource: row.resource.pid,
            version: row.version,
          },
        });
      } else if (row.version) {
        this.$router.push({
          name: this.detailRouteName,
          params: { pid: row.resource.pid, version: row.version },
        });
      } else if (row.pid) {
        this.$router.push({
          name: this.detailRouteName,
          params: { pid: row.pid },
        });
      } else {
        this.$router.push({
          name: this.detailRouteName,
          params: { name: row.name },
        });
      }
    },
  },
};
</script>
