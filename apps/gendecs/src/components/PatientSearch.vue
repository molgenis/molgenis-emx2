<template>
  <div>
    <h2>Enter patient number</h2>

    <input v-model="patientId" placeholder="patient number" type="number"/>
    <input type="submit" @click="fetchPatient">
    <p>Patient number is: {{patientId}}</p>
    <br/>
    <div v-if="loading">loading...</div>
    <div v-else-if="graphqlError">Error: {{ graphqlError }}</div>
    <div v-else>result: {{ rows }}</div>
  </div>

</template>

<script>
import {request} from "graphql-request";

export default {
  name: "PatientSearch",
  props: {
    id: Number
  },
  data() {
    return {
      rows: Array,
      loading: false,
      graphqlError: null,
      patientId: 0
    }
  },
  methods: {
    fetchPatient() {
      // let query = "{Patients(id: " + this.patientId + "){gender}}";
      let query = "{Patients{identifier gender}}"
      // let query = '{Patients(search : male) {' +
      //     'identifier gender}' +
      //     '}'

      console.log(query);
      this.loading = true;
      //do query
      request("graphql", query)
          .then((data) => {
            console.log(data);
            this.rows = data["Patients"];

            this.loading = false;
          })
          .catch((error) => {
            if (Array.isArray(error.response.errors)) {
              this.graphqlError = error.response.errors[0].message;
            } else {
              this.graphqlError = error;
            }
            this.loading = false;
          });
    }
  }
}
</script>

<style scoped>

</style>