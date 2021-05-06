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
import VariableListItem from '@/components/lifecycle/VariableListItem.vue'
import { mapState, mapActions } from 'vuex'
export default {
  name: "VariablesListView",
  components: { VariableListItem},
  computed: {
    ...mapState(['variableDetails', 'variables'])
  },
  methods: {
    ...mapActions(['fetchVariableDetails'])
  }
}
</script>
