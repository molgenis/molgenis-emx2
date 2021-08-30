<template>
  <div>
    <form v-on:submit.prevent="onSubmit">
      <input type="text" v-model="query" />
      <button type="submit">Search</button>
    </form>
    <hr />
    <div class="row">
      <div class="col-3">
        <ul class="nav flex-column">
          <li class="nav-item">
            <a class="nav-link active" href="#"
              >Institutions -
              <span v-if="results.institutions.count">{{
                results.institutions.count
              }}</span></a
            >
          </li>
          <li class="nav-item">
            <a class="nav-link" href="#">Cohorts</a>
          </li>
          <li class="nav-item">
            <a class="nav-link" href="#">Networks</a>
          </li>
        </ul>
      </div>

      <div class="col-9">
        <h3 class="text-capitalize">{{ type }} results</h3>
        <ul>
          <li
            v-for="institution in results.institutions.items"
            :key="institution.name"
          >
            {{ institution }}
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script>
import { request } from "graphql-request";
import institutions from "../store/query/institutions.gql";

export default {
  name: "SearchResults",
  props: {
    query: {
      type: String,
      default: () => "",
    },
    type: {
      type: String,
      default: () => "institutions",
    },
  },
  data() {
    return {
      results: {
        institutions: {
          items: [],
          count: null,
        },
        cohorts: {
          items: [],
          count: null,
        },
        networks: {
          items: [],
          count: null,
        },
      },
    };
  },
  methods: {
    async search() {
      const params = { search: this.query };
      const resp = await request("graphql", institutions, params).catch((e) =>
        console.error(e)
      );
      this.results.institutions.items = resp.Institutions;
      this.results.institutions.count = resp.Institutions_agg.count;
    },
  },
  created() {
    this.search();
  },
};
</script>

<style></style>
