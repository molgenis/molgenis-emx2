<template>
  <div class="card mb-1">
    <div class="card-body">
      <h5 class="card-title">{{ variable.name }}</h5>
      <dl class="mt-3 row">
        <dt class="col-2 mb-3">description</dt>
        <dd class="col-10">
          <template
            v-if="variable.cohortMapping && variable.cohortMapping.description"
          >
            {{ variable.cohortMapping.description }}
          </template>
          <template v-else> -</template>
        </dd>

        <dt class="col-2 mb-3">harmonization status</dt>
        <dd class="col-10">
          <template v-if="variable.cohortMapping">
            {{ variable.cohortMapping.match.name }}
          </template>
          <template v-else> -</template>
        </dd>

        <dt class="col-2 mb-3">variables used</dt>
        <dd class="col-10">
          <ul class="list-unstyled" v-if="variable.cohortMapping">
            <li
              v-for="fromVariable in variable.cohortMapping.fromVariable"
              :key="fromVariable.name"
            >
              <router-link
                :to="{
                  name: 'VariableDetailView',
                  query: { ...$route.query, fromName: fromVariable.name },
                }"
              >
                {{ variable.cohortMapping.fromTable.name }}.{{
                  fromVariable.name
                }}
              </router-link>
            </li>
            <li
              v-for="fromVariable in variable.cohortMapping
                .fromVariablesOtherTables"
              :key="fromVariable.name"
            >
              <router-link
                :to="{
                  name: 'VariableDetailView',
                  query: {
                    ...$route.query,
                    fromTable: fromVariable.table.name,
                    fromName: fromVariable.name,
                  },
                }"
              >
                {{ fromVariable.table.name }}.{{ fromVariable.name }}
              </router-link>
            </li>
          </ul>
          <span v-else> - </span>
        </dd>

        <dt class="col-2">syntax</dt>
        <dd class="col-10">
          <pre v-if="variable.cohortMapping">{{
            variable.cohortMapping.syntax
          }}</pre>
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
  },
};
</script>
