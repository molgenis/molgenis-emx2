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
import { fetchDetails } from "../store/repository/variableRepository";
import { Spinner } from "@mswertz/emx2-styleguide";
import HarmonizationDefinition from "../components/HarmonizationDefinition.vue";

export default {
  name: "ResourceHarmonizationDetails",
  components: { Spinner, HarmonizationDefinition },
  props: {
    name: String,
    network: String,
    version: String,
    sourceCohort: String,
  },
  data() {
    return {
      variable: null,
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
  async created() {
    this.variable = await fetchDetails(this.name, this.network, this.version);
  },
};
</script>

<style></style>
