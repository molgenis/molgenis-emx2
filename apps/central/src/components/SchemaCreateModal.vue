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
              v-model="schemaName"
              label="name"
              :defaultValue="schemaName"
              :required="true"
            />
            <InputSelect
              label="template"
              description="Load existing database template"
              v-model="template"
              :options="templates"
            />
            <InputBoolean
              v-if="template"
              label="load example data"
              description="Include example data in the template"
              v-model="includeDemoData"
            />
            <InputText
              v-model="schemaDescription"
              label="description (optional)"
              :defaultValue="schemaDescription"
            />
          </LayoutForm>
        </div>
      </template>
      <template v-slot:footer>
        <ButtonAlt @click="$emit('close')">Close</ButtonAlt>
        <ButtonAction @click="executeCreateSchema">
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
  ButtonAlt,
  IconAction,
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
    ButtonAlt,
    InputBoolean,
    LayoutModal,
    InputString,
    InputText,
    InputSelect,
    LayoutForm,
    Spinner,
    IconAction,
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
      templates: [null, "PET_STORE", "DATA_CATALOGUE"],
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
