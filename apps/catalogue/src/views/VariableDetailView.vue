<template>
  <div>
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
    <router-view :variable="variable"></router-view>
  </div>
</template>

<script>
import { fetchDetails } from "../store/repository/variableRepository";
import { mapMutations } from "vuex";

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
  computed: {
    crumbs() {
      const variableCrumb = {
        label: "variables",
        to: { name: "variableDetails" },
      };
      return this.variable
        ? [variableCrumb, { label: this.variable.name }]
        : [variableCrumb];
    },
  },
  methods: {
    ...mapMutations(["setBreadCrumbs"]),
  },
  async created() {
    this.variable = await fetchDetails(this.name, this.network, this.version);
    this.setBreadCrumbs([
      { label: "variable explorer", to: { name: "variableDetails" } },
      { label: "detail" },
    ]);
  },
};
</script>
