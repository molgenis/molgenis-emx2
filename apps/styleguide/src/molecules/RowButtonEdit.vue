<template>
  <div>
    <RowEditModal v-if="open" :table="table" :pkey="pkey" @close="closeForm" />
    <IconAction v-else icon="edit" @click="openForm" />
  </div>
</template>

<script>
import RowButtonAdd from "./RowButtonAdd";
import RowEditModal from "./RowEditModal.vue";
import IconAction from "../components/IconAction";

export default {
  extends: RowButtonAdd,
  components: {
    RowEditModal,
    IconAction
  },
  props: {
    pkey: Object
  },
  computed: {
    title() {
      return `Update ${this.table}`;
    }
  }
};
</script>

<docs>
    Normally you would not instantiate a mixin component, so this is only for quick testing
    ```
    <!-- normally you don't need graphqlURL, default url = 'graphql' just works -->
    <RowButtonEdit table="Order" :pkey="{'name':'spike'}" graphqlURL="/pet store/graphql"/>
    ```
</docs>
