<template>
  <button class="list-group-item list-group-item-action">
    <div
      class="text-capitalize mg-variable-header mg-list-group-item-header"
      @click="toggleShowDetail"
    >
      <i
        v-if="!showDetail"
        class="fa fa-caret-up mr-2 hover-rotate-clockwize"
      ></i>
      <i v-else class="fa fa-caret-down mr-2"></i>
      {{ variable.label }}
      <span class="mg-network-label">
        {{ variable.release.resource.acronym }}
      </span>
    </div>
    <p class="mt-3" v-if="showDetail">
      <router-link
        class="nav-link"
        :to="{
          name: 'singleVariableDetails',
          params: {
            network: variable.release.resource.acronym,
            version: variable.release.version,
            name: variable.name,
          },
        }"
        >view details
      </router-link>
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

          <template v-if="variableDetails.permittedValues">
            <dt class="col-2">permitted values</dt>
            <dd class="col-10">
              <ul class="list-inline">
                <li
                  class="list-inline-item"
                  v-for="(permittedValue, index) in permittedValuesByOrder"
                  :key="index"
                >
                  {{ permittedValue.label }} = {{ permittedValue.value }}
                </li>
              </ul>
            </dd>
          </template>

          <dt class="col-2">n repeats</dt>
          <dd class="col-10">
            <span v-if="variableDetails.repeats">{{
              variableDetails.repeats.length
            }}</span>
            <span v-else>none</span>
          </dd>

          <dt class="col-2">mapped by</dt>
          <dd class="col-10">
            <span v-if="variableDetails.mappings">
              <span
                v-for="mapping in variableDetails.mappings"
                :key="mapping.fromTable.release.resource.acronym"
              >
                {{ mapping.fromTable.release.resource.acronym }}
              </span>
            </span>
            <span v-else>none</span>
          </dd>
        </dl>
      </template>
      <template v-else> <Spinner class="mt-2" /> Fetching data.. </template>
    </p>
  </button>
</template>

<script>
import { Spinner } from "@mswertz/emx2-styleguide";
export default {
  name: "VariableListItem",
  components: { Spinner },
  props: {
    variable: Object,
    variableDetails: Object,
  },
  data() {
    return {
      showDetail: false,
    };
  },
  computed: {
    permittedValuesByOrder() {
      return this.variableDetails.permittedValues
        .map((pv) => pv) // clone to avoid prop mutation
        .sort((a, b) => a.order <= b.order);
    },
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
.mg-network-label {
  float: right;
}

.mg-variable-header:hover {
  cursor: pointer;
}
.mg-list-group-item-header {
  display: block;
  width: 100%;
}
.list-group-item:hover .hover-rotate-clockwize {
  transform: rotate(90deg);
  transition: transform 0.2s;
}

.list-group-item .nav-link {
  padding-left: 0;
}
</style>
