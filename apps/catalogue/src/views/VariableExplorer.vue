<template>
  <div class="container-fluid">
    <div class="row">
      <div class="col-3">
        <h5>Filters</h5>
        <h6 class="mt-3">Networks</h6>
        <input-ref table="Networks" v-model="networks" :list="true"></input-ref>
        <h6 class="mt-3">Topics</h6>
        <tree-component
          :items="keywords"
          v-model="selectedKeywords"
        ></tree-component>
      </div>
      <div class="col-9">
        <div class="row">
          <div class="col-3">
            <h3>
              Variables <span v-if="variableCount">({{ variableCount }})</span>
            </h3>
          </div>
          <div class="col-9">
            <InputSearch v-model="searchInput" placeholder="Search variables" />
          </div>
        </div>
        <div class="row">
          <div class="col-12">
            <filter-wells :filters="filters" />
          </div>
        </div>
        <div class="row">
          <div class="col">
            <ul class="nav nav-tabs">
              <li class="nav-item">
                <router-link
                  class="nav-link"
                  :class="{ active: $route.query.tab !== 'harmonization' }"
                  :to="{ path: 'variable-explorer', query: { tab: 'detail' } }"
                >
                  Details
                </router-link>
              </li>
              <li class="nav-item">
                <router-link
                  class="nav-link"
                  :class="{ active: $route.query.tab === 'harmonization' }"
                  :to="{
                    path: 'variable-explorer',
                    query: { tab: 'harmonization' },
                  }"
                >
                  Harmonization
                </router-link>
              </li>
            </ul>
            <template v-if="$route.query.tab === 'harmonization'">
              <harmonization-view />
            </template>
            <template v-else>
              <variables-details-view />
            </template>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import VariablesDetailsView from "./VariablesDetailsView";
import HarmonizationView from "./HarmonizationView";
import { mapActions, mapGetters, mapState, mapMutations } from "vuex";
import { InputSearch } from "@mswertz/emx2-styleguide";
import TreeComponent from "../../../styleguide/src/tree/TreeComponent";
import InputRef from "../../../styleguide/src/forms/InputRef";
import FilterWells from "../../../styleguide/src/tables/FilterWells";

export default {
  name: "VariableExplorer",
  components: {
    VariablesDetailsView,
    HarmonizationView,
    InputSearch,
    TreeComponent,
    FilterWells,
    InputRef,
  },
  computed: {
    ...mapState(["filters", "keywords"]),
    ...mapGetters([
      "variables",
      "variableCount",
      "searchString",
      "selectedKeywords",
      "selectedNetworks",
    ]),
    searchInput: {
      get() {
        return this.$store.state.searchInput;
      },
      set(value) {
        this.$store.commit("setSearchInput", value);
      },
    },
    networks: {
      get() {
        return this.selectedNetworks;
      },
      set(value) {
        this.setSelectedNetworks(value);
      },
    },
  },
  methods: {
    ...mapMutations(["setSelectedNetworks"]),
    ...mapActions(["fetchVariables", "fetchKeywords", "fetchSchema"]),
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
    selectedNetworks() {
      this.fetchVariables();
    },
  },
  async created() {
    await this.fetchSchema();
    if (!this.variables.lenght) {
      // Only on initial creation
      this.fetchVariables();
    }
    this.fetchKeywords();
  },
};
</script>

<style></style>
