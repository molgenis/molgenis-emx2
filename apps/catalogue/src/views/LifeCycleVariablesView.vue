<template>
  <div>
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
import VariableListItem from '../components/lifecycle/VariableListItem.vue'
import { mapGetters, mapActions } from 'vuex'
export default {
  name: "LifeCycleVariablesView",
  components: { VariableListItem},
  computed: {
    ...mapGetters(['variables', 'variableCount', 'variableDetails']),
  },
  methods: {
    ...mapActions(['fetchVariableDetails'])
  }
}
</script>
