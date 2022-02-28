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
    <div id="app">
      <h2>Select HPO root term</h2>
      <SearchAutoComplete :items= "allHpoTerms"
      @selectedHpoTerm="apiCall"
      >
      </SearchAutoComplete>

      <br/>
      <PatientSearch @geneOfPatient="geneToHpo">
      </PatientSearch>

    </div>
    <br/> <br/>
    <h3 v-if="loadingHpo">here are results from the HPO api call</h3>
    {{ hpoResults }}
    <br/> <br/>
    <h3 v-if="loadingHpo">here are results from the Gene api call</h3>
    {{ geneAssociates }}

  </div>
</template>

<script>
import {
  ButtonAction,
  MessageError,
  MessageSuccess,
  Spinner
} from "@mswertz/emx2-styleguide";
import SearchAutoComplete from "./SearchAutoComplete";
import PatientSearch from "./PatientSearch";
import hpoData from '../js/autoSearchData.js';

export default {
  components: {
    ButtonAction,
    SearchAutoComplete,
    PatientSearch,
    MessageError,
    MessageSuccess,
    Spinner
  },
  data() {
    return {
      count: 0,
      selected: 1234,
      hpoResults: null,
      geneEntered: '',
      geneAssociates: null,
      loadingHpo: false,
      allHpoTerms: hpoData
    };
  },
  methods: {
    plusOne() {
      this.count++;
    },
    async apiCall(hpoTerm) {
      let resultData = await fetch("https://hpo.jax.org/api/hpo/search/?q=" + hpoTerm)
        .then(response => response.json());
      this.loadingHpo = true;
      this.hpoResults = resultData['terms'];
      this.getHpoId();
    },
    async geneToHpo(geneOfPatient) {
      // HMGCL
      console.log("ik be nu hier")
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

    }
  },
};
</script>
