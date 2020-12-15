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
      selected="description"
      :collection-acronym="collectionAcronym"
    />
    <div class="card mt-2">
      <div class="card-body">
        <dl>
          <dt>Collection Acronym</dt>
          <dd>{{ collection.acronym }}</dd>
        </dl>
        <dl>
          <dt>Collection Name</dt>
          <dd>{{ collection.name }}</dd>
        </dl>
        <dl>
          <dt>Collection Type</dt>
          <dd>
            <span v-for="type in collection.type">{{ type.name }}</span>
          </dd>
        </dl>
        <dl>
          <dt>Organisation</dt>
          <dd>{{ collection.organisation }}</dd>
        </dl>
        <dl>
          <dt>Website</dt>
          <dd>
            <a v-if="collection.website" href="collection.website">{{
              collection.website
            }}</a>
          </dd>
        </dl>
        <dl>
          <dt>Description</dt>
          <dd>{{ collection.description }}</dd>
        </dl>
        <dl>
          <dt>SupplementaryInformation</dt>
          <dd>{{ collection.supplementaryInformation }}</dd>
        </dl>
      </div>
    </div>
  </div>
</template>
<script>
import { request } from "graphql-request";
import { MessageError, ReadMore } from "@mswertz/emx2-styleguide";
import DatasetList from "./DatasetList";
import CollectionTabs from "./CollectionTabs";

export default {
  components: {
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
        `query Collections($acronym:String){Collections(filter:{acronym:{equals:[$acronym]}}){name,acronym,type{name},organisation,description,website, investigators{name}, supplementaryInformation}}`,
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
