<template>
  <div>
    <!-- whilst loading -->
    <LayoutModal v-if="loading" :show="true" :title="title">
      <template #body>
        <Spinner />
      </template>
    </LayoutModal>
    <!-- when update succesfull show result before close -->
    <LayoutModal
      v-else-if="success"
      :show="true"
      :title="title"
      @close="$emit('close')"
    >
      <template #body>
        <MessageSuccess>{{ success }}</MessageSuccess>
        Go to edit <a :href="'/' + schemaName + '/schema/'">schema</a><br>
        Go to upload <a :href="'/' + schemaName + '/updownload/'">files</a>
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
        <Spinner v-if="loading" />
        <div v-else>
          <MessageError v-if="graphqlError">
            {{ graphqlError }}
          </MessageError>
          <LayoutForm :key="key">
            <InputString
              v-model="schemaName"
              :default-value="schemaName"
              label="name"
            />
            <InputText
              v-model="schemaDescription"
              :default-value="schemaDescription"
              label="description"
            />
          </LayoutForm>
        </div>
      </template>
      <template #footer>
        <ButtonAlt @click="$emit('close')">
          Close
        </ButtonAlt>
        <ButtonAction
          @click="executeCreateSchema"
        >
          Create database
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
  InputString,
  InputText,
  LayoutForm,
  LayoutModal,
  MessageError,
  MessageSuccess,
  Spinner,
} from '../ui/index.js'

export default {
  components: {
    ButtonAction,
    ButtonAlt,
    InputString,
    InputText,
    LayoutForm,
    LayoutModal,
    MessageError,
    MessageSuccess,
    Spinner,
  },
  emits: ['close'],
  data: function() {
    return {
      graphqlError: null,
      key: 0,
      loading: false,
      schemaDescription: null,
      schemaName: null,
      success: null,
    }
  },
  computed: {
    endpoint() {
      return '/api/graphql'
    },
    title() {
      return 'Create database'
    },
  },
  methods: {
    executeCreateSchema() {
      this.loading = true
      this.graphqlError = null
      this.success = null
      request(
        this.endpoint,
        'mutation createSchema($name:String){createSchema(name:$name){message}}',
        {
          name: this.schemaName,
        },
      )
        .then((data) => {
          this.success = data.createSchema.message
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
