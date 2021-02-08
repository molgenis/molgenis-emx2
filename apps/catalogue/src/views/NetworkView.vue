<template>
  <div>
    <h1>Catalogue</h1>
    <MessageError v-if="error">{{ error }}</MessageError>
    <p>
      MOLGENIS network catalogue is a user friendly system to browse and manage
      metadata for human research data resources, such as cohorts, registries,
      biobanks, and multi-center studies thereof, such as EU projects and
      harmonisations studies.
    </p>
    <h2>Where do you want to start today?</h2>
    <div class="row justify-content-between container-fluid mt-4 mb-4">
      <RouterLink to="institutions" class="btn btn-info col-2 pt-4">
        <span class="badge badge-light">{{ institutions }}</span>
        <h3>Institutions</h3>
        <p class="text-left">
          Such as Universities, Companies, Hospitals, Governemental bodies.
        </p>
      </RouterLink>
      <RouterLink to="databanks" class="btn btn-danger col-2 pt-4">
        <span class="badge badge-light">{{ databanks }}</span>
        <h3>Databanks</h3>
        <p class="text-left">
          Collected data such as Cohorts, Registries, Biobanks
        </p>
      </RouterLink>
      <RouterLink to="variables" class="btn btn-warning col-2 pt-4">
        <span class="badge badge-light">{{ variables }}</span>
        <h3>Variables</h3>
        <p class="text-left">Variables as collected within databanks.</p>
      </RouterLink>
      <RouterLink to="variables" class="btn btn-success col-2 pt-4">
        <span class="badge badge-light">{{ variables }}</span>
        <h3>Harmonisations</h3>
        <p class="text-left">
          Collections of harmonized variables such as standards, common data
          elements.
        </p>
      </RouterLink>
      <RouterLink to="projects" class="btn btn-primary col-2 pt-4">
        <span class="badge badge-light">{{ projects }}</span>
        <h3>Projects</h3>
        <p class="text-left">Where harmonized data is used for research</p>
      </RouterLink>
    </div>
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
      institutions: null,
      databanks: null,
      projects: null,
      tables: null,
      variables: null,
      error: null,
    };
  },
  methods: {
    reload() {
      request(
        "graphql",
        `query {Institutions_agg{count},Databanks_agg{count},Projects_agg{count},Tables_agg{count},Variables_agg{count}}`
      )
        .then((data) => {
          this.institutions = data.Institutions_agg.count;
          this.databanks = data.Databanks_agg.count;
          this.projects = data.Projects_agg.count;
          this.tables = data.Tables_agg.count;
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
