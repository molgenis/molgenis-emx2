<template>
  <div class="container-fluid bg-white">
    <div class="row p-4">
      <div class="col-4">
        <InputSearch
          v-model="search"
          id="search-input"
          :isClearBtnShown="true"
        />
        <InputOntology
          id="topics-ontology-input"
          label="Areas of Information"
          v-model="areasFilter"
          :isMultiSelect="true"
          tableId="AreasOfInformationCohorts"
          :show-expanded="true"
          schemaName="CatalogueOntologies"
        />
        <InputOntology
          id="sample-ontology-input"
          label="Sample categories"
          v-model="samplesFilter"
          :isMultiSelect="true"
          tableId="SampleCategories"
          :show-expanded="true"
          schemaName="CatalogueOntologies"
        />
        <InputOntology
          id="diseases-ontology-input"
          label="Diseases"
          v-model="diseasesFilter"
          :isMultiSelect="true"
          tableId="Diseases"
          :show-expanded="true"
          schemaName="CatalogueOntologies"
        />
      </div>
      <div class="col-8">
        <div v-if="allFilters.length > 0">
          Filters: {{ allFilters.join(",") }}
          <a href="" @click.prevent="clearFilters">(clear filters)</a>
        </div>
        {{ allResources.length }} results found ({{
          datasources.length
        }}
        datasources, {{ cohorts.length }} cohorts,
        {{ databanks.length }} databanks):
        <div class="card-columns">
          <div v-for="resource in allResources" class="card">
            <div class="card-body" style="height: 200px; overflow-x: hidden">
              <i class="far fa-star float-right"></i>
              <span class="badge badge-secondary float-left">{{
                resource.type ? resource.type[0].name : resource.class
              }}</span>
              <h5 class="card-title clear pt-4">
                <router-link :to="resource.url" target="_blank">{{
                  resource.name
                }}</router-link>
              </h5>
              <p class="card-text">{{ resource.description }}</p>
              <div>Samples: {{ resource.samples?.join(" ") }}</div>
              <div>Data: {{ resource.data?.join(" ") }}</div>
              <div>Disease:{{ resource.disease?.join(" ") }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { InputSearch, InputOntology } from "molgenis-components";
import { gql, request } from "graphql-request";
export default {
  components: {
    InputSearch,
    InputOntology,
  },
  data() {
    return {
      search: null,
      areasFilter: [],
      diseasesFilter: [],
      samplesFilter: [],
      cohorts: [],
      databanks: [],
      datasources: [],
      dataResources: [],
    };
  },
  computed: {
    allResources() {
      return [...this.cohorts, ...this.databanks, ...this.datasources];
    },
    allFilters() {
      return [
        ...this.areasFilter.map((e) => e.name),
        ...this.samplesFilter.map((e) => e.name),
        ...this.diseasesFilter.map((e) => e.name),
      ];
    },
  },
  methods: {
    clearFilters() {
      this.diseasesFilter = [];
      this.samplesFilter = [];
      this.areasFilter = [];
    },
    async reload() {
      const cohortQuery = gql`
        query Cohorts($filter: CohortsFilter) {
          Cohorts(filter: $filter) {
            id
            name
            type {
              name
            }
            collectionEvents {
              areasOfInformation {
                name
              }
              sampleCategories {
                name
              }
            }
            subcohorts {
              mainMedicalCondition {
                name
              }
            }
          }
        }
      `;
      const databankQuery = gql`
        query Databanks($filter: DatabanksFilter) {
          Databanks(filter: $filter) {
            id
            name
            type {
              name
            }
            biospecimenCollected {
              name
            }
            areasOfInformation {
              name
            }
            populationDisease {
              name
            }
          }
        }
      `;
      const datasourceQuery = gql`
        query DataSources($filter: DataSourcesFilter) {
          DataSources(filter: $filter) {
            id
            name
            type {
              name
            }
            linkedResources {
              linkedResource {
                biospecimenCollected {
                  name
                }
                areasOfInformation {
                  name
                }
              }
            }
          }
        }
      `;

      const cohortVariables = { filter: {} };
      const databankVariables = { filter: {} };
      const datasourceVariables = { filter: {} };
      if (this.search) {
        cohortVariables.filter._search = this.search;
        databankVariables.filter._search = this.search;
        datasourceVariables.filter._search = this.search;
      }
      if (this.areasFilter.length > 0) {
        cohortVariables.filter.collectionEvents = {
          areasOfInformation: { equals: this.areasFilter },
        };
        databankVariables.filter.areasOfInformation = {
          equals: this.areasFilter,
        };
        datasourceVariables.filter.linkedResources = {
          linkedResource: { areasOfInformation: { equals: this.areasFilter } },
        };
      }
      if (this.samplesFilter.length > 0) {
        cohortVariables.filter.collectionEvents = {
          sampleCategories: { equals: this.samplesFilter },
        };
        databankVariables.filter.biospecimenCollected = {
          equals: this.samplesFilter,
        };
        datasourceVariables.filter.linkedResources = {
          linkedResource: {
            biospecimenCollected: { equals: this.samplesFilter },
          },
        };
      }
      if (this.diseasesFilter.length > 0) {
        cohortVariables.filter.subcohorts = {
          mainMedicalCondition: { equals: this.diseasesFilter },
        };
        databankVariables.filter.populationDisease = {
          equals: this.diseasesFilter,
        };
        datasourceVariables.filter.linkedResources = {
          linkedResource: {
            populationDisease: { equals: this.diseasesFilter },
          },
        };
      }
      const cohortResp = await request(
        "graphql",
        cohortQuery,
        cohortVariables
      ).catch((e) => console.error(e));
      const databankResp = await request(
        "graphql",
        databankQuery,
        databankVariables
      ).catch((e) => console.error(e));
      const datasourceResp = await request(
        "graphql",
        datasourceQuery,
        datasourceVariables
      ).catch((e) => console.error(e));
      this.cohorts = cohortResp.Cohorts
        ? cohortResp.Cohorts.map((d) => {
            d.class = "cohort";
            d.data = this.getCohortData(d);
            d.disease = this.getCohortDisease(d);
            d.samples = this.getCohortSamples(d);
            d.url = "/cohorts/" + d.id;
            return d;
          })
        : [];
      this.databanks = databankResp.Databanks
        ? databankResp.Databanks.map((d) => {
            d.class = "databank";
            d.data = this.getDatabankData(d);
            d.samples = this.getDatabankSamples(d);
            d.disease = this.getDatabankDisease(d);
            d.url = "/databanks/" + d.id;
            return d;
          })
        : [];
      this.datasources = datasourceResp.DataSources
        ? datasourceResp.DataSources.map((d) => {
            d.class = "datasource";
            d.url = "/datasources/" + d.id;
            d.data = this.getDatasourceData(d);
            d.samples = this.getDatasourceSamples(d);
            d.disase = this.getDatabankDisease(d);
            return d;
          })
        : [];
    },
    getDatasourceData(datasource) {
      return [
        ...new Set(
          datasource.linkedResources
            ?.map((l) =>
              l.linkedResource.areasOfInformation?.map((e) => e.name)
            )
            .flat()
        ),
      ];
    },
    getDatasourceSamples(datasource) {
      return [
        ...new Set(
          datasource.linkedResources
            ?.map((l) =>
              l.linkedResource.biospecimenCollected?.map((e) => e.name)
            )
            .flat()
        ),
      ];
    },
    getDatasourceDisease(datasource) {
      return [
        ...new Set(
          datasource.linkedResources
            ?.map((l) => l.linkedResource.populationDisease?.map((e) => e.name))
            .flat()
        ),
      ];
    },
    getDatabankData(databank) {
      return databank.areasOfInformation?.map((e) => e.name);
    },
    getDatabankSamples(databank) {
      return databank.biospecimenCollected?.map((e) => e.name);
    },
    getDatabankDisease(databank) {
      return databank.populationDisease?.map((e) => e.name);
    },
    getCohortData(cohort) {
      return [
        ...new Set(
          cohort.collectionEvents
            ?.map((e) => e.areasOfInformation?.map((a) => a.name.trim()))
            .flat()
        ),
      ];
    },
    getCohortSamples(cohort) {
      return [
        ...new Set(
          cohort.collectionEvents
            ?.map((e) => e.sampleCategories?.map((a) => a.name.trim()))
            .flat()
        ),
      ];
    },
    getCohortDisease(cohort) {
      return [
        ...new Set(
          cohort.subcohorts
            ?.map((e) => e.mainMedicalCondition?.map((a) => a.name.trim()))
            .flat()
        ),
      ];
    },
  },
  created() {
    this.reload();
  },
  watch: {
    search() {
      this.reload();
    },
    areasFilter() {
      this.reload();
    },
    samplesFilter() {
      this.reload();
    },
    diseasesFilter() {
      this.reload();
    },
  },
};
</script>
