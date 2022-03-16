<template>
  <div>
    <h2>Enter patient number</h2>

    <InputInt v-model="patientId" placeholder="patient number" type="number"/>
    <ButtonOutline @click="fetchPatient">Submit</ButtonOutline>

  </div>

</template>

<script>
import {request} from "graphql-request";
import {
  InputInt,
  ButtonOutline,
} from "@mswertz/emx2-styleguide";

export default {
  name: "PatientSearch",
  emits: "geneOfPatient",
  components: {
    InputInt,
    ButtonOutline,
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
    }
  },
  methods: {
    async fetchPatient() {
      let query1 = "filter: {patients: {identifier: { eq: '1' } }  " +
          "} {patients{identifier vcfdata {url}}";
      // let query = "{patients (filter: {identifier: {eq :\"1\"}}) {identifier genesymbol vcfdata {url} }}";
      let query = "{patients{identifier gender birthdate genesymbol}}
      let resultPatients = [];
      this.loading = true;
      //do query
      await request("graphql", query)
          .then((data) => {
            this.dataLoaded = true;
            this.rows = data["patients"];
            resultPatients = data["patients"];
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
    },
    getCorrectPatient(patients) {
      for (let i = 0; i < patients.length; i++) {
        if(patients[i].identifier === parseInt(this.patientId)) {
          // let geneOfPatient = patients[i].genesymbol;
          // this.$emit('geneOfPatient', geneOfPatient);
          // /:schema/api/file/:table/:column/:id
          fetch('/GenDecS/api/file/patients/vcfdata/97abb0c67d884a63896d7874615012d1')
            .then(response => response.text())
            .then(textString => {
              let rows = textString.split('\n');
              console.log(rows);
              // .then(response => response.blob())
              // .then(blob => {
              //   let a = document.createElement("a");
              //   a.href = window.URL.createObjectURL(blob);
              //   a.download = "vcfdata.vcf";
              //   document.body.appendChild(a);
              //   a.click();
              //   a.remove();
          });
        }
      }
    }
  }
}
</script>

<style scoped>

</style>