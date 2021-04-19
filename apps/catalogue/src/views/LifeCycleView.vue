<template>
  <div class="container-fluid">
    <h1>Hello LifeCycle</h1>
    <div class="list-group">
      <variable-list-item 
        v-for="(variable, index) in variables" :key=index 
        :variable="variable"
        :variableDetails="variableDetails[variable.name]"
        @request-variable-detail="fetchVariableDetails(variable.name)">
      </variable-list-item>
    </div>
  </div>
</template>

<script>
import { request, gql } from "graphql-request"
import VariableListItem from '../components/lifecycle/VariableListItem.vue';
export default {
  name: "LifeCycleView",
  components: { VariableListItem},
  data() {
    return {
      loading: true,
      variables: [],
      detailsShown: [],
      variableDetails: {}
    };
  },
  methods: {
    async onVariableClicked (name) {
      if(this.isDetailShown(name)) {
        this.detailsShown.splice(this.detailsShown.indexOf(), 1)
      } else {
        this.detailsShown.push(name)
        this.fetchVariableDetails(name)
      }
    },
    isDetailShown (name) {
      return this.detailsShown.includes(name)
    },
    async fetchVariableDetails (variableName) {
      if(this.variableDetails[variableName]) {
        // cache hit
        return this.variableDetails[variableName]
      }
      // else fetch 
      const query = gql`query Variables ($filter: VariablesFilter) { 
        Variables (limit: 1, filter: $filter){ 
          name,
          label, 
          format {
            name
          },
          unit {
            name
          },
          description,
          repeats { 
            name
          } 
        } 
      }`
      const variables = {
        "filter": {
          "name": {
            "like": [`${variableName}`]
          },
          "release": 
            {"equals": [
              {"resource": {
                 "acronym": "LifeCycle"
               },
               "version": "1.0.0"
              }]
            }
        }
      }
      const resp = await request('graphql', query, variables).catch(this.onError)
      this.$set(this.variableDetails, variableName, resp.Variables[0])
    },
    async reload() {
      this.loading = true
      const query = gql`query Variables ($search: String, $filter: VariablesFilter) { 
        Variables (limit: 100, search: $search, filter: $filter){ 
          name,
          release {
            resource {
              acronym
            },
            version
          },
          label, 
          repeats { 
            name 
          } 
        } 
      }`
      const variables = {
        // "search": "height",
        "filter": {
          "release": 
            {"equals": [
              {"resource": {
                 "acronym": "LifeCycle"
               },
               "version": "1.0.0"
              }]
            }
        }
      }
      const resp = await request('graphql', query, variables).catch(this.onError)
      this.variables = resp.Variables
      this.loading = false
    },
    onError(e) {
      this.graphqlError = e.response ? e.response.errors[0].message : e
    }
  },
  created() {
    this.reload();
  },
};
</script>

<style>
</style>