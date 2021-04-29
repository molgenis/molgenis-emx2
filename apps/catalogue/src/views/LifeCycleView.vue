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
        <h5>Topics</h5>
        <keyword-tree :keywords="keywords" v-model="selectedKeywords"></keyword-tree>
      </div>
      <div class="col-9">
        <div class="mb-3">
          <span><strong>Selected topics: </strong></span> 
          <a 
            class="mg-selected-topic-bage badge badge-pill badge-light mr-1" 
            href="#"
            v-for="(selectedKeywordsObject, index) in selectedKeywordsObjects" :key=index 
            @click="removeKeywordFromSelection(selectedKeywordsObject.name)"
          >{{selectedKeywordsObject.definition}} x
          </a>
        </div>
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
import { mapActions, mapGetters, mapMutations } from 'vuex'

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
    },
    selectedKeywordsObjects () {
      return this.selectedKeywords.map(selecteName => this.keywords.find((k) => k.name === selecteName))
    }
  },
  methods: {
    ...mapActions(['fetchVariables', 'fetchKeywords']),
    ...mapMutations(['removeKeywordFromSelection']),
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

<style scoped>
  .mg-selected-topic-bage:hover {
    text-decoration: line-through;
  }
</style>
