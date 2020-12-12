<template>
  <div>
    <RowEditModal
      v-if="open"
      :table="table"
      @close="closeForm"
      :graphqlURL="graphqlURL"
    />
    <IconAction v-else icon="plus" @click="openForm" />
  </div>
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
<RowButtonAdd table="Variables" graphqlURL="/CohortsCentral/graphql"/>
```
</docs>
