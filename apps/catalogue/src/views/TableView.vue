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
      >{{ table.release.resource.pid }}
    </RouterLink>
    /
    <h6 class="d-inline">Release</h6>
    <RouterLink
      :to="{
        name: 'Releases-details',
        params: { pid: pid, version: version },
      }"
    >
      {{ table.release.version }}
    </RouterLink>
    <h1>Table: {{ table.name }}</h1>
    <p>{{ table.description ? table.description : "Description: N/A" }}</p>

    <MessageError v-if="graphqlError"> {{ graphqlError }}</MessageError>
    <h6>Mappings/ETLs</h6>
    <ul v-if="table.mappings || table.mappingsTo">
      <li v-for="(m, index) in table.mappings" :key="index">
        From:
        <RouterLink
          :to="{
            name: 'tablemapping',
            params: {
              frompid: m.fromRelease.resource.pid,
              fromVersion: m.fromRelease.version,
              fromTable: m.fromTable.name,
              topid: table.release.resource.pid,
              toVersion: table.release.version,
              toTable: table.name,
            },
          }"
        >
          {{ getType(m.fromRelease.resource.mg_tableclass) }}:
          {{ m.fromRelease.resource.pid }} - Version:
          {{ m.fromRelease.version }} - Table:
          {{ m.fromTable.name }}
        </RouterLink>
      </li>
      <li v-for="(m, index) in table.mappingsTo" :key="index">
        To:
        <RouterLink
          :to="{
            name: 'tablemapping',
            params: {
              topid: m.toRelease.resource.pid,
              toVersion: m.toRelease.version,
              toTable: m.toTable.name,
              frompid: table.release.resource.pid,
              fromVersion: table.release.version,
              fromTable: table.name,
            },
          }"
        >
          {{ getType(m.toRelease.resource.mg_tableclass) }}:
          {{ m.toRelease.resource.pid }} - Version: {{ m.toRelease.version }} -
          Table:
          {{ m.toTable.name }}
        </RouterLink>
      </li>
    </ul>
    <p v-else>N/A</p>
    <h6>Variables</h6>
    <TableExplorer
      v-if="tab == 'Variables'"
      table="Variables"
      :showHeader="false"
      :showFilters="[]"
      :showColumns="['name', 'label', 'format', 'description', 'notes']"
      :showCards="true"
      :filter="{
        table: { name: { equals: name } },
        release: {
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
      if (this.table.release) {
        return this.table.release.resource.mg_tableclass.split(".")[1];
      }
    },
  },
  methods: {
    getType(mg_tableclass) {
      return mg_tableclass.split(".")[1];
    },
    openVariable(row) {
      this.$router.push({
        name: "Variables-details",
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
        `query Tables($pid:String,$version:String,$name:String){Tables(filter:{release:{version:{equals:[$version]},resource:{pid:{equals:[$pid]}}},name:{equals:[$name]}})
        {name,unitOfObservation{name,definition,ontologyTermURI},release{version,resource{pid,name,mg_tableclass}}, description,label,
        mappings{fromRelease{resource{pid,mg_tableclass}version}fromTable{name}}
         mappingsTo{toRelease{resource{pid,mg_tableclass}version}toTable{name}}
         }}`,
        {
          pid: this.pid,
          version: this.version,
          name: this.name,
        }
      )
        .then((data) => {
          this.table = data.Tables[0];
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
