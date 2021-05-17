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
              :key="mapping.fromTable.release.acronym"
            >
              {{ mapping.fromTable.release.resource.acronym }}
            </span>
          </span>
          <span v-else>none</span>
        </dd>
      </dl>
    </template>
  </p>
</template>

<script>
export default {
  name: "VariableDetails",
  props: {
    variableDetails: Object,
  },
};
</script>
