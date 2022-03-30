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
      let query = "{patients{identifier vcfdata{url}}}"
      let resultPatients = [];
      this.loading = true;
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
          let url = patients[i].vcfdata.url;

          fetch(url)
              .then(response => response.blob())
              .then(blob => {
                let a = document.createElement("a");
                a.href = window.URL.createObjectURL(blob);
                a.download = "vcfdata.vcf";
                document.body.appendChild(a);
                a.click();
                a.remove();
          });
        }
      }
    }
  }
}
</script>

<style scoped>
h2{
  text-align: center;
}
</style>