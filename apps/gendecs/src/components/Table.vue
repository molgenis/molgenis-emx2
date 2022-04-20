<template>
  <div>
<!--    <TableSimple-->
<!--        :defaultValue="['Duck']"-->
<!--        :columns="['firstName','lastName','tags']"-->
<!--        :rows="[{'firstName':'Donald','lastName':'Duck'},{'firstName':'Scrooge','lastName':'McDuck','tags':['blue','green']}]"-->
<!--    >-->
<!--    </TableSimple>-->
      <TableSimple
          :columns="['VCFSourceFile', 'Identifier','Chromosome','Position', 'RefSNPNumber', 'Reference', 'Alternative', 'Quality', 'Filter', 'Information']"
          :rows="vcfData">
    </TableSimple>
<!--    <TableMolgenis :selection.sync="selected" :columns.sync="columns" :data="vcfData" @select="click"-->
<!--                   @deselect="click" @click="click"-->
<!--                   :showSelect="true">-->
<!--      <template v-slot:header>columns</template>-->
<!--    </TableMolgenis>-->
<!--    selected: {{ JSON.stringify(selected) }}-->
  </div>

</template>

<script>
import {
  TableSimple,
  TableMolgenis
} from "@mswertz/emx2-styleguide";

export default {
  name: "Table",
  components: {
    TableSimple,
    TableMolgenis
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