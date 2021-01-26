<template>
  <div>
    <h1>Datasets</h1>
    <DatasetList
      :collectionAcronym="collectionAcronym"
      :networkAcronym="networkAcronym"
    />
  </div>
</template>

<script>
import DatasetList from "../components/DatasetList";

export default {
  components: { DatasetList },
  props: {
    collectionAcronym: String,
    networkAcronym: String,
  },
};
</script>
