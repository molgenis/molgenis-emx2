<template>
  <div class="container-fluid">
    <div class="row">
      <div class="col-6 col-sm-5 col-md-4 col-lg-3">
        <h5>Filters</h5>
        <div v-if="!network" class="bg-white px-1">
          <InputRefList
            id="networks-input-ref"
            label="Networks"
            tableName="Networks"
            v-model="networks"
            refLabel="${id}"
          ></InputRefList>
        </div>
        <div v-if="hasKeywords" class="bg-white px-1">
          <InputOntology
            id="topics-ontology-input"
            label="Topics"
            v-model="keywords"
            :isMultiSelect="true"
            tableName="Keywords"
            :show-expanded="true"
            schemaName="CatalogueOntologies"
          />
        </div>
        <div class="bg-white px-1">
          <InputRefList
            id="cohorts-input-ref"
            label="Cohorts"
            tableName="Cohorts"
            v-model="cohorts"
            refLabel="${id}"
            :maxNum="100"
            :orderBy="{ pid: 'ASC' }"
            :filter="
              network ? { networks: { pid: { equals: network } } } : null
            "
          ></InputRefList>
        </div>
      </div>
      <div class="col-6 col-sm-7 col-md-8 col-lg-9">
        <div class="ml-2">
          <div class="row">
            <div class="col-3">
              <h3>
                Variables
                <span v-if="variableCount">({{ variableCount }})</span>
              </h3>
            </div>
            <div class="col-9">
              <InputSearch
                id="search-variables-input"
                v-model="searchInput"
                placeholder="Search variables"
                :isClearBtnShown="true"
              />
            </div>
          </div>
          <div class="row">
            <div class="col-12">
              <filter-wells :filters="filtersFiltered" />
            </div>
          </div>
          <div class="row">
            <div class="col">
              <ul class="nav nav-tabs">
                <li class="nav-item">
                  <router-link
                    class="nav-link"
                    :class="{ active: $route.query.tab !== 'harmonization' }"
                    :to="{ path: this.$route.path, query: { tab: 'detail' } }"
                  >
                    Details
                  </router-link>
                </li>
                <li class="nav-item">
                  <router-link
                    class="nav-link"
                    :class="{ active: $route.query.tab === 'harmonization' }"
                    :to="{
                      path: this.$route.path,
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
                <variables-details-view :network="network" />
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import VariablesDetailsView from "./VariablesDetailsView.vue";
import HarmonizationView from "./HarmonizationView.vue";
import { mapActions, mapGetters, mapState, mapMutations } from "vuex";
import {
  InputSearch,
  InputOntology,
  InputRefList,
  FilterWells,
} from "molgenis-components";

export default {
  name: "VariableExplorer",
  components: {
    VariablesDetailsView,
    HarmonizationView,
    InputSearch,
    InputOntology,
    FilterWells,
    InputRefList,
  },
  props: {
    network: {
      type: String,
      default: null,
    },
  },
  computed: {
    ...mapState(["filters"]),
    ...mapGetters([
      "variables",
      "variableCount",
      "searchString",
      "selectedNetworks",
      "selectedKeywords",
      "selectedCohorts",
    ]),
    filtersFiltered() {
      //filter if network filter set external
      return this.filters.filter(
        (f) => f.name !== "networks" || this.network == null
      );
    },
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
    cohorts: {
      get() {
        return this.selectedCohorts;
      },
      set(value) {
        this.setSelectedCohorts(value);
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
    ...mapMutations([
      "setSelectedNetworks",
      "setSelectedKeywords",
      "setSelectedCohorts",
    ]),
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
    selectedCohorts() {
      this.fetchVariables();
    },
  },
  async created() {
    await this.fetchSchema();
    if (!this.variables.length) {
      // Only on initial creation
      this.fetchVariables();
    }
    if (this.network) {
      this.setSelectedNetworks([{ id: this.network }]);
    }
    this.fetchKeywords();
  },
};
</script>

<style></style>
