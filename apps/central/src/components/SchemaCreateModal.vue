<template>
  <div>
    <!-- whilst loading -->
    <LayoutModal v-if="loading" :title="title" :show="true">
      <template v-slot:body>
        <Spinner />
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
        Go to edit <a :href="'/' + schemaName + '/schema/'">schema</a><br />
        Go to upload <a :href="'/' + schemaName + '/updownload/'">files</a>
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
            <InputText
              v-model="schemaDescription"
              label="description (optional)"
              :defaultValue="schemaDescription"
            />
            <InputRadio
              label="load data (optional)"
              description="Choose options below to load pre-existing schema and/or contents into your newly created database"
              v-model="option"
              :options="Object.keys(options)"
              :required="true"
            />
            <InputString
              v-if="option == 'from source URL'"
              v-model="sourceURLs"
              label="source URL"
              description="You can automatically populate your database from one or more url that has contents equal as when you download a zip."
              :list="true"
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
  InputRadio,
  LayoutForm,
  LayoutModal,
  MessageError,
  MessageSuccess,
  Spinner,
} from "@mswertz/emx2-styleguide";

export default {
  components: {
    MessageSuccess,
    MessageError,
    ButtonAction,
    ButtonAlt,
    LayoutModal,
    InputString,
    InputText,
    InputRadio,
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
      sourceURLs: [],
      option: "none",
      options: {
        none: [],
        "cohort catalogue": [
          "/public_html/apps/data/datacatalogue",
          "/public_html/apps/data/datacatalogue/Cohorts",
        ],
        "from source URL": [],
      },
    };
  },
  computed: {
    title() {
      return "Create database";
    },
    endpoint() {
      return "/api/graphql";
    },
    host() {
      if (window) {
        return window.location.origin;
      }
    },
  },
  methods: {
    executeCreateSchema() {
      this.loading = true;
      this.graphqlError = null;
      this.success = null;
      request(
        this.endpoint,
        `mutation createSchema($name:String, $description:String, $sourceURLs: [String]){createSchema(name:$name, description:$description, sourceURLs: $sourceURLs){message}}`,
        {
          name: this.schemaName,
          description: this.schemaDescription,
          sourceURsL: this.sourceURLs,
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
  watch: {
    option() {
      this.sourceURL = this.options[this.option];
    },
  },
};
</script>
