<template>
  <div>
    {{ datasourceAcronym }}
    <MessageError v-if="error">{{ error }}</MessageError>
    <h1>
      <small v-if="datasource.type">{{
        datasource.type.map((t) => t.name).join(",")
      }}</small
      ><small v-else>Datasource:</small><br />{{ datasource.name }} ({{
        datasource.acronym
      }})
    </h1>
    <label> Website: </label>
    <a :href="datasource.website">{{ datasource.website }}</a> <br />
    <label> Type(s): </label>
    <span v-for="type in datasource.type">{{ type.name }}</span
    ><br />
    <NavTabs :options="['Data', 'Description']" v-model="tab" />
    <div v-if="tab == 'Data'" class="tab-pane show active">
      <h4>Databanks:</h4>
      <DatabankList
        :datasourceAcronym="datasourceAcronym"
        :showSearch="false"
      />
    </div>
    <div v-if="tab == 'Description'" class="tab-pane show active">
      <label>Description:</label>
      <p>{{ datasource.description }}</p>
      <p>{{ datasource.originator ? datasource.originator.acronym : "N/A" }}</p>
      <label>Quality:</label>
      <p>{{ datasource.quality }}</p>
      <label>Lag time:</label>
      <p>{{ datasource.lagTime }}</p>
      <label>Prompt:</label>
      <p>{{ datasource.prompt }}</p>
      <label>Originator:</label>
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
import DatabankList from "../components/DatabankList";

export default {
  components: {
    MessageError,
    ReadMore,
    TableList,
    VariablesList,
    InputSelect,
    NavTabs,
    InputSelect,
    DatabankList,
  },
  props: {
    datasourceAcronym: String,
    institutionAcronym: String,
  },
  data() {
    return {
      error: null,
      datasource: {},
      version: null,
      tab: "Data",
    };
  },
  methods: {
    reload() {
      request(
        "graphql",
        `query Datasources($acronym:String){Datasources(filter:{acronym:{equals:[$acronym]}}){name,acronym,type{name},provider{acronym,name}, description,website, investigators{name}, supplementaryInformation, releases{version}}}`,
        {
          acronym: this.datasourceAcronym,
        }
      )
        .then((data) => {
          console.log(data);
          this.datasource = data.Datasources[0];
          if (this.datasource.releases) {
            this.version = this.datasource.releases[
              this.datasource.releases.length - 1
            ].version;
          }
        })
        .catch((error) => {
          console.log(error);

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
