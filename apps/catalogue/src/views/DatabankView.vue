<template>
  <div>
    <MessageError v-if="error">{{ error }}</MessageError>
    <h1>
      <small v-if="databank.type">{{
        databank.type.map((t) => t.name).join(",")
      }}</small
      ><small v-else>Databank:</small><br />{{ databank.name }} ({{
        databank.acronym
      }})
    </h1>
    <label> Website: </label>
    <a :href="databank.website">{{ databank.website }}</a> <br />
    <label> Type(s): </label>
    <span v-for="type in databank.type">{{ type.name }}</span
    ><br />
    <NavTabs :options="['Description', 'Data']" v-model="tab" />
    <div v-if="tab == 'Description'" class="tab-pane show active">
      <label>Description:</label>
      <p>{{ databank.description }}</p>
      <p>{{ databank.originator ? databank.originator.acronym : "N/A" }}</p>
      <label>Population:</label>
      <p v-if="databank.datasource">
        Underlying population: {{ databank.datasource.population }}
      </p>
      <p v-if="databank.datasource">
        Datasource population: {{ databank.datasource.inclusionCriteria }}
      </p>
      <p>Databank population: {{ databank.population }}</p>
      <h6>Quality:</h6>
      <p>{{ databank.quality }}</p>
      <h6>Lag time:</h6>
      <p>{{ databank.lagTime }}</p>
      <h6>Prompt:</h6>
      <p>{{ databank.prompt }}</p>
      <h6>Purpose:</h6>
      <p>{{ databank.purpose }}</p>
      <h6>Collection:</h6>
      <p>{{ databank.collection }}</p>
      <h6>StartYear:</h6>
      <p>{{ databank.startYear }}</p>
      <h6>Completeness:</h6>
      <p>{{ databank.completeness }}</p>
    </div>
    <div v-if="tab == 'Data'" class="tab-pane show active">
      <span v-if="databank.releases == null">No data loaded</span>
      <span v-else>
        <InputSelect
          label="choose a release"
          v-if="databank.releases"
          v-model="version"
          :options="databank.releases.map((r) => r.version)" />
        <VariablesList
          v-if="version"
          :resourceAcronym="databankAcronym"
          :version="version"
      /></span>
    </div>
    <!--<h4>Tables:</h4>
    <TableList
      :databankAcronym="databankAcronym"
      :institutionAcronym="institutionAcronym"
    />-->
  </div>
</template>

<script>
import { request } from "graphql-request";
import {
  MessageError,
  ReadMore,
  InputSelect,
  NavTabs,
} from "@mswertz/emx2-styleguide";
import TableList from "../components/TableList";
import VariablesList from "../components/VariablesList";

export default {
  components: {
    MessageError,
    ReadMore,
    TableList,
    VariablesList,
    InputSelect,
    NavTabs,
    InputSelect,
  },
  props: {
    databankAcronym: String,
    institutionAcronym: String,
  },
  data() {
    return {
      error: null,
      databank: {},
      version: null,
      tab: "Description",
    };
  },
  methods: {
    reload() {
      request(
        "graphql",
        `query Databanks($acronym:String){Databanks(filter:{acronym:{equals:[$acronym]}}){name,acronym,datasource{population,inclusionCriteria},purpose, population,updateFrequency,completeness, startYear,endYear, type{name},provider{acronym,name}, description,website,prompt, lagTime, supplementaryInformation, releases{version}}}`,
        {
          acronym: this.databankAcronym,
        }
      )
        .then((data) => {
          this.databank = data.Databanks[0];
          if (this.databank.releases) {
            this.version = this.databank.releases[
              this.databank.releases.length - 1
            ].version;
          }
        })
        .catch((error) => {
          this.error = error.response.errors[0].message;
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
  created() {
    this.reload();
  },
  watch: {
    databankAcronym() {
      this.reload();
    },
  },
};
</script>
