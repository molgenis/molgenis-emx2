<template>
  <div class="container bg-white p-3">
    <h1>
      <span v-if="schemaName && schemaName == 'Minerva'">
        Proof-of-concept catalogue tool for MINERVA metadata pilot</span
      >
      <span v-else>Data catalogue</span>
    </h1>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <p v-if="schemaName && schemaName == 'Minerva'" class="text-danger">
      Disclaimer: contents not for public disclosure.
    </p>
    <p>
      Browse and manage metadata for data resources, such as cohorts,
      registries, biobanks, and multi-center collaborations thereof such as
      networks, common data models and studies.
    </p>
    <h2>Metadata on data collections</h2>
    <div class="card-columns">
      <RouterLink
        to="Institutions"
        class="card card-body bg-dark text-white card-height"
      >
        <span class="badge badge-light float-right">{{ institutions }}</span>
        <h3>Institutions</h3>
        <p class="text-left">
          Contributors to the catalogue such as universities, companies, medical
          centres and research institutes
        </p>
      </RouterLink>
      <RouterLink
        to="Networks"
        class="card card-body bg-danger text-white card-height"
      >
        <span class="badge badge-light float-right">{{ networks }}</span>
        <h3>Networks</h3>
        <p class="text-left">Collaborations of multiple institutions</p>
      </RouterLink>
      <RouterLink
        to="Datasources"
        class="card card-body bg-primary text-white card-height"
      >
        <span class="badge badge-light float-right">{{ datasources }}</span>
        <h3>Data sources</h3>
        <p class="text-left">
          Collections of data banks covering the same population
        </p>
      </RouterLink>

      <RouterLink
        to="Cohorts"
        class="card card-body bg-primary text-white card-height"
        v-if="cohorts > 0"
      >
        <span class="badge badge-light float-right">{{ cohorts }}</span>
        <h3>Cohorts</h3>
        <p class="text-left">
          Systematic observations of large groups of individuals over time.
        </p>
      </RouterLink>

      <RouterLink
        to="Databanks"
        class="card card-body bg-info text-white card-height"
      >
        <span class="badge badge-light float-right">{{ databanks }}</span>
        <h3>Data banks</h3>
        <p class="text-left">Data collections such as registries or biobanks</p>
      </RouterLink>
      <RouterLink
        to="Studies"
        class="card card-body bg-success text-white card-height"
      >
        <span class="badge badge-light float-right">{{ studies }}</span>
        <h3>Studies</h3>
        <p class="text-left">
          Collaborations of multiple institutions, addressing research questions
          using data sources and/or data banks
        </p>
      </RouterLink>
    </div>

    <h2>Browse data definitions</h2>
    <div class="card-columns">
      <div class="card card-body border border-dark rounded card-height">
        <h3>Collected data dictionaries</h3>
        <div class="text-left">
          Data dictionaries of collected data in databanks and/or cohorts.
          <ul>
            <li>
              <RouterLink to="source-data-dictionaries">
                Source Data dictionaries ({{ sourceDataDictionaries }})
              </RouterLink>
            </li>
            <li>
              <RouterLink to="source-tables">
                Source Tables ({{ sourceTables }})
              </RouterLink>
            </li>
            <li>
              <RouterLink to="source-variables">
                Source Variables ({{ sourceVariables }})
              </RouterLink>
            </li>
          </ul>
        </div>
      </div>
      <div class="card card-body border border-dark rounded card-height">
        <h3>Common data models</h3>
        <div class="text-left">
          Data dictionaries of standards for integrated analysis
          <ul>
            <li>
              <RouterLink to="target-data-dictionaries">
                Target Data dictionaries ({{ targetDataDictionaries }})
              </RouterLink>
            </li>
            <li>
              <RouterLink to="target-tables">
                Target Tables ({{ targetTables }})
              </RouterLink>
            </li>
            <li>
              <RouterLink to="target-variables">
                Target Variables ({{ targetVariables }})
              </RouterLink>
            </li>
          </ul>
        </div>
      </div>
      <div class="card card-body border border-dark card-height">
        <h3>Data model mappings</h3>
        <div class="text-left">
          Mappings between collected data dictionaries and standard models
          <ul>
            <li>
              <RouterLink to="variable-mappings">
                Variable mappings ({{ variableMappings }})
              </RouterLink>
            </li>
            <li>
              <RouterLink to="table-mappings">
                Table mappings ({{ tableMappings }})
              </RouterLink>
            </li>
          </ul>
        </div>
      </div>
    </div>
    <p>
      This catalogue software has been made possible by contributions from H2020
      EUCAN-connect, CINECA, LifeCycle, Longitools and ATHLETE, members of
      European Human Exposome Network, BBMRI-ERIC, IMI Conception and EMA
      Minerva.
    </p>
  </div>
</template>

<style scoped>
.btn .badge {
  position: absolute;
  top: 5px;
  right: 5px;
}

.card-height {
  min-height: 200px;
}
</style>

<script>
import { request } from "graphql-request";
import { MessageError } from "molgenis-components";

export default {
  components: {
    MessageError,
  },
  data() {
    return {
      schemaName: null,
      institutions: null,
      databanks: null,
      cohorts: null,
      networks: null,
      projects: null,
      models: null,
      datasources: null,
      sourceVariables: null,
      targetVariables: null,
      sourceTables: null,
      targetTables: null,
      graphqlError: null,
      sourceDataDictionaries: null,
      targetDataDictionaries: null,
      variableMappings: null,
      tableMappings: null,
      studies: null,
    };
  },
  methods: {
    reload() {
      request(
        "graphql",
        `query {_schema{name}, Institutions_agg{count}, Studies_agg{count}, Cohorts_agg{count},Databanks_agg{count},Datasources_agg{count},Networks_agg{count},SourceTables_agg{count},TargetTables_agg{count},Models_agg{count},Studies_agg{count} SourceDataDictionaries_agg{count},TargetDataDictionaries_agg{count}, SourceVariables_agg{count},TargetVariables_agg{count},VariableMappings_agg{count}, TableMappings_agg{count}}`
      )
        .then((data) => {
          this.schemaName = data._schema.name;
          this.institutions = data.Institutions_agg.count;
          this.databanks = data.Databanks_agg.count;
          this.cohorts = data.Cohorts_agg.count;
          this.networks = data.Networks_agg.count;
          this.datasources = data.Datasources_agg.count;
          this.sourceDataDictionaries = data.SourceDataDictionaries_agg.count;
          this.sourceTables = data.SourceTables_agg.count;
          this.sourceVariables = data.SourceVariables_agg.count;
          this.models = data.Models_agg.count;
          this.targetDataDictionaries = data.TargetDataDictionaries_agg.count;
          this.targetTables = data.TargetTables_agg.count;
          this.targetVariables = data.TargetVariables_agg.count;
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
