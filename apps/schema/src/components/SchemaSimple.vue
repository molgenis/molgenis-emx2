<template>
  <div>
    <ButtonAlt class="pl-0" v-if="!toc" @click="toc = true">
      show table of contents
    </ButtonAlt>
    <div class="row">
      <div v-if="toc" class="col-2">
        <div class="fixedContainer">
          <SchemaToc :tables="schema.tables" />
          <ButtonAction @click="saveSchema">Save</ButtonAction>&nbsp;
          <ButtonAction @click="loadSchema">Reset</ButtonAction>
          <ButtonAlt class="pl-0" @click="toc = false">
            hide table of contents
          </ButtonAlt>
          <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
          <MessageError v-if="error">{{ error }}</MessageError>
          <MessageWarning v-if="warning">{{ warning }}</MessageWarning>
        </div>
      </div>
      <div :class="toc ? 'col-10' : 'col-12'">
        <Spinner v-if="loading" />
        <div v-else :key="timestamp">
          <Yuml :schema="schema" :key="JSON.stringify(schema)" />
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
  top: 0;
}
</style>

<script>
import { request } from "graphql-request";
import Yuml from "./Yuml";
import SchemaEditor from "./SchemaEditor";
import SchemaToc from "./SchemaToc";
import {
  ButtonAction,
  MessageError,
  MessageSuccess,
  MessageWarning,
  IconAction,
  ButtonAlt,
} from "@mswertz/emx2-styleguide";

export default {
  components: {
    Yuml,
    SchemaEditor,
    ButtonAction,
    MessageError,
    MessageSuccess,
    MessageWarning,
    SchemaToc,
    IconAction,
    ButtonAlt,
  },
  data() {
    return {
      schema: {},
      loading: false,
      error: null,
      warning: null,
      success: null,
      timestamp: Date.now(),
      toc: true,
    };
  },
  methods: {
    saveSchema() {
      this.error = null;
      this.loading = true;
      request(
        "graphql",
        `mutation change($tables:[MolgenisTableInput]){change(tables:$tables){message}}`,
        {
          tables: this.schema.tables,
        }
      )
        .then((data) => {
          this.loadSchema();
          this.timestamp = Date.now();
          this.success = `Schema saved`;
          this.warning = null;
        })
        .catch((error) => {
          if (error.response.status === 403) {
            this.error = "Forbidden. Do you need to login?";
            this.showLogin = true;
          } else {
            this.error = error.response.errors[0].message;
            console.error(JSON.stringify(this.error));
          }
        });
      this.loading = false;
    },
    loadSchema() {
      this.error = null;
      this.loading = true;
      this.schema = {};
      this.tables = null;
      request(
        "graphql",
        "{_schema{name,tables{name,inherit,externalSchema,description,jsonldType,columns{name,columnType,columnFormat,inherited,key,refSchema,refTable,refLink,mappedBy,required,description,jsonldType,validationExpression,visibleExpression}}}}"
      )
        .then((data) => {
          this.schema = this.addOldNames(data._schema);
          this.timestamp = Date.now();
        })
        .catch((error) => {
          this.error = error.response.errors[0].message;
          if (
            this.error.includes("Field '_schema' in type 'Query' is undefined")
          ) {
            this.error =
              "Schema is unknown or permission denied (might you need to login with authorized user?)";
          }
        })
        .finally(() => {
          this.loading = false;
          this.warning = null;
        });
    },
    addOldNames(schema) {
      if (schema) {
        if (schema.tables) {
          schema.tables.forEach((t) => {
            t.oldName = t.name;
            if (t.columns) {
              t.columns.forEach((c) => (c.oldName = c.name));
            } else {
              t.columns = [];
              ("");
            }
          });
        } else {
          schema.tables = [];
        }
      }
      return schema;
    },
  },
  created() {
    this.loadSchema();
  },
  watch: {
    schema: {
      deep: true,
      handler() {
        this.warning = "Unsaved changes";
      },
    },
  },
};
</script>
