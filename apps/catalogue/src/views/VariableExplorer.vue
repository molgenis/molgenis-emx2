<template>
  <div class="container-fluid">
    <h1>Variable Explorer</h1>

    <div class="row">
      <div class="col-3">
        <h5>
          Filters
        </h5>
        <h6>
          Topics
        </h6>
        <tree-component :items="keywords" v-model="selected"></tree-component>
      </div>
      <div class="col-9">
        <div class="row">
          <div class="col-3">
            <h5>
              Variables <span v-if="variableCount">({{ variableCount }})</span>
            </h5>
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
        <!-- <div class="row">
          <div class="col">
            <ul class="nav nav-tabs">
              <li class="nav-item">
                <router-link class="nav-link" :to="{ name: 'variableDetails' }">
                  Details
                </router-link>
              </li>
              <li class="nav-item">
                <router-link
                  class="nav-link"
                  :to="{ name: 'variableHarmonization' }"
                >
                  Harmonization
                </router-link>
              </li>
            </ul>
            <router-view></router-view>
          </div>
        </div> -->
        <router-view></router-view>
      </div>
    </div>
  </div>
</template>

<script>
import { mapActions, mapGetters, mapState, mapMutations } from "vuex";
import { FilterWells, InputSearch, TreeComponent } from "@mswertz/emx2-styleguide";

export default {
  name: "VariableExplorer",
  components: {
    InputSearch,
    TreeComponent,
    FilterWells
  },
  computed: {
    ...mapState(["filters", "keywords"]),
    ...mapGetters(["variables", "variableCount", "searchString", "selectedKeywords"]),
    ...mapMutations(["setSelectedKeywords"]),
    searchInput: {
      get() {
        return this.$store.state.searchInput
      },
      set(value) {
        this.$store.commit('setSearchInput', value);
      },
    },
    selected: {
      get() {
        return this.selectedKeywords;
      },
      set(value) {
        this.setSelectedKeywords(value);
      },
    },
    selectedKeywordsObjects() {
      return this.selectedKeywords.map((selecteName) =>
        this.keywords.find((k) => k.name === selecteName)
      );
    },
  },
  methods: {
    ...mapActions(["fetchVariables", "fetchKeywords"]),
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
    if (!this.variables.lenght) {
      // Only on initial creation
      this.fetchVariables();
    }
    this.fetchKeywords();
  },
};
</script>

<style></style>
