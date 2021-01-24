<template>
  <div>
    <MessageError v-if="error">{{ error }}</MessageError>
    <h1>Collection: {{ collectionAcronym }}</h1>
    <div>
      <ReadMore
        :text="collection.description"
        :length="200"
        v-if="collection.description"
      />
    </div>
    <CollectionTabs
      selected="variables"
      :collection-acronym="collectionAcronym"
    />
    <VariablesList :collectionAcronym="collectionAcronym" />
  </div>
</template>
<script>
import { request } from "graphql-request";
import { MessageError, ReadMore } from "@mswertz/emx2-styleguide";
import DatasetList from "./DatasetList";
import CollectionTabs from "./CollectionTabs";
import VariablesList from "./VariablesList";

export default {
  components: {
    VariablesList,
    CollectionTabs,
    DatasetList,
    MessageError,
    ReadMore,
  },
  props: {
    collectionAcronym: String,
  },
  data() {
    return {
      error: null,
      collection: {},
    };
  },
  methods: {
    reload() {
      console.log("collections reload");
      request(
        "graphql",
        `query Collections($acronym:String){Collections(filter:{acronym:{equals:[$acronym]}}){name,acronym,type{name},description,website,datasets{name,label}}}`,
        {
          acronym: this.collectionAcronym,
        }
      )
        .then((data) => {
          this.collection = data.Collections[0];
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
    collectionAcronym() {
      this.reload();
    },
  },
};
</script>
