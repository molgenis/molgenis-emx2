<template>
  <p v-if="count == 0">No variables found</p>
  <div v-else class="mt-2">
    <Pagination class="mb-2" :count="count" :limit="limit" v-model="page" />
    <MessageError v-if="error">{{ error }}</MessageError>
    <div class="card-columns">
      <div class="card" v-for="variable in variables">
        <div class="card-body">
          <h5 class="card-title">{{ variable.name }}</h5>
          <p class="cart-text">
            <i>{{ variable.label }}</i>
          </p>
          <dl class="card-text">
            <div v-if="!datasetName">
              <dt>dataset</dt>
              <dd>
                <RouterLink
                  :to="{
                    name: 'dataset-description',
                    params: {
                      collectionAcronym: variable.dataset.collection.acronym,
                      datasetName: variable.dataset.name,
                    },
                  }"
                >
                  {{ variable.dataset.collection.acronym }}:{{
                    variable.dataset.name
                  }}
                </RouterLink>
              </dd>
            </div>
            <dt>format</dt>
            <dd>
              {{ variable.format ? variable.format.name : "N/A" }}
            </dd>
            <dt>unit</dt>
            <dd>{{ variable.unit ? variable.unit.name : "N/A" }}</dd>
            <span v-if="variable.description">
              <dt>description</dt>
              <dd>
                {{ variable.description }}
              </dd>
            </span>
            <span v-if="variable.valueLabels">
              <dt>values</dt>
              <dd class="border p-1" style="background: #eee">
                <div v-for="v in variable.valueLabels">{{ v }}</div>
              </dd>
            </span>
            <span v-if="variable.missingValues">
              <dt>missing</dt>
              <dd class="border p-1" style="background: #eee">
                <div v-for="v in variable.missingValues">{{ v }}</div>
              </dd>
            </span>
            <span v-if="variable.harmonisations">
              <dt>harmo</dt>
              <dd class="p-1">
                <HarmonisationDetails
                  v-for="h in variable.harmonisations"
                  :sourceCollection="h.sourceDataset.collection.acronym"
                  :source-dataset="h.sourceDataset.name"
                  :target-collection="variable.dataset.collection.acronym"
                  :target-dataset="variable.dataset.name"
                  :target-variable="variable.name"
                  :match="variable.match ? variable.match.name : 'unknown'"
                />
              </dd>
            </span>
          </dl>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
dt {
  float: left;
  clear: left;
  width: 100px;
  font-weight: bold;
}

dd {
  margin: 0 0 0 110px;
  padding: 0 0 0.5em 0;
}
</style>

<script>
import { request } from "graphql-request";
import { MessageError, Pagination } from "@mswertz/emx2-styleguide";
import HarmonisationDetails from "./HarmonisationDetails";

export default {
  components: {
    HarmonisationDetails,
    Pagination,
    MessageError,
  },
  props: {
    collectionAcronym: String,
    datasetName: String,
  },
  data() {
    return {
      variables: [],
      count: 0,
      error: null,
      page: 1,
      limit: 20,
    };
  },
  methods: {
    reload() {
      let filter = {};
      if (this.collectionAcronym) {
        filter.collection = { acronym: { equals: this.collectionAcronym } };
      }
      if (this.datasetName) {
        filter.dataset = { name: { equals: this.datasetName } };
      }
      console.log(JSON.stringify(filter));
      request(
        "graphql",
        `query Variables($filter:VariablesFilter,$offset:Int,$limit:Int){Variables(offset:$offset,limit:$limit,filter:$filter){name, dataset{name,collection{acronym}},label, format{name},unit{name}, description,harmonisations{match{name},sourceDataset{name,collection{acronym}}}}
        ,Variables_agg(filter:$filter){count}}`,
        {
          filter: filter,
          offset: (this.page - 1) * 10,
          limit: this.limit,
        }
      )
        .then((data) => {
          this.variables = data.Variables;
          this.count = data.Variables_agg.count;
        })
        .catch((error) => {
          this.error = error.response.errors[0].message;
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
  watch: {
    collectionAcronym() {
      this.reload();
    },
    datasetName() {
      this.reload();
    },
    page() {
      this.reload();
    },
  },
  created() {
    this.reload();
  },
};
</script>
