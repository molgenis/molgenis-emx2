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
import { fetchFromVariableDetails } from "../store/repository/variableRepository";
import { Spinner } from "@mswertz/emx2-styleguide";
import HarmonizationDefinition from "../components/HarmonizationDefinition.vue";
import { mapActions } from "vuex";

export default {
  name: "ResourceHarmonizationDetails",
  components: { Spinner, HarmonizationDefinition },
  props: {
    name: String,
    network: String,
    version: String,
    sourceCohort: String,
    variable: Object,
  },
  data() {
    return {
      fromVariables: null,
    };
  },
  computed: {
    repeats() {
      let repeats = [
        {
          ...this.variable,
          cohortMapping: this.variable.mappings.find(
            (mapping) =>
              mapping.fromTable.release.resource.acronym === this.sourceCohort
          ),
        },
      ];
      if (this.variable.repeats) {
        repeats = repeats.concat(
          this.variable.repeats.map((repeat) => {
            if (repeat.mappings) {
              repeat.cohortMapping = repeat.mappings.find(
                (mapping) =>
                  mapping.fromTable.release.resource.acronym ===
                  this.sourceCohort
              );
            }
            return repeat;
          })
        );
      }
      return repeats;
    },
  },
  methods: {
    ...mapActions(["fetchSchema"]),
  },
  async created() {
    if (this.variable.mappings) {
      const fromNames = this.variable.mappings
        .filter(
          (mapping) =>
            mapping.fromRelease.resource.acronym === this.sourceCohort
        )
        .flatMap((cohortMapping) =>
          cohortMapping.fromVariable
            ? cohortMapping.fromVariable.flatMap((fv) => fv.name)
            : []
        );
      const schema = await this.fetchSchema();
      const resp = await fetchFromVariableDetails(
        fromNames,
        schema,
        this.version
      );
      this.fromVariables = resp;
    }
  },
};
</script>

<style></style>
