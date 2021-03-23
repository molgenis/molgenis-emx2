<template>
  <LayoutModal
    :show="true"
    :title="'Drop Table \'' + table + '\''"
    @close="$emit('close')"
  >
    <template #body>
      <MessageSuccess v-if="success">
        {{ success }}
      </MessageSuccess>
      <MessageError v-else-if="graphqlError">
        {{ graphqlError }}
      </MessageError>
      <div v-else>
        Removing table <strong>'{{ table }}'</strong> from schema
        <strong>'{{ schema }}'</strong> <br>Are you sure?
      </div>
    </template>
    <template #footer>
      <ButtonAlt @click="$emit('close')">
        Close
      </ButtonAlt>
      <ButtonAction
        v-if="!success && !success" @click="dropTable"
      >
        Drop Table
      </ButtonAction>
    </template>
  </LayoutModal>
</template>

<script>
import {request} from 'graphql-request'

import {
  ButtonAction,
  ButtonAlt,
  LayoutModal,
  MessageError,
  MessageSuccess,
} from '@/components/ui/index.js'

export default {
  components: {
    ButtonAction,
    ButtonAlt,
    LayoutModal,
    MessageError,
    MessageSuccess,
  },
  props: {
    schema: String,
    table: String,
  },
  emits: ['close'],
  data: function() {
    return {
      graphqlError: null,
      success: null,
    }
  },
  methods: {
    dropTable() {
      this.loading = true
      this.success = null
      this.graphqlError = null
      request(
        'graphql',
        'mutation drop($name:String){drop(tables:[$name]){message}}',
        {
          name: this.table,
        },
      )
        .then((data) => {
          this.success = data.drop.message
          this.$emit('close')
        })
        .catch((error) => {
          if (error.response && error.response.status === 403) {
            this.graphqlError = 'Forbidden. Do you need to login?'
            this.showLogin = true
          } else this.graphqlError = error
        })

      this.loading = false
    },
  },
}
</script>
