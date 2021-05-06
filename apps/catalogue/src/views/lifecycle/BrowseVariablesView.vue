<template>
  <div class="container-fluid">
    <div class="row">
      <div class="col-3">
        <h1>Cohort catalogue</h1>
      </div>
      <div class="col-9">
      </div>
    </div>
    <div class="row">
      <div class="col-3">
        <variable-filters-view />
      </div>
      <div class="col-9">
        <div class="mb-3">
          <variable-selected-filters-view />
        </div>
        <ul class="nav nav-tabs">
          <li class="nav-item">
            <span class="badge badge-pill badge-primary" style="float:right;margin-bottom:-5px;">{{ variableCount }}</span>
            <router-link
                class="nav-link"
                :to="{ name: 'VariablesView'}">
              Variables
            </router-link>
          </li>
          <li class="nav-item">
            <router-link
                class="nav-link"
                :to="{ name: 'HarmonizationView'}">
              Harmonization
            </router-link>
          </li>
        </ul>
        <router-view class="mt-2"></router-view>
      </div>
    </div>

  </div>
</template>

<script>
import VariableListItem from '@/components/lifecycle/VariableListItem'
import VariableSelectedFiltersView from '@/views/lifecycle/VariableSelectedFiltersView'
import VariableFiltersView from '@/views/lifecycle/VariableFiltersView'
import {mapActions, mapState} from 'vuex'

export default {
  name: "VariablesView",
  components: {VariableSelectedFiltersView, VariableListItem, VariableFiltersView},
  data() {
    return {
      activeTab: 'variables' // variables or harmonization
    }
  },
  computed: {
    ...mapState(['variableCount', 'filters']),
  },
  watch: {
    filters () {
      this.fetchVariables()
    }
  },
  methods: {
    ...mapActions(['fetchVariables']),
    onError(e) {
      this.graphqlError = e.response ? e.response.errors[0].message : e
    }
  },
  created() {
    this.fetchVariables()
  }
}
</script>

<style scoped>
.mg-selected-topic-bage:hover {
  text-decoration: line-through;
}
</style>
