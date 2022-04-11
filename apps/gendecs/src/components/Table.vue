<template>
  <div>
    <table>
      <thead>
        <tr>
          <th>Gene</th>
          <th>Variant</th>
          <th>diseases</th>
        </tr>
      </thead>
      <tbody>

        <tr v-for="(item, index) in vcfData" :key="index">
          <td>{{item.gene}}</td>
          <td>{{item.variant}}</td>
          <td>{{item.disease}}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
export default {
  name: "Table",
  props: {
    vcfData : {
      type: [],
    }
  }
}
</script>

<style scoped>

</style>