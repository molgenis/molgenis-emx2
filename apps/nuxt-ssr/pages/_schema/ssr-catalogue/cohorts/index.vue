<template>
  <div class="container mt-3">
    <h1>Cohorts</h1>
    <div class="pb-1">
      <input-search id="search" v-model="search"></input-search>
      <pagination
        v-if="count > limit"
        v-model="page"
        :count="count"
      ></pagination>
    </div>
    <ul class="mt-3 list-group">
      <li v-for="row in cohorts" :key="row.pid" class="list-group-item">
        <nuxt-link :to="'cohorts/' + row.pid">
          {{ row.name }}
        </nuxt-link>
      </li>
    </ul>
    <div class="p-3">
      <pagination
        v-if="count > limit && limit > 20"
        v-model="page"
        :count="count"
        :limit="limit"
        :defaultValue="page"
      ></pagination>
    </div>
  </div>
</template>

<script>
import cohortsQuery from "../../../../store/gql/cohorts.gql";
import { Pagination, InputSearch } from "molgenis-components";

export default {
  name: "Cohorts",
  components: { Pagination, InputSearch },
  data() {
    return {
      cohorts: [],
      search:
        this.$route.query.search === undefined ? "" : this.$route.query.search,
      count: 0,
      page: 1,
      limit: 20,
    };
  },
  async fetch() {
    const resp = await this.$axios
      .post(this.$route.params.schema + "/graphql", {
        query: cohortsQuery,
        variables: {
          offset: this.pagingOffset,
          limit: this.limit,
          search: this.search,
        },
      })
      .catch((e) => console.error(e));

    if (!resp) return;

    this.cohorts = resp.data.data.Cohorts;
    this.count = resp.data.data.Cohorts_agg.count;
  },
  computed: {
    pagingOffset() {
      return this.$route.query.offset === undefined
        ? 0
        : parseInt(this.$route.query.offset);
    },
  },
  watch: {
    page(newVal, oldVal) {
      if (newVal !== oldVal) {
        this.$router.push({
          path: this.$route.path,
          query: { ...this.$route.query, offset: (newVal - 1) * this.limit },
        });
      }
    },
    search(newVal, oldVal) {
      if (newVal !== oldVal) {
        let query = { ...this.$route.query, search: newVal };
        if (newVal === "") {
          delete query.search;
        }
        this.$router.push({ path: this.$route.path, query });
      }
    },
    $route() {
      this.$nuxt.refresh();
    },
  },
};
</script>
