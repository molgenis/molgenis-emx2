<template>
  <div class="container-fluid">
    <div class="row">
      <div class="col-3">
        <h1>Variables</h1>
      </div>
      <div class="col-9"></div>
    </div>
    <div class="row">
      <div class="col-3">
        <variable-filters-view />
      </div>
      <div class="col-9">
        <div class="row">
          <div class="col-12">
            <search-box v-model="variableSearch" placeholder="Search variables..." />
          </div>
        </div>
        <div class="row">
          <div class="col-12">
            <div class="mb-3 mt-3">
              <variable-selected-filters-view />
            </div>
          </div>
        </div>
        <div class="row">
          <div class="col-12">
            <ul class="nav nav-tabs">
              <li class="nav-item">
                <span
                  class="badge badge-pill badge-primary"
                  style="float: right; margin-bottom: -5px"
                  >{{ variableCount }}</span
                >
                <router-link class="nav-link" :to="{ name: 'VariablesView' }">
                  Variables
                </router-link>
              </li>
              <li class="nav-item">
                <router-link
                  class="nav-link"
                  :to="{ name: 'MappingView', url: 'mapping' }"
                >
                  Mapping
                </router-link>
              </li>
              <li class="nav-item">
                <router-link
                  class="nav-link"
                  :to="{ name: 'MappingDetailView', url: 'mapping/detail' }"
                >
                  Detail
                </router-link>
              </li>
            </ul>
            <router-view class="mt-2"></router-view>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>

import VariableListItem from "@/components/lifecycle/VariableListItem";
import { mapActions, mapState, mapMutations } from "vuex";
import VariableFiltersView from "@/views/lifecycle/VariableFiltersView";
import VariableSelectedFiltersView from "@/views/lifecycle/VariableSelectedFiltersView";
import SearchBox from "@/components/lifecycle/SearchBox.vue";

export default {
  name: "VariablesView",
  components: {
    VariableSelectedFiltersView,
    VariableListItem,
    VariableFiltersView,
    SearchBox,
  },
  data() {
    return {
      activeTab: "variables", // variables or harmonization
      variableSearch: ''
    };
  },
  computed: {
    ...mapState(["variableCount", "filters"]),
  },
  watch: {
    filters() {
      this.fetchVariables();
    },
    variableSearch() {  
      this.setVariableSearch(this.variableSearch)
      this.fetchVariables();
    }
  },
  methods: {
    ...mapActions(["fetchVariables"]),
    ...mapMutations(["setVariableSearch"]),
    onError(e) {
      this.graphqlError = e.response ? e.response.errors[0].message : e;
    },
  },
  created() {
    this.fetchVariables();
  },
};
</script>

<style scoped>
.mg-selected-topic-bage:hover {
  text-decoration: line-through;
}
</style>
