<template>
  <RowEditModal
    v-if="open"
    :table="table"
    :graphqlURL="graphqlURL"
    :pkey="pkey"
    @close="closeForm"
  />
  <IconAction v-else icon="pencil-alt" @click="openForm" />
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
  },
  computed: {
    title() {
      return `Update ${this.table}`;
    },
  },
};
</script>

<docs>
Normally you would not instantiate a mixin component, so this is only for quick testing
```
<!-- normally you don't need graphqlURL, default url = 'graphql' just works -->
<RowButtonEdit table="Pet" :pkey="{'name':'spike'}" graphqlURL="/pet%20store/graphql"/>
```
</docs>
