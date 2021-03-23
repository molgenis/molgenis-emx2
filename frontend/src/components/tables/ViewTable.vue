<template>
  <div>
    <router-link v-if="schema" to="/">
      Back to {{ schema.name }}
    </router-link>
    <TableExplorer :table="table" />
  </div>
</template>

<script>
import {TableExplorer} from '@/components/ui/index.js'

export default {
  components: {
    TableExplorer,
  },
  props: {
    schema: Object,
    table: String,
  },
}
</script>
