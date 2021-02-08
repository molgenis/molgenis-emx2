<template>
  <div>
    <h1>Databanks</h1>
    <p>Cohorts, registries, databases, and more...</p>
    <DatabankList />
  </div>
</template>

<script>
import DatabankList from "../components/DatabankList";

export default {
  components: {
    DatabankList,
  },
  props: {},
};
</script>
