<template>
<div class="container mt-3">
  <h2>Cohorts</h2>
  <ul class="mt-3 list-group">
    <li v-for="row in rows" :key="row.pid" class="list-group-item">
      <nuxt-link :to="'cohorts/' + row.pid" >
         {{ row.name }}
      </nuxt-link>
    </li>
  </ul>
</div>

</template>

<script>
import query from "../../../../store/gql/cohorts.gql";
  export default {
    name: "Cohorts",
    async asyncData({ params, $axios, store }) {
      const resp = await $axios({
        url: store.state.schema + "/graphql",
        method: "post",
        data: {query} ,
      }).catch((e) => console.error(e));

      return { cohorts: resp.data.data.Cohorts }
    },
    computed: {
      rows () {
        return this.cohorts ? this.cohorts : []
      }
    }
 
  }
</script>