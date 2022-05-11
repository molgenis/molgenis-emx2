<template>
  <div class="container-fluid pt-3">
    <ul v-if="variable" class="nav nav-tabs">
      <li
        class="nav-item"
        v-for="resource in resourceListSorted"
        :key="resource"
      >
        <router-link
          class="nav-link"
          :class="{
            active: $route.query.sourceCohort === resource,
          }"
          :to="{
            path: $route.path,
            query: {
              ...$route.query,
              sourceCohort: resource,
            },
          }"
        >
          {{ resource }}
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
  computed: {
    resourceListSorted() {
      if (this.variable.mappings) {
        return this.variable.mappings
          .map((m) => m.fromTable.dataDictionary.resource.pid)
          .sort();
      } else {
        return [];
      }
    },
  },
  async created() {
    // initialy select the first mapping
    if (
      this.variable.mappings &&
      this.variable.mappings[0] &&
      !this.$route.query.sourceCohort
    ) {
      this.$router.replace({
        path: this.$route.path,
        query: {
          ...this.$route.query,
          sourceCohort: this.resourceListSorted[0],
        },
      });
    }
  },
};
</script>

<style></style>
