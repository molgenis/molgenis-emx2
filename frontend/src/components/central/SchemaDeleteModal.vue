<template>
  <div>
    <!-- whilst loading -->
    <LayoutModal v-if="loading" :show="true" :title="title">
      <template #body>
        <Spinner />
      </template>
    </LayoutModal>
    <!-- when completed -->
    <LayoutModal
      v-else-if="success"
      :show="true"
      :title="title"
      @close="$emit('close')"
    >
      <template #body>
        <MessageSuccess>{{ success }}</MessageSuccess>
      </template>
      <template #footer>
        <ButtonAction @click="$emit('close')">
          Close
        </ButtonAction>
      </template>
    </LayoutModal>
    <!-- create schema -->
    <LayoutModal
      v-else :show="true"
      :title="title"
      @close="$emit('close')"
    >
      <template #body>
        <MessageError v-if="graphqlError">
          {{ graphqlError }}
        </MessageError>
        Are you sure you want to delete database '{{ schemaName }}'?
      </template>
      <template #footer>
        <ButtonAlt @click="$emit('close')">
          Close
        </ButtonAlt>
        <ButtonAction
          @click="executeDeleteSchema"
        >
          Delete database
        </ButtonAction>
      </template>
    </LayoutModal>
  </div>
</template>

<script>
import {request} from 'graphql-request'

import {
  ButtonAction,
  ButtonAlt,
  LayoutModal,
  MessageError,
  MessageSuccess,
  Spinner,
} from '../ui/index.js'

export default {
  components: {
    ButtonAction,
    ButtonAlt,
    LayoutModal,
    MessageError,
    MessageSuccess,
    Spinner,
  },
  props: {
    schemaName: String,
  },
  emits: ['close'],
  data: function() {
    return {
      graphqlError: null,
      key: 0,
      loading: false,
      success: null,
    }
  },
  computed: {
    endpoint() {
      return '/api/graphql'
    },
    title() {
      return 'Delete database'
    },
  },
  methods: {
    executeDeleteSchema() {
      this.loading = true
      this.graphqlError = null
      this.success = null
      request(
        this.endpoint,
        'mutation deleteSchema($name:String){deleteSchema(name:$name){message}}',
        {
          name: this.schemaName,
        },
      )
        .then((data) => {
          this.success = data.deleteSchema.message
          this.loading = false
        })
        .catch((error) => {
          if (error.response.status === 403) {
            this.graphqlError =
              error.message + 'Forbidden. Do you need to login?'
          } else {
            this.graphqlError = error.response.errors[0].message
          }
          this.loading = false
        })
    },
  },
}
</script>
