<template>
  <div class="position:absolute; overflow-y: scroll">
    <MessageError v-if="error">{{ error }}</MessageError>
    <p>Collections:</p>
    <ul class="nav flex-column">
      <li v-for="collection in collections" class="nav-item border-top">
        <RouterLink
          class="nav-link"
          :class="{
            'font-weight-bold text-secondary':
              collection.acronym === collectionAcronym,
          }"
          :to="{
            name: 'collection',
            params: { collectionAcronym: collection.acronym },
          }"
        >
          {{ collection.acronym }}
        </RouterLink>
        <ul
          v-if="collectionAcronym == collection.acronym"
          class="nav flex-column"
        >
          <li class="nav-item nav-link">Datasets:</li>
          <li v-for="dataset in collection.datasets" class="nav-item pl-4">
            <div class="nav-link">
              <RouterLink
                :class="{
                  'font-weight-bold  text-secondary':
                    dataset.name === datasetName,
                }"
                :to="{
                  name: 'dataset-variables',
                  params: {
                    collectionAcronym: collection.acronym,
                    datasetName: dataset.name,
                  },
                }"
              >
                {{ dataset.name }}
              </RouterLink>

              <RouterLink
                class="float-right"
                :class="{
                  'font-weight-bold  text-secondary':
                    dataset.name === datasetName,
                }"
                :to="{
                  name: 'dataset-harmonisations',
                  params: {
                    collectionAcronym: collection.acronym,
                    datasetName: dataset.name,
                  },
                }"
              >
                <i class="ml-2 fa fa-check-circle" />
              </RouterLink>
              <RouterLink
                class="float-right"
                :class="{
                  'font-weight-bold  text-secondary':
                    dataset.name === datasetName,
                }"
                :to="{
                  name: 'dataset-description',
                  params: {
                    collectionAcronym: collection.acronym,
                    datasetName: dataset.name,
                  },
                }"
              >
                <i class="ml-2 fa fa-question-circle" />
              </RouterLink>
              <RouterLink
                class="float-right"
                :class="{
                  'font-weight-bold  text-secondary':
                    dataset.name === datasetName,
                }"
                :to="{
                  name: 'dataset-variables',
                  params: {
                    collectionAcronym: collection.acronym,
                    datasetName: dataset.name,
                  },
                }"
              >
                <i class="ml-2 fa fa-table" />
              </RouterLink>
            </div>
          </li>
        </ul>
      </li>
    </ul>
  </div>
</template>

<script>
import { request } from "graphql-request";
import { MessageError } from "@mswertz/emx2-styleguide";

export default {
  components: {
    MessageError,
  },
  props: {
    collectionAcronym: String,
    datasetName: String,
  },
  data() {
    return {
      error: null,
      collections: [],
    };
  },
  methods: {
    reload() {
      console.log("collections reload");
      request("graphql", `{Collections{acronym,name,datasets{name,label}}}`)
        .then((data) => {
          this.collections = data.Collections;
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
};
</script>
