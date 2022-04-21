<template>
  <div id="wrapper">
    <div id="titlediv">
      <h1>Welcome to the Genomics viewer of GenDecS</h1>
      <p>This page contains the genomics viewer prototype of GenDecS. Here you can enter
        a HPO root-term/phenotype. The entered HPO term (and it's associates) is/are matched with the
        patient data from the file: {{ this.vcffile }}. If a result is found a "match found!" message will
        appear together with a table containing the matched variants. If no match is found a "no match found"
        message will show.
      </p>
    </div>
    <div id="searchdiv">
      <SearchAutoComplete :items= "allHpoTerms" :readOnly="this.readOnly"
                          @selectedHpoTerms="addHpoResult" class="inputForm"
      ></SearchAutoComplete>

      <InputCheckbox
            label="Search for parents and children"
            v-model="searchAssociates"
            :options="['Search for parents', 'Search for children']"
            description="check this box if you want to search for parents and children of
                        your HPO term"/>
    </div>

    <div id="bottemdiv">
      <ButtonOutline @click="main">Search for matches</ButtonOutline>

      <div class="results" v-if="loading">
        <p v-if="parentSearch" >Searching for matches with parents: {{ hpoParents }}
          and the entered term(s): {{ selectedHpoTerms }}
        </p>
        <Spinner/>
      </div>

      <div class="results" v-else>
        <div v-if="foundMatch">
          <MessageSuccess>Match found!</MessageSuccess>
          <p>The HPO term(s) {{ selectedHpoTerms }} is/are associated with the following variants:</p>
          <Table :vcfData="this.matchedVariants"></Table>
        </div>
        <div v-if="noMatch">
          <MessageError>No match Found</MessageError>
          <p>{{ selectedHpoTerms }} resulted in zero matches</p>
        </div>

      </div>
      <ButtonOutline @click="clearData">Click this for new search</ButtonOutline>

    </div>
  </div>
</template>

<script>
import {ButtonOutline, InputCheckbox, MessageError, MessageSuccess, Spinner} from "@mswertz/emx2-styleguide";
import SearchAutoComplete from "./SearchAutoComplete";
import PatientSearch from "./PatientSearch";
import Table from "./Table"
import hpoData from "../js/autoSearchData.js";
import request from "graphql-request";

export default {
  components: {
    SearchAutoComplete,
    PatientSearch,
    MessageError,
    MessageSuccess,
    ButtonOutline,
    InputCheckbox,
    Spinner,
    Table,
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
      genesHpo: null,
      loading: false,
      readOnly: false,
      noMatch: false,
      parentSearch: false,
      vcffile: "",
      fileData: null,
      matchedVariants: []
    };
  },
  async created() {
    this.vcffile = this.$route.params.vcf.toString();
    this.fileData = await this.getVariantData();
  },
  methods: {
    addHpoResult(selectedHpoTerms) {
      for (let i = 0; i < selectedHpoTerms.length; i++) {
        this.selectedHpoTerms.push(selectedHpoTerms[i])
      }
    },
    /**
     * Function gets the Hpo term that is selected by the user as selectedHpoTerm.
     * This is then used to gather the ID of the term using an api call.
     * */
    async hpoTermToId(hpoTerm) {
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
    /**
     * Function gets the Hpo id that is selected by the user as hpoId.
     * This is then used to gather the HPO term of the id using an api call.
     * */
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
    /**
     * Function that gets the HPO id of the entered HPO term. This id is sent to the backend.
     * The parents and children of this term are returned by the backend.
     * */
    async getHpoAssociates(hpoIds) {
      for (let i = 0; i < this.hpoIds.length; i++) {
        let requestOptions = {
          method: 'POST',
          body: JSON.stringify({ hpoId: hpoIds[i],
          hpoTerm: this.selectedHpoTerms[i],
          searchAssociates: this.searchAssociates})
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
        await this.addAssociates(data);
      }
    },
    async addAssociates(data) {
      if(this.searchAssociates.includes("Search for parents")) {
        for (let i = 0; i < data["parents"].length; i++) {
          this.parentSearch = true;
          let parentId = data["parents"][i];
          let parentTerm = await this.hpoIdToTerm(parentId.replace("_", ":"));
          this.hpoParents.push(parentTerm);
        }
      }
      if(this.searchAssociates.includes("Search for children")) {
        for (let i = 0; i < data["children"].length; i++) {
          this.hpoChildren.push(data["children"][i]);
        }
      }
    },
    /**
     *
     */
    async matchVariantWithHpo() {
      for (const property in this.fileData) {
        this.addMatchedVariants(property);
      }
      if (this.matchedVariants.length >= 1) {
        this.foundMatch = true;
      } else {
        this.noMatch = true;
      }
    },
    addMatchedVariants(property) {
      let InfoLine = this.fileData[property].Information;
      let splitInfoLine = InfoLine.split("|");
      let hpoTermsToMatch = splitInfoLine[splitInfoLine.length - 2].replace("[", "").replace("]","").split(",");
      let termIndex = [];
      let matchesNeeded = this.selectedHpoTerms.length;
      let foundMatches = 0;

      for (let i = 0; i < hpoTermsToMatch.length; i++) {
        let currentHpoTerm = hpoTermsToMatch[i].trim();
        if (this.selectedHpoTerms.length > 1) {
          for (let j = 0; j < this.selectedHpoTerms.length; j++) {
            // todo make objects of hpo Terms? to make multiple hpo terms + parents/children checking easier
            if (this.selectedHpoTerms[j].includes(currentHpoTerm))  {
              foundMatches++;
            }
          }
          if (matchesNeeded === foundMatches) {
            this.matchedVariants.push(this.fileData[property]);
            termIndex.push(i);
            foundMatches = 0;
          }
        } else {
          if(this.selectedHpoTerms.includes(currentHpoTerm) ||
              this.hpoParents.includes(currentHpoTerm) ||
              this.hpoChildren.includes(currentHpoTerm)) {
            this.matchedVariants.push(this.fileData[property]);
            termIndex.push(i);
          }
        }
      }
      if (termIndex.length > 0) {
        this.addGeneAndDisease(property, termIndex, splitInfoLine);
      }
    },
    addGeneAndDisease(property, termIndex, splitInfoLine) {
      let diseaseIds = [];

      for (let j = 0; j < termIndex.length; j++) {
        let index = termIndex[j];
        diseaseIds.push(splitInfoLine[splitInfoLine.length - 1].split(",")[index].replace("[", "").replace("]", ""));
      }
      let gene = splitInfoLine[3];
      this.fileData[property].Diseases = diseaseIds;
      this.fileData[property].Gene = gene;
      this.fileData[property].Information = splitInfoLine.slice(0, splitInfoLine.length - 2).toString().replaceAll(",", "|");
      termIndex = [];
    },
    async getVariantData() {
      let query = "{vcfVariants{VCFSourceFile Chromosome Position RefSNPNumber Reference Alternative Quality Filter Information }}"
      // let newQuery = `{
      //     Patients(gender: "male") {
      //       identifier
      //       gender
      //       birthdate
      //       vcfdata
      //       }
      //     }`
      let vcfData = await request("graphql", query)
          .then((data) => {
            return data["vcfVariants"];
          })
          .catch((error) => {
            if (Array.isArray(error.response.errors)) {
              this.graphqlError = error.response.errors[0].message;
            } else {
              this.graphqlError = error;
            }
          });

      return this.getCorrectFileData(vcfData);
    },
    getCorrectFileData(fileData) {
      //check if the file name exists in the data
      if(fileData.some(e => e.VCFSourceFile === this.vcffile)){
        for (const property in fileData) {
          if (fileData[property].VCFSourceFile === this.vcffile) {
            let InfoLine = fileData[property].Information;
            let splitInfoLine = InfoLine.split("|");
            let hpoTermsArray = splitInfoLine[splitInfoLine.length - 2].split(",");
            if (hpoTermsArray.length === 1) {
              delete fileData[property];
            } else {
              fileData[property].Position = fileData[property].Position.toString();
            }
          } else {
            delete fileData[property];
          }
        }
        return this.removeEmpty(fileData);
      } else {
        alert("The file: " + this.vcffile + " is not present in the database");
      }
    },
    removeEmpty(obj) {
      return Object.fromEntries(Object.entries(obj).filter(([_, v]) => v != null));
    },
    async addHpoAssociates() {
      let hpoIds = [];
      for (let i = 0; i < this.selectedHpoTerms.length; i++) {
        let hpoId = await this.hpoTermToId(this.selectedHpoTerms[i]);
        hpoIds.push(hpoId.replace(":", "_"));
      }
      this.hpoIds = hpoIds;

      await this.getHpoAssociates(this.hpoIds);
      this.searchAssociates = null;
    },
    async main() {
      this.loading = true;
      this.foundMatch = false;

      if(this.searchAssociates != null) {
        await this.addHpoAssociates();
      }
      await this.matchVariantWithHpo();
      this.loading = false;
      this.readOnly = true;
    },
    clearData() {
      this.geneAssociates = null;
      this.selectedHpoTerms = [];
      this.hpoChildren = [];
      this.hpoParents = [];
      this.hpoIds = null;
      this.searchAssociates = null;
      this.patientGenes = null;
      this.genesHpo = null;
      this.loading = false;
      this.readOnly = false;
      this.foundMatch = false;
      this.noMatch = false;
      this.matchedVariants = [];
    }
  },
};
</script>

<style scoped>
#wrapper {
  overflow: hidden; /* add this to contain floated children */
}
#searchdiv {
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