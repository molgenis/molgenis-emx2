<template>
  <span>
    <RowEditModal
      v-if="open"
      :table="table"
      :defaultValue="defaultValue"
      @close="closeForm"
      :graphqlURL="graphqlURL"
      :visible-columns="visibleColumns"
    />
    <IconAction icon="plus" @click="openForm" />
  </span>
</template>

<script>
import RowEditModal from "./RowEditModal.vue";
import IconAction from "../forms/IconAction";

export default {
  data: function () {
    return {
      open: false,
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

Example with names
```
<!-- normally you don't need graphqlURL, default url = 'graphql' just works -->
<RowButtonAdd table="Person_details" graphqlURL="/testNamesWithSpaces/graphql"/>
```
</docs>
