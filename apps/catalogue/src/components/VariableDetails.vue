<template>
  <p class="mt-3">
    <template v-if="variableDetails">
      <dl class="row">
        <dt class="col-2">name</dt>
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

        <template v-if="showMappedBy && !variableDetails.repeats">
          <dt class="col-2">mapped by</dt>
          <dd class="col-10">
            <span v-if="variableDetails.mappings">
              <span v-for="cohort in mappedByCohorts" :key="cohort">
                {{ cohort }}
              </span>
            </span>
            <span v-else>none</span>
          </dd>
        </template>
      </dl>
    </template>
  </p>
</template>

<script>
export default {
  name: "VariableDetails",
  props: {
    variableDetails: Object,
    showMappedBy: {
      type: Boolean,
      default: () => true,
    },
  },
  computed: {
    permittedValuesByOrder() {
      return this.variableDetails.permittedValues
        .map((pv) => pv) // clone to avoid prop mutation
        .sort((a, b) => a.order <= b.order);
    },
    mappedByCohorts() {
      //order alphabetically
      return this.variableDetails.mappings
        .map((mapping) => mapping.fromTable.dataDictionary.resource.pid)
        .sort();
    },
  },
};
</script>
