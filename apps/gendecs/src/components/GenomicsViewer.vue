<template>
  <div id="wrapper">
    <div id="titlediv">
      <h1>Welcome to the Genomics viewer of GenDecS</h1>
      <p>this page contains the prototype for GenDecS. Here you can enter a patient number together
        with a HPO root-term/phenotype. When a number is entered a vcfdata file will be downloaded.
        This data will be filtered on possible disease causing genes. Then the filtered genes will
        be matched with HPO terms. The then found terms will be checked if they match with the
        given HPO term(s) or its associates if checked. The resulting found variant are reported in
        a file. A "match found!" message will appear if a match is found.
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
          <p>The {{ selectedHpoTerms }} is associated with the following gene(s): {{ patientGenes }}.
            Which was found in the patient vcf data</p>
          <Table :vcfData="this.tableData"></Table>
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
import {
  MessageError,
  MessageSuccess,
  ButtonOutline,
  InputCheckbox,
  Spinner
} from "@mswertz/emx2-styleguide";
import SearchAutoComplete from "./SearchAutoComplete";
import PatientSearch from "./PatientSearch";
import Table from "./Table"
import hpoData from "../js/autoSearchData.js";

export default {
  components: {
    SearchAutoComplete,
    PatientSearch,
    MessageError,
    MessageSuccess,
    ButtonOutline,
    InputCheckbox,
    Spinner,
    Table
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
      patientGenes: [],
      genesHpo: null,
      loading: false,
      readOnly: false,
      noMatch: false,
      parentSearch: false,
      tableData: []
    };
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
     * Function that sends an api call to the backend to parse the vcf data.
     * Respsonse: genes with their hpo term. Adds the response to this.genesHpo
     * example: {"ODAD2":"Female infertility","HPSE2":"Urinary incontinence"}
     */
    async matchVcfWithHpo() {
      let requestOptions = {
        method: 'POST',
        body: JSON.stringify({ hpoTerms : this.selectedHpoTerms,
          hpoChildren : this.hpoChildren,
          hpoParents : this.hpoParents})
      };
      let variantData = await fetch('/patients/api/gendecs/vcffile', requestOptions)
          .then((response) => {
            if (response.ok) {
              return response.json();
            }
            throw new Error("Something went wrong with vcf api");
          })
          .catch((error) => {
            console.log(error);
          });
      this.tableData = variantData;
      for (let i = 0; i < variantData.length; i++) {
        this.patientGenes.push(variantData[i].gene);
      }
      if (this.patientGenes.length === 0) {
        this.noMatch = true;
      } else if(this.patientGenes.length !== 0) {
        this.foundMatch = true;
      } else if (this.patientGenes === 'undefined') {
        this.noMatch = true;
      }
    },
    async main() {
      console.log(this.$route.params.id.toString());
      this.loading = true;
      this.foundMatch = false;

      if(this.searchAssociates != null) {
        let hpoIds = [];
        for (let i = 0; i < this.selectedHpoTerms.length; i++) {
          let hpoId = await this.hpoTermToId(this.selectedHpoTerms[i]);
          hpoIds.push(hpoId.replace(":", "_"));
        }
        this.hpoIds = hpoIds;

        await this.getHpoAssociates(this.hpoIds);
        this.searchAssociates = null;
      }
      await this.matchVcfWithHpo();
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