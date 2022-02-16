<template>
  <div class="container mt-3">
    <h1>Cohorts</h1>
    <div class="pb-1">
      <pagination v-if="count > limit" v-model="page" :count="count"></pagination>
    </div>
    <ul class="mt-3 list-group">
      <li v-for="row in rows" :key="row.pid" class="list-group-item">
        <nuxt-link :to="'cohorts/' + row.pid">
          {{ row.name }}
        </nuxt-link>
      </li>
    </ul>
    <div class="p-3">
      <pagination v-if="count > limit" v-model="page" :count="count" :limit="limit" :defaultValue="page"></pagination>
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
      page: 1,
      limit: 20
    };
  },
  async asyncData({ $axios, store, params, query }) {
    const offset = query.offset === undefined ? 0 : parseInt(query.offset)
    console.log('offset: ' + offset)
    const resp = await $axios({
      url: store.state.schema + "/graphql",
      method: "post",
      data: { query: cohortsQuery, variables: { offset, limit: 20 } },
    }).catch((e) => console.error(e));

    if (!resp) return;

    return {
      cohorts: resp.data.data.Cohorts,
      count: resp.data.data.Cohorts_agg.count,
    };
  },
  computed: {
    rows() {
      return this.cohorts ? this.cohorts : [];
    },
  },
  watch: {
    page (newVal, oldVal) {
      if(newVal !== oldVal) {
        this.$router.push({path: this.$route.path, query: { offset: (newVal - 1) * this.limit }})
      }
    },
    $route () {
      this.$nuxt.refresh()
    }
  }
};
</script>