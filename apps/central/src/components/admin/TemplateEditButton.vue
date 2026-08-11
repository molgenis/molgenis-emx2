<template>
  <div>
    <IconAction v-if="!isModalShown" :icon="icon" @click="open" />
    <LayoutModal
      ref="templateEditModal"
      :title="modalTitle"
      :show="isModalShown"
      @close="close"
    >
      <template #body>
        <LayoutForm>
          <InputSelect
            id="template-create-schema"
            label="Schema"
            :description="schemaDescription"
            v-model="selectedSchema"
            :options="schemas"
            :readonly="action === 'update'"
          ></InputSelect>
          <InputSelect
            id="template-create-api"
            label="API"
            description="API to connect template to"
            v-model="selectedApi"
            :options="apis"
            :readonly="action === 'update'"
          ></InputSelect>
          <InputSelect
            v-if="tables.length"
            id="template-create-table"
            label="Table"
            description="Optional: table this endpoint should query. Leave empty to use the entry type's default table."
            v-model="selectedTable"
            :options="tables"
            :required="false"
            placeholder="Use default table"
          ></InputSelect>
          <FormGroup label="Template">
            <div
              class="border rounded"
              id="monaco-container"
              ref="monacoEditor"
              style="height: 500px"
            ></div>
          </FormGroup>
        </LayoutForm>
      </template>
      <template v-slot:footer>
        <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
        <MessageError v-if="error">{{ error }}</MessageError>
        <ButtonAlt @click="close">Close</ButtonAlt>
        <ButtonAction @click="saveTemplate">{{ buttonTitle }}</ButtonAction>
      </template>
    </LayoutModal>
  </div>
</template>
<script>
import {
  ButtonAction,
  ButtonAlt,
  IconAction,
  LayoutModal,
  InputSelect,
  InputText,
  LayoutForm,
  FormGroup,
  MessageSuccess,
  MessageError,
} from "molgenis-components";
import { request } from "graphql-request";
import { markRaw } from "vue";
import { editor } from "monaco-editor";

const BUILT_IN_TEMPLATE_SCHEMA = "default";

