<template>
  <dl v-if="variable" class="mt-3 row">
    <dt class="col-2 mb-3">name</dt>
    <dd class="col-10">
      {{ variable.name }}
    </dd>
    <dt class="col-2 mb-3">description</dt>
    <dd class="col-10">
      <template v-if="mapping.description">{{ mapping.description }}</template>
      <template v-else> - </template>
    </dd>

    <dt class="col-2 mb-3">harmonization status</dt>
    <dd class="col-10">
      {{ mapping.match.name }}
    </dd>

    <dt class="col-2 mb-3">variables used</dt>
    <dd class="col-10">
      <ul class="list-unstyled">
        <li
          v-for="fromVariable in mapping.fromVariable"
          :key="fromVariable.name"
        >
          {{ fromVariable.name }}
        </li>
      </ul>
    </dd>

    <dt class="col-2">syntax</dt>
    <dd class="col-10">
      <pre>{{ mapping.syntax }}</pre>
    </dd>

    <dt class="col-2">repeats</dt>
    <dd class="col-10">
      <dl class="row" v-for="repeat in repeats" :key="repeat.name">
        <dt class="col-2">{{ repeat.name }}</dt>
        <dd class="col-10">
          <span v-if="repeat.cohortMapping">{{
            repeat.cohortMapping.match.name
          }}</span>
          <span v-else>unmapped</span>
        </dd>
      </dl>
    </dd>
  </dl>
  <div class="mt-2" v-else><Spinner /> Fetching data..</div>
</template>

<script>
import { request } from "graphql-request";
import variableDetails from "../store/query/variableDetails.gql";
import { Spinner } from "@mswertz/emx2-styleguide";
export default {
  name: "ResourceHarmonizationDetails",
  components: { Spinner },
  props: {
    name: String,
    acronym: String,
  },
  data() {
    return {
      variable: null,
    };
  },
  methods: {
    async fetch(name) {
      const params = { filter: { name: { equals: name } } };
      const resp = await request("graphql", variableDetails, params).catch(
        (e) => console.error(e)
      );
      this.variable = resp.Variables[0];
    },
  },
  computed: {
    mapping() {
      return this.variable.mappings.find(
        (mapping) => mapping.fromTable.release.resource.acronym === this.acronym
      );
    },
    repeats() {
      if (!this.variable.repeats) {
        return [];
      }
      return this.variable.repeats.map((repeat) => {
        if (repeat.mappings) {
          repeat.cohortMapping = repeat.mappings.find(
            (mapping) =>
              mapping.fromTable.release.resource.acronym === this.acronym
          );
        }
        return repeat;
      });
    },
  },
  created() {
    this.fetch(this.name);
  },
};
</script>

<style></style>
