<template>
  <table class="table table-bordered table-sm table-">
    <thead>
      <th>Name</th>
      <th>Start</th>
      <th>End</th>
      <th>Age</th>
    </thead>
    <tbody>
      <tr v-for="e in collectionEvents" :key="e.name">
        <td>{{ e.name }}</td>
        <td>{{ e.startYear }}</td>
        <td>{{ e.endYear }}</td>
        <td>
          {{ e.ageMin ? e.ageMin.name : "" }} -
          {{ e.ageMax ? e.ageMax.name : "" }}
        </td>
      </tr>
    </tbody>
  </table>
</template>
<script>
export default {
  props: {
    collectionEvents: Array,
    color: String,
  },
};
</script>
