<template>
  <div class="container-fluid">
    <div class="row">
      <div class="col-6 col-sm-5 col-md-4 col-lg-3">
        <h5>Filters</h5>
        <h6 class="mt-3">Networks</h6>
        <input-ref table="Networks" v-model="networks" :list="true"></input-ref>
        <template v-if="hasKeywords">
          <h6 class="mt-3">Topics</h6>
          <InputOntology
            table="Keywords"
            v-model="keywords"
            :list="true"
            :show-expanded="true"
          />
        </template>
      </div>
      <div class="col-6 col-sm-7 col-md-8 col-lg-9">
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
                  :to="{ name: 'variableExplorer', query: { tab: 'detail' } }"
                >
                  Details
                </router-link>
              </li>
              <li class="nav-item">
                <router-link
                  class="nav-link"
                  :class="{ active: $route.query.tab === 'harmonization' }"
                  :to="{
                    name: 'variableExplorer',
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
import {
  InputSearch,
  InputOntology,
  InputRef,
  FilterWells,
} from "@mswertz/emx2-styleguide";

export default {
  name: "VariableExplorer",
  components: {
    VariablesDetailsView,
    HarmonizationView,
    InputSearch,
    InputOntology,
    FilterWells,
    InputRef,
  },
  computed: {
    ...mapState(["filters"]),
    ...mapGetters([
      "variables",
      "variableCount",
      "searchString",
      "selectedNetworks",
      "selectedKeywords",
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
    keywords: {
      get() {
        return this.selectedKeywords;
      },
      set(value) {
        this.setSelectedKeywords(value);
      },
    },
    hasKeywords() {
      return !!(
        this.$store.state.keywords && this.$store.state.keywords.length
      );
    },
  },
  methods: {
    ...mapMutations(["setSelectedNetworks", "setSelectedKeywords"]),
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
    if (!this.variables.length) {
      // Only on initial creation
      this.fetchVariables();
    }
    this.fetchKeywords();
  },
};
</script>

<style></style>
