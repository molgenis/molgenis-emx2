<template>
  <div class="container-fluid">
    <div class="row">
      <div class="col-12">
        <h1>Cohort catalogue</h1>
      </div>
    </div>
    <div class="row">
      <div class="col-3">
        <h5>Topics</h5>
        <keyword-tree :keywords="keywords" v-model="selectedKeywords"></keyword-tree>
      </div>
      <div class="col-9">
        <h5>Variables ({{ variableCount}})</h5>
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
              Harmonizarion
            </router-link>
          </li>
        </ul>
        <router-view class="mt-2"></router-view>
      </div>
    </div>
    
  </div>
</template>

<script>
import VariableListItem from '../components/lifecycle/VariableListItem.vue'
import KeywordTree from '../components/lifecycle/KeywordTree.vue'
import { mapActions, mapGetters } from 'vuex'

export default {
  name: "LifeCycleView",
  components: { VariableListItem, KeywordTree},
  data() {
    return {
      activeTab: 'variables' // variables or harmonization
    }
  },
  computed: {
    ...mapGetters(['variableCount', 'keywords']),
    selectedKeywords: {
      get () {
        return this.$store.state.selectedKeywords
      },
      set (value) {
        this.$store.commit('setSelectedKeywords', value)
      }
    }
  },
  methods: {
    ...mapActions(['fetchVariables', 'fetchKeywords']),
    onError(e) {
      this.graphqlError = e.response ? e.response.errors[0].message : e
    }
  },
  watch: {
    selectedKeywords () {
      this.fetchVariables()
    }
  },
  created() {
    this.fetchVariables()
    this.fetchKeywords()
  }
}
</script>
