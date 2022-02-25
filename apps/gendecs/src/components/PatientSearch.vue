<template>
  <div>
    <h2>Enter patient number</h2>

    <InputInt v-model="patientId" placeholder="patient number" type="number"/>
    <ButtonOutline @click="fetchPatient">Submit</ButtonOutline>
<!--    <p>Patient number is: {{patientId}}</p>-->
    <br/>
    <h3 v-if="dataLoaded">Here is the patient Data:</h3>
    <div v-if="loading">loading...</div>
    <div v-else-if="graphqlError">Error: {{ graphqlError }}</div>
    <div v-else>{{ rows }}</div>

  </div>

</template>

<script>
import {request} from "graphql-request";
import {InputInt, ButtonOutline} from "@mswertz/emx2-styleguide";

export default {
  name: "PatientSearch",
  emits: "geneOfPatient",
  components: {
    InputInt,
    ButtonOutline
  },
  props: {
    id: Number
  },
  data() {
    return {
      rows: null,
      loading: false,
      graphqlError: null,
      patientId: 0,
      dataLoaded: false
    }
  },
  methods: {
    async fetchPatient() {
      let query = "{Patients{id gender birthdate genesymbol}}";
      let resultPatients = [];
      this.loading = true;
      //do query
      await request("graphql", query)
          .then((data) => {
            this.dataLoaded = true;
            this.rows = data["Patients"];
            resultPatients = data["Patients"];
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
      this.getCorrectPatient(resultPatients);
      // console.log(resultPatients[0]['gender']);
    },
    getCorrectPatient(patients) {
      for (let i = 0; i < patients.length; i++) {
        if(patients[i].id === parseInt(this.patientId)) {
          let geneOfPatient = patients[i].genesymbol;
          this.$emit('geneOfPatient', geneOfPatient);
          console.log(geneOfPatient);
        }
      }
      }
  }
}
</script>

<style scoped>

</style>