<template>
  <div>
    <nav aria-label="breadcrumb">
      <ol class="breadcrumb">
        <li class="breadcrumb-item">
          <router-link :to="{ name: 'variableDetails' }">
            Variables
          </router-link>
        </li>
        <li class="breadcrumb-item active" aria-current="page">{{ name }}</li>
      </ol>
    </nav>
    <h3 v-if="details">{{ details.label }}</h3>
    <ul class="nav nav-tabs">
      <li class="nav-item">
        <router-link class="nav-link" :to="{ name: 'singleVariableDetails' }">
          Details
        </router-link>
      </li>
      <li class="nav-item">
        <router-link
          class="nav-link"
          :to="{ name: 'singleVariableHarmonization' }"
        >
          Harmonization
        </router-link>
      </li>
    </ul>
    <router-view></router-view>
  </div>
</template>

<script>
import { mapGetters, mapActions } from "vuex";
export default {
  name: "VariableDetailView",
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
  created() {
    this.fetchVariableDetails(this.name);
  },
};
</script>

<style scoped>
nav {
  margin-top: -1rem;
  margin-left: -2rem;
  margin-right: -2rem;
}
</style>
