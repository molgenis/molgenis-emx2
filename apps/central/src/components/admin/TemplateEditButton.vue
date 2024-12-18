<template>
  <div>
    <IconAction v-if="!isModalShown" :icon="icon" @click="open" />
    <LayoutModal
      v-else
      ref="templateEditModal"
      :title="modalTitle"
      :show="isModalShown"
      @close="close"
    >
      <template #body>
        <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
        <MessageError v-if="error">{{ error }}</MessageError>
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
          <InputText
            id="template-jslt"
            v-model="jsltTemplate"
            label="JSLT template"
          />
        </LayoutForm>
      </template>
      <template v-slot:footer>
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
  MessageSuccess,
  MessageError,
} from "molgenis-components";
import { request } from "graphql-request";

export default {
  components: {
    IconAction,
    LayoutModal,
    ButtonAction,
    ButtonAlt,
    InputSelect,
    InputText,
    LayoutForm,
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
    open() {
      this.isModalShown = true;
    },
    close() {
      this.isModalShown = false;
    },
  },
};
</script>
