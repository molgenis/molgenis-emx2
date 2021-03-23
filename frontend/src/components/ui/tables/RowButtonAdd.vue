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
import IconAction from '../forms/IconAction.vue'
import RowEditModal from './RowEditModal.vue'

export default {
  components: {
    IconAction,
    RowEditModal,
  },
  props: {
    graphqlURL: {
      defaultValue: 'graphql',
      type: String,
    },
    table: String,
  },
  emits: ['close'],
  data: function() {
    return {
      open: false,
    }
  },
  computed: {
    title() {
      return 'Add new row to table ' + this.table
    },
  },
  methods: {
    closeForm() {
      this.open = false
      this.$emit('close')
    },
    openForm() {
      this.open = true
    },
  },
}
</script>
