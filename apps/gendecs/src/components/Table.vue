<template>
  <div>
      <TableSimple
          :columns="['VCFSourceFile', 'Identifier','Chromosome','Position', 'RefSNPNumber', 'Reference', 'Alternative', 'Quality', 'Filter', 'Information']"
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
      columns: ['VCFSourceFile', 'Identifier','Chromosome','Position', 'RefSNPNumber', 'Reference', 'Alternative', 'Quality', 'Filter', 'Information']
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