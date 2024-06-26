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
      {{ variable.name }} <i v-if="variable.label"> - {{ variable.label }}</i>
      <span class="mg-model-label">
        {{ variable.resource.id }}
      </span>
    </div>
    <div class="mt-3" v-if="showDetail">
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
          <span v-if="mappedByString">
            {{ mappedByString }}
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
                  model: variable.resource.id,
                },
              }"
            >
              view details
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
                  model: variable.resource.id,
                },
              }"
            >
              view details
            </router-link>
          </span>
          <span v-else>none</span>
        </dd>
      </dl>
    </div>
  </li>
</template>

<script>
import { Spinner } from "molgenis-components";

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
      return this.variable.permittedValues
        .map((pv) => pv) // clone to avoid prop mutation
        .sort((a, b) => a.order <= b.order);
    },
    mappedByString() {
      const mappings = this.variable.mappings
        ? Object.values(this.variable.mappings).map(
            (mapping) => mapping.sourceDataset.resource.id
          )
        : [];
      if (this.variable.repeats) {
        Object.values(this.variable.repeats).forEach((repeat) => {
          if (repeat.mappings) {
            mappings.push(
              ...Object.values(repeat.mappings).map(
                (mapping) => mapping.sourceDataset.resource.id
              )
            );
          }
        });
      }
      return [...new Set(mappings)].join(", ");
    },
  },
  methods: {
    toggleShowDetail(isHeaderClicked) {
      if (!this.showDetail) {
        this.showDetail = true;
      } else {
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
