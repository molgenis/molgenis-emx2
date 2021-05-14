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
    <variable-details v-if="details" :variableDetails="details" />
  </div>
</template>

<script>
import { mapGetters, mapActions } from "vuex";
import VariableDetails from "../components/VariableDetails.vue";
export default {
  name: "VariableDetailView",
  components: { VariableDetails },
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
