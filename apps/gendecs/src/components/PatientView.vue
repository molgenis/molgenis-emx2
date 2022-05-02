<template>
  <div id="wrapper">
    <h3>selected patient:</h3>
    <div id="left-div">
      <TableSimple
          v-model="selectedItems"
          :columns="['identifier','gender','birthdate', 'vcfdata']"
          :rows="[{'identifier':this.patientId,'gender':this.gender, 'birthdate': this.birthdate, 'vcfdata': this.vcfdata}]"
      >
      </TableSimple>
    </div>

    <div id="right-div">
      <p>
        If you want to perform a search in the genetic data press this button:
      </p>
      <ButtonOutline @click="goToGendecs">
        Go to Genomics viewer of GenDecS
      </ButtonOutline>
    </div>

  </div>

</template>

<script>
import {
  TableSimple,
  ButtonOutline
} from "@mswertz/emx2-styleguide";
import request from "graphql-request";

export default {
  name: "PatientView",
  components : {
    TableSimple,
    ButtonOutline
  },
  async created() {
    this.patientId = this.$route.params.id.toString();
    await this.fetchPatient();
  },
  data() {
    return {
      selectedItems: null,
      patientId: null,
      birthdate: null,
      gender: null,
      vcfdata: null,
    }
  },
  methods: {
    async fetchPatient() {
      let query = "{Patients{identifier gender birthdate vcfdata}}"
      // let newQuery = `{
      //     Patients(gender: "male") {
      //       identifier
      //       gender
      //       birthdate
      //       vcfdata
      //       }
      //     }`
      let resultPatients = [];
      await request("graphql", query)
          .then((data) => {
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
          this.gender = patients[i].gender;
          this.birthdate = patients[i].birthdate;
          this.vcfdata = patients[i].vcfdata;
        }
      }
    },
    goToGendecs() {
      this.$router.push({path: '/' + this.vcfdata + '/patientView/genomicsViewer'})
    }
  }
}
</script>

<style scoped>
#wrapper {
  width: 100%;
  overflow: hidden;
}

#left-div {
  width: 50%;
  float: left;
}

#right-div {
  width: 50%;
  overflow: hidden;
  padding: 10px;
}
</style>