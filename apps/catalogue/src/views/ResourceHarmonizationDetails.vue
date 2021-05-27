<template>
  <div v-if="variable" class="mt-2">
    <harmonization-definition
      v-for="repeatedVariable in repeats"
      :key="repeatedVariable.name"
      :variable="repeatedVariable"
    />
  </div>
  <div class="mt-2" v-else><Spinner /> Fetching data..</div>
</template>

<script>
import { request } from "graphql-request";
import variableDetails from "../store/query/variableDetails.gql";
import { Spinner } from "@mswertz/emx2-styleguide";
import HarmonizationDefinition from "../components/HarmonizationDefinition.vue";

export default {
  name: "ResourceHarmonizationDetails",
  components: { Spinner, HarmonizationDefinition },
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
      const resp = await request(
        "graphql",
        variableDetails,
        params
      ).catch((e) => console.error(e));
      this.variable = resp.Variables[0];
    },
  },
  computed: {
    repeats() {
      let repeats = [
        {
          ...this.variable,
          cohortMapping: this.variable.mappings.find(
            (mapping) =>
              mapping.fromTable.release.resource.acronym === this.acronym
          ),
        },
      ];
      if (this.variable.repeats) {
        repeats = repeats.concat(
          this.variable.repeats.map((repeat) => {
            if (repeat.mappings) {
              repeat.cohortMapping = repeat.mappings.find(
                (mapping) =>
                  mapping.fromTable.release.resource.acronym === this.acronym
              );
            }
            return repeat;
          })
        );
      }
      return repeats;
    },
  },
  created() {
    this.fetch(this.name);
  },
};
</script>

<style></style>
