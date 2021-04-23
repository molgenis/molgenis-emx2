<template>
  <button 
    class="list-group-item list-group-item-action " 
    @click="onItemClicked"
  >
    <div class="d-flex w-100 justify-content-between">
      <span class="text-capitalize"><strong>{{ variable.label }}</strong></span>
      <span v-if="variable.repeats" class="badge badge-primary badge-pill">{{ variable.repeats.length }} repeats</span>
    </div>

    <p class="mt-3" v-if="showDetail">
      <template v-if="variableDetails">
        <dl class="row">
          <dt class="col-2">variable</dt>
          <dd class="col-10" >
            {{variableDetails.name}}
          </dd>
          <dt class="col-2">description</dt>
          <dd class="col-10">
            <span v-if="variableDetails.description">{{ variableDetails.description }}</span>
            <span v-else>-</span>
          </dd>
          <dt class="col-2">unit</dt>
          <dd class="col-10">
            <span v-if="variableDetails.unit">{{ variableDetails.unit.name}}</span>
            <span v-else>-</span>
            </dd>
          <dt class="col-2">format</dt>
          <dd class="col-10">
            <span v-if="variableDetails.format">{{ variableDetails.format.name}}</span>
            <span v-else>-</span>
            </dd>
        </dl>

        <div v-if="variableDetails.repeats">
          <label>Measurements</label>
          <ul class="list-group">
            <li class="list-group-item"  v-for="(repeat, index) in variableDetails.repeats" :key=index >
              {{repeat.name}}
            </li>
          </ul>
        </div>
      </template>
    </p>

  </button>
</template>

<script>
export default {
  name: "VariableListItem",
  props: {
    variable: Object,
    variableDetails: Object
  },
  data() {
    return {
      showDetail: false,
    }
  },
  methods: {
    onItemClicked () {
      if(!this.showDetail) {
        this.$emit('request-variable-detail', this.variable.name)
        this.showDetail = true
      } else {
        this.showDetail = false
      }
    }
  }
}
</script>

<style scoped>
.list-group-item, .list-group-item-action:hover {
  cursor: pointer;
}
</style>

