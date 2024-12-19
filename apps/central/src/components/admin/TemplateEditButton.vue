<template>
  <div>
    <IconAction v-if="!isModalShown" :icon="icon" @click="open" />
    <LayoutModal
      ref="templateEditModal"
      :title="modalTitle"
      :show="isModalShown"
      @close="close"
      @open="initializeMonaco"
    >
      <template #body>
        <LayoutForm>
          <InputSelect
            id="template-create-schema"
            label="Schema"
            description="Schema to connect template to"
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
        <ButtonAction @click="doEditTemplate">{{ buttonTitle }}</ButtonAction>
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
import { toRaw } from "vue";
import { editor } from "monaco-editor";

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
    type: {
      type: String,
      required: true,
      validator: (value) => {
        return ["insert", "update"].includes(value);
      },
    },
  },
  data() {
    return {
      editor: null,
      error: null,
      success: null,
      loading: false,
      action: this.type,
      isModalShown: false,
      selectedSchema: this.schema,
      schemas: [],
      selectedApi: this.api,
      apis: [
        "beacon_individuals",
        "beacon_biosamples",
        "beacon_biosamples",
        "beacon_catalogs",
        "beacon_g_variants",
        "beacon_datasets",
        "beacon_analyses",
        "beacon_cohorts",
        "beacon_runs",
        "beacon_individuals",
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
      return this.action === "update" ? "Edit" : "Add";
    },
  },
  created() {
    this.getSchemaList();
    if (this.selectedSchema === "default") {
      this.action = "insert";
    }
  },
  methods: {
    doEditTemplate() {
      this.loading = true;
      this.error = null;
      this.success = null;
      this.jsltTemplate = toRaw(this.editor).getValue(0);
      request(
        "_SYSTEM_/graphql",
        "mutation " +
          this.action +
          "($endpoint:String, $schema:String, $template:String) { " +
          this.action +
          "(Templates: { endpoint: $endpoint, schema: $schema, template: $template }) { message } }",
        {
          endpoint: this.selectedApi,
          schema: this.selectedSchema,
          template: this.jsltTemplate,
        }
      )
        .then((data) => {
          this.success =
            this.action === "insert"
              ? data.insert.message
              : data.update.message;
          this.loading = false;
        })
        .catch((error) => {
          if (error.response.status === 403) {
            this.error = error.message + "Forbidden. Do you need to login?";
          } else {
            this.error = error.response.errors[0].message;
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
    initializeMonaco() {
      this.editor = editor.create(this.$refs.monacoEditor, {
        value: this.jsltTemplate,
        language: "json",
        automaticLayout: true,
        scrollBeyondLastLine: false,
        minimap: { enabled: false },
        quickSuggestions: false,
      });
    },
    close() {
      this.error = null;
      this.success = null;
      this.isModalShown = false;
    },
    open() {
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
  },
};
</script>
