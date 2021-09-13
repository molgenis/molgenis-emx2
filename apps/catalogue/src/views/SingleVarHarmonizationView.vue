<template>
  <div class="container-fluid pt-3">
    <ul v-if="variable" class="nav nav-tabs">
      <li
        class="nav-item"
        v-for="mapping in variable.mappings"
        :key="mapping.fromTable.release.resource.pid"
      >
        <router-link
          class="nav-link"
          :class="{
            active:
              $route.query.sourceCohort ===
              mapping.fromTable.release.resource.pid,
          }"
          :to="{
            name: 'VariableDetailView',
            query: {
              ...$route.query,
              sourceCohort: mapping.fromTable.release.resource.pid,
            },
          }"
        >
          {{ mapping.fromTable.release.resource.pid }}
        </router-link>
      </li>
    </ul>
    <template v-if="$route.query.tab === 'harmonization'">
      <resource-harmonization-details
        :variable="variable"
        :name="name"
        :sourceCohort="$route.query.sourceCohort"
      />
    </template>
  </div>
</template>

<script>
import ResourceHarmonizationDetails from "./ResourceHarmonizationDetails";
export default {
  name: "SingleVarHarmonizationView",
  components: { ResourceHarmonizationDetails },
  props: {
    name: String,
    network: String,
    version: String,
    variable: Object,
  },
  async created() {
    // initialy select the first mapping
    if (
      this.variable.mappings &&
      this.variable.mappings[0] &&
      !this.$route.query.sourceCohort
    ) {
      this.$router.replace({
        name: "VariableDetailView",
        query: {
          ...this.$route.query,
          sourceCohort:
            this.variable.mappings[0].fromTable.release.resource.pid,
        },
      });
    }
  },
};
</script>

<style></style>
