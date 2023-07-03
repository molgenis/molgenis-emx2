<template>
  <li
    class="list-group-item"
    :class="{ 'list-group-item-action': !showDetail }"
    @click.stop="toggleShowDetail"
  >
    <div class="text-capitalize mg-variable-header mg-list-group-item-header">
      <i
        v-if="!showDetail"
        class="fa fa-caret-up mr-2 hover-rotate-clockwize"
      ></i>
      <i v-else class="fa fa-caret-down mr-2"></i>
      {{ variable.name }} <i v-if="variable.label"> - {{ variable.label }}</i>
      <span class="mg-model-label">
        {{ variable.resource.id }}
      </span>
    </div>
    <p class="mt-3" v-if="showDetail">
      <template v-if="variable">
        <dl class="row">
          <dt class="col-2">variable</dt>
          <dd class="col-10">
            {{ variable.name }}
          </dd>

          <dt class="col-2">label</dt>
          <dd class="col-10">
            {{ variable.label }}
          </dd>

          <dt class="col-2">description</dt>
          <dd class="col-10">
            <span v-if="variable.description">{{ variable.description }}</span>
            <span v-else>-</span>
          </dd>

          <dt class="col-2">unit</dt>
          <dd class="col-10">
            <span v-if="variable.unit">{{ variable.unit.name }}</span>
            <span v-else>-</span>
          </dd>

          <dt class="col-2">format</dt>
          <dd class="col-10">
            <span v-if="variable.format">{{ variable.format.name }}</span>
            <span v-else>-</span>
          </dd>

          <template v-if="variable.permittedValues">
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
            <span v-if="variable.repeats">{{ variable.repeats.length }}</span>
            <span v-else>none</span>
          </dd>

          <dt class="col-2">mapped by</dt>
          <dd class="col-10">
            <span v-if="variable.mappings">
              {{ mappedByString }}
              <HarmonizationDefinition
                v-for="mapping in variable.mappings"
                :mapping="mapping"
                :variable="variable"
              />
            </span>
            <span v-else>none</span>
          </dd>
        </dl>
      </template>
    </p>
  </li>
</template>

<script>
import HarmonizationDefinition from "./HarmonizationDefinition.vue";

export default {
  name: "VariableListItem",
  components: {
    HarmonizationDefinition,
  },
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
      return this.variable.permittedValues
        .map((pv) => pv) // clone to avoid prop mutation
        .sort((a, b) => a.order <= b.order);
    },
    mappedByString() {
      return Object.values(this.variable.mappings)
        .map((mapping) => mapping.source.id)
        .join(", ");
    },
  },
  methods: {
    toggleShowDetail() {
      if (this.showDetail) {
        this.showDetail = false;
      } else {
        this.showDetail = true;
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
