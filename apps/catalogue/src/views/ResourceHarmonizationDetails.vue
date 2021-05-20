<template>
  <dl v-if="details" class="mt-3 row">
    <dt class="col-2 mb-3">description</dt>
    <dd class="col-10">
      {{ details.description }}
    </dd>

    <dt class="col-2 mb-3">variables used</dt>
    <dd class="col-10">
      <ul class="list-unstyled">
        <li v-for="variable in details.fromVariable" :key="variable.name">
          {{ variable.name }}
        </li>
      </ul>
    </dd>

    <dt class="col-2">syntax</dt>
    <dd class="col-10">
      <pre>{{ details.syntax }}</pre>
    </dd>
  </dl>
</template>

<script>
import { mapGetters, mapActions } from "vuex";
export default {
  name: "ResourceHarmonizationDetails",
  props: {
    name: String,
    acronym: String,
  },
  computed: {
    ...mapGetters(["mappingDetailsByVariableAndMapping"]),
    details() {
      return this.mappingDetailsByVariableAndMapping(this.name, this.acronym);
    },
  },
  methods: {
    ...mapActions(["fetchVariableDetails", "fetchMappingDetails"]),
  },
  async created() {
    await this.fetchVariableDetails(this.name);
    this.fetchMappingDetails({ name: this.name, acronym: this.acronym });
  },
};
</script>

<style></style>
