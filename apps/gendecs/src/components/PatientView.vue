<template>
  <div>
    <h3>selected patient:</h3>
    <TableSimple
        v-model="selectedItems"
        :defaultValue="['Duck']"
        :columns="['identifier','gender','birthdate', 'vcfdata']"
        :rows="[{'identifier':this.identifier,'gender':this.gender, 'birthdate': this.birthdate, 'vcfdata': this.vcfdata}]"
        @click="click"
    >
      <template v-slot:rowheader="slotProps">
        my row with props {{ JSON.stringify(slotProps) }}
      </template>
    </TableSimple>

    <router-link :to="{path: '/' + this.patientId + '/patientView/genomicsViewer'}">go to GenDecS genomics viewer</router-link>

  </div>

</template>

<script>
import {
  TableSimple,
} from "@mswertz/emx2-styleguide";
import request from "graphql-request";

export default {
  name: "PatientView",
  components : {
    TableSimple,
  },
  created() {
    this.patientId = this.$route.params.id.toString();
    console.log(this.$route.params.id.toString());
    this.fetchPatient();
  },
  data() {
    return {
      selectedItems: null,
      rows: null,
      patientId: null,
      identifier: null,
      birthdate: null,
      gender: null,
      vcfdata: null,
    }
  },
  methods: {
    click(value) {
      alert("click " + JSON.stringify(value));
    },
    async fetchPatient() {
      let query = "{Patients{identifier gender birthdate}}"
      let resultPatients = [];
      await request("graphql", query)
          .then((data) => {
            this.rows = data["Patients"];
            resultPatients = data["Patients"];
          })
          .catch((error) => {
            if (Array.isArray(error.response.errors)) {
              this.graphqlError = error.response.errors[0].message;
            } else {
              this.graphqlError = error;
            }
          });
      this.getCorrectPatient(resultPatients);
    },
    getCorrectPatient(patients) {
      for (let i = 0; i < patients.length; i++) {
        if(patients[i].identifier === parseInt(this.patientId)) {
          this.identifier = this.patientId;
          this.gender = patients[i].gender;
          this.birthdate = patients[i].birthdate;
          this.vcfdata = patients[i].vcfdata;
        }
      }
    }
  }
}
</script>

<style scoped>

</style>