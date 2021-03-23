<template>
  <LayoutModal v-if="open" :title="title" @close="closeForm">
    <template #body>
      <MessageSuccess v-if="success">
        {{ success }}
      </MessageSuccess>
      <MessageError v-else-if="error">
        {{ error }}
      </MessageError>
      <div v-else>
        Delete
        <strong>{{ table }}({{ pkeyAsString }})</strong>
        <br>Are you sure?
        <br>
      </div>
    </template>
    <template #footer>
      <ButtonAlt @click="closeForm">
        Close
      </ButtonAlt>
      <ButtonAction v-if="!success && !success" @click="executeDelete">
        Delete
      </ButtonAction>
    </template>
  </LayoutModal>
  <IconDanger v-else icon="trash" @click="openForm" />
</template>

<script>
import ButtonAction from '../forms/ButtonAction.vue'
import ButtonAlt from '../forms/ButtonAlt.vue'
import IconDanger from '../forms/IconDanger.vue'
import LayoutModal from '../layout/LayoutModal.vue'
import MessageError from '../forms/MessageError.vue'
import MessageSuccess from '../forms/MessageSuccess.vue'
import {request} from 'graphql-request'
import RowButtonAdd from './RowButtonAdd.vue'

export default {
  components: {
    ButtonAction,
    ButtonAlt,
    IconDanger,
    LayoutModal,
    MessageError,
    MessageSuccess,
  },
  extends: RowButtonAdd,
  props: {
    graphqlURL: {type: String, default: 'graphql'},
    pkey: Object,
  },
  emits: ['close'],
  data: function() {
    return {
      error: null,
      success: null,
    }
  },
  computed: {
    pkeyAsString() {
      return this.flattenObject(this.pkey)
    },
    title() {
      return `Delete from ${this.table}`
    },

  },
  methods: {
    executeDelete() {
      let query = `mutation delete($pkey:[${this.table}Input]){delete(${this.table}:$pkey){message}}`
      let variables = {pkey: [this.pkey]}
      request(this.graphqlURL, query, variables)
        .then((data) => {
          this.success = data.delete.message
          this.$emit('close')
        })
        .catch((error) => {
          this.error = error.response.errors[0].message
        })
    }, // duplicated code from MolgenisTable, think of util lib
    flattenObject(object) {
      let result = ''
      Object.keys(object).forEach((key) => {
        if (object[key] === null) {
          // nothing
        } else if (typeof object[key] === 'object') {
          result += this.flattenObject(object[key])
        } else {
          result += ' ' + object[key]
        }
      })
      return result
    },
  },
}
</script>
