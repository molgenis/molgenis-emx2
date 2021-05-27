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
              acronym: mapping.fromTable.release.resource.acronym,
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
  },
  data() {
    return {
      variable: {},
    };
  },
  methods: {
    async fetch(name) {
      const params = { filter: { name: { equals: name } } };
      const resp = await request("graphql", variableDetails, params).catch(
        (e) => console.error(e)
      );
      this.variable = resp.Variables[0];
    },
  },
  async created() {
    await this.fetch(this.name);
    // initialy select the first mapping
    if (this.variable.mappings[0] && !this.$route.params.acronym) {
      this.$router.push({
        name: "resourceHarmonizationDetails",
        params: {
          name: this.name,
          acronym: this.variable.mappings[0].fromTable.release.resource.acronym,
        },
      });
    }
  },
};
</script>

<style></style>
