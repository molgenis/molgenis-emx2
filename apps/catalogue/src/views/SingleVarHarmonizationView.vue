<template>
  <div class="container-fluid pt-3">
    <ul v-if="details" class="nav nav-tabs">
      <li
        class="nav-item"
        v-for="mapping in details.mappings"
        :key="mapping.fromTable.release.resource.acronym"
      >
        <router-link
          class="nav-link"
          :to="{
            name: 'resourceHarmonizationDetails',
            params: {
              name,
              acronym: mapping.fromTable.release.resource.acronym,
            },
          }"
        >
          {{ mapping.fromTable.release.resource.acronym }}
        </router-link>
      </li>
    </ul>
    <router-view :key="$route.fullPath"></router-view>
  </div>
</template>

<script>
import { mapGetters, mapActions } from "vuex";
export default {
  name: "SingleVarHarmonizationView",
  props: {
    name: String,
  },
  computed: {
    ...mapGetters(["variableDetails"]),
    details() {
      return this.variableDetails[this.name];
    },
  },
  methods: {
    ...mapActions(["fetchVariableDetails"]),
  },
  async created() {
    await this.fetchVariableDetails(this.name);
    // initialy select the first mapping
    if (!this.$route.params.acronym) {
      this.$router.push({
        name: "resourceHarmonizationDetails",
        params: {
          name: this.name,
          acronym: Object.keys(this.details.mappings)[0],
        },
      });
    }
  },
};
</script>

<style></style>
