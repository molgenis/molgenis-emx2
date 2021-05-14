<template>
  <div class="container-fluid">
    <h1>Variable Explorer</h1>

    <div class="row">
      <div class="col">
        <h5>
          Variables <span v-if="variableCount">({{ variableCount }})</span>
        </h5>
      </div>
      <div class="col">
        <InputSearch v-model="searchInput" placeholder="Search variables" />
      </div>
    </div>

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
import { InputSearch } from "@mswertz/emx2-styleguide";
export default {
  name: "VariableExplorer",
  components: { InputSearch },
  computed: {
    ...mapGetters(["variableCount", "searchString"]),
    searchInput: {
      get() {
        return this.$store.state.searchInput;
      },
      set(value) {
        this.$store.commit("setSearchInput", value);
      },
    },
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
    searchString() {
      this.fetchVariables();
    },
  },
  created() {
    this.fetchVariables();
  },
};
</script>

<style></style>
