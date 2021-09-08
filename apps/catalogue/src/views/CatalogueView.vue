<template>
  <div class="container bg-white p-3">
    <h1>Data catalogue</h1>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <p>
      Browse and manage metadata for data resources, such as cohorts,
      registries, biobanks, and multi-center studies thereof, such as EU
      projects and harmonisations studies. This catalogue software has been made
      possible by contributions from H2020 EUCAN-connect, LifeCycle, Longitools
      and ATHLETE as well as IMI Conception and EMA.
    </p>
    <h2>Collected data</h2>
    <div class="row justify-content-between">
      <RouterLink to="institutions" class="btn btn-dark col m-2">
        <span class="badge badge-light">{{ institutions }}</span>
        <h3>Institutions</h3>
        <p class="text-left">
          Contributors to the catalogue such as universities, companies, medical
          centres and research institutes
        </p>
      </RouterLink>
      <RouterLink to="datasources" class="btn btn-secondary col m-2">
        <span class="badge badge-light">{{ datasources }}</span>
        <h3>Data sources</h3>
        <p class="text-left">
          Collections of data banks covering the same population
        </p>
      </RouterLink>
      <RouterLink to="databanks" class="btn btn-info col m-2">
        <span class="badge badge-light">{{ databanks }}</span>
        <h3>Data banks</h3>
        <p class="text-left">Data collections such as registries or biobanks</p>
      </RouterLink>
      <RouterLink
        to="cohorts"
        class="btn btn-primary col m-2"
        v-if="cohorts > 0"
      >
        <span class="badge badge-light">{{ cohorts }}</span>
        <h3>Cohorts</h3>
        <p class="text-left">
          Systematic observations of large groups of individuals over time.
        </p>
      </RouterLink>
    </div>
    <h2>Data use</h2>
    <div class="row justify-content-around mt-4">
      <RouterLink to="networks" class="btn btn-danger col-3 m-2">
        <span class="badge badge-light">{{ networks }}</span>
        <h3>Networks</h3>
        <p class="text-left">Collaborations of multiple institutions</p>
      </RouterLink>
      <RouterLink to="models" class="btn btn-warning col-3 m-2">
        <span class="badge badge-light">{{ models }}</span>
        <h3>Common data models</h3>
        <p class="text-left">
          Common Data Element models and Harmonization models
        </p>
      </RouterLink>
      <RouterLink to="studies" class="btn btn-success col-3 m-2">
        <span class="badge badge-light">{{ studies }}</span>
        <h3>Studies</h3>
        <p class="text-left">
          Collaborations of multiple institutions, addressing research questions
          using data sources and/or data banks
        </p>
      </RouterLink>
    </div>
    <h2>Browse data definitions</h2>
    <div class="row justify-content-between container-fluid mt-4 mb-4">
      <RouterLink to="releases" class="btn btn-outline-dark col-2">
        <span class="badge badge-light">{{ releases }}</span>
        <h3>Releases</h3>
        <p class="text-left">
          Data releases from databanks, models or networks.
        </p>
      </RouterLink>
      <RouterLink to="tables" class="btn btn-outline-dark col-2">
        <span class="badge badge-light">{{ tables }}</span>
        <h3>Tables</h3>
        <p class="text-left">
          Raw listing of all tables described across all releases of all
          databanks and common models.
        </p>
      </RouterLink>
      <RouterLink to="variables" class="btn btn-outline-dark col-2">
        <span class="badge badge-light">{{ variables }}</span>
        <h3>Variables</h3>
        <p class="text-left">
          Raw listing of all variables described across all releases of all
          databanks and common models.
        </p>
      </RouterLink>
      <RouterLink to="tablemappings" class="btn btn-outline-dark col-2">
        <span class="badge badge-light">{{ tableMappings }}</span>
        <h3>Table Mappings</h3>
        <p class="text-left">
          Raw listing of all mappings described between tables in databanks and
          those in common data models.
        </p>
      </RouterLink>
      <RouterLink to="/variable-explorer" class="btn btn-outline-dark col-2">
        <span class="badge badge-light">{{ variableMappings }}</span>
        <h3>Variable Mappings</h3>
        <p class="text-left">
          List of all mappings described variables in databanks and those in
          common data models.
        </p>
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
      cohorts: null,
      networks: null,
      projects: null,
      models: null,
      datasources: null,
      variables: null,
      tables: null,
      graphqlError: null,
      releases: null,
      variableMappings: null,
      tableMappings: null,
      studies: null,
    };
  },
  methods: {
    reload() {
      request(
        "graphql",
        `query {Institutions_agg{count},Cohorts_agg{count},Databanks_agg{count},Datasources_agg{count},Networks_agg{count},Tables_agg{count},Models_agg{count},Studies_agg{count} Releases_agg{count}, Variables_agg{count},VariableMappings_agg{count}, TableMappings_agg{count}}`
      )
        .then((data) => {
          this.institutions = data.Institutions_agg.count;
          this.databanks = data.Databanks_agg.count;
          this.cohorts = data.Cohorts_agg.count;
          this.networks = data.Networks_agg.count;
          this.models = data.Models_agg.count;
          this.datasources = data.Datasources_agg.count;
          this.releases = data.Releases_agg.count;
          this.tables = data.Tables_agg.count;
          this.variables = data.Variables_agg.count;
          this.variableMappings = data.VariableMappings_agg.count;
          this.tableMappings = data.TableMappings_agg.count;
          this.studies = data.Studies_agg.count;
        })
        .catch((error) => {
          this.graphqlError = error.response.errors[0].message;
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
