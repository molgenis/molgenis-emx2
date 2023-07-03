<template>
  <div class="container-fluid">
    <MessageError v-if="error">{{ error }}</MessageError>
    <div class="row">
      <div class="col-6 col-sm-5 col-md-4 col-lg-3">
        <h5>Filters</h5>
        <div v-if="!network" class="bg-white px-1">
          <InputRefList
            id="networks-input-ref"
            label="Networks"
            tableName="Networks"
            v-model="networks"
            refLabel="${id}"
            @update:modelValue="reload"
          ></InputRefList>
        </div>
        <div class="bg-white px-1">
          <InputOntology
            id="topics-ontology-input"
            label="Topics"
            v-model="keywords"
            :isMultiSelect="true"
            tableName="Keywords"
            :show-expanded="true"
            schemaName="CatalogueOntologies"
            @update:modelValue="reload"
          />
        </div>
        <div class="bg-white px-1">
          <InputRefList
            id="cohorts-input-ref"
            label="Cohorts"
            tableName="Cohorts"
            v-model="cohorts"
            refLabel="${id}"
            :maxNum="100"
            :orderBy="{ pid: 'ASC' }"
            :filter="network ? { networks: { id: { equals: network } } } : null"
          ></InputRefList>
        </div>
      </div>
      <div class="col-6 col-sm-7 col-md-8 col-lg-9">
        <div class="ml-2">
          <div class="row">
            <div class="col-3">
              <h3>
                Variables
                <span v-if="variablesCount">({{ variablesCount }})</span>
              </h3>
            </div>
            <div class="col-9">
              <InputSearch
                id="search-variables-input"
                v-model="searchInput"
                placeholder="Search variables"
                :isClearBtnShown="true"
              />
            </div>
          </div>
          <div class="row">
            <div class="col">
              <ul class="nav nav-tabs">
                <li class="nav-item">
                  <router-link
                    class="nav-link"
                    :class="{ active: $route.query.tab !== 'harmonization' }"
                    :to="{ path: this.$route.path, query: { tab: 'detail' } }"
                  >
                    Details
                  </router-link>
                </li>
                <li class="nav-item">
                  <router-link
                    class="nav-link"
                    :class="{ active: $route.query.tab === 'harmonization' }"
                    :to="{
                      path: this.$route.path,
                      query: { tab: 'harmonization' },
                    }"
                  >
                    Harmonization
                  </router-link>
                </li>
              </ul>
              <template v-if="$route.query.tab === 'harmonization'">
                <harmonization-view
                  :variables="variables"
                  :cohorts="cohorts"
                  :key="variables?.length"
                />
              </template>
              <template v-else>
                <variables-details-view
                  :variables="variables"
                  :key="variables?.length"
                />
              </template>
              <Spinner v-if="isLoading" />
              <button
                class="btn btn-link mt-2 mb-3"
                v-else-if="variables?.length < variablesCount"
                @click="fetchAdditionalVariables"
              >
                Show more variables
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import VariablesDetailsView from "./VariablesDetailsView.vue";
import HarmonizationView from "./HarmonizationView.vue";
import {
  InputSearch,
  InputOntology,
  InputRefList,
  FilterWells,
  Spinner,
  MessageError,
} from "molgenis-components";
import { gql, request } from "graphql-request";

const LIMIT = 50;

