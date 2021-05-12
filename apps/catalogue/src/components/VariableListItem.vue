<template>
  <button class="list-group-item list-group-item-action">
    <span
      class="text-capitalize mg-variable-header mg-list-group-item-header"
      @click="toggleShowDetail"
    >
      <strong>
        <i
          v-if="!showDetail"
          class="fa fa-caret-up mr-2 hover-rotate-clockwize"
        ></i>
        <i v-else class="fa fa-caret-down mr-2"></i>
        {{ variable.label }}
      </strong>
    </span>

    <p class="mt-3" v-if="showDetail">
      <template v-if="variableDetails">
        <dl class="row">
          <dt class="col-2">variable</dt>
          <dd class="col-10">
            {{ variableDetails.name }}
          </dd>
          <dt class="col-2">description</dt>
          <dd class="col-10">
            <span v-if="variableDetails.description">{{
              variableDetails.description
            }}</span>
            <span v-else>-</span>
          </dd>
          <dt class="col-2">unit</dt>
          <dd class="col-10">
            <span v-if="variableDetails.unit">{{
              variableDetails.unit.name
            }}</span>
            <span v-else>-</span>
          </dd>
          <dt class="col-2">format</dt>
          <dd class="col-10">
            <span v-if="variableDetails.format">{{
              variableDetails.format.name
            }}</span>
            <span v-else>-</span>
          </dd>
          <dt class="col-2">n repeats</dt>
          <dd class="col-10">
            <span v-if="variableDetails.repeats">{{
              variableDetails.repeats.length
            }}</span>
            <span v-else>none</span>
          </dd>
        </dl>
      </template>
    </p>
  </button>
</template>

<script>
export default {
  name: "VariableListItem",
  props: {
    variable: Object,
    variableDetails: Object,
  },
  data() {
    return {
      showDetail: false,
    };
  },
  methods: {
    toggleShowDetail() {
      if (!this.showDetail) {
        this.$emit("request-variable-detail", this.variable.name);
        this.showDetail = true;
      } else {
        this.showDetail = false;
      }
    },
  },
};
</script>

<style scoped>
.mg-variable-header:hover {
  cursor: pointer;
}
.mg-list-group-item-header {
  display: block;
  width: 100%;
  font-weight: bolder;
}
.list-group-item:hover .hover-rotate-clockwize {
  transform: rotate(90deg);
  transition: transform 0.2s;
}
</style>
