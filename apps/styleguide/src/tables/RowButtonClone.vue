<template>
  <span>
    <RowEditModal
      v-if="open"
      :table="table"
      :graphqlURL="graphqlURL"
      :pkey="pkey"
      :clone="true"
      @close="closeForm"
      :visible-columns="visibleColumns" />
    <IconAction icon="copy" @click="openForm"
  /></span>
</template>

<script>
import RowButtonAdd from "./RowButtonAdd";
import RowEditModal from "./RowEditModal.vue";
import IconAction from "../forms/IconAction";

export default {
  extends: RowButtonAdd,
  components: {
    RowEditModal,
    IconAction,
  },
  props: {
    pkey: Object,
    visibleColumns: Array,
  },
  computed: {
    title() {
      return `Clone ${this.table}`;
    },
  },
};
</script>

<docs>
Normally you would not instantiate a mixin component, so this is only for quick testing
```
<!-- normally you don't need graphqlURL, default url = 'graphql' just works -->
<RowButtonClone table="Pet" :pkey="{'name':'spike'}" graphqlURL="/pet%20store/graphql"/>
```
</docs>
