<template>
  <LayoutModal
    :show="true"
    :title="'Drop Column \'' + column + '\''"
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
        Removing column <strong>{{ column }}</strong> in table
        <strong>{{ table }}</strong> <br>Are you sure?
      </div>
    </template>
    <template #footer>
      <ButtonAlt @click="$emit('close')">
        Close
      </ButtonAlt>
      <ButtonAction
        v-if="!success && !success" @click="dropColumn"
      >
        Drop Column
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
} from '@mswertz/emx2-styleguide'

export default {
  components: {
    ButtonAction,
    ButtonAlt,
    LayoutModal,
    MessageError,
    MessageSuccess,
  },
  props: {
    column: String,
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
    dropColumn() {
      this.loading = true
      this.success = null
      this.graphqlError = null
      request(
        'graphql',
        'mutation drop($table:String,$column:String){drop(columns:[{table:$table,column:$column}]){message}}',
        {
          column: this.column,
          table: this.table,
        },
      )
        .then((data) => {
          this.success = data.drop.message
          this.$emit('close')
        })
        .catch((graphqlError) => {
          if (graphqlError.response && graphqlError.response.status === 403) {
            this.graphqlError = 'Forbidden. Do you need to login?'
            this.showLogin = true
          } else this.graphqlError = graphqlError
        })
        .finally((this.loading = false))
    },
  },
}
</script>
