<template>
  <div>
    <RowEditModal v-if="open" :table="table" @close="closeForm" />
    <IconAction v-else icon="plus" @click="openForm" />
  </div>
</template>

<script>
    import RowEditModal from "./RowEditModal.vue";
    import IconAction from "../components/IconAction";

    export default {
  data: function() {
    return {
      open: false
    };
  },
  components: {
    RowEditModal,
    IconAction
  },
  props: {
    table: String
  },
  computed: {
    title() {
      return "Add new row to table " + this.table;
    }
  },
  methods: {
    openForm() {
      this.open = true;
    },
    closeForm() {
      this.open = false;
      this.$emit("close");
    }
  }
};
</script>

<docs>
    Example
    ```
    <RowButtonAdd table="Pet"/>
    ```
</docs>
