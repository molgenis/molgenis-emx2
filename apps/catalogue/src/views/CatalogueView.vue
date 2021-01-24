<template>
  <div>
    <h1>MOLGENIS Catalogue</h1>
    <MessageError v-if="error">{{ error }}</MessageError>
    <p>
      MOLGENIS catalogue is a user friendly system to browse and manage metadata
      for human research data collections, such as cohorts, registries,
      biobanks, and multi-center studies thereof, such as EU consortia and
      harmonisations studies.
    </p>
    <h2>Where do you want to start today?</h2>
    <div class="row justify-content-between container-fluid mt-4 mb-4">
      <RouterLink to="providers" class="btn btn-primary col-2">
        <span class="badge badge-light">{{ providers }}</span>
        <h3>Providers</h3>
        <p class="text-left">
          Universities, Companies, Institutes providing access to data resources
          (collections, harmonisation networks)
        </p>
      </RouterLink>
      <RouterLink to="collections" class="btn btn-warning col-2">
        <span class="badge badge-light">{{ collections }}</span>
        <h3>Collections</h3>
        <p class="text-left">
          Data resources collected data in Cohorts, Registries, Biobanks
        </p>
      </RouterLink>
      <RouterLink to="networks" class="btn btn-success col-2">
        <span class="badge badge-light">{{ networks }}</span>
        <h3>Networks</h3>
        <p class="text-left">
          Data resources pooled and/or harmonised data derived multiple
          collected collections, in context of consortia and multi-center
          studies.
        </p>
      </RouterLink>
      <RouterLink to="datasets" class="btn btn-info col-2">
        <span class="badge badge-light">{{ datasets }}</span>
        <h3>Datasets</h3>
        <p class="text-left">
          Datasets as made available within either collections or networks, e.g.
          listing observations on patients, general population, medicine intake,
          etc
        </p>
      </RouterLink>
      <RouterLink to="variables" class="btn btn-danger col-2">
        <span class="badge badge-light">{{ variables }}</span>
        <h3>Variables</h3>
        <p class="text-left">
          The atomics data items made available within collected or harmonised
          datasets.
        </p>
      </RouterLink>
    </div>
    <h2>Explanation</h2>
    <p>
      An overview of the relationships between contents of this catalogue is
      shown below:
    </p>
    <img src="diagram.svg" />
    <p>
      <a
        href="https://docs.google.com/presentation/d/1LLhHa_Y3D6oTyurtAoh7fG5caqXndjgBCNIkT124g70/edit#slide=id.p"
      >
        source
      </a>
    </p>
  </div>
</template>

<style scoped>
.btn .badge {
  position: absolute;
  top: 5px;
  right: 5px;
}
</style>

<script>
import { request } from "graphql-request";
import { MessageError } from "@mswertz/emx2-styleguide";

export default {
  components: {
    MessageError,
  },
  data() {
    return {
      providers: null,
      collections: null,
      networks: null,
      datasets: null,
      variables: null,
      error: null,
    };
  },
  methods: {
    reload() {
      request(
        "graphql",
        `query {Providers_agg{count},Collections_agg{count},Networks_agg{count},Datasets_agg{count},Variables_agg{count}}`
      )
        .then((data) => {
          this.providers = data.Providers_agg.count;
          this.collections = data.Collections_agg.count;
          this.networks = data.Networks_agg.count;
          this.datasets = data.Datasets_agg.count;
          this.variables = data.Variables_agg.count;
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
