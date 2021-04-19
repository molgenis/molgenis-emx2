<template>
  <button 
    class="list-group-item list-group-item-action " 
    @click="onItemClicked"
  >
    <div class="d-flex w-100 justify-content-between">
      <span class="mb-1">{{ variable.label }}</span>
      <span v-if="variable.repeats" class="badge badge-primary badge-pill">{{ variable.repeats.length }} repeats</span>
    </div>

    <p class="mb-1" v-if="showDetail">
      <template v-if="variableDetails">
        <dl class="row">
          <dt class="col-sm-3">Name</dt>
          <dd class="col-sm-9">A description list is perfect for defining terms.</dd>
          <dt class="col-sm-3" v-if="variableDetails.description">Description</dt>
          <dd class="col-sm-9" v-if="variableDetails.description">{{ variableDetails.description }}</dd>
          <dt class="col-sm-3">Unit</dt>
          <dd class="col-sm-9" v-if="variableDetails.unit">{{ variableDetails.unit.name}}</dd>
          <dt class="col-sm-3">Format</dt>
          <dd class="col-sm-9" v-if="variableDetails.format">{{ variableDetails.format.name}}</dd>
        </dl>

        <div v-if="variableDetails.repeats">
          <ul class="list-group">
            <li class="list-group-item"  v-for="(repeat, index) in variableDetails.repeats" :key=index >
              <small class="text-muted">{{repeat.name}}</small>
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

