<template>
  <div>
      <TableSimple
          :columns="['Gene', 'Identifier','Chromosome','Position', 'RefSNPNumber', 'Reference', 'Alternative', 'Quality', 'Filter', 'Information', 'Diseases']"
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
      columns: ['Gene', 'Identifier','Chromosome','Position', 'RefSNPNumber', 'Reference', 'Alternative', 'Quality', 'Filter', 'Information', 'Diseases']
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