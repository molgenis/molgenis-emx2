<template>
  <div v-if="release" class="container bg-white">
    <div class="p-2 bg-dark text-white">
      <h6>
        <RouterLink to="/" class="text-white"> home</RouterLink>
        /
        <RouterLink to="/list/Releases" class="text-white">
          releases
        </RouterLink>
        /
      </h6>
    </div>
    <h1>
      <small>Release:</small><br />{{ release.resource.acronym }} ({{
        release.version
      }})
    </h1>
    <h6>Resource:</h6>
    <RouterLink
      v-if="release.resource.mg_tableclass.includes('Network')"
      :to="{
        name: 'network',
        params: { projectAcronym: release.resource.acronym },
      }"
      >{{ release.resource.acronym }} -
      {{ release.resource.name }}
    </RouterLink>
    <RouterLink
      v-if="release.resource.mg_tableclass.includes('Databank')"
      :to="{
        name: 'databank',
        params: { databankAcronym: release.resource.acronym },
      }"
      >{{ release.resource.acronym }} -
      {{ release.resource.name }}
    </RouterLink>
    <RouterLink
      v-if="release.resource.mg_tableclass.includes('Model')"
      :to="{
        name: 'model',
        params: { modelAcronym: release.resource.acronym },
      }"
      >{{ release.resource.acronym }} -
      {{ release.resource.name }}
    </RouterLink>
    <div class="mt-4">
      <TableExplorer
        table="Tables"
        :filter="{
          release: {
            version: { equals: version },
            resource: { acronym: { equals: acronym } },
          },
        }"
        @click="openTable"
      />
      <TableExplorer
        table="Variables"
        :filter="{
          release: {
            version: { equals: version },
            resource: { acronym: { equals: acronym } },
          },
        }"
        @click="openVariable(row)"
      />
    </div>
  </div>
</template>

<script>
import { request } from "graphql-request";
import VariablesList from "../components/VariablesList";
import TopicSelector from "../components/TopicSelector";
import VariableTree from "../components/VariableTree";
import { TableExplorer } from "@mswertz/emx2-styleguide";

export default {
  components: { VariableTree, TopicSelector, VariablesList, TableExplorer },
  props: {
    acronym: String,
    version: String,
  },
  data() {
    return {
      release: null,
    };
  },
  methods: {
    openTable(row) {
      this.$router.push({
        name: "table",
        params: {
          acronym: this.acronym,
          version: this.version,
          tableName: row.name,
        },
      });
    },
    openVariable() {
      alert("todo implement");
    },
    reload() {
      console.log(this.version + " " + this.resourceAcronym);
      request(
        "graphql",
        `query Releases($acronym:String,$version:String){
        Releases(filter:{resource:{acronym:{equals:[$acronym]}},version:{equals:[$version]}}){resource{acronym,name,mg_tableclass},version}
        }`,
        {
          acronym: this.acronym,
          version: this.version,
        }
      )
        .then((data) => {
          this.release = data.Releases[0];
        })
        .catch((error) => {
          this.error = error.response.errors[0].message;
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
