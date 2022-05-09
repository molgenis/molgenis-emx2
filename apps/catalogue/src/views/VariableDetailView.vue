<template>
  <div v-if="variable">
    <h3 v-if="!$route.query.fromName">{{ variable.label }}</h3>
    <ul class="nav nav-tabs" v-if="!$route.query.fromName">
      <li class="nav-item">
        <router-link
          class="nav-link"
          :class="{ active: $route.query.tab !== 'harmonization' }"
          :to="{
            path: this.$route.path,
            query: { ...$route.query, tab: 'detail' },
          }"
        >
          Details
        </router-link>
      </li>
      <li v-if="variable.mappings" class="nav-item">
        <router-link
          class="nav-link"
          :class="{ active: $route.query.tab === 'harmonization' }"
          :to="{
            path: this.$route.path,
            query: { ...$route.query, tab: 'harmonization' },
          }"
        >
          Harmonization
        </router-link>
      </li>
      <li v-else class="nav-item" disabled>
        <a class="nav-link">Harmonization</a>
      </li>
    </ul>
    <template
      v-if="$route.query.tab === 'harmonization' && !$route.query.fromName"
    >
      <single-var-harmonization-view :variable="variable" />
    </template>
    <template v-else-if="$route.query.fromName">
      <from-variable-details
        :version="version"
        :sourceCohort="$route.query.sourceCohort"
        :fromName="$route.query.fromName"
        :toName="variable.label"
      />
    </template>
    <template v-else>
      <single-var-details-view :variable="variable" />
    </template>
  </div>
</template>

<script>
import { fetchDetails } from "../store/repository/variableRepository";
import SingleVarDetailsView from "./SingleVarDetailsView";
import SingleVarHarmonizationView from "./SingleVarHarmonizationView";
import FromVariableDetails from "./FromVariableDetails";

export default {
  name: "VariableDetailView",
  components: {
    SingleVarDetailsView,
    SingleVarHarmonizationView,
    FromVariableDetails,
  },
  props: {
    name: String,
    model: String,
    version: String,
  },
  data() {
    return {
      variable: {},
    };
  },
  async created() {
    this.variable = await fetchDetails(this.name, this.model, this.version);
  },
};
</script>
