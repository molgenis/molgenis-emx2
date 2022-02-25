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
      <SearchAutoComplete :items="[
        'Abnormal cellular phenotype',
        'Abnormality of blood and blood-forming tissues',
        'Abnormality of head or neck',
        'Abnormality of limbs',
        'Abnormality of metabolism/homeostasis',
        'Abnormality of prenatal development or birth',
        'Abnormality of the breast',
        'Abnormality of the cardiovascular system',
        'Abnormality of the digestive system',
        'Abnormality of the ear',
        'Abnormality of the endocrine system',
        'Abnormality of the eye',
        'Abnormality of the genitourinary system',
        'Abnormality of the immune system',
        'Abnormality of the integument',
        'Abnormality of the musculoskeletal system',
        'Abnormality of the nervous system',
        'Abnormality of the respiratory system',
        'Abnormality of the thoracic cavity',
        'Abnormality of the voice',
        'Constitutional symptom',
        'Growth abnormality',
        'Neoplasm'
      ]"
      @selectedHpoTerm="apiCall"
      >
      </SearchAutoComplete>

    </div>
    <div>
      <br/>
      <h2>Enter gene</h2>
      <form>
      <input v-model="geneEntered" type="text">
      <input type="submit" @click="geneToHpo">
      </form>
        {{ geneEntered }}
    </div>

    <div>
      <br/>
      <PatientSearch>
      </PatientSearch>
    </div>

    <h3>here are results from the HPO api call</h3>
    {{ hpoResults }}
    <br/> <br/>
    <h3>here are results from the Gene api call</h3>
    {{ geneAssociates }}

  </div>
</template>

<script>
import { ButtonAction } from "@mswertz/emx2-styleguide";
import SearchAutoComplete from "./SearchAutoComplete";
import PatientSearch from "./PatientSearch";

export default {
  components: {
    ButtonAction,
    SearchAutoComplete,
    PatientSearch
  },
  data() {
    return {
      count: 0,
      selected: 1234,
      hpoResults: {},
      geneEntered: '',
      geneAssociates: {}

    };
  },
  methods: {
    plusOne() {
      this.count++;
    },
    async apiCall(hpoTerm) {
      let resultData = await fetch("https://hpo.jax.org/api/hpo/search/?q=" + hpoTerm)
        .then(response => response.json());
      this.hpoResults = resultData['terms'];
      this.getHpoId();
    },
    async geneToHpo() {
      // HMGCL
      let resultData = await fetch("https://hpo.jax.org/api/hpo/search/?q=" + this.geneEntered)
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
      console.log(hpoChildrenName);

    }
  },
};
</script>
