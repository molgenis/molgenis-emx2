<template>
  <div>
    <MessageError v-if="error">{{ error }}</MessageError>
    <h1>
      <small>Institute</small><br />{{ Institute.name }} ({{
        Institute.acronym
      }})
    </h1>
    <label>Website:</label>
    <a :href="Institute.website">{{ Institute.website }}</a> <br />
    <label>Description:</label>
    <p>{{ Institute.description }}</p>
    <h4>Databanks:</h4>
    <br />
    <DatabankList :institutionAcronym="institutionAcronym" />
  </div>
</template>

<script>
import { request } from "graphql-request";
import { MessageError, ReadMore } from "@mswertz/emx2-styleguide";
import DatabankList from "../components/DatabankList";

export default {
  components: {
    DatabankList,
    MessageError,
    ReadMore,
  },
  props: {
    institutionAcronym: String,
  },
  data() {
    return {
      error: null,
      Institute: {},
    };
  },
  methods: {
    reload() {
      request(
        "graphql",
        `query Institutes($acronym:String){Institutes(filter:{acronym:{equals:[$acronym]}}){name,acronym,description,website,collections{acronym,name}}}`,
        {
          acronym: this.institutionAcronym,
        }
      )
        .then((data) => {
          this.Institute = data.Institutes[0];
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
    institutionAcronym() {
      this.reload();
    },
  },
};
</script>
