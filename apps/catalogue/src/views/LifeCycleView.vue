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
        <filters />
      </div>
      <div class="col-9">
        <div class="mb-3">
          <selected-filters />
        </div>
        <h5>Variables ({{ variableCount }})</h5>
        <ul class="nav nav-tabs">
          <li class="nav-item">
            <router-link
                class="nav-link"
                :to="{ name: 'LifeCycleVariablesView'}">
              Details
            </router-link>

          </li>
          <li class="nav-item">
            <router-link
                class="nav-link"
                :to="{ name: 'LifeCycleHarmonizationView'}">
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
import Filters from '@/views/Filters'
import SelectedFilters from "@/views/SelectedFilters";
import {mapActions, mapState} from 'vuex'

export default {
  name: "LifeCycleView",
  components: {SelectedFilters, VariableListItem, Filters},
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
