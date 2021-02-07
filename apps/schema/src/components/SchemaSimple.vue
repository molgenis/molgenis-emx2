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
    SchemaToc,
    IconAction,
    ButtonAlt,
  },
  data() {
    return {
      schema: {},
      loading: false,
      error: null,
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
          this.success = `Schema saved`;
          this.timestamp = Date.now();
          this.loadSchema();
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
        "{_schema{name,tables{name,inherit,externalSchema,description,jsonldType,columns{name,columnType,columnFormat,inherited,key,refSchema,refTable,refLink,mappedBy,nullable,description,jsonldType,validationExpression,visibleExpression}}}}"
      )
        .then((data) => {
          this.schema = data._schema;
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
        .finally((this.loading = false));
    },
  },
  created() {
    this.loadSchema();
  },
};
</script>
