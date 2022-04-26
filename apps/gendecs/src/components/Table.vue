<template>
  <div>
      <TableSimple
          :columns="['MatchedWith', 'Gene','Chromosome','Position','Reference', 'Alternative', 'Information', 'Diseases']"
          :rows="vcfData">
    </TableSimple>
  </div>

</template>

<script>
import {
  TableSimple,
} from "@mswertz/emx2-styleguide";

export default {
  name: "Table",
  components: {
    TableSimple,
  },
  props: {
    vcfData : {
      type: [],
    }
  },
  data() {
    return {
      selectedItems: null,
      selected: null,
      columns: ['Gene','Chromosome','Position', 'Reference', 'Alternative', 'Information', 'Diseases']
    }
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