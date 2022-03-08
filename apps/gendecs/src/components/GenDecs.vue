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
    <MessageSuccess v-if="foundMatch">Match found!</MessageSuccess>

    <SearchAutoComplete :items= "allHpoTerms"
      @selectedHpoTerm="apiCall" class="inputForm"
    ></SearchAutoComplete>

    <br/>
    <PatientSearch @geneOfPatient="geneToHpo" class="inputForm">
    </PatientSearch>

    <br/>
    <h3 v-if="loadingHpo">here are results from the HPO api call</h3>
    {{ hpoResults }}
    <br/> <br/>
    <h3 v-if="loadingGeneAssociates">here are results from the Gene api call</h3>
    {{ geneAssociates }}

  </div>
</template>

<script>
import {
  ButtonAction,
  MessageError,
  MessageSuccess,
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
  },
  data() {
    return {
      hpoResults: null,
      geneEntered: '',
      geneAssociates: null,
      loadingHpo: false,
      loadingGeneAssociates: false,
      allHpoTerms: hpoData,
      selectedHpoTerm: null,
      foundMatch: false
    };
  },
  methods: { //2 abnormality of the radius
    async apiCall(selectedHpoTerm) {
      this.selectedHpoTerm = selectedHpoTerm;
      let resultData = await fetch("https://hpo.jax.org/api/hpo/search/?q=" + selectedHpoTerm)
        .then(response => response.json());
      this.loadingHpo = true;
      this.hpoResults = resultData['terms'];
      this.getHpoId();
    },
    async geneToHpo(geneOfPatient) {
      let resultData = await fetch("https://hpo.jax.org/api/hpo/search/?q=" + geneOfPatient)
        .then(response => response.json());
      let entrezId = resultData['genes'][0].entrezGeneId;

      let resultData2 = await fetch("https://hpo.jax.org/api/hpo/gene/" + entrezId)
        .then(response => response.json());
      let geneTermAssoc = [];
      for (let i = 0; i < resultData2['termAssoc'].length; i++) {
        geneTermAssoc.push(resultData2['termAssoc'][i].name);
      }
      this.geneAssociates = geneTermAssoc;
      this.loadingGeneAssociates = true;
      this.checkIfMatch();
    },
    getHpoId() {
      let id = this.hpoResults[0].id;
      this.getChildren(id);
    },
    async getChildren(id) {
      let resultData = await fetch("https://www.ebi.ac.uk/ols/api/ontologies/hp/children?id=" + id, {
      })
        .then(response => response.json());

      let hpoChildren = resultData['_embedded']['terms'];
      let hpoChildrenName = [];
      for (let i = 0; i < hpoChildren.length; i++) {
        hpoChildrenName.push(hpoChildren[i].label);
      }
      console.log("Children of " + this.hpoResults[0].name + ": " + hpoChildrenName);
    },
    checkIfMatch() {
      if(this.geneAssociates.includes(this.selectedHpoTerm)) {
        this.foundMatch = true;
      }
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
