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
    <div v-if="loading">
      <Spinner />
    </div>
    <div v-else>
      <h2>Metadata on data collections</h2>
      <div class="row">
        <div class="col-auto mb-3">
          <RouterLink
            to="organisations"
            class="card card-body bg-dark text-white card-height h-100"
          >
            <h3>
              Organisations
              <span class="badge badge-light text-inline float-right">{{
                organisations
              }}</span>
            </h3>
            <p class="text-left">
              Contributors to the catalogue such as departments from
              universities, companies, medical centres and research institutes
            </p>
          </RouterLink>
        </div>
        <div class="col-auto mb-3">
          <RouterLink
            to="datasources"
            class="card card-body bg-primary text-white card-height h-100"
          >
            <h3>
              Data sources
              <span class="badge badge-light float-right">{{
                datasources
              }}</span>
            </h3>
            <p class="text-left">
              Collections of data banks covering the same population
            </p>
          </RouterLink>
        </div>
        <div class="col-auto mb-3" v-if="cohorts > 0">
          <RouterLink
            to="cohorts"
            class="card card-body bg-primary text-white card-height h-100"
          >
            <h3>
              Cohorts
              <span class="badge badge-light float-right">{{ cohorts }}</span>
            </h3>
            <p class="text-left">
              Systematic observations of large groups of individuals over time.
            </p>
          </RouterLink>
        </div>
        <div class="col-auto mb-3">
          <RouterLink
            to="databanks"
            class="card card-body bg-info text-white card-height h-100"
          >
            <h3>
              Databanks
              <span class="badge badge-light float-right">{{ databanks }}</span>
            </h3>
            <p class="text-left">
              Collections of data banks covering the same population
            </p>
          </RouterLink>
        </div>
        <div class="col-auto mb-3">
          <RouterLink
            to="networks"
            class="card card-body bg-danger text-white card-height h-100"
          >
            <h3>
              Networks
              <span class="badge badge-light float-right">{{ networks }}</span>
            </h3>
            <p class="text-left">Collaborations of multiple organisations</p>
          </RouterLink>
        </div>
        <div class="col-auto mb-3">
          <RouterLink
            to="studies"
            class="card card-body bg-success text-white card-height h100"
          >
            <h3>
              Studies
              <span class="badge badge-light float-right">{{ studies }}</span>
            </h3>
            <p class="text-left">
              Collaborations of multiple institutions, addressing research
              questions using data sources and/or data banks
            </p>
          </RouterLink>
        </div>
        <div class="col-auto mb-3">
          <RouterLink
            to="models"
            class="card card-body bg-warning text-dark card-height h-100"
          >
            <h3>
              Data models
              <span class="badge badge-light float-right">{{ models }}</span>
            </h3>
            <p class="text-left">
              Standard/harmonized data dictionaries for integrated analysis
            </p>
          </RouterLink>
        </div>
      </div>

      <h2>Browse data definitions</h2>
      <div class="row">
        <div class="col-auto mb-3">
          <div class="card card-body border border-dark rounded card-height">
            <h3>Dataset and variable definitions</h3>
            <div class="text-left">
              <ul>
                <li>
                  <RouterLink to="datasets">
                    Datasets ({{ datasets }})</RouterLink
                  >
                </li>
                <li>
                  <RouterLink to="variables">
                    Variables ({{ variables }})
                  </RouterLink>
                </li>
              </ul>
            </div>
          </div>
        </div>
        <div class="col-auto mb-3">
          <div class="card card-body border border-dark card-height">
            <h3>Mappings to standards</h3>
            <div class="text-left">
              <ul>
                <li>
                  <RouterLink to="dataset-mappings">
                    Dataset mappings ({{ datasetMappings }})
                  </RouterLink>
                </li>
                <li>
                  <RouterLink to="variable-mappings">
                    Variable mappings ({{ variableMappings }})
                  </RouterLink>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
      <p>
        This catalogue software has been made possible by contributions from
        H2020 EUCAN-connect, CINECA, LifeCycle, Longitools and ATHLETE, members
        of European Human Exposome Network, BBMRI-ERIC, IMI Conception and EMA
        Minerva.
      </p>
    </div>
  </div>
</template>

<style scoped>
.card {
  width: 20rem;
}

.badge {
  font-size: small;
  vertical-align: top;
  top: -10px;
  right: -10px;
  position: relative;
}

.btn {
  position: absolute;
  top: 5px;
  right: 5px;
}

.card-height {
  min-height: 200px;
}
</style>

<script>
import { request, gql } from "graphql-request";
import { MessageError, Spinner } from "molgenis-components";

export default {
  components: {
    MessageError,
    Spinner,
  },
  data() {
    return {
      loading: true,
      schemaName: null,
      organisations: null,
      cohorts: null,
      networks: null,
      projects: null,
      models: null,
      datasources: null,
      databanks: null,
      datasets: null,
      variables: null,
      graphqlError: null,
      variableMappings: null,
      datasetMappings: null,
      studies: null,
    };
  },
  methods: {
    reload() {
      request(
        "graphql",
        gql`
          query {
            _schema {
              name
            }
            Organisations_agg {
              count
            }
            Studies_agg {
              count
            }
            Cohorts_agg {
              count
            }
            DataSources_agg {
              count
            }
            Databanks_agg {
              count
            }
            Networks_agg {
              count
            }
            Datasets_agg {
              count
            }
            Models_agg {
              count
            }
            Studies_agg {
              count
            }
            VariableMappings_agg {
              count
            }
            Variables_agg {
              count
            }
            DatasetMappings_agg {
              count
            }
          }
        `
      )
        .then((data) => {
          this.schemaName = data._schema.name;
          this.organisations = data.Organisations_agg.count;
          this.cohorts = data.Cohorts_agg.count;
          this.networks = data.Networks_agg.count;
          this.datasources = data.DataSources_agg.count;
          this.databanks = data.Databanks_agg.count;
          this.models = data.Models_agg.count;
          this.datasets = data.Datasets_agg.count;
          this.variables = data.Variables_agg.count;
          this.variableMappings = data.VariableMappings_agg.count;
          this.datasetMappings = data.DatasetMappings_agg.count;
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
  mounted() {
    this.reload();
  },
};
</script>
