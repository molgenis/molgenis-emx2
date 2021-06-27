<template>
  <RowEditModal
    v-if="open"
    :table="table"
    :graphqlURL="graphqlURL"
    :pkey="pkey"
    @close="closeForm"
    :visible-columns="visibleColumns"
  />
  <IconAction v-else icon="pencil-alt" @click="openForm" />
</template>

<script>
import RowEditModal from "./RowEditModal.vue";
import IconAction from "../forms/IconAction";

export default {
  components: {
    RowEditModal,
    IconAction,
  },
  props: {
    pkey: Object,
    visibleColumns: Array,
    table: String,
    graphqlURL: {
      defaultValue: "graphql",
      type: String,
    },
    defaultValue: Object,
  },
  data() {
    return {
      open: false,
    };
  },
  computed: {
    title() {
      return `Update ${this.table}`;
    },
  },
  methods: {
    openForm() {
      //on open of plus we reset the pkey
      this.open = true;
    },
    closeForm() {
      this.open = false;
      this.$emit("close");
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
