<template>
  <div class="container-fluid">
    <div class="d-flex justify-content-between">
      <div class="form-inline">
        <h1>Schema editor: {{ schema.name }}</h1>
        <ButtonAction @click="saveSchema" class="ml-2">Save</ButtonAction>&nbsp;
        <ButtonAction @click="loadSchema" class="ml-2">Reset</ButtonAction>
      </div>
      <div>
        <ButtonAction @click="showDiagram = !showDiagram">
          {{ showDiagram ? 'Hide' : 'Show' }} Diagram
        </ButtonAction>
      </div>
    </div>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
    <div class="row">
      <div class="col-2 bg-white">
        <div class="fixedContainer mr-n3 overflow-auto">
          <SchemaToc :tables.sync="schema.tables" />
        </div>
      </div>
      <div class="bg-white col ml-2 overflow-auto">
        <Spinner v-if="loading" />
        <div v-else :key="timestamp">
          <Yuml
            :schema="schema"
            :key="JSON.stringify(schema)"
            v-if="showDiagram"
          />
          <SchemaEditor v-model="schema" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.fixedContainer {
  position: -webkit-sticky; /* Safari */
  position: sticky;
  max-height: 100vh;
  top: 0;
}
</style>

<script>
import {request} from 'graphql-request';
import Yuml from './Yuml';
import SchemaEditor from './SchemaEditor';
import SchemaToc from './SchemaToc';
import {
  ButtonAction,
  MessageError,
  MessageSuccess,
  Spinner
} from '@mswertz/emx2-styleguide';

export default {
  components: {
    Yuml,
    SchemaEditor,
    ButtonAction,
    MessageError,
    MessageSuccess,
    SchemaToc,
    Spinner
  },
  data() {
    return {
      schema: {},
      loading: false,
      graphqlError: null,
      warning: null,
      success: null,
      timestamp: Date.now(),
      showDiagram: false
    };
  },
  methods: {
    saveSchema() {
      this.graphqlError = null;
      this.loading = true;
      request(
        'graphql',
        `mutation change($tables:[MolgenisTableInput]){change(tables:$tables){message}}`,
        {
          tables: this.schema.tables
        }
      )
        .then(() => {
          this.loadSchema();
          this.timestamp = Date.now();
          this.success = `Schema saved`;
          this.warning = null;
        })
        .catch((error) => {
          if (error.response.status === 403) {
            this.graphqlError = 'Forbidden. Do you need to login?';
            this.showLogin = true;
          } else {
            this.graphqlError = error.response.errors[0].message;
          }
        });
      this.loading = false;
    },
    loadSchema() {
      this.graphqlError = null;
      this.loading = true;
      this.schema = {};
      this.tables = null;
      request(
        'graphql',
        '{_schema{name,tables{name,tableType,inherit,externalSchema,description,semantics,columns{name,columnType,inherited,key,refSchema,refTable,refLink,refBack,required,description,semantics,validation,visible}}}}'
      )
        .then((data) => {
          this.schema = this.addOldNamesAndRemoveMeta(data._schema);
          this.timestamp = Date.now();
        })
        .catch((error) => {
          this.graphqlError = error.response.errors[0].message;
          if (
            this.graphqlError.includes(
              "Field '_schema' in type 'Query' is undefined"
            )
          ) {
            this.error =
              'Schema is unknown or permission denied (might you need to login with authorized user?)';
          }
        })
        .finally(() => {
          this.loading = false;
          this.warning = null;
        });
    },
    addOldNamesAndRemoveMeta(schema) {
      if (schema) {
        if (schema.tables) {
          let tables = schema.tables.filter(
            (table) => table.tableType != 'ONTOLOGIES'
          );
          tables.forEach((t) => {
            t.oldName = t.name;
            if (t.columns) {
              t.columns = t.columns
                .filter((c) => !c.name.startsWith('mg_'))
                .map((c) => {
                  c.oldName = c.name;
                  return c;
                })
                .filter((c) => !c.inherited);
            } else {
              t.columns = [];
            }
          });
          schema.tables = tables;
        } else {
          schema.tables = [];
        }
      }
      return schema;
    }
  },
  created() {
    this.loadSchema();
  },
  watch: {
    schema: {
      deep: true,
      handler() {
        this.warning = 'Unsaved changes';
      }
    }
  }
};
</script>
