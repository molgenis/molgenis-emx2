<template>
  <div class="container-fluid">
    <h1>Variable Explorer</h1>

    <h5>
      Variables <span v-if="variableCount">({{ variableCount }})</span>
    </h5>
    <ul class="nav nav-tabs">
      <li class="nav-item">
        <router-link class="nav-link" :to="{ name: 'variableDetails' }">
          Details
        </router-link>
      </li>
      <li class="nav-item">
        <router-link class="nav-link" :to="{ name: 'variableHarmonization' }">
          Harmonizarion
        </router-link>
      </li>
    </ul>
    <router-view></router-view>
  </div>
</template>

<script>
import { mapActions, mapGetters } from "vuex";
export default {
  name: "VariableExplorer",
  computed: {
    ...mapGetters(["variableCount"]),
  },
  methods: {
    ...mapActions(["fetchVariables"]),
    onError(e) {
      this.graphqlError = e.response ? e.response.errors[0].message : e;
    },
  },
  watch: {
    selectedKeywords() {
      this.fetchVariables();
    },
  },
  created() {
    this.fetchVariables();
  },
};
</script>

<style></style>
