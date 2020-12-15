<template>
  <div class="height:100%; overflow-y: scroll">
    <MessageError v-if="error">{{ error }}</MessageError>
    <ul class="nav flex-column">
      Collections & datasets:
      <li v-for="collection in collections" class="nav-item border-top">
        <RouterLink
          class="nav-link"
          :class="{
            'font-weight-bold text-secondary':
              collection.acronym === collectionAcronym,
          }"
          :to="{
            name: 'collection-description',
            params: { collectionAcronym: collection.acronym },
          }"
        >
          {{ collection.acronym }}
        </RouterLink>
        <ul
          v-if="collectionAcronym == collection.acronym"
          class="nav flex-column"
        >
          <li v-for="dataset in collection.datasets" class="nav-item pl-4">
            <div class="nav-link">
              <RouterLink
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
                <span
                  class="ml-2 fa-stack has-badge"
                  :data-count="
                    Array.isArray(dataset.variables)
                      ? dataset.variables
                          .map((v) =>
                            v.harmonisations_agg !== undefined
                              ? v.harmonisations_agg.count
                              : 0
                          )
                          .reduce((a, b) => a + b, 0)
                      : 0
                  "
                >
                  <i class="fa fa-check-circle fa-stack-2x"
                /></span>
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
                <span
                  class="ml-2 fa-stack has-badge"
                  :data-count="dataset.variables_agg.count"
                >
                  <i class="fa fa-table fa-stack-2x"></i>
                </span>
              </RouterLink>
            </div>
          </li>
        </ul>
      </li>
    </ul>
  </div>
</template>

<style>
.fa-stack[data-count]:after {
  position: absolute;
  right: 0%;
  top: 1%;
  content: attr(data-count);
  font-size: 60%;
  padding: 0.6em;
  border-radius: 999px;
  line-height: 0.75em;
  color: white;
  background: rgba(255, 0, 0, 0.85);
  text-align: center;
  min-width: 2em;
  font-weight: bold;
}
</style>

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
      request(
        "graphql",
        `{Collections{acronym,name,datasets{name,label,variables_agg{count},variables{harmonisations_agg{count}}}}}`
      )
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
