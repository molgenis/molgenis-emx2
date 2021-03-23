<template>
  <RowEditModal
    v-if="open"
    :graphql-u-r-l="graphqlURL"
    :pkey="pkey"
    :table="table"
    @close="closeForm"
  />
  <IconAction v-else icon="pencil" @click="openForm" />
</template>

<script>
import IconAction from '../forms/IconAction.vue'
import RowButtonAdd from './RowButtonAdd.vue'
import RowEditModal from './RowEditModal.vue'

export default {
  components: {
    IconAction,
    RowEditModal,
  },
  extends: RowButtonAdd,
  props: {
    pkey: Object,
  },
  computed: {
    title() {
      return `Update ${this.table}`
    },
  },
}
</script>
