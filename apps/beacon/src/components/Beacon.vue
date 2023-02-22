<template>
  <div>
    <h1>Beacon v2 GUI</h1>

    <InputSelect
      label="Use schema"
      id="schema-select"
      v-model="schema"
      :options="['fairdatahub']"
    />
    <!-- todo retrieve schema names using Client.js-->

    <InputSelect
      label="Sex"
      id="sex-select"
      v-model="sex"
      :options="['NCIT_C16576', 'NCIT_C20197', 'NCIT_C124294', 'NCIT_C17998']"
    />
    DEBUG: {{ JSON.stringify(sex) }}

    <InputRefSelect
      label="Disease"
      id="disease-ref"
      columnType="REF"
      tableName="Diseases"
      :modelValue="disease"
      @update:modelValue="diseaseChangeHandler"
    />
    DEBUG: {{ JSON.stringify(disease, null, 2) }}

    <InputRefSelect
      label="Phenotype"
      id="phenotype-ref"
      columnType="REF"
      tableName="Phenotypes"
      v-model="phenotype"
    />
    DEBUG: {{ JSON.stringify(phenotype) }}

    <InputRefSelect
      label="Causal gene"
      id="gene-ref"
      columnType="REF"
      tableName="Genes"
      v-model="gene"
    />
    DEBUG: {{ JSON.stringify(gene) }}

    <InputInt
      label="Age"
      id="age-input-int"
      v-model="ageCurrent"
      description="Age this year, in whole years"
    />
    DEBUG: {{ JSON.stringify(ageCurrent) }}

    <InputInt
      label="Onset"
      id="onset-input-int"
      v-model="ageOnset"
      description="Age at symptom onset, in whole years"
    />
    DEBUG: {{ JSON.stringify(ageOnset) }}

    <InputInt
      label="Onset"
      id="age_diagnosis-input"
      v-model="ageDiagnosis"
      description="Age at diagnosis, in whole years"
    />
    DEBUG: {{ JSON.stringify(ageDiagnosis) }}

    <ButtonAction @click="postBeaconQuery"> Run query </ButtonAction>

    RESPONSE SUMMARY:
    {{ JSON.stringify(this.responseSummary) }}
  </div>
</template>

<script>
import {
  InputSelect,
  ButtonAction,
  InputRefSelect,
  InputOntology,
  InputInt,
} from "molgenis-components";

import { request } from "graphql-request";
import axios from "axios";

export default {
  components: {
    InputSelect,
    ButtonAction,
    InputRefSelect,
    InputOntology,
    InputInt,
  },
  data() {
    return {
      schema: "fairdatahub",
      sex: "NCIT_C16576",
      disease: null,
      phenotype: null,
      gene: null,
      ageCurrent: 50,
      ageOnset: 20,
      ageDiagnosis: 25,
      responseSummary: null,
    };
  },
  computed: {
    endpoint() {
      return "/api/beacon/individuals";
    },
  },
  methods: {
    postBeaconQuery() {
      this.loading = true;
      this.responseSummary = null;
      let gene = this.gene.name; // todo needs a null check
      let sex = this.sex;
      axios
        .post(
          this.endpoint, // todo ideally, leave out portion of filter if value is null
          `{
            "query": {
              "filters": [
              {
                "id":"NCIT_C28421",
                "value":"${sex}",
                "operator":"="
              },
                {
                  "id": "NCIT_C16612",
                  "value": "${gene}",
                  "operator": "="
                }
              ]
            }
          }`
        )
        .then((data) => {
          this.responseSummary = data;
          this.loading = false;
        });
    },
    diseaseChangeHandler(newDisease) {
      this.disease = newDisease; // todo needs a lookup to get the ontologyURI
    },
  },
};
</script>
