<template>
  <div>
    <MessageError v-if="error">{{ error }}</MessageError>
    <h1>
      <small>Provider</small><br />{{ provider.name }} ({{ provider.acronym }})
    </h1>
    <label>Website:</label>
    <a :href="provider.website">{{ provider.website }}</a> <br />
    <label>Description:</label>
    <p>{{ provider.description }}</p>
    <h4>Databanks:</h4>
    <br />
    <DatabankList :providerAcronym="providerAcronym" />
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
    providerAcronym: String,
  },
  data() {
    return {
      error: null,
      provider: {},
    };
  },
  methods: {
    reload() {
      request(
        "graphql",
        `query Providers($acronym:String){Providers(filter:{acronym:{equals:[$acronym]}}){name,acronym,description,website,collections{acronym,name}}}`,
        {
          acronym: this.providerAcronym,
        }
      )
        .then((data) => {
          this.provider = data.Providers[0];
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
    providerAcronym() {
      this.reload();
    },
  },
};
</script>
