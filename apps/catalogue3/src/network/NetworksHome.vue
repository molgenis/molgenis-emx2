<template>
  <div class="container-fluid">
    <h1>European Networks Health Data and Cohort Catalogue.</h1>
    <p>
      This catalogue contains metadata on cohorts/data sources, the variables
      they collect, and/or harmonization efforts to enable integrated reuse of
      their rich valuable data. The contents is grouped by 'networks', such as
      harmonization projects, EU projects or by navigating all cohorts directly
      below.
    </p>
    <InputSearch
      id="networks-home-search-input"
      v-model="searchTerms"
      placeholder="search cohorts"
    />
    <div v-if="harmonizationNetworks.length > 0">
      <h2>Networks</h2>
      <p>
        In this section you find networks that aim to enable data reuse across
        multiple projects, data sources and institutions.
      </p>
      <div class="row">
        <div
          class="col-xl-4 col-lg-4 col-md-6 col-sm-12 mb-4 d-flex align-items-stretch"
          v-for="network in harmonizationNetworks"
          :key="network.id"
        >
          <NetworkCard :network="network" />
        </div>
      </div>
    </div>
    <div v-if="consortiaNetworks.length > 0">
      <h2>EU projects</h2>
      <p>
        In this section you can navigate the catalogue based on consortia funded
        by the European Union
      </p>
      <div class="row">
        <div
          class="col-xl-4 col-lg-4 col-md-6 col-sm-12 mb-4 d-flex align-items-stretch"
          v-for="network in consortiaNetworks"
          :key="network.id"
        >
          <NetworkCard :network="network" />
        </div>
      </div>
    </div>
    <div v-if="otherNetworks.length > 0">
      <h2>Other networks</h2>
      <p>In this section you can navigate other networks</p>
      <div class="row">
        <div
          class="col-xl-4 col-lg-4 col-md-6 col-sm-12 mb-4 d-flex align-items-stretch"
          v-for="network in otherNetworks"
          :key="network.id"
        >
          <NetworkCard :network="network" />
        </div>
      </div>
    </div>
    <div v-if="networks">No networks found</div>
  </div>
</template>

<script>
import { request } from "graphql-request";

import NetworkCard from "../components/NetworkCards.vue";
import { InputSearch } from "molgenis-components";

export default {
  components: {
    NetworkCard,
    InputSearch,
  },
  data() {
    return {
      networks: [],
      searchTerms: null,
    };
  },
  computed: {
    harmonizationNetworks() {
      if (this.networks) {
        return this.networks.filter(
          (network) =>
            network.type &&
            network.type.some((type) => type.name === "harmonization")
        );
      } else {
        return [];
      }
    },
    consortiaNetworks() {
      if (this.networks) {
        return this.networks.filter(
          (network) =>
            network.type && network.type.some((type) => type.name === "h2020")
        );
      } else {
        return [];
      }
    },
    otherNetworks() {
      if (this.networks) {
        return this.networks.filter(
          (network) =>
            !network.type ||
            (!network.type.some((type) => type.name === "h2020") &&
              !network.type.some((type) => type.name === "harmonization"))
        );
      } else {
        return [];
      }
    },
    searchFilter() {
      return this.searchTerms
        ? ',filter: { _search: "' + this.searchTerms + '"}'
        : "";
    },
  },
  methods: {
    async fetchData() {
      const result = await request(
        "graphql",
        `{Networks(orderby:{id: ASC}${this.searchFilter})
          {
          id
          pid
          name
          acronym
          website
          description
          leadOrganisation{
          name
          }
          logo {
          url
          }
          startYear
          endYear
          fundingStatement
          acknowledgements
          leadOrganisation {
            name
          }
          type {name}
          }}`
      ).catch((error) => console.log(error));

      this.networks = result.Networks ? result.Networks : [];
    },
  },
  mounted: async function () {
    this.fetchData();
  },
  watch: {
    searchTerms() {
      this.fetchData();
    },
  },
};
</script>

<style scoped>
.card-img-top {
  width: 100%;
  object-fit: contain;
}
</style>
