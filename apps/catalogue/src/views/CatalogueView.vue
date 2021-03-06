<template>
  <div class="container bg-white p-3">
    <h1>Health data catalogue</h1>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <p>
      Browse and manage metadata for human research data resources, such as
      cohorts, registries, biobanks, and multi-center studies thereof, such as
      EU projects and harmonisations studies. This catalogue software has been
      made possible by contributions from H2020 EUCAN-connect, LifeCycle,
      Longitools and ATHLETE as well as IMI Conception and EMA.
    </p>
    <h2>Data collections</h2>
    <div class="row justify-content-between mt-4 mb-4 p-3">
      <RouterLink to="list/Institutions" class="btn btn-info col-3">
        <span class="badge badge-light">{{ institutions }}</span>
        <h3>Institutions</h3>
        <p class="text-left">
          Universities, Companies, Medical Centers and Research Institutes
        </p>
      </RouterLink>
      <RouterLink to="/list/Datasources" class="btn btn-warning col-3">
        <span class="badge badge-light">{{ datasources }}</span>
        <h3>Datasources</h3>
        <p class="text-left">
          Families of databanks collected from same population
        </p>
      </RouterLink>
      <RouterLink to="/list/Databanks" class="btn btn-danger col-3">
        <span class="badge badge-light">{{ databanks }}</span>
        <h3>Databanks</h3>
        <p class="text-left">Such as Cohorts, Registries, Biobanks</p>
      </RouterLink>
    </div>
    <h2>Data use</h2>
    <div class="row justify-content-around mt-4 mb-4">
      <RouterLink to="/list/Networks" class="btn btn-primary col-3">
        <span class="badge badge-light">{{ networks }}</span>
        <h3>Networks</h3>
        <p class="text-left">
          Collaborations of multiple intitutions, datasources and/or databanks.
        </p>
      </RouterLink>
      <RouterLink to="/list/Models" class="btn btn-secondary col-3">
        <span class="badge badge-light">{{ models }}</span>
        <h3>Common Models</h3>
        <p class="text-left">
          Common Data Element models and Harmonisation models
        </p>
      </RouterLink>
      <RouterLink to="/list/Studies" class="btn btn-success col-3">
        <span class="badge badge-light">{{ studies }}</span>
        <h3>Studies</h3>
        <p class="text-left">
          Collaborations of multiple intitutions, datasources and/or databanks.
        </p>
      </RouterLink>
    </div>
    <h2>Browse by institute and contacts</h2>
    <div class="row justify-content-around container-fluid mt-4 mb-4">
      <RouterLink to="list/Contacts" class="btn btn-info col-2">
        <span class="badge badge-light">{{ institutions }}</span>
        <h3>Contacts</h3>
        <p class="text-left">Researchers, data managers,</p>
      </RouterLink>
      <RouterLink to="list/Affiliations" class="btn btn-info col-2">
        <span class="badge badge-light">{{ affiliations }}</span>
        <h3>Affiliations</h3>
        <p class="text-left">Departments, divisions and research groups.</p>
      </RouterLink>
    </div>
    <h2>Browse data definitions</h2>
    <div class="row justify-content-between container-fluid mt-4 mb-4">
      <RouterLink to="/list/Releases" class="btn btn-outline-dark col-2">
        <span class="badge badge-light">{{ releases }}</span>
        <h3>Releases</h3>
        <p class="text-left">
          Data releases from databanks, models or networks.
        </p>
      </RouterLink>
      <RouterLink to="/list/Tables" class="btn btn-outline-dark col-2">
        <span class="badge badge-light">{{ tables }}</span>
        <h3>Tables</h3>
        <p class="text-left">
          Raw listing of all tables described across all releases of all
          databanks and common models.
        </p>
      </RouterLink>
      <RouterLink to="/list/Variables" class="btn btn-outline-dark col-2">
        <span class="badge badge-light">{{ variables }}</span>
        <h3>Variables</h3>
        <p class="text-left">
          Raw listing of all variables described across all releases of all
          databanks and common models.
        </p>
      </RouterLink>
      <RouterLink to="/list/TableMappings" class="btn btn-outline-dark col-2">
        <span class="badge badge-light">{{ tableMappings }}</span>
        <h3>Table Mappings</h3>
        <p class="text-left">
          Raw listing of all mappings described between tables in databanks and
          those in common data models.
        </p>
      </RouterLink>
      <RouterLink
        to="/list/VariableMappings"
        class="btn btn-outline-dark col-2"
      >
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
      affiliations: null,
    };
  },
  methods: {
    reload() {
      request(
        "graphql",
        `query {Institutions_agg{count}, Databanks_agg{count},Datasources_agg{count},Networks_agg{count},Tables_agg{count},Models_agg{count},Studies_agg{count} Releases_agg{count}, Affiliations_agg{count}, Variables_agg{count},VariableMappings_agg{count}, TableMappings_agg{count}}`
      )
        .then((data) => {
          this.institutions = data.Institutions_agg.count;
          this.databanks = data.Databanks_agg.count;
          this.networks = data.Networks_agg.count;
          this.models = data.Models_agg.count;
          this.datasources = data.Datasources_agg.count;
          this.releases = data.Releases_agg.count;
          this.tables = data.Tables_agg.count;
          this.variables = data.Variables_agg.count;
          this.variableMappings = data.VariableMappings_agg.count;
          this.tableMappings = data.TableMappings_agg.count;
          this.studies = data.Studies_agg.count;
          this.affiliations = data.Affiliations_agg.count;
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
