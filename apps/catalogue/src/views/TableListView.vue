<template>
  <div>
    <h1>Tables</h1>
    <TableList
      :collectionAcronym="collectionAcronym"
      :consortiumAcronym="consortiumAcronym"
    />
  </div>
</template>

<script>
import TableList from "../components/TableList";

export default {
  components: { TableList },
  props: {
    databankAcronym: String,
    consortiumAcronym: String,
  },
};
</script>