export default {
  components: {
    IconAction,
    LayoutModal,
    ButtonAction,
    ButtonAlt,
    InputSelect,
    InputText,
    LayoutForm,
    FormGroup,
    MessageSuccess,
    MessageError,
  },
  props: {
    icon: {
      type: String,
      default: "edit",
    },
    template: {
      type: String,
      required: false,
    },
    schema: {
      type: String,
      required: false,
    },
    api: {
      type: String,
      required: false,
    },
    tableName: {
      type: String,
      required: false,
    },
    type: {
      type: String,
      required: true,
      validator: (value) => {
        return ["insert", "update"].includes(value);
      },
    },
  },
  emits: ["saved"],
  data() {
    return {
      editor: null,
      error: null,
      success: null,
      loading: false,
      action: this.initialAction(),
      isModalShown: false,
      hasSaved: false,
      selectedSchema: this.schema,
      schemas: [],
      selectedTable: this.tableName,
      tables: [],
      selectedApi: this.api,
      apis: [
        "beacon_analyses",
        "beacon_biosamples",
        "beacon_catalogs",
        "beacon_cohorts",
        "beacon_datasets",
        "beacon_g_variants",
        "beacon_individuals",
        "beacon_runs",
        "VCF",
      ],
      jsltTemplate: this.template,
    };
  },
  computed: {
    modalTitle() {
      return this.action === "update" ? "Edit template" : "Add template";
    },
    buttonTitle() {
      return this.action === "update" ? "Save" : "Add";
    },
    schemaDescription() {
      return this.action === "update"
        ? "Schema this template is connected to"
        : "Schema to connect template to";
    },
  },
  created() {
    this.getSchemaList();
    this.getTableList(this.selectedSchema);
  },
  methods: {
    /**
     * Built-in templates (schema "default") are shipped with the server and rewritten on every
     * startup, so they are never updated in place: editing one is the starting point for a new,
     * schema specific template.
     */
    initialAction() {
      return this.type === "update" && this.schema !== BUILT_IN_TEMPLATE_SCHEMA
        ? "update"
        : "insert";
    },
    saveTemplate() {
      this.error = null;
      this.success = null;
      if (
        !this.selectedSchema ||
        this.selectedSchema === BUILT_IN_TEMPLATE_SCHEMA
      ) {
        this.error =
          "Select a schema. Built-in default templates cannot be changed, saving creates a template for the selected schema.";
        return;
      }
      if (!this.selectedApi) {
        this.error = "Select an API.";
        return;
      }
      this.loading = true;
      this.jsltTemplate = this.editor
        ? this.editor.getValue()
        : this.jsltTemplate;
      request(
        "_SYSTEM_/graphql",
        "mutation save($endpoint:String, $schema:String, $tableName:String, $template:String) {" +
          " save(Templates: { endpoint: $endpoint, schema: $schema, tableName: $tableName, template: $template }) { message } }",
        {
          endpoint: this.selectedApi,
          schema: this.selectedSchema,
          tableName: this.selectedTable || null,
          template: this.jsltTemplate,
        }
      )
        .then((data) => {
          this.success = data.save.message;
          this.hasSaved = true;
          // the template now exists, so further edits update it instead of adding another one
          this.action = "update";
          this.loading = false;
        })
        .catch((error) => {
          if (error.response?.status === 403) {
            this.error = error.message + "Forbidden. Do you need to login?";
          } else {
            this.error = error.response?.errors?.[0]?.message ?? error.message;
          }
          this.loading = false;
        });
    },
    getSchemaList() {
      this.loading = true;
      request("graphql", `{_schemas{id,label,description}}`)
        .then((data) => {
          this.schemas = data._schemas.map((schema) => schema.id);
          this.loading = false;
        })
        .catch((error) => {
          console.error("internal server error", error);
          this.error = "internal server error" + error;
          this.loading = false;
        });
    },
    getTableList(schemaId) {
      if (!schemaId || schemaId === BUILT_IN_TEMPLATE_SCHEMA) {
        this.tables = [];
        return;
      }
      request(schemaId + "/graphql", `{_schema{tables{name}}}`)
        .then((data) => {
          this.tables = data._schema.tables.map((table) => table.name);
          this.clearTableIfUnknown();
        })
        .catch((error) => {
          console.error("could not load tables for schema " + schemaId, error);
          this.tables = [];
        });
    },
    clearTableIfUnknown() {
      if (this.selectedTable && !this.tables.includes(this.selectedTable)) {
        this.selectedTable = null;
      }
    },
    initializeMonaco() {
      this.disposeMonaco();
      this.editor = markRaw(
        editor.create(this.$refs.monacoEditor, {
          value: this.jsltTemplate,
          language: "json",
          automaticLayout: true,
          scrollBeyondLastLine: false,
          minimap: { enabled: false },
          quickSuggestions: false,
        })
      );
    },
    disposeMonaco() {
      if (this.editor) {
        this.editor.dispose();
        this.editor = null;
      }
    },
    close() {
      this.disposeMonaco();
      this.isModalShown = false;
      if (this.hasSaved) {
        this.hasSaved = false;
        this.$emit("saved");
      }
    },
    open() {
      // start from the stored template again, the previous session may have left edits behind
      this.error = null;
      this.success = null;
      this.action = this.initialAction();
      this.selectedSchema = this.schema;
      this.selectedApi = this.api;
      this.selectedTable = this.tableName;
      this.jsltTemplate = this.template;
      this.getTableList(this.selectedSchema);
      this.isModalShown = true;
    },
  },
  watch: {
    isModalShown(newVal) {
      if (newVal) {
        this.$nextTick(() => {
          this.initializeMonaco();
        });
      }
    },
    selectedSchema(newVal, oldVal) {
      // when the user picks a different schema, reload its tables
      if (newVal !== oldVal) {
        this.getTableList(newVal);
      }
    },
  },
  beforeUnmount() {
    this.disposeMonaco();
  },
};
</script>
