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
            label="Search with less specific terms"
            v-model="searchAssociates"
            :options="['Search less specific']"
            description="check this box if you want to search with terms that are less
        specific than the entered term. But are associated with the HPO term"/>
    </div>

    <div id="bottemdiv">
      <div>
        <ButtonOutline @click="main" v-if="!doneSearch">Search for matches</ButtonOutline>

        <ButtonOutline @click="clearData" v-if="doneSearch">Click this for new search</ButtonOutline>
      </div>

      <div class="results" v-if="loading">
        <p v-if="parentSearch" >Searching for matches with parents:
          <span v-for="hpoObject in this.selectedHpoTerms"> {{ hpoObject.parents }}, </span>
          and the entered term(s): <span v-for="hpoObject in this.selectedHpoTerms"> {{ hpoObject.term }}, </span>
        </p>
        <Spinner/>
      </div>

      <div class="results" v-else>
        <div v-if="foundMatch">
          <MessageSuccess>Match found!</MessageSuccess>
          <p>The entered term(s):
            <span v-for="hpoObject in this.selectedHpoTerms"> {{ hpoObject.term }}, </span>
            is/are associated with the following variants:
          </p>

          <Table :vcfData="this.matchedVariants"></Table>
        </div>
        <div v-if="noMatch">
          <MessageError>No match Found</MessageError>
          <p>
            <span v-for="hpoObject in this.selectedHpoTerms"> {{ hpoObject.term }}, </span>
            resulted in zero matches.
            Maybe try another HPO term or search with parents and or children </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {ButtonOutline, InputCheckbox, MessageError, MessageSuccess, Spinner} from "@mswertz/emx2-styleguide";
import SearchAutoComplete from "./SearchAutoComplete";
import Table from "./Table"
import hpoData from "../js/autoSearchData.js";
import request from "graphql-request";

