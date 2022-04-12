<template>
  <div>
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