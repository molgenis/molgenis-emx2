<template>
  <div class="container-fluid">
    <div class="container-fluid bg-white">
      <RouterLink
        class="btn btn-primary ml-2 float-right"
        :to="{ name: 'NetworkCohorts', params: { network: network } }"
        >View {{ network }} cohorts
      </RouterLink>
      <h1>{{ network }} harmonized variables</h1>
      <p>
        This page lists all variables harmonized in the {{ network }} network.
        You can search and filter variables, and drill down to see their
        harmonization details.
      </p>
    </div>
    <div class="bg-white pt-4">
      <VariableExplorer :network="network" />
    </div>
  </div>
</template>

<script>
import VariableExplorer from "../views/VariableExplorer.vue";

export default {
  components: { VariableExplorer },
  props: {
    network: {
      type: String,
      required: true,
    },
  },
};
</script>
