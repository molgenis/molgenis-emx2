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
    <label>Collections:</label><br />
    <CollectionList :providerAcronym="providerAcronym" />
  </div>
</template>

<script>
import { request } from "graphql-request";
import { MessageError, ReadMore } from "@mswertz/emx2-styleguide";
import CollectionList from "../components/CollectionList";

export default {
  components: {
    CollectionList,
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
      console.log("provider reload");
      request(
        "graphql",
        `query Organisations($acronym:String){Organisations(filter:{acronym:{equals:[$acronym]}}){name,acronym,description,website,resources{acronym,name}}}`,
        {
          acronym: this.providerAcronym,
        }
      )
        .then((data) => {
          this.provider = data.Organisations[0];
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
