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
      <br/> <br/>
      <h2>Enter patient number</h2>
      <input v-model="patientNumber" placeholder="patient number "/>
      <p>Patient number is: {{patientNumber}}</p>
      <br/>
      {{ hpoResults }}
    </div>
  </div>
</template>

<script>
import { ButtonAction } from "@mswertz/emx2-styleguide";
import SearchAutoComplete from "./SearchAutoComplete";

export default {
  components: {
    ButtonAction,
    SearchAutoComplete
  },
  data() {
    return {
      count: 0,
      patientNumber: 0,
      selected: 1234,
      hpoResults: {}
    };
  },
  methods: {
    plusOne() {
      this.count++;
    },
    async apiCall(hpoTerm) {
      console.log("in api call");

      let resultData = await fetch("https://hpo.jax.org/api/hpo/search/?q=" + hpoTerm)
        .then(response => response.json());
        // .then(data => console.log(data['terms']));
      // let {results} = await res.json();
      console.log(resultData['terms']);
      this.hpoResults = resultData['terms'];

    }
  },
};
</script>
