<template>
<div>
  <h1>Cohorts</h1>
  <table-display  :columns="columns" :rows="rows"></table-display>
</div>

</template>

<script>
import query from "../../../../store/gql/cohorts.gql";
import { TableDisplay} from "molgenis-components"
// import { TableDisplay } from "molgenis-components";
  export default {
    name: "Cohorts",
    components: { TableDisplay },
    data() {
      return {
        columns:[ { name: 'pid', label: 'PID' }, { name: 'name', label: 'Name' }]
      }
    },
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