<template>
  <div>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>

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
      <h6>{{ resourceType }}</h6>
      <p>
        <RouterLink
          :to="{
            name: resourceType.toLowerCase(),
            params: { acronym: release.resource.acronym },
          }"
          >{{ release.resource.acronym }} -
          {{ release.resource.name }}
        </RouterLink>
      </p>
      <h6>Release</h6>
      <p>{{ release.version }}</p>
      <h6>Data models used</h6>
      <p v-if="release.models">
        <ReleasesList :releases="release.models" />
      </p>
      <h6>Tables in this release</h6>
      <div class="mt-4">
        <TableExplorer
          table="Tables"
          :showHeader="false"
          :filter="{
            release: {
              _or: tableFilters,
            },
          }"
          @click="openTable"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { request } from "graphql-request";
import VariablesList from "../components/VariablesList";
import TopicSelector from "../components/TopicSelector";
import VariableTree from "../components/VariableTree";
import { TableExplorer, MessageError } from "@mswertz/emx2-styleguide";
import ModelsList from "../components/ModelsList";
import ReleasesList from "../components/ReleasesList";

export default {
  components: {
    ReleasesList,
    ModelsList,
    VariableTree,
    TopicSelector,
    VariablesList,
    TableExplorer,
    MessageError,
  },
  props: {
    acronym: String,
    version: String,
  },
  data() {
    return {
      release: null,
      graphqlError: null,
    };
  },
  computed: {
    resourceType() {
      if (this.release) {
        return this.release.resource.mg_tableclass.split(".")[1].slice(0, -1);
      }
    },
    tableFilters() {
      let result = [];
      result.push({
        version: { equals: this.version },
        resource: { acronym: { equals: this.acronym } },
      });
      if (this.release.models) {
        this.release.models.forEach((r) => {
          result.push({
            version: { equals: r.version },
            resource: { acronym: { equals: r.resource.acronym } },
          });
        });
      }
      return result;
    },
  },
  methods: {
    openTable(row) {
      this.$router.push({
        name: "table",
        params: {
          acronym: row.release.resource.acronym,
          version: row.release.version,
          name: row.name,
        },
      });
    },
    reload() {
      request(
        "graphql",
        `query Releases($acronym:String,$version:String){
        Releases(filter:{resource:{acronym:{equals:[$acronym]}},version:{equals:[$version]}}){models{version,resource{name,acronym}},resource{acronym,name,mg_tableclass},version}
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
          this.graphqlError = error.response.errors[0].message;
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
