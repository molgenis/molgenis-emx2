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
    <h6>Tables in this release</h6>
    <div class="mt-4">
      <TableExplorer
        table="Tables"
        :showHeader="false"
        :filter="{
          release: {
            version: { equals: version },
            resource: { acronym: { equals: acronym } },
          },
        }"
        @click="openTable"
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
  computed: {
    resourceType() {
      if (this.release) {
        return this.release.resource.mg_tableclass.split(".")[1].slice(0, -1);
      }
    },
  },
  methods: {
    openTable(row) {
      this.$router.push({
        name: "table",
        params: {
          acronym: this.acronym,
          version: this.version,
          name: row.name,
        },
      });
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
