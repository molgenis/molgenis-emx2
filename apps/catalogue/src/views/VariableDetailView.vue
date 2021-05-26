<template>
  <div>
    <nav aria-label="breadcrumb">
      <ol class="breadcrumb">
        <li class="breadcrumb-item">
          <router-link :to="{ name: 'variableDetails' }">
            Variables
          </router-link>
        </li>
        <li class="breadcrumb-item active" aria-current="page">{{ name }}</li>
      </ol>
    </nav>
    <h3 v-if="variable">{{ variable.label }}</h3>
    <ul class="nav nav-tabs">
      <li class="nav-item">
        <router-link class="nav-link" :to="{ name: 'singleVariableDetails' }">
          Details
        </router-link>
      </li>
      <li class="nav-item">
        <router-link
          class="nav-link"
          :to="{ name: 'singleVariableHarmonization' }"
        >
          Harmonization
        </router-link>
      </li>
    </ul>
    <router-view></router-view>
  </div>
</template>

<script>
import { request } from "graphql-request";
import variableDetails from "../store/query/variableDetails.gql";

export default {
  name: "VariableDetailView",
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
  created() {
    this.fetch(this.name);
  },
};
</script>

<style scoped>
nav {
  margin-top: -1rem;
  margin-left: -2rem;
  margin-right: -2rem;
}
</style>
