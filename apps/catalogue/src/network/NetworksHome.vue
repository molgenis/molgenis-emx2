<template>
  <div class="container-fluid">
    <h1>Welcome to the European Networks Health Data and Cohort Catalogue.</h1>
    <p>
      This catalogue contains metadata on cohorts/data sources, the variables
      they collect, and/or harmonization efforts to enable integrated reuse of
      their rich valuable data. The contents is grouped by 'networks', such as
      harmonization projects, EU projects or by navigating all cohorts directly
      below.
    </p>
    <InputSearch v-model="searchTerms" placeholder="search cohorts" />
    <div v-if="harmonizationNetworks.length > 0">
      <h2>Harmonization networks</h2>
      <p>
        In this section you find networks that aim to enable data reuse across
        multiple projects.
      </p>
      <div class="row">
        <div
          class="col-xl-4 col-lg-4 col-md-6 col-sm-12 mb-4 d-flex align-items-stretch"
          v-for="network in harmonizationNetworks"
          :key="network.pid"
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
          :key="network.pid"
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
          :key="network.pid"
        >
          <NetworkCard :network="network" />
        </div>
      </div>
    </div>
    <div v-if="networks.length == 0">No networks found</div>
  </div>
</template>

<script>
import { request } from "graphql-request";

import NetworkCard from "../components/NetworkCards";
import { InputSearch } from "@mswertz/emx2-styleguide";

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
            network.type && network.type.some((t) => t.name === "harmonization")
        );
      } else {
        return [];
      }
    },
    consortiaNetworks() {
      if (this.networks) {
        return this.networks.filter(
          (network) =>
            network.type && network.type.some((t) => t.name === "h2020")
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
            (!network.type.some((t) => t.name === "h2020") &&
              !network.type.some((t) => t.name === "harmonization"))
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
        `{Networks(orderby:{pid: ASC}${this.searchFilter})
          {
          pid
          name
          localName
          acronym
          website
          description
          institution{
          name
          }
          logo {
          url
          }
          startYear
          endYear
          fundingStatement
          acknowledgements
          partners {
          institution {
          name
          }
          department
          }
          type {name}
          }}`
      ).catch((error) => console.log(error));
      this.networks = result.Networks;
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

<style>
.card-img-top {
  width: 100%;
  object-fit: contain;
}
</style>
