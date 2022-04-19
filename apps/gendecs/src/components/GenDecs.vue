<template>
 <div>
   <router-link to="/1/patientView">go to gendecs</router-link>
   <div>
     <TableExplorer
         table="Patients"
         graphqlURL="/gendecs/graphql"
         :showSelect="false" @click="click" :showColumns.sync="showColumns" :showFilters.sync="showFilters"
         :showPage.sync="page" :showLimit.sync="limit"
         :showOrderBy.sync="showOrderBy" :showOrder.sync="showOrder"/>
   </div>
 </div>
</template>

<script>
import {
  TableExplorer,
} from "@mswertz/emx2-styleguide";

export default {
  name: "GenDecs",
  components : {
    TableExplorer
  },
  data() {
    return {
      showColumns: ['identifier', "gender", "birthdate", "vcfdata"],
      showFilters: ['identifier'],
      page: 1,
      limit: 10,
      showOrder: 'DESC', showOrderBy: 'identifier'
    }
  },
  methods: {
    click(event) {
      console.log(event);
      console.log(event.identifier);
      let routData2 = this.$router.resolve({path: "/" + event.identifier + "/patientView"});
      window.open(routData2.href, "_blank");
    }
  }
}
</script>

<style scoped>

</style>