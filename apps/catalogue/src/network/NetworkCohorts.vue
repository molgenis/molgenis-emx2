<template>
  <CohortCards table="Cohorts" :network="network" :orderBy="{ pid: 'ASC' }" />
</template>

<script>
import CohortCards from "../components/CohortCards";

export default {
  components: {
    CohortCards,
  },
  props: {
    network: String,
  },
};
</script>
