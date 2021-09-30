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
        <small>Release:</small><br />{{ release.resource.pid }} ({{
          release.version
        }})
      </h1>
      <h6>{{ resourceType }}</h6>
      <p>
        <RouterLink
          :to="{
            name: resourceType.toLowerCase(),
            params: { pid: release.resource.pid },
          }"
          >{{ release.resource.pid }} -
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
import { TableExplorer, MessageError } from "@mswertz/emx2-styleguide";
import ReleasesList from "../components/ReleasesList";

export default {
  components: {
    ReleasesList,
    TableExplorer,
    MessageError,
  },
  props: {
    pid: String,
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
        return this.release.resource.mg_tableclass.split(".")[1];
      }
      return null;
    },
    tableFilters() {
      let result = [];
      result.push({
        version: { equals: this.version },
        resource: { pid: { equals: this.pid } },
      });
      if (this.release.models) {
        this.release.models.forEach((r) => {
          result.push({
            version: { equals: r.version },
            resource: { pid: { equals: r.resource.pid } },
          });
        });
      }
      return result;
    },
  },
  methods: {
    openTable(row) {
      this.$router.push({
        name: "Tables-details",
        params: {
          pid: row.release.resource.pid,
          version: row.release.version,
          name: row.name,
        },
      });
    },
    reload() {
      request(
        "graphql",
        `query Releases($pid:String,$version:String){
        Releases(filter:{resource:{pid:{equals:[$pid]}},version:{equals:[$version]}}){models{version,resource{name,pid}},resource{pid,name,mg_tableclass},version}
        }`,
        {
          pid: this.pid,
          version: this.version,
        }
      )
        .then((data) => {
          this.release = data.Releases[0];
        })
        .catch((error) => {
          this.graphqlError = error;
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
