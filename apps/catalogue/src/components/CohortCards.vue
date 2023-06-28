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
    <InputSearch
      id="cohort-cards-search-input"
      v-model="searchTerms"
      placeholder="search cohorts"
    />
    <p>Found {{ count }} cohorts.</p>
    <div class="row">
      <div
        class="col-xl-3 col-lg-4 col-md-6 col-sm-12 mb-4 d-flex align-items-stretch"
        v-for="cohort in cohorts"
        :key="cohort.id"
      >
        <div class="card col-12 p-0">
          <div class="card-header bg-white">
            <h5 class="card-title mb-0" style="min-height: 4em">
              {{ cohort.id }}: {{ cohort.name }}
            </h5>
            <RouterLink
              :to="{
                name: 'NetworkCohortDetailView',
                params: { network: network, id: cohort.id },
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
                      v-if="cohort.design"
                      :key="design"
                    >
                      {{ cohort.design.name }}
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
                  <td v-else></td>
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
                      {{ cohort.organisations.map((o) => o.id).join(", ") }}
                    </div>
                    <span v-else></span>
                  </td>
                </tr>
                <tr>
                  <td>
                    <a
                      v-if="cohort.website"
                      :href="cohort.website"
                      target="__blank"
                      >website</a
                    >&nbsp;
                    <a
                      v-if="cohort.contactEmail"
                      :href="'mailto:' + cohort.contactEmail"
                      >email</a
                    >
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
import { InputSearch } from "molgenis-components";
import { Client } from "molgenis-components";

export default {
  components: {
    InputSearch,
  },
  props: {
    table: {
      type: String,
      required: true,
    },
    network: String,
    orderBy: Object,
  },
  data() {
    return {
      cohorts: [],
      count: null,
      searchTerms: null,
    };
  },
  computed: {
    graphqlFilter() {
      let filter = this.filter ? this.filter : {};
      if (this.network) {
        filter.networks = { id: { equals: this.network } };
      }
      return filter;
    },
  },
  async created() {
    this.limit = 1000;
    this.client = Client.newClient();
    const resp = await this.client.fetchTableData(this.table, {
      filter: this.graphqlFilter,
      orderby: this.orderBy,
    });
    this.cohorts = resp[this.table] ? resp[this.table] : [];
    this.count = resp[this.table + "_agg"].count;
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
