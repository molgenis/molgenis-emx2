<template>
  <div>
    <h1>Welcome to GenDecS!</h1>
    <p>this page contains the prototype for GenDecS. Here you can enter a patient number together
      with a HPO root-term/phenotype. When entered This prototype will gather the patient data from
      the patient database. This data will be filtered on possible disease causing genes and
      added to a new database. Then the filtered genes will be matched with HPO terms. The then
      found terms will be checked if they match with the given root term/phenotype. This information
      is then reported back.
    </p>

    <SearchAutoComplete :items= "allHpoTerms"
                        @selectedHpoTerm="hpoTermToId" class="inputForm"
    ></SearchAutoComplete>
    <div v-if="loadingOwl">
      <Spinner/>
    </div>
    <div>
      <InputCheckbox
          label="Search for parents and children"
          v-model="searchAssociates"
          :options="['Search for parents and children']"
          description="check this box if you want to search for parents and children of
                      your HPO term"
      />
    </div>
    <br/>

    <PatientSearch class="inputForm"></PatientSearch>
    <br/>
    <p>Place the downloaded vcfdata file in data/gendecs. If finished please press this button</p>
    <ButtonOutline @click="vcfToHpo">Check</ButtonOutline>

    <MessageSuccess v-if="foundMatch">Match found! </MessageSuccess>
    <p v-if="foundMatch"> {{ selectedHpoTerm }} has a match with the following gene: {{ patientGene }}.
      Which was found in the test patient database</p>

  </div>
</template>

<script>
import {
  ButtonAction,
  MessageError,
  MessageSuccess,
  ButtonOutline,
  InputCheckbox,
  Spinner
} from "@mswertz/emx2-styleguide";
import SearchAutoComplete from "./SearchAutoComplete";
import PatientSearch from "./PatientSearch";
import hpoData from "../js/autoSearchData.js";

export default {
  components: {
    ButtonAction,
    SearchAutoComplete,
    PatientSearch,
    MessageError,
    MessageSuccess,
    ButtonOutline,
    InputCheckbox,
    Spinner
  },
  data() {
    return {
      hpoResults: null,
      geneAssociates: null,
      allHpoTerms: hpoData,
      selectedHpoTerm: null,
      foundMatch: false,
      hpoChildren: null,
      hpoParents: null,
      hpoId: null,
      searchAssociates: null,
      loadingOwl: false,
      patientGene: null
    };
  },
  methods: {
    async hpoTermToId(selectedHpoTerm) {
      /**
      * Function gets the Hpo term that is selected by the user as selectedHpoTerm.
      * This is then used to gather the ID of the term using an api call.
      * */
      this.loadingOwl = true;

      this.selectedHpoTerm = selectedHpoTerm;
      let resultData = await fetch("https://hpo.jax.org/api/hpo/search/?q=" + selectedHpoTerm)
        .then(response => response.json());
      this.hpoResults = resultData['terms'];
      this.hpoId = this.hpoResults[0].id;

      if(this.searchAssociates != null) {
        this.sendHpo(this.hpoId.replace(":", "_"));
      }
      this.loadingOwl = false;
    },
    checkIfMatch() {
      /**
      * Function that checks if the HPO terms that are associated with the patient gene has a match
      * with the entered HPO term by the user.
      * */
      if(this.geneAssociates.includes(this.selectedHpoTerm) ||
          this.hpoParents.includes(this.selectedHpoTerm) ||
          this.hpoChildren.includes(this.selectedHpoTerm)) {
        this.foundMatch = true;
      }
    },
    sendHpo(hpoId) {
      /**
      * Function that gets the HPO id of the entered HPO term. This id is sent to the backend.
      * The parents and children of this term are returned by the backend.
      * */
      let requestOptions = {
        method: 'POST',
        body: JSON.stringify({ hpoId: hpoId })
      };
      fetch('/patients/api/gendecs/queryHpo', requestOptions)
          .then(async response => {
            let data = await response.json();
            this.hpoParents = data["parents"];
            this.hpoChildren = data["children"];

            // check for error response
            if (!response.ok) {
              // get error message from body or default to response status
              const error = (data && data.message) || response.status;
              return Promise.reject(error);
            }
            this.postId = data.id;
          })
          .catch(error => {
            this.errorMessage = error;
            console.error('There was an error!', error);
          });
    },
    vcfToHpo() {
      let genes;
      fetch('/patients/api/gendecs/vcffile')
        .then(async response => {
          console.log(response.json());
            });
    }
  },
};
</script>

<style scoped>
/*.inputForm {*/
/*  float: left;*/
/*  padding: 50px;*/
/*}*/
</style>
