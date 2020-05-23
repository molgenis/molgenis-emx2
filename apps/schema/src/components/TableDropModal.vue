<template>
  <LayoutModal
    :show="true"
    :title="'Drop Table \'' + table + '\''"
    @close="$emit('close')"
  >
    <template v-slot:body>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <MessageError v-else-if="error">{{ error }}</MessageError>
      <div v-else>
        Removing table <strong>'{{ table }}'</strong> from schema
        <strong>'{{ schema }}'</strong> <br />Are you sure?
      </div>
    </template>
    <template v-slot:footer>
      <ButtonAlt @click="$emit('close')">Close</ButtonAlt>
      <ButtonAction v-if="!success && !success" @click="dropTable"
        >Drop Table
      </ButtonAction>
    </template>
  </LayoutModal>
</template>

<script>
import { request } from 'graphql-request'

import {
  LayoutModal,
  MessageSuccess,
  MessageError,
  ButtonAlt,
  ButtonAction
} from '@mswertz/emx2-styleguide'

export default {
  components: {
    LayoutModal,
    MessageSuccess,
    MessageError,
    ButtonAction,
    ButtonAlt
  },
  props: {
    schema: String,
    table: String
  },
  data: function() {
    return {
      success: null,
      error: null
    }
  },
  methods: {
    dropTable() {
      this.loading = true
      this.success = null
      this.error = null
      request(
        'graphql',
        `mutation drop($name:String){drop(tables:[$name]){message}}`,
        {
          name: this.table
        }
      )
        .then(data => {
          this.success = data.drop.message
          this.$emit('close')
        })
        .catch(error => {
          if (error.response && error.response.status === 403) {
            this.error = 'Forbidden. Do you need to login?'
            this.showLogin = true
          } else this.error = error
        })

      this.loading = false
    }
  }
}
</script>
