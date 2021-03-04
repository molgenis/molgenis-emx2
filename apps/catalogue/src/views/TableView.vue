<template>
  <div v-if="error">
    <MessageError>{{ error }}</MessageError>
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
        name: resourceType.toLowerCase(),
        params: { acronym: acronym },
      }"
      >{{ table.release.resource.acronym }}
    </RouterLink>
    /
    <h6 class="d-inline">Release</h6>
    <RouterLink
      :to="{
        name: 'release',
        params: { acronym: acronym, version: version },
      }"
    >
      {{ table.release.version }}
    </RouterLink>
    <h1>Table: {{ table.name }}</h1>
    <p>{{ table.description ? table.description : "Description: N/A" }}</p>

    <MessageError v-if="error"> {{ error }}</MessageError>
    <div class="row">
      <div class="col">
        <h6>Topics</h6>
        <OntologyTerms :terms="table.topics" color="dark" />
        <h6>Unit of observation</h6>
        <OntologyTerms :terms="[table.unitOfObservation]" color="dark" />
      </div>
    </div>
    <h6>Mappings/ETLs</h6>
    <ul v-if="table.mappings || table.mappingsTo">
      <li v-for="m in table.mappings">
        From:
        <RouterLink
          :to="{
            name: 'tablemapping',
            params: {
              fromAcronym: m.fromRelease.resource.acronym,
              fromVersion: m.fromRelease.version,
              fromTable: m.fromTable.name,
              toAcronym: table.release.resource.acronym,
              toVersion: table.release.version,
              toTable: table.name,
            },
          }"
        >
          {{ getType(m.fromRelease.resource.mg_tableclass) }}:
          {{ m.fromRelease.resource.acronym }} - Version:
          {{ m.fromRelease.version }} - Table:
          {{ m.fromTable.name }}
        </RouterLink>
      </li>
      <li v-for="m in table.mappingsTo">
        To:
        <RouterLink
          :to="{
            name: 'tablemapping',
            params: {
              toAcronym: m.toRelease.resource.acronym,
              toVersion: m.toRelease.version,
              toTable: m.toTable.name,
              fromAcronym: table.release.resource.acronym,
              fromVersion: table.release.version,
              fromTable: table.name,
            },
          }"
        >
          {{ getType(m.toRelease.resource.mg_tableclass) }}:
          {{ m.toRelease.resource.acronym }} - Version:
          {{ m.toRelease.version }} - Table:
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
      :showFilters="['topics', 'format']"
      :showColumns="['name', 'label', 'format', 'unit', 'mandatory', 'topics']"
      :showCards="true"
      :filter="{
        table: { name: { equals: name } },
        release: {
          version: { equals: version },
          resource: { acronym: { equals: acronym } },
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
    acronym: String,
    version: String,
    name: String,
  },
  data() {
    return {
      error: null,
      table: null,
      tab: "Variables",
    };
  },
  computed: {
    resourceType() {
      if (this.table.release) {
        return this.table.release.resource.mg_tableclass
          .split(".")[1]
          .slice(0, -1);
      }
    },
  },
  methods: {
    getType(mg_tableclass) {
      return mg_tableclass.split(".")[1].slice(0, -1);
    },
    openVariable(row) {
      this.$router.push({
        name: "variable",
        params: {
          acronym: this.acronym,
          version: this.version,
          table: this.name,
          name: row.name,
        },
      });
    },
    reload() {
      request(
        "graphql",
        `query Tables($acronym:String,$version:String,$name:String){Tables(filter:{release:{version:{equals:[$version]},resource:{acronym:{equals:[$acronym]}}},name:{equals:[$name]}})
        {name,unitOfObservation{name,definition,ontologyTermURI},release{version,resource{acronym,name,mg_tableclass}},topics{name,ontologyTermURI,definition}, description,label,topics{name}
        mappings{fromRelease{resource{acronym,mg_tableclass}version}fromTable{name}}
         mappingsTo{toRelease{resource{acronym,mg_tableclass}version}toTable{name}}
         }}`,
        {
          acronym: this.acronym,
          version: this.version,
          name: this.name,
        }
      )
        .then((data) => {
          this.table = data.Tables[0];
        })
        .catch((error) => {
          if (error.response) this.error = error.response.errors[0].message;
          else this.error = error;
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
