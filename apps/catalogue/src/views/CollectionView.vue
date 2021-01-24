<template>
  <div>
    <h1>
      <small>Collection:</small><br />{{ collection.name }} ({{
        collection.acronym
      }})
    </h1>
    <label> Type(s): </label>
    <span v-for="type in collection.type">{{ type.name }}</span>
    <label>Description:</label>
    <p>{{ collection.description }}</p>
    <label>Datasets</label>
    <DatasetList :collectionAcronym="collectionAcronym" />
  </div>
</template>

<script>
import { request } from "graphql-request";
import { MessageError, ReadMore } from "@mswertz/emx2-styleguide";
import DatasetList from "./DatasetListView";

export default {
  components: {
    MessageError,
    ReadMore,
    DatasetList,
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
        `query Collections($acronym:String){Collections(filter:{acronym:{equals:[$acronym]}}){name,acronym,type{name},provider{acronym,name}, description,website, investigators{name}, supplementaryInformation, datasets{name}}}`,
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
