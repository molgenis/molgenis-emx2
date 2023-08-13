<template>
  <CohortCards table="Cohorts" :network="network" :orderBy="{ id: 'ASC' }" />
</template>

<script>
import CohortCards from "../components/CohortCards.vue";

export default {
  components: {
    CohortCards,
  },
  props: {
    network: String,
  },
};
</script>