export default {
  name: "VariableExplorer",
  components: {
    VariablesDetailsView,
    HarmonizationView,
    InputSearch,
    InputOntology,
    FilterWells,
    InputRefList,
    Spinner,
    MessageError,
  },
  props: {
    network: {
      type: String,
      default: null,
    },
  },
  data() {
    return {
      isLoading: false,
      cohorts: [],
      variables: [],
      variablesCount: null,
      variablesOffset: 0,
      keywords: [],
      error: null,
      networks: [],
      searchInput: null,
    };
  },
  computed: {
    selectedKeywords() {
      return this.keywords.map((term) => term.name);
    },
    selectedCohorts() {
      return this.cohorts.map((cohort) => cohort.id);
    },
    selectedNetworks() {
      if (this.network) {
        return [this.network];
      } else {
        return this.networks?.map((network) => network.id);
      }
    },
    selectedModels() {
      return this.networks
        ?.map((network) => network.models.map((model) => model.id))
        .flat();
    },
  },
  methods: {
    async fetchAdditionalVariables() {
      this.variablesOffset = this.variablesOffset + LIMIT;
      this.isLoading = true;
      try {
        const result = await fetchVariables(
          this.selectedModels,
          this.selectedCohorts,
          this.selectedKeywords,
          this.search,
          this.variablesOffset
        );
        this.variables.push(...result.Variables);
      } catch (error) {
        this.error = error.response ? error.response.errors[0].message : error;
      }
      this.isLoading = false;
    },
    async reload() {
      this.error = null;
      this.isLoading = true;
      this.variables = [];
      try {
        const result1 = await fetchCohortsAndNetworks(this.selectedNetworks);
        this.cohorts = result1.Cohorts;
        this.networks = result1.Networks;
        const result2 = await fetchVariables(
          this.selectedModels,
          this.selectedCohorts,
          this.selectedKeywords,
          this.search,
          this.variablesOffset
        );
        this.variables = result2.Variables;
        this.variablesCount = result2.Variables_agg.count;
      } catch (error) {
        this.error = error.response ? error.response.errors[0].message : error;
      }
      this.isLoading = false;
    },
  },
  async created() {
    this.reload();
  },
};

//todo: move these to central place?
/** get cohorts optionally filtered by networks */
async function fetchCohortsAndNetworks(networkIdArray) {
  const query = gql`
    query CohortsAndNetworks(
      $cohortsFilter: CohortsFilter
      $networksFilter: NetworksFilter
    ) {
      Cohorts(filter: $cohortsFilter) {
        id
        name
      }
      Networks(filter: $networksFilter) {
        name
        models {
          id
        }
      }
    }
  `;
  const queryParams = {};
  if (networkIdArray) {
    queryParams.cohortsFilter = {
      networks: { id: { equals: networkIdArray } },
    };
    queryParams.networksFilter = { id: { equals: networkIdArray } };
  }
  const queryResponse = await request("graphql", query, queryParams);
  return queryResponse;
}

/** get target variables including mappings from the cohorts filtered by networks, keywords, cohorts and/or search terms */
async function fetchVariables(models, cohorts, keywords, search, offset) {
  const query = gql`
    query Variables(
      $search: String
      $variablesFilter: VariablesFilter
      $offset: Int
      $limit: Int
    ) {
      Variables(
        limit: $limit
        offset: $offset
        search: $search
        filter: $variablesFilter
        orderby: { label: ASC }
      ) {
        name
        keywords {
          name
        }
        description
        unit {
          name
        }
        format {
          name
        }
        dataset {
          name
        }
        resource {
          id
          name
        }
        label
        repeats {
          name
        }
        mappings {
          source {
            id
          }
          targetVariable {
            name
          }
          targetDataset {
            resource {
              id
            }
          }
          sourceDataset {
            resource {
              id
            }
          }
          match {
            name
          }
          syntax
          description
          sourceVariablesOtherDatasets {
            dataset {
              name
            }
            name
          }
        }
        permittedValues {
          value
          label
        }
      }
      Variables_agg(search: $search, filter: $variablesFilter) {
        count
      }
    }
  `;
  const queryParams = {
    offset,
    search,
    limit: LIMIT,
    variablesFilter: { resource: { mg_tableclass: { like: ["Models"] } } },
  };
  if (keywords?.length > 0) {
    queryParams.variablesFilter.keywords = { name: { equals: keywords } };
  }
  if (models?.length > 0) {
    queryParams.variablesFilter.resource = { id: { equals: models } };
  }
  console.log(queryParams);

  // if(cohorts?.length > 0) {
  //   queryParams.variablesFilter.datasets = {resource:{id:{equals: cohorts}}}
  // }
  const queryResponse = await request("graphql", query, queryParams);
  return queryResponse;
}
</script>

<style></style>
