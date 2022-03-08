<template>
  <div class="container mt-3">
    <h1>Cohorts</h1>
    <div class="pb-1">
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
        v-if="count > limit"
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
import { Pagination } from "molgenis-components";
export default {
  name: "Cohorts",
  components: { Pagination },
  data() {
    return {
      cohorts: [],
      count: 0,
      page: 1,
      limit: 20,
    };
  },
  async fetch() {
    const resp = await this.$axios
      .post(this.$route.params.schema + "/graphql", {
        query: cohortsQuery,
        variables: { offset: this.pagingOffset, limit: this.limit },
      })
      .catch((e) => console.error(e));

    if (!resp) return;

    this.cohorts = resp.data.data.Cohorts;
    this.count = resp.data.data.Cohorts_agg.count;
  },
  computed: {
    pagingOffset () {
      return this.$route.query.offset === undefined ? 0 : parseInt(this.$route.query.offset);
    }
  },
  watch: {
    page(newVal, oldVal) {
      if (newVal !== oldVal) {
        this.$router.push({
          path: this.$route.path,
          query: { offset: (newVal - 1) * this.limit },
        });
      }
    },
    $route() {
      this.$nuxt.refresh();
    },
  },
};
</script>