<template>
  <div>
    <h1>Variable explorer</h1>
    <h4 v-if="network">
      path: network = {{ network
      }}<span v-if="table"> / table = {{ table }}</span>
      (
      <router-link to="/explorer2">show all</router-link>
      )
    </h4>
    <div class="row">
      <div class="col-3">
        <FilterSidebar :filters.sync="filters" />
      </div>
      <div class="col-9">
        <FilterWells :filters.sync="filters" />
        <InputSearch v-model="searchInput" placeholder="Search variables" />
        <div v-if="!filters[0].conditions || filters[0].conditions.length == 0">
          <- Select network
        </div>
        <table v-else class="table bg-white">
          <thead>
            <tr>
              <th scope="col">Variable</th>
              <th scope="col">Description</th>
              <th scope="col">Mapppings</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="v in variables"
              :key="v.name + v.table.name + v.release.resource.acronym"
            >
              <td>
                <router-link
                  :to="{
                    name: 'explorer2details',
                    params: {
                      network: v.release.resource.acronym,
                      table: v.table.name,
                      variable: v.name,
                    },
                  }"
                >
                  {{ v.release.resource.acronym }}/{{ v.table.name }}/{{
                    v.name
                  }}
                </router-link>
              </td>
              <td>
                {{ v.label }}
                <span v-if="v.unit">(unit: {{ v.unit.name }})</span>
                <span v-if="v.repeats_agg > 0">
                  repeats: {{ v.repeats_agg.count }}</span
                >
              </td>
              <td>
                <span v-if="v.mappings">{{
                  v.mappings.map((m) => m.fromRelease.resource.acronym)
                }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    <pre>
    graphqlFilter = {{ graphqlFilter }}
    filters = {{ filters }}
      </pre
    >
  </div>
</template>

<script>
import {
  FilterSidebar,
  FilterWells,
  InputSearch,
} from "@mswertz/emx2-styleguide";
import { request } from "graphql-request";

export default {
  components: {
    FilterSidebar,
    FilterWells,
    InputSearch,
  },
  props: {
    network: String,
    table: String,
  },
  data() {
    return {
      searchInput: null,
      filters: [
        {
          name: "Networks",
          columnType: "REF",
          refTable: "Networks",
          showFilter: true,
        },
        {
          name: "Keywords",
          columnType: "REF",
          refTable: "Keywords",
          showFilter: true,
        },
        {
          name: "Tables",
          columnType: "REF",
          refTable: "Tables",
          showFilter: true,
        },
      ],
      variables: [],
      graphqlError: null,
      loading: false,
    };
  },
  computed: {
    graphqlFilter() {
      let graphqlFilter = {};
      if (this.searchInput) {
        graphqlFilter._search = this.searchInput;
      }
      //{
      //   "resource": {
      //     "acronym": "LifeCycle"
      //   },
      //   "version": "1.0.0"
      // }
      if (this.filters[0].conditions && this.filters[0].conditions.length > 0) {
        graphqlFilter.release = {
          equals: this.filters[0].conditions.map((c) => {
            return {
              resource: {
                acronym: c.acronym,
              },
              version: "1.0.0",
            };
          }),
        };
      }
      if (this.filters[1].conditions && this.filters[1].conditions.length > 0) {
        graphqlFilter.keywords = { equals: this.filters[1].conditions };
      }
      if (this.filters[2].conditions && this.filters[2].conditions.length > 0) {
        graphqlFilter.table = { equals: this.filters[2].conditions };
      }
      if (this.network) {
        graphqlFilter.release = {
          equals: [{ resource: { acronym: this.network }, version: "1.0.0" }],
        };
      }
      if (this.table) {
        graphqlFilter.table = {
          equals: [
            {
              release: {
                resource: { acronym: this.network },
                version: "1.0.0",
              },
              name: this.table,
            },
          ],
        };
      }
      return graphqlFilter;
    },
  },
  methods: {
    reload() {
      this.variables = [];
      request(
        "graphql",
        `query Variables($filter:VariablesFilter){Variables(filter:$filter, limit:100)
          {release{resource{acronym}},table{name},name,unit{name},label,repeats_agg{count},mappings{fromRelease{resource{acronym}}}}
        }`,
        {
          filter: this.graphqlFilter,
        }
      )
        .then((data) => {
          this.variables = data.Variables;
        })
        .catch((error) => {
          if (error.response)
            this.graphqlError = error.response.errors[0].message;
          else this.graphqlError = error;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    setFilters() {
      if (this.network) this.filters[0].showFilter = false;
      else this.filters[0].showFilter = true;
      if (this.table) this.filters[2].showFilter = false;
      else this.filters[2].showFilter = true;
    },
  },
  created() {
    this.setFilters();
    this.reload();
  },
  watch: {
    network() {
      this.setFilters();
      this.reload();
    },
    table() {
      this.setFilters();
      this.reload();
    },
    searchInput() {
      this.reload();
    },
    filters: {
      deep: true,
      handler() {
        this.reload();
      },
    },
  },
};
</script>
