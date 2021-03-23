<template>
  <div class="container-fluid">
    <ButtonAlt v-if="!toc" class="pl-0" @click="toc = true">
      show table of contents
    </ButtonAlt>
    <div class="row">
      <div v-if="toc" class="col-2 bg-white">
        <div class="fixedContainer">
          <ButtonAlt class="pl-0" @click="toc = false">
            hide table of contents
          </ButtonAlt>
          <br>
          <ButtonAction @click="saveSchema">
            Save
          </ButtonAction>&nbsp;
          <ButtonAction @click="loadSchema">
            Reset
          </ButtonAction>
          <MessageSuccess v-if="success">
            {{ success }}
          </MessageSuccess>
          <MessageError v-if="graphqlError">
            {{ graphqlError }}
          </MessageError>
          <MessageWarning v-if="warning">
            {{ warning }}
          </MessageWarning>
          <SchemaToc :tables="schema.tables" />
        </div>
      </div>
      <div
        class="bg-white"
        :class="toc ? 'col-10' : 'col-12'"
        style="overflow-y: scroll;"
      >
        <Spinner v-if="loading" />
        <div v-else :key="timestamp">
          <Yuml :key="JSON.stringify(schema)" :schema="schema" />
          <SchemaEditor v-model="schema" />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {request} from 'graphql-request'
import SchemaEditor from './SchemaEditor.vue'
import SchemaToc from './SchemaToc.vue'
import Yuml from './Yuml.vue'
import {ButtonAction, ButtonAlt, MessageError, MessageSuccess, MessageWarning, Spinner} from '@/components/ui/index.js'

export default {
  components: {
    ButtonAction,
    ButtonAlt,
    MessageError,
    MessageSuccess,
    MessageWarning,
    SchemaEditor,
    SchemaToc,
    Spinner,
    Yuml,
  },
  data() {
    return {
      graphqlError: null,
      loading: false,
      schema: {},
      success: null,
      timestamp: Date.now(),
      toc: true,
      warning: null,
    }
  },
  watch: {
    schema: {
      deep: true,
      handler() {
        this.warning = 'Unsaved changes'
      },
    },
  },
  created() {
    this.loadSchema()
  },
  methods: {
    addOldNames(schema) {
      if (schema) {
        if (schema.tables) {
          schema.tables.forEach((t) => {
            t.oldName = t.name
            if (t.columns) {
              t.columns = t.columns
                .map((c) => {
                  c.oldName = c.name
                  return c
                })
                .filter((c) => !c.inherited)
            } else {
              t.columns = []
            }
          })
        } else {
          schema.tables = []
        }
      }
      return schema
    },
    loadSchema() {
      this.graphqlError = null
      this.loading = true
      this.schema = {}
      this.tables = null
      request(
        'graphql',
        '{_schema{name,tables{name,inherit,externalSchema,description,semantics,columns{name,columnType,columnFormat,inherited,key,refSchema,refTable,refLink,mappedBy,required,description,semantics,validationExpression,visibleExpression}}}}',
      )
        .then((data) => {
          this.schema = this.addOldNames(data._schema)
          this.timestamp = Date.now()
        })
        .catch((error) => {
          this.graphqlError = error.response.errors[0].message
          if (
            this.graphqlError.includes(
              'Field \'_schema\' in type \'Query\' is undefined',
            )
          ) {
            this.error =
              'Schema is unknown or permission denied (might you need to login with authorized user?)'
          }
        })
        .finally(() => {
          this.loading = false
          this.warning = null
        })
    },
    saveSchema() {
      this.graphqlError = null
      this.loading = true
      request(
        'graphql',
        'mutation change($tables:[MolgenisTableInput]){change(tables:$tables){message}}',
        {
          tables: this.schema.tables,
        },
      )
        .then(() => {
          this.loadSchema()
          this.timestamp = Date.now()
          this.success = 'Schema saved'
          this.warning = null
        })
        .catch((error) => {
          if (error.response.status === 403) {
            this.graphqlError = 'Forbidden. Do you need to login?'
            this.showLogin = true
          } else {
            this.graphqlError = error.response.errors[0].message
          }
        })
      this.loading = false
    },
  },
}
</script>

<style scoped>
.fixedContainer {
  max-height: 100vh;
  overflow-y: scroll;
  position: -webkit-sticky; /* Safari */
  position: sticky;
  top: 0;
}
</style>
