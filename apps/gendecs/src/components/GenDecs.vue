<template>
  <div id="wrapper">
    <div id="titlediv">
      <h1>Welcome to GenDecS!</h1>
      <p>this page contains the prototype for GenDecS. Here you can enter a patient number together
        with a HPO root-term/phenotype. When a number is entered a vcfdata file will be downloaded.
        This data will be filtered on possible disease causing genes. Then the filtered genes will
        be matched with HPO terms. The then found terms will be checked if they match with the
        given HPO term(s) or its associates if checked. The resulting found variants are reported in
        a file. A "match found!" message will appear if a match is found.
      </p>
    </div>
    <div id="searchdiv">
      <SearchAutoComplete :items= "allHpoTerms"
                          @selectedHpoTerms="addHpoResult" class="inputForm"
      ></SearchAutoComplete>

      <InputCheckbox
            label="Search for parents and children"
            v-model="searchAssociates"
            :options="['Search for parents and children']"
            description="check this box if you want to search for parents and children of
                        your HPO term"/>
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
        <p v-if="foundMatch"> {{ selectedHpoTerms }} has a match with the following gene(s): {{ patientGenes }}.
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
      selectedHpoTerms: [],
      foundMatch: false,
      hpoChildren: [],
      hpoParents: [],
      hpoIds: null,
      searchAssociates: null,
      patientGenes: null,
      genesHpo: null,
      loading: false,
      newForm: false
    };
  },
  methods: {
    addHpoResult(selectedHpoTerms) {
      for (let i = 0; i < selectedHpoTerms.length; i++) {
        this.selectedHpoTerms.push(selectedHpoTerms[i])
      }
    },
    async hpoTermToId(hpoTerm) {
      /**
      * Function gets the Hpo term that is selected by the user as selectedHpoTerm.
      * This is then used to gather the ID of the term using an api call.
      * */
      let resultData = await fetch("https://hpo.jax.org/api/hpo/search/?q=" + hpoTerm)
          .then((response) => {
            if (response.ok) {
              return response.json();
            }
            throw new Error("Something went wrong");
          })
          .catch((error) => {
            console.log(error);
          });
      let hpoResults = resultData['terms'];

      return hpoResults[0].id;
    },
    async hpoIdToTerm(hpoId) {
      let resultData = await fetch("https://hpo.jax.org/api/hpo/search/?q=" + hpoId)
          .then((response) => {
            if (response.ok) {
              return response.json();
            }
            throw new Error("Something went wrong");
          })
          .catch((error) => {
            console.log(error);
          });
      let hpoResults = resultData['terms'];

      return hpoResults[0].name;
    },
    async getHpoAssociates(hpoIds) {
      /**
      * Function that gets the HPO id of the entered HPO term. This id is sent to the backend.
      * The parents and children of this term are returned by the backend.
      * */
      for (let i = 0; i < this.hpoIds.length; i++) {
        let requestOptions = {
          method: 'POST',
          body: JSON.stringify({ hpoId: hpoIds[i] })
        };

        let data = await fetch('/patients/api/gendecs/queryHpo', requestOptions)
            .then((response) => {
              if (response.ok) {
                return response.json();
              }
              throw new Error("Something went wrong");
            })
            .catch((error) => {
              console.log(error);
            });

        for (let i = 0; i < data["parents"].length; i++) {
          let parentId = data["parents"][i];
          let parentTerm = await this.hpoIdToTerm(parentId.replace("_", ":"));
          this.hpoParents.push(parentTerm);
        }
        console.log("data children" + data["children"]);
        for (let j = 0; j < data["children"].length; j++) {
          this.hpoChildren.push(data["children"][i]);
        }
      }
    },
    async matchVcfWithHpo() {
      /**
       * Function that sends an api call to the backend to parse the vcf data.
       * Respsonse: genes with their hpo term. Adds the response to this.genesHpo
       * example: {"ODAD2":"Female infertility","HPSE2":"Urinary incontinence"}
       *
       */
      let requestOptions = {
        method: 'POST',
        body: JSON.stringify({ hpoTerms : this.selectedHpoTerms,
          hpoChildren : this.hpoChildren,
          hpoParents : this.hpoParents})
      };
      this.genesHpo = await fetch('/patients/api/gendecs/vcffile', requestOptions)
          .then((response) => {
            if (response.ok) {
              return response.json();
            }
            throw new Error("Something went wrong");
          })
          .catch((error) => {
            console.log(error);
          });

      if(this.genesHpo.length !== 0) {
        this.foundMatch = true;
        this.patientGenes = Object.keys(this.genesHpo);
      }
    },
    async main() {
      this.loading = true;
      if(this.searchAssociates != null) {
        let hpoIds = [];
        for (let i = 0; i < this.selectedHpoTerms.length; i++) {
          let hpoId = await this.hpoTermToId(this.selectedHpoTerms[i]);
          hpoIds.push(hpoId.replace(":", "_"));
        }
        this.hpoIds = hpoIds

        await this.getHpoAssociates(this.hpoIds);
        this.searchAssociates = null;
      }
      await this.matchVcfWithHpo();
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