export default {
  components: {
    SearchAutoComplete,
    MessageError,
    MessageSuccess,
    ButtonOutline,
    InputCheckbox,
    Spinner,
    Table,
  },
  data() {
    return {
      allHpoTerms: hpoData,
      selectedHpoTerms: [],
      foundMatch: false,
      hpoIds: null,
      searchAssociates: [],
      loading: false,
      readOnly: false,
      noMatch: false,
      parentSearch: false,
      vcffile: "",
      fileData: null,
      matchedVariants: [],
      doneSearch: false
    };
  },
  async created() {
    this.vcffile = this.$route.params.vcf.toString();
    this.fileData = await this.getVariantData();
  },
  methods: {
    addHpoResult(selectedHpoTerms) {
      for (let i = 0; i < selectedHpoTerms.length; i++) {
        let hpoObject = {"term" : "", "parents" : [], "children" : []};
        hpoObject.term = selectedHpoTerms[i];
        this.selectedHpoTerms.push(hpoObject);
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
          hpoTerm: this.selectedHpoTerms[i].term,
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
        await this.addAssociates(data, i);
      }
    },
    async addAssociates(data, i) {
      if(this.searchAssociates.includes("Search less specific")) {
        for (let j = 0; j < data["parents"].length; j++) {
          this.parentSearch = true;
          let parentId = data["parents"][j];
          let parentTerm = await this.hpoIdToTerm(parentId.replace("_", ":"));
          this.selectedHpoTerms[i].parents.push(parentTerm);
        }
      }
      if(this.searchAssociates.includes("Search more specific")) {
        this.selectedHpoTerms[i].children = data["children"];
      }
    },
    /**
     * Function that loops through the properties of this.fileData and calls this.addMatchedVariants.
     * after this.addMatchedVariants is done the function checks if matches are found.
     */
    async matchVariantWithHpo() {
      let matchesNeeded = this.selectedHpoTerms.length;

      for (const property in this.fileData) {
        this.addMatchedVariants(property, matchesNeeded);
      }
      if (this.matchedVariants.length >= 1) {
        this.foundMatch = true;
      } else {
        this.noMatch = true;
      }
    },
    /**
     * Function that gets property of this.fileData together with the number of entered hpo terms as
     * matchedNeeded. It loops through the information line of fileData[property.Information which contains
     * the HPO terms which are annotated to this variant. When multiple terms are entered it checks
     * if both of these terms (or its parents/children) match to the current variant line.
     * When one term is entered it just checks if it or its parents/children matches.
     * When a term is matched a check is performed which checks if the current term has not matched
     * before on this current line to prevent the resulting table from containing duplicate terms.
     * @param property property of this.fileData
     * @param matchesNeeded number of entered terms
     */
    addMatchedVariants(property, matchesNeeded) {
      let InfoLine = this.fileData[property].Information;
      this.fileData[property].MatchedWith = [];
      let splitInfoLine = InfoLine.split("|");
      let hpoTermsToMatch = splitInfoLine[splitInfoLine.length - 2].replace("[", "").replace("]","").split(",");
      if (splitInfoLine[splitInfoLine.length - 2].split(",").length !== splitInfoLine[splitInfoLine.length - 1].split(",").length) {
        // console.log(splitInfoLine.toString());
        // console.log("lengte hpo en disease " + splitInfoLine[splitInfoLine.length - 2].split(",").length);
        // console.log( "en " + splitInfoLine[splitInfoLine.length - 1].split(",").length);
      }
      let termIndex = [];
      let matchedTerms = [];
      let foundMatches = 0;

      for (let i = 0; i < hpoTermsToMatch.length; i++) {
        let currentHpoTerm = hpoTermsToMatch[i].trim();
        if (matchesNeeded > 1) {
          for (let j = 0; j < matchesNeeded; j++) {
            if (this.selectedHpoTerms[j].term === currentHpoTerm ||
              this.selectedHpoTerms[j].parents.includes(currentHpoTerm) ||
              this.selectedHpoTerms[j].children.includes(currentHpoTerm))  {
              if(!matchedTerms.includes(currentHpoTerm)) {
                foundMatches++;
                matchedTerms.push(currentHpoTerm);
              }
            }
          }
          if (matchesNeeded === foundMatches) {
            termIndex.push(i);
            foundMatches = 0;
            this.fileData[property].MatchedWith = matchedTerms;
          }
        } else {
          if(this.selectedHpoTerms[0].term === currentHpoTerm ||
              this.selectedHpoTerms[0].parents.includes(currentHpoTerm) ||
              this.selectedHpoTerms[0].children.includes(currentHpoTerm)) {
            if(!matchedTerms.includes(currentHpoTerm)) {
              termIndex.push(i);
              matchedTerms.push(currentHpoTerm);
              this.fileData[property].MatchedWith.push(currentHpoTerm);

            }
          }
        }
      }
      if (termIndex.length > 0) {
        this.addGeneAndDisease(property, termIndex, splitInfoLine);
      }
    },
    /**
     * Function that adds the gene and disease Ids to this.fileData for the resulting table.
     * It removes the annotated data (diseases and hpo terms) from the information line.
     * @param property property of this.fileData
     * @param termIndex Array containing the indexes of the matched terms
     * @param splitInfoLine info line that is split on '|'
     */
    addGeneAndDisease(property, termIndex, splitInfoLine) {
      let diseaseIds = [];
      for (let j = 0; j < termIndex.length; j++) {
        let index = termIndex[j];
        // TODO create a solution for this problems temporary work around
        // When uploading the annotations of HPO and disease are of equal length.
        // but here in the app there are differences for some lines. How?
        if (index === splitInfoLine[splitInfoLine.length - 1].split(",").length ||
          index > splitInfoLine[splitInfoLine.length - 1 ].split(",").length) {
          index = index = 0;
        }
        diseaseIds.push(splitInfoLine[splitInfoLine.length - 1].split(",")[index].replace("[", "").replace("]", ""));
      }
      let gene = splitInfoLine[3];
      this.fileData[property].Diseases = diseaseIds;
      this.fileData[property].Gene = gene;
      this.fileData[property].Information = splitInfoLine.slice(0, splitInfoLine.length - 2).toString().replaceAll(",", "|");
      this.matchedVariants.push(this.fileData[property]);

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
        let hpoId = await this.hpoTermToId(this.selectedHpoTerms[i].term);
        hpoIds.push(hpoId.replace(":", "_"));
      }
      this.hpoIds = hpoIds;

      await this.getHpoAssociates(this.hpoIds);
      this.searchAssociates = [];
    },
    async main() {
      this.searchAssociates.push("Search more specific");
      if (this.selectedHpoTerms.length === 0) {
        alert("Please make sure to enter 1 or multiple HPO terms");
      } else {
        this.loading = true;
        this.foundMatch = false;

        await this.addHpoAssociates();
        await this.matchVariantWithHpo();
        this.loading = false;
        this.readOnly = true;
        this.doneSearch = true;
      }
    },
    clearData() {
      this.selectedHpoTerms = [];
      this.hpoIds = null;
      this.searchAssociates = [];
      this.patientGenes = null;
      this.loading = false;
      this.readOnly = false;
      this.foundMatch = false;
      this.noMatch = false;
      this.matchedVariants = [];
      this.doneSearch = false;
    }
  },
};
</script>

<style scoped>
#wrapper {
  overflow: hidden; /* add this to contain floated children */
}
#searchdiv {
  width: 70%;
  margin: 0 auto;
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