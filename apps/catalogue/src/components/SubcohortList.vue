<template>
  <table class="table table-bordered table-sm table-">
    <thead>
      <th>Name</th>
      <th>noParticipants</th>
      <th>geographicRegion</th>
      <th>ageCategories</th>
      <th>Disease</th>
    </thead>
    <tbody>
      <tr v-for="c in subcohorts" :key="c.name">
        <td>{{ c.name }}</td>
        <td>{{ c.noParticipants }}</td>
        <td>
          <OntologyTerms :terms="c.geographicRegion" :color="color" />
        </td>
        <td>
          <OntologyTerms :terms="c.ageCategories" :color="color" />
        </td>
        <td>
          <OntologyTerms :terms="c.disease" :color="color" />
        </td>
      </tr>
    </tbody>
  </table>
</template>
<script>
import OntologyTerms from "./OntologyTerms";

export default {
  components: { OntologyTerms },
  props: {
    subcohorts: Array,
    color: String,
  },
};
</script>
