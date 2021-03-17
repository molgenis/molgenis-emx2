<template>
    <RowEditModal
        v-if="open"
        :graphql-u-r-l="graphqlURL"
        :table="table"
        @close="closeForm"
    />
    <IconAction v-else icon="plus" @click="openForm" />
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
    table: String,
    graphqlURL: {
      defaultValue: "graphql",
      type: String,
    },
  },
  data: function () {
    return {
      open: false,
    };
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
