<template>
  <div>
    <nav class="mg-page-nav" aria-label="breadcrumb">
      <ol class="breadcrumb">
        <li class="breadcrumb-item">
          <router-link :to="{ name: 'variableDetails' }">
            variables
          </router-link>
        </li>
        <li class="breadcrumb-item">
          <router-link :to="{ name: 'singleVariableDetails' }">
            {{ name }}
          </router-link>
        </li>
        <li class="breadcrumb-item" aria-current="page">
          <router-link :to="{ name: 'resourceHarmonizationDetails' }">
            {{ sourceCohort }}
          </router-link>
        </li>
        <li class="breadcrumb-item active" aria-current="page">
          {{ fromName }}
        </li>
      </ol>
    </nav>
    <div v-if="variable">
      <h3>{{ variable.label }}</h3>
      <div class="row">
        <variable-details class="col" :variableDetails="variable" :showMappedBy="false" />
      </div>
    </div>
    <variable-details></variable-details>
  </div>
</template>

<script>
import { fetchFromVariableDetails } from "../store/repository/variableRepository";
import VariableDetails from "../components/VariableDetails.vue";

export default {
  name: "FromVariableDetails",
  components: { VariableDetails },
  props: {
    name: String,
    network: String,
    version: String,
    sourceCohort: String,
    fromName: String
  },
  data() {
    return {
      variable: {},
    };
  },
  async created() {
    this.variable = await fetchFromVariableDetails(
      this.fromName,
      this.sourceCohort,
      this.version
    );
  },
};
</script>

<style></style>
