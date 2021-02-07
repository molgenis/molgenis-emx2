<template>
  <div>
    <h1>Tables</h1>
    <TableList
      :resourceAcronym="databankAcronym"
      :projectAcronym="projectAcronym"
    />
  </div>
</template>

<script>
import TableList from "../components/TableList";

export default {
  components: { TableList },
  props: {
    databankAcronym: String,
    projectAcronym: String,
  },
};
</script>
