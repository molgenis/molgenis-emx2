<template>
  <div>
    <RowEditModal
      v-if="open"
      :table="table"
      :pkey.sync="pkey"
      :defaultValue="defaultValue"
      @close="closeForm"
      :graphqlURL="graphqlURL"
      :visible-columns="visibleColumns"
    />
    <IconAction v-else icon="plus" @click="openForm" />
  </div>
</template>

<script>
import RowEditModal from "./RowEditModal.vue";
import IconAction from "../forms/IconAction";

export default {
  data() {
    return {
      open: false,
      pkey: null,
    };
  },
  components: {
    RowEditModal,
    IconAction,
  },
  props: {
    table: String,
    graphqlURL: {
      defaultValue: "graphql",
      type: String,
    },
    visibleColumns: Array,
    defaultValue: Object,
  },
  computed: {
    title() {
      return "Add new row to table " + this.table;
    },
  },
  methods: {
    openForm() {
      //on open of plus we reset the pkey
      this.pkey = null;
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
Example
```
<!-- normally you don't need graphqlURL, default url = 'graphql' just works -->
<RowButtonAdd table="Variables" graphqlURL="/CohortNetwork/graphql"/>
```
</docs>
