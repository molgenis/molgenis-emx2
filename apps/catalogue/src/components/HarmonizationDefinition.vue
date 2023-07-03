<template>
  <div class="card mb-1">
    <div class="card-body">
      <dl class="mt-3 row">
        <dt class="col-2 mb-3">source</dt>
        <dd class="col-10">
          <template v-if="mapping.source">
            {{ mapping.source.id }}
          </template>
          <template v-else> -</template>
        </dd>
        <dt class="col-2 mb-3">variables used</dt>
        <dd class="col-10">
          <ul class="list-unstyled" v-if="mapping">
            <li
              v-for="sourceVariable in mapping.sourceVariables"
              :key="sourceVariable.name"
            >
              {{ mapping.sourceDataset.name }}.{{ sourceVariable.name }}
            </li>
            <li
              v-for="sourceVariable in mapping.sourceVariablesOtherDatasets"
              :key="sourceVariable.name"
            >
              {{ sourceVariable.dataset.name }}.{{ sourceVariable.name }}
            </li>
          </ul>
          <span v-else> - </span>
        </dd>

        <dt class="col-2 mb-3">harmonization status</dt>
        <dd class="col-10">
          <template v-if="mapping">
            {{ mapping.match.name }}
          </template>
          <template v-else> -</template>
        </dd>

        <dt class="col-2 mb-3">description</dt>
        <dd class="col-10">
          <template v-if="mapping.description">
            {{ mapping.description }}
          </template>
          <template v-else> -</template>
        </dd>
        <dt class="col-2">syntax</dt>
        <dd class="col-10">
          <pre v-if="mapping">{{ mapping.syntax }}</pre>
          <span v-else> - </span>
        </dd>
      </dl>
    </div>
  </div>
</template>

<script>
export default {
  name: "HarmonizationDefinition",
  props: {
    variable: Object,
    mapping: Object,
  },
};
</script>
