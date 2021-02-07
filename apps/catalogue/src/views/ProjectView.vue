<template>
  <div>
    <h1>
      <small>Consortium:</small><br />{{ consortium.name }} ({{
        consortium.acronym
      }})
    </h1>
    <label>Description:</label>
    <ReadMore
      :text="consortium.description"
      :length="1000"
      v-if="consortium.description"
    />
    <h4>Tables:</h4>
    <TableList :consortiumAcronym="consortium.acronym" />
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
    consortiumAcronym: String,
  },
  data() {
    return {
      error: null,
      consortium: {},
    };
  },
  methods: {
    reload() {
      request(
        "graphql",
        `query Consortia($acronym:String){Consortia(filter:{acronym:{equals:[$acronym]}}){name,acronym,type{name},Institute{acronym,name}, description,website, investigators{name}, supplementaryInformation, tables{name}}}`,
        {
          acronym: this.consortiumAcronym,
        }
      )
        .then((data) => {
          this.consortium = data.Consortia[0];
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
    consortiumAcronym() {
      this.reload();
    },
  },
};
</script>
