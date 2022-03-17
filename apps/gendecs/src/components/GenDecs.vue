<template>
  <div id="wrapper">
    <div id="titlediv">
      <h1>Welcome to GenDecS!</h1>
      <p>this page contains the prototype for GenDecS. Here you can enter a patient number together
        with a HPO root-term/phenotype. When entered This prototype will gather the patient data from
        the patient database. This data will be filtered on possible disease causing genes and
        added to a new database. Then the filtered genes will be matched with HPO terms. The then
        found terms will be checked if they match with the given root term/phenotype. This information
        is then reported back.
      </p>
    </div>
    <div id="searchdiv">
      <SearchAutoComplete :items= "allHpoTerms"
                          @selectedHpoTerm="addHpoResult" class="inputForm"
      ></SearchAutoComplete>

      <InputCheckbox
            label="Search for parents and children"
            v-model="searchAssociates"
            :options="['Search for parents and children']"
            description="check this box if you want to search for parents and children of
                        your HPO term"
        />
    </div>
    <div id="patientdiv">
      <PatientSearch class="inputForm"></PatientSearch>
    </div>

    <div id="bottemdiv">
      <p>Place the downloaded vcfdata file in data/gendecs. If finished please press this button</p>

      <ButtonOutline @click="main">Search for matches</ButtonOutline>
      <div class="results" v-if="loading">
        <Spinner/>
      </div>
      <div class="results" v-else>
        <MessageSuccess v-if="foundMatch">Match found! </MessageSuccess>
        <p v-if="foundMatch"> {{ selectedHpoTerm }} has a match with the following gene(s): {{ patientGenes }}.
          Which was found in the patient vcf data</p>
      </div>
    </div>

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
      geneAssociates: null,
      allHpoTerms: hpoData,
      selectedHpoTerm: null,
      foundMatch: false,
      hpoChildren: null,
      hpoParents: null,
      hpoId: null,
      searchAssociates: null,
      patientGenes: null,
      genesHpo: null,
      loading: false
    };
  },
  methods: {
    addHpoResult(selectedHpoterm) {
      this.selectedHpoTerm = selectedHpoterm;
    },
    async hpoTermToId() {
      /**
      * Function gets the Hpo term that is selected by the user as selectedHpoTerm.
      * This is then used to gather the ID of the term using an api call.
      * */

      let resultData = await fetch("https://hpo.jax.org/api/hpo/search/?q=" + this.selectedHpoTerm)
        .then(response => response.json());
      let hpoResults = resultData['terms'];
      this.hpoId = hpoResults[0].id;

      if(this.searchAssociates != null) {
        this.sendHpo(this.hpoId.replace(":", "_"));
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
    async vcfToHpo() {
      /**
       * Function that sends an api call to the backend to parse the vcf data.
       * Respsonse: genes with their hpo term. Adds the response to this.genesHpo
       * example: {"ODAD2":"Female infertility","HPSE2":"Urinary incontinence"}
       */
      this.genesHpo = await fetch('/patients/api/gendecs/vcffile')
            .then(response => response.json());
      this.searchForMatch();
    },
    searchForMatch() {
      /**
       * Function that searches for a match between this.genesHpo and the selected HPO term
       * and its children and parents.
       */
      let keys = Object.keys(this.genesHpo);
      let matchGene = [];

      console.log(this.selectedHpoTerm);
      keys.forEach((key) => {
        //Female infertility
        if(this.genesHpo[key] === this.selectedHpoTerm ||
            this.genesHpo[key] === this.hpoChildren ||
            this.genesHpo[key] === this.hpoParents) {
          matchGene.push(this.getKeyByValue(this.genesHpo, this.genesHpo[key]));
        }
      });
      if(matchGene.length !== 0) {
        this.foundMatch = true;
        this.patientGenes = matchGene;
      }
    },
    getKeyByValue(object, value) {
      return Object.keys(object).find(key => object[key] === value);
    },
    async main() {
      this.loading = true;
      await this.hpoTermToId();
      await this.vcfToHpo();
      this.loading = false;
    }
  },
};
</script>

<style scoped>
#wrapper {
  overflow: hidden; /* add this to contain floated children */
}
#searchdiv {
  width: 50%;
  float:left;
  padding: 10px;
}
#patientdiv {
  width: 50%;
  float: left;
  padding: 10px;
}
#bottemdiv {
  width: 100%;
  float:left;
  padding: 10px;
  text-align: center;
}
#titlediv {
  width: 100%;
  float: left;
  padding: 10px;
}
h1{
  text-align: center;
}
.results {
  padding: 10px;
}
</style>