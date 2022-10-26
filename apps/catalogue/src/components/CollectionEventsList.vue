<template>
  <div class="table-responsive">
    <table class="table table-bordered table-sm">
      <thead class="text-nowrap">
        <th>Name</th>
        <th>Description</th>
        <th>Number of participants</th>
        <th>Start</th>
        <th>End</th>
        <th>Subcohorts</th>
        <th>Data sources</th>
        <th>Sample sources</th>
        <th>Areas of information</th>
        <th>Age Groups</th>
      </thead>
      <tbody>
        <tr v-for="e in collectionEvents" :key="e.name">
          <td>{{ e.name }}</td>
          <td>{{ e.description ? e.description : "N/A" }}</td>
          <td>{{ e.numberOfParticipants }}</td>
          <td>
            <OntologyTerms :terms="[e.startYear]" />
          </td>
          <td>
            <OntologyTerms :terms="[e.endYear]" />
          </td>
          <td>
            <OntologyTerms :terms="e.subcohorts" />
          </td>
          <td>
            <OntologyTerms :terms="e.dataCategories" />
          </td>
          <td>
            <OntologyTerms :terms="e.sampleCategories" />
          </td>
          <td>
            <OntologyTerms :terms="e.areasOfInformation" />
          </td>
          <td>
            <OntologyTerms :terms="e.ageGroups" />
          </td>
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
    collectionEvents: Array,
    color: String,
  },
};
</script>
