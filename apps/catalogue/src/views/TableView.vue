<template>
  <div v-if="graphqlError">
    <MessageError>{{ graphqlError }}</MessageError>
  </div>
  <div v-else-if="table" class="container bg-white">
    <div class="p-2 bg-dark text-white mb-3">
      <h6>
        <RouterLink to="/" class="text-white"> home</RouterLink>
        /
        <RouterLink to="/list/Tables" class="text-white"> tables</RouterLink>
        /
      </h6>
    </div>
    <h6 class="d-inline">{{ resourceType }}:&nbsp;</h6>
    <RouterLink
      :to="{
        name: resourceType + '-details',
        params: { pid: pid },
      }"
      >{{ table.dataDictionary.resource.pid }}
    </RouterLink>
    /
    <h6 class="d-inline">Data dictionary</h6>
    <RouterLink
      :to="{
        name:
          tableName == 'SourceTables'
            ? 'SourceDataDictionaries-details'
            : 'TargetDataDictionaries-details',
        params: { resource: pid, version: version },
      }"
    >
      {{ table.dataDictionary.version }}
    </RouterLink>
    <h1>{{ labelPrefix }} Table: {{ table.name }}</h1>
    <p>{{ table.description ? table.description : "Description: N/A" }}</p>

    <MessageError v-if="graphqlError"> {{ graphqlError }}</MessageError>
    <h6>Mappings/ETLs</h6>
    <ul v-if="table.mappings">
      <li v-for="(m, index) in table.mappings" :key="index">
        <RouterLink
          :to="{
            name: 'tablemapping',
            params: {
              fromPid: m.fromDataDictionary.resource.pid,
              fromVersion: m.fromDataDictionary.version,
              fromTable: m.fromTable.name,
              toPid: m.toDataDictionary.resource.pid,
              toVersion: m.toDataDictionary.version,
              toTable: m.toTable.name,
            },
          }"
        >
          <span>
            {{ m.fromDataDictionary.resource.pid }}
            - Version: {{ m.fromDataDictionary.version }} - Table:
            {{ m.toTable.name }}
          </span>
        </RouterLink>
      </li>
    </ul>
    <p v-else>N/A</p>
    <h6>Variables</h6>
    <TableExplorer
      v-if="tab == 'Variables'"
      :table="
        tableName == 'SourceTables' ? 'SourceVariables' : 'TargetVariables'
      "
      :showHeader="false"
      :showFilters="[]"
      :showColumns="['name', 'label', 'format', 'description', 'notes']"
      :showCards="true"
      :filter="{
        table: { name: { equals: name } },
        dataDictionary: {
          version: { equals: version },
          resource: { pid: { equals: pid } },
        },
      }"
      @click="openVariable"
    />
  </div>
</template>
<script>
import { request } from "graphql-request";
import { MessageError, TableExplorer } from "@mswertz/emx2-styleguide";
import VariablesList from "../components/VariablesList";
import Property from "../components/Property";
import OntologyTerms from "../components/OntologyTerms";

export default {
  components: {
    OntologyTerms,
    VariablesList,
    Property,
    MessageError,
    TableExplorer,
  },
  props: {
    tableName: String,
    pid: String,
    version: String,
    name: String,
  },
  data() {
    return {
      graphqlError: null,
      table: null,
      tab: "Variables",
    };
  },
  computed: {
    resourceType() {
      if (this.table.dataDictionary) {
        return this.table.dataDictionary.resource.mg_tableclass.split(".")[1];
      }
    },
    labelPrefix() {
      if (this.tableName === "SourceTables") {
        return "Source";
      } else if (this.tableName === "TargetTables") {
        return "Target";
      } else {
        return "";
      }
    },
  },
  methods: {
    getType(mg_tableclass) {
      return mg_tableclass.split(".")[1];
    },
    openVariable(row) {
      this.$router.push({
        name:
          this.tableName == "SourceTables"
            ? "SourceVariables-details"
            : "TargetVariables-details",
        params: {
          pid: this.pid,
          version: this.version,
          table: this.name,
          name: row.name,
        },
      });
    },
    reload() {
      request(
        "graphql",
        `query ${this.tableName}($pid:String,$version:String,$name:String){${this.tableName}(filter:{dataDictionary:{version:{equals:[$version]},resource:{pid:{equals:[$pid]}}},name:{equals:[$name]}})
        {dataDictionary{resource{pid,mg_tableclass},version}name,unitOfObservation{name,definition,ontologyTermURI},description,label,
        mappings{fromDataDictionary{resource{pid,mg_tableclass}version}fromTable{name}toDataDictionary{resource{pid,mg_tableclass}version}toTable{name}}
          }}`,
        {
          pid: this.pid,
          version: this.version,
          name: this.name,
        }
      )
        .then((data) => {
          this.table = data.SourceTables
            ? data.SourceTables[0]
            : data.TargetTables[0];
        })
        .catch((error) => {
          if (error.response)
            this.graphqlError = error.response.errors[0].message;
          else this.graphqlError = error;
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
  created() {
    this.reload();
  },
};
</script>
