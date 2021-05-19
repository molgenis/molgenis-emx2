<template>
  <dl v-if="details" class="mt-3 row">
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
    ...mapActions(["fetchMappingDetails"]),
  },
  created() {
    this.fetchMappingDetails({ name: this.name, acronym: this.acronym });
  },
};
</script>

<style></style>
