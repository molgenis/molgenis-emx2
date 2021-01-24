<template>
  <div>
    <h1>Collections</h1>
    <p>Cohorts, registries, databases, and more...</p>
    <CollectionList />
  </div>
</template>

<script>
import CollectionList from "../components/CollectionList";

export default {
  components: {
    CollectionList,
  },
  props: {},
};
</script>
