<template>
  <div>
    <RouterLink
      class="btn btn-primary float-right"
      :to="{ name: 'NetworkVariables', params: { network: network } }"
    >
      View {{ network }} variables
    </RouterLink>
    <h1 class="bg-white">{{ network }} cohorts</h1>
    <p>This page lists all cohorts partner in the {{ network }} network.</p>
    <InputSearch v-model="searchTerms" placeholder="search cohorts" />
    <p>Found {{ count }} cohorts.</p>
    <div class="row">
      <div
        class="col-xl-3 col-lg-4 col-md-6 col-sm-12 mb-4 d-flex align-items-stretch"
        v-for="cohort in data"
        :key="cohort.pid"
      >
        <div class="card col-12 p-0">
          <div class="card-header bg-white">
            <h5 class="card-title mb-0" style="min-height: 4em">
              {{ cohort.pid }}: {{ cohort.name }}
            </h5>
            <RouterLink
              :to="{
                name: 'NetworkCohortDetailView',
                params: { network: network, pid: cohort.pid },
              }"
              class="btn btn-outline-primary float-right"
            >
              View details
            </RouterLink>
          </div>
          <div class="card-body flex-column h-100">
            <div class="card-text">
              <table class="text-align-top">
                <tr>
                  <td><label>Design:</label></td>
                  <td>
                    <span
                      class="font-weight-bold mr-2 mb-2 badge bade-lg badge-primary"
                      v-for="design in cohort.design"
                      :key="design"
                    >
                      {{ design }}
                    </span>
                  </td>
                </tr>
                <tr>
                  <td><label>CollectionType:</label></td>
                  <td>
                    <span
                      class="font-weight-bold mr-2 mb-2 badge bade-lg badge-primary"
                      v-for="collectionType in cohort.collectionType"
                      :key="collectionType.name"
                    >
                      {{ collectionType.name }}
                    </span>
                  </td>
                </tr>
                <tr>
                  <td><label>No Participants:</label></td>
                  <td v-if="cohort.numberOfParticipants">
                    {{ Number(cohort.numberOfParticipants).toLocaleString() }}
                  </td>
                  <td v-else>N/A</td>
                </tr>
                <tr>
                  <td><label>Countries:</label></td>
                  <td>
                    <span
                      class="font-weight-bold mr-2 mb-2 badge bade-lg badge-primary"
                      style="max-width: 15em"
                      v-for="country in cohort.countries"
                      :key="country.name"
                    >
                      {{ country.name }}
                    </span>
                  </td>
                </tr>
                <tr>
                  <td>Institution:</td>
                  <td>
                    <div v-if="cohort.institution">
                      {{ cohort.institution.map((i) => i.pid).join(", ") }}
                    </div>
                    <span v-else>N/A</span>
                  </td>
                </tr>
                <tr>
                  <td>
                    <a :href="cohort.website" target="__blank">website</a>&nbsp;
                    <a :href="'mailto:' + cohort.contactEmail">email</a>
                  </td>
                </tr>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { TableMixin, InputSearch } from "@mswertz/emx2-styleguide";

import Property from "../components/Property";
import ContributorList from "./ContributorList";

export default {
  extends: TableMixin,
  components: {
    Property,
    ContributorList,
    InputSearch,
  },
  props: {
    network: String,
  },
  computed: {
    graphqlFilter() {
      let filter = this.filter ? this.filter : {};
      if (this.network) {
        filter.networks = { pid: { equals: this.network } };
      }
      return filter;
    },
  },
  created() {
    this.limit = 1000;
  },
};
</script>

<style scoped>
table {
  border-collapse: separate;
  border-spacing: 5px;
}

td {
  vertical-align: top;
  padding: 0px;
}
</style>
