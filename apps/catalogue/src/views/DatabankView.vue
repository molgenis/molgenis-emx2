<template>
  <div>
    <h1>
      <small>Databanks:</small><br />{{ databank.name }} ({{
        databank.acronym
      }})
    </h1>
    <label> Website: </label>
    <a :href="databank.website">{{ databank.website }}</a> <br />
    <label> Type(s): </label>
    <span v-for="type in databank.type">{{ type.name }}</span
    ><br />
    <label>Description:</label>
    <p>{{ databank.description }}</p>
    <h4>Tables:</h4>
    <TableList
      :databankAcronym="databankAcronym"
      :providerAcronym="providerAcronym"
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
    providerAcronym: String,
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
        `query Databanks($acronym:String){Databanks(filter:{acronym:{equals:[$acronym]}}){name,acronym,type{name},provider{acronym,name}, description,website, investigators{name}, supplementaryInformation, tables{name}}}`,
        {
          acronym: this.databankAcronym,
        }
      )
        .then((data) => {
          this.databank = data.Databanks[0];
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
