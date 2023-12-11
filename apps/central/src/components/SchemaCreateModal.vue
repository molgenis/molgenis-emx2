<template>
  <div>
    <!-- whilst loading -->
    <LayoutModal v-if="loading" :title="title" :show="true">
      <template v-slot:body>
        <Spinner />
        creating schema, may take a while ...
      </template>
    </LayoutModal>
    <!-- when update succesfull show result before close -->
    <LayoutModal
      v-else-if="success"
      :title="title"
      :show="true"
      @close="$emit('close')"
    >
      <template v-slot:body>
        <MessageSuccess>{{ success }}</MessageSuccess>
        <div v-if="template">
          Go to <a :href="'/' + schemaName">{{ schemaName }}</a
          ><br />
        </div>
        <div v-else>
          Go to edit <a :href="'/' + schemaName + '/schema/'">schema</a><br />
          Go to upload <a :href="'/' + schemaName + '/updownload/'">files</a>
        </div>
      </template>
      <template v-slot:footer>
        <ButtonAction @click="$emit('close')">Close</ButtonAction>
      </template>
    </LayoutModal>
    <!-- create schema -->
    <LayoutModal v-else :title="title" :show="true" @close="$emit('close')">
      <template v-slot:body>
        <Spinner v-if="loading" />
        <div v-else>
          <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
          <LayoutForm :key="key">
            <InputString
              id="schema-create-name"
              v-model="schemaName"
              label="name"
              :defaultValue="schemaName"
              :required="true"
              :errorMessage="validate(schemaName)"
            />
            <InputSelect
              id="schema-create-template"
              label="template"
              description="Load existing database template"
              v-model="template"
              :options="templates"
            />
            <InputBoolean
              id="schema-create-sample-data"
              v-if="template"
              label="load example data"
              description="Include example data in the template"
              v-model="includeDemoData"
            />
            <InputText
              id="schema-create-description"
              v-model="schemaDescription"
              label="description (optional)"
              :defaultValue="schemaDescription"
            />
          </LayoutForm>
        </div>
      </template>
      <template v-slot:footer>
        <ButtonAlt @click="$emit('close')">Close</ButtonAlt>
        <ButtonAction
          :disabled="validate(this.schemaName)"
          @click="executeCreateSchema"
        >
          Create database
        </ButtonAction>
      </template>
    </LayoutModal>
  </div>
</template>

<script>
import { request } from "graphql-request";

import {
  ButtonAction,
  ButtonDanger,
  ButtonAlt,
  InputString,
  InputText,
  InputBoolean,
  InputSelect,
  LayoutForm,
  LayoutModal,
  MessageError,
  MessageSuccess,
  Spinner,
} from "molgenis-components";

export default {
  components: {
    MessageSuccess,
    MessageError,
    ButtonAction,
    ButtonDanger,
    ButtonAlt,
    InputBoolean,
    LayoutModal,
    InputString,
    InputText,
    InputSelect,
    LayoutForm,
    Spinner,
  },
  data: function () {
    return {
      key: 0,
      loading: false,
      graphqlError: null,
      success: null,
      schemaName: null,
      schemaDescription: null,
      template: null,
      templates: [
        null,
        "PET_STORE",
        "FAIR_DATA_HUB",
        "DATA_CATALOGUE",
        "DATA_CATALOGUE_COHORT_STAGING",
        "DATA_CATALOGUE_NETWORK_STAGING",
        "RD3",
        "JRC_COMMON_DATA_ELEMENTS",
        "ERN_DASHBOARD",
        "ERN_CRANIO",
        "BIOBANK_DIRECTORY",
      ],
      includeDemoData: false,
    };
  },
  computed: {
    title() {
      return "Create database";
    },
    endpoint() {
      return "/api/graphql";
    },
  },
  methods: {
    validate(name) {
      const simpleName = /^[a-zA-Z][a-zA-Z0-9_ ]*$/;
      if (name === null) {
        return undefined;
      }
      if (
        simpleName.test(name) &&
        typeof name === "string" &&
        name.length < 32
      ) {
        return undefined;
      } else {
        return "Table name must start with a letter, followed by letters, underscores, a space or numbers, i.e. [a-zA-Z][a-zA-Z0-9_]*. Maximum length: 31 characters";
      }
    },
    executeCreateSchema() {
      this.loading = true;
      this.graphqlError = null;
      this.success = null;
      request(
        this.endpoint,
        `mutation createSchema($name:String, $description:String, $template: String, $includeDemoData: Boolean){createSchema(name:$name, description:$description, template: $template, includeDemoData: $includeDemoData){message}}`,
        {
          name: this.schemaName,
          description: this.schemaDescription,
          template: this.template,
          includeDemoData: this.includeDemoData,
        }
      )
        .then((data) => {
          this.success = data.createSchema.message;
          this.loading = false;
        })
        .catch((error) => {
          if (error.response.status === 403) {
            this.graphqlError =
              error.message + "Forbidden. Do you need to login?";
          } else {
            this.graphqlError = error.response.errors[0].message;
          }
          this.loading = false;
        });
    },
  },
};
</script>
