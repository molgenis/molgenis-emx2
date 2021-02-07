<template>
  <div>
    <h1>MOLGENIS Catalogue</h1>
    <MessageError v-if="error">{{ error }}</MessageError>
    <p>
      MOLGENIS catalogue is a user friendly system to browse and manage metadata
      for human research data resources, such as cohorts, registries, biobanks,
      and multi-center studies thereof, such as EU projects and harmonisations
      studies.
    </p>
    <h2>Where do you want to start today?</h2>
    <div class="row justify-content-between container-fluid mt-4 mb-4">
      <RouterLink to="institutions" class="btn btn-primary col-2">
        <span class="badge badge-light">{{ institutions }}</span>
        <h3>Institutions</h3>
        <p class="text-left">Such as Universities, Companies, institutions</p>
      </RouterLink>
      <RouterLink to="databanks" class="btn btn-warning col-2">
        <span class="badge badge-light">{{ databanks }}</span>
        <h3>Databanks</h3>
        <p class="text-left">Such as Cohorts, Registries, Biobanks</p>
      </RouterLink>
      <RouterLink to="projects" class="btn btn-success col-2">
        <span class="badge badge-light">{{ projects }}</span>
        <h3>Projects</h3>
        <p class="text-left">Collaborations of multiple databanks.</p>
      </RouterLink>
      <RouterLink to="tables" class="btn btn-info col-2">
        <span class="badge badge-light">{{ tables }}</span>
        <h3>Tables</h3>
        <p class="text-left">
          Tables within either Databanks or as defined by Consortia.
        </p>
      </RouterLink>
      <RouterLink to="variables" class="btn btn-danger col-2">
        <span class="badge badge-light">{{ variables }}</span>
        <h3>Variables</h3>
        <p class="text-left">
          The atomics data items made available within collected or harmonised
          tables.
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
