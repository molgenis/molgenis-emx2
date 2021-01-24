<template>
  <div>
    <h1>
      <small>Collection:</small><br />{{ collection.name }} ({{
        collection.acronym
      }})
    </h1>
    <label> Website: </label>
    <a :href="collection.website">{{ collection.website }}</a> <br />
    <label> Type(s): </label>
    <span v-for="type in collection.type">{{ type.name }}</span
    ><br />
    <label>Description:</label>
    <p>{{ collection.description }}</p>
    <h4>Datasets:</h4>
    <DatasetList
      :collectionAcronym="collectionAcronym"
      :providerAcronym="providerAcronym"
    />
  </div>
</template>

<script>
import { request } from "graphql-request";
import { MessageError, ReadMore } from "@mswertz/emx2-styleguide";
import DatasetList from "../components/DatasetList";

export default {
  components: {
    MessageError,
    ReadMore,
    DatasetList,
  },
  props: {
    collectionAcronym: String,
    providerAcronym: String,
  },
  data() {
    return {
      error: null,
      collection: {},
    };
  },
  methods: {
    reload() {
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
