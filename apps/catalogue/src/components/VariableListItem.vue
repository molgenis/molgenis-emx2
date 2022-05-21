<template>
  <li
    class="list-group-item"
    :class="{ 'list-group-item-action': !showDetail }"
    @click.stop="toggleShowDetail(false)"
  >
    <div
      @click.stop="toggleShowDetail(true)"
      class="text-capitalize mg-variable-header mg-list-group-item-header"
    >
      <i
        v-if="!showDetail"
        class="fa fa-caret-up mr-2 hover-rotate-clockwize"
      ></i>
      <i v-else class="fa fa-caret-down mr-2"></i>
      {{ variable.label }}
      <span class="mg-model-label">
        {{ variable.dataDictionary.resource.pid }} ({{
          variable.dataDictionary.version
        }})
      </span>
    </div>
    <p class="mt-3" v-if="showDetail">
      <router-link
        v-if="network"
        class="nav-link"
        :to="{
          name: 'NetworkVariableDetailView',
          params: {
            name: variable.name,
            network: network,
          },
          query: {
            model: variable.dataDictionary.resource.pid,
            version: variable.dataDictionary.version,
          },
        }"
        >view details
      </router-link>
      <router-link
        v-else
        class="nav-link"
        :to="{
          name: 'VariableDetailView',
          params: {
            name: variable.name,
          },
          query: {
            model: variable.dataDictionary.resource.pid,
            version: variable.dataDictionary.version,
          },
        }"
        >view details
      </router-link>
      <template v-if="variable.variableDetails">
        <dl class="row">
          <dt class="col-2">variable</dt>
          <dd class="col-10">
            {{ variable.variableDetails.name }}
          </dd>

          <dt class="col-2">description</dt>
          <dd class="col-10">
            <span v-if="variable.variableDetails.description">{{
              variable.variableDetails.description
            }}</span>
            <span v-else>-</span>
          </dd>

          <dt class="col-2">unit</dt>
          <dd class="col-10">
            <span v-if="variable.variableDetails.unit">{{
              variable.variableDetails.unit.name
            }}</span>
            <span v-else>-</span>
          </dd>

          <dt class="col-2">format</dt>
          <dd class="col-10">
            <span v-if="variable.variableDetails.format">{{
              variable.variableDetails.format.name
            }}</span>
            <span v-else>-</span>
          </dd>

          <template v-if="variable.variableDetails.permittedValues">
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
            <span v-if="variable.variableDetails.repeats">{{
              variable.variableDetails.repeats.length
            }}</span>
            <span v-else>none</span>
          </dd>

          <dt class="col-2">mapped by</dt>
          <dd class="col-10">
            <span v-if="variable.variableDetails.mappings">
              <span
                v-for="mapping in variable.variableDetails.mappings"
                :key="mapping.fromTable.dataDictionary.resource.pid"
              >
                {{ mapping.fromTable.dataDictionary.resource.pid }}
              </span>
            </span>
            <span v-else>none</span>
          </dd>
        </dl>
      </template>
      <template v-else>
        <Spinner class="mt-2" />
        Fetching data..
      </template>
    </p>
  </li>
</template>

<script>
import { Spinner } from "@mswertz/emx2-styleguide";

export default {
  name: "VariableListItem",
  components: { Spinner },
  props: {
    variable: Object,
    network: String,
  },
  data() {
    return {
      showDetail: false,
    };
  },
  computed: {
    permittedValuesByOrder() {
      return this.variable.variableDetails.permittedValues
        .map((pv) => pv) // clone to avoid prop mutation
        .sort((a, b) => a.order <= b.order);
    },
  },
  methods: {
    toggleShowDetail(isHeaderClicked) {
      if (!this.showDetail) {
        this.$emit("request-variable-detail", this.variable.name);
        this.showDetail = true;
      } else if (isHeaderClicked) {
        this.showDetail = false;
      }
    },
  },
};
</script>

<style scoped>
.mg-model-label {
  float: right;
  font-variant: all-petite-caps;
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
