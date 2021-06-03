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
      <li v-if="variable.mappings" class="nav-item">
        <router-link
          class="nav-link"
          :to="{ name: 'singleVariableHarmonization' }"
        >
          Harmonization
        </router-link>
      </li>
      <li v-else class="nav-item" disabled>
        <a class="nav-link">Harmonization</a>
      </li>
    </ul>
    <router-view></router-view>
  </div>
</template>

<script>
import { fetchDetails } from "../store/repository/variableRepository";

export default {
  name: "VariableDetailView",
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
  async created() {
    this.variable = await fetchDetails(this.name, this.network, this.version);
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
