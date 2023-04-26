<template>
  <div class="table-responsive">
    <table class="table table-bordered table-sm overflow-auto">
      <thead>
        <th>Name</th>
        <th>Description</th>
        <th>Number of participants</th>
        <th>Countries</th>
        <th>Regions</th>
        <th>Age groups</th>
        <th>Disease</th>
        <th>Other inclusion criteria</th>
      </thead>
      <tbody>
        <tr v-for="c in subcohorts" :key="c.name">
          <td>{{ c.name }}</td>
          <td>{{ c.description ? c.description : "N/A" }}</td>
          <td>{{ c.numberOfParticipants }}</td>
          <td>
            <OntologyTerms :terms="c.countries" :color="color" />
          </td>
          <td>
            <OntologyTerms :terms="c.regions" :color="color" />
          </td>
          <td>
            <OntologyTerms :terms="c.ageGroups" :color="color" />
          </td>
          <td>
            <OntologyTerms :terms="c.mainMedicalCondition" :color="color" />
          </td>
          <td>{{ c.inclusionCriteria ? c.inclusionCriteria : "N/A" }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
<script>
import OntologyTerms from "./OntologyTerms.vue";

export default {
  components: { OntologyTerms },
  props: {
    subcohorts: Array,
    color: String,
  },
};
</script>
