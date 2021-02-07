<template>
  <div>
    <MessageError v-if="error">{{ error }}</MessageError>
    <h1>
      <small>Databank:</small><br />{{ databank.name }} ({{ databank.acronym }})
    </h1>
    <label> Website: </label>
    <a :href="databank.website">{{ databank.website }}</a> <br />
    <label> Type(s): </label>
    <span v-for="type in databank.type">{{ type.name }}</span
    ><br />
    <label>Description:</label>
    <p>{{ databank.description }}</p>
    <label>Quality:</label>
    <p>{{ databank.quality }}</p>
    <label>Lag time:</label>
    <p>{{ databank.lagTime }}</p>
    <label>Prompt:</label>
    <p>{{ databank.prompt }}</p>
    <label>Originator:</label>
    <p>{{ databank.originator ? databank.originator.acronym : "N/A" }}</p>
    <h4>Tables:</h4>
    <TableList
      :databankAcronym="databankAcronym"
      :institutionAcronym="institutionAcronym"
    />
  </div>
</template>

<script>
import { request } from "graphql-request";
import { MessageError, ReadMore } from "@mswertz/emx2-styleguide";
import TableList from "../components/TableList";

export default {
  components: {
    MessageError,
    ReadMore,
    TableList,
  },
  props: {
    databankAcronym: String,
    institutionAcronym: String,
  },
  data() {
    return {
      error: null,
      databank: {},
    };
  },
  methods: {
    reload() {
      request(
        "graphql",
        `query Databanks($acronym:String){Databanks(filter:{acronym:{equals:[$acronym]}}){name,acronym,type{name},institution{acronym,name}, description,website, quality,investigators{name}, supplementaryInformation, tables{name},originator{acronym}}}`,
        {
          acronym: this.databankAcronym,
        }
      )
        .then((data) => {
          this.databank = data.Databanks[0];
          console.log("doh " + JSON.stringify(this.databank));
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
