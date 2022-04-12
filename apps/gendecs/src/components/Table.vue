<template>
  <div>
<!--    <table>-->
<!--      <thead>-->
<!--        <tr>-->
<!--          <th>Gene</th>-->
<!--          <th>Variant</th>-->
<!--          <th>diseases</th>-->
<!--        </tr>-->
<!--      </thead>-->
<!--      <tbody>-->

<!--        <tr v-for="(item, index) in vcfData" :key="index">-->
<!--          <td>{{item.gene}}</td>-->
<!--          <td style="white-space: nowrap; overflow: scroll;">{{item.variant}}</td>-->
<!--          <td>{{item.disease}}</td>-->
<!--        </tr>-->
<!--      </tbody>-->
<!--    </table>-->
      <TableSimple
          :columns="['gene','variant','disease']"
          :rows="vcfData"
          style="overflow-x:auto"
      />
  </div>

</template>

<script>
import {
  TableSimple,
  TableDisplay
} from "@mswertz/emx2-styleguide";

export default {
  name: "Table",
  components: {
    TableSimple,
    TableDisplay
  },
  props: {
    vcfData : {
      type: [],
    }
  },
  data() {
    return {
      selectedItems: null,
    };
  }
}
</script>

<style scoped>
table {
  display: block;
  overflow-x: auto;
  white-space: nowrap;
}
</style>