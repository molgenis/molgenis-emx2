<template>
  <div class="container-fluid">
    <h1>Variable Explorer</h1>

    <div class="row">
      <div class="col-3">
        <tree-component :items="keywords" v-model="selected"></tree-component>
      </div>
      <div class="col-9">
        <h5>
          Variables <span v-if="variableCount">({{ variableCount }})</span>
        </h5>
      </div>
      <div class="col">
        <InputSearch v-model="search" placeholder="Search variables" />
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
          Harmonization
        </router-link>
      </li>
    </ul>
    <router-view></router-view>
  </div>
</template>

<script>
import { mapActions, mapGetters, mapState, mapMutations } from "vuex";
import { InputSearch, TreeComponent } from "@mswertz/emx2-styleguide";
export default {
  name: "VariableExplorer",
  components: { 
    InputSearch,
    TreeComponent
  },
  computed: {
    ...mapState(['keywords', 'selectedKeywords', 'searchInput']),
    ...mapGetters(["variables", "variableCount", "searchString"]),
    ...mapMutations(['setSelectedKeywords', 'setSearchInput']),
    search: {
      get() {
        return this.searchInput;
      },
      set(value) {
        this.setSearchInput(value);
      },
    },
    selected: {
      get () {
        return this.selectedKeywords
      },
      set (value) {
        this.setSelectedKeywords(value)
      }
    },
    selectedKeywordsObjects () {
      return this.selectedKeywords.map(selecteName => this.keywords.find((k) => k.name === selecteName))
    }
  },
  methods: {
    ...mapActions(['fetchVariables', 'fetchKeywords']),
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
