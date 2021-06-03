<template>
  <div class="container-fluid pt-3">
    <ul v-if="variable" class="nav nav-tabs">
      <li
        class="nav-item"
        v-for="mapping in variable.mappings"
        :key="mapping.fromTable.release.resource.acronym"
      >
        <router-link
          class="nav-link"
          :to="{
            name: 'resourceHarmonizationDetails',
            params: {
              name,
              version,
              network,
              sourceCohort: mapping.fromTable.release.resource.acronym,
            },
          }"
        >
          {{ mapping.fromTable.release.resource.acronym }}
        </router-link>
      </li>
    </ul>
    <router-view :key="$route.fullPath"></router-view>
  </div>
</template>

<script>
import { request } from "graphql-request";
import variableDetails from "../store/query/variableDetails.gql";
export default {
  name: "SingleVarHarmonizationView",
  props: {
    name: String,
    network: String,
    version: String,
  },
  data() {
    return {
      variable: {},
    };
  },
  methods: {
    async fetch(name, network, version) {
      const params = {
        filter: {
          name: { equals: name },
          release: {
            equals: [
              {
                resource: {
                  acronym: network,
                },
                version,
              },
            ],
          },
        },
      };
      const resp = await request("graphql", variableDetails, params).catch(
        (e) => console.error(e)
      );
      this.variable = resp.Variables[0];
    },
  },
  async created() {
    await this.fetch(this.name, this.network, this.version);
    // initialy select the first mapping
    if (
      this.variable.mappings &&
      this.variable.mappings[0] &&
      !this.$route.params.acronym
    ) {
      this.$router.push({
        name: "resourceHarmonizationDetails",
        params: {
          name: this.name,
          network: this.network,
          version: this.version,
          sourceCohort:
            this.variable.mappings[0].fromTable.release.resource.acronym,
        },
      });
    }
  },
};
</script>

<style></style>
