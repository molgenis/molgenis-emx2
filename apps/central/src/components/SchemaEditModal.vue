<template>
  <div>
    <!-- whilst loading -->
    <LayoutModal v-if="loading" :title="title" :show="true">
      <template v-slot:body>
        <Spinner />
      </template>
    </LayoutModal>
    <!-- when completed -->
    <LayoutModal
      v-else-if="success"
      :title="title"
      :show="true"
      @close="$emit('close')"
    >
      <template v-slot:body>
        <MessageSuccess>{{ success }}</MessageSuccess>
      </template>
      <template v-slot:footer>
        <ButtonAction @click="$emit('close')">Close</ButtonAction>
      </template>
    </LayoutModal>
    <!-- edit schema -->
    <LayoutModal v-else :title="title" :show="true" @close="$emit('close')">
      <template v-slot:body>
        <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
        <InputText
            v-model="newSchemaDescription"
            label="description"
            :defaultValue="schemaDescription"
        />
      </template>
      <template v-slot:footer>
        <ButtonAlt @click="$emit('close')">Close</ButtonAlt>
        <ButtonAction @click="executeDeleteSchema"
          >Edit database
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
  LayoutForm,
  LayoutModal,
  MessageError,
  MessageSuccess,
  Spinner,
  InputText,
} from "molgenis-components";

export default {
  components: {
    MessageSuccess,
    MessageError,
    ButtonAction,
    ButtonAlt,
    LayoutModal,
    LayoutForm,
    Spinner,
    IconAction,
    InputText,
  },
  props: {
    schemaName: String,
    schemaDescription: String
  },
  data: function () {
    return {
      key: 0,
      loading: false,
      graphqlError: null,
      success: null,
      newSchemaDescription: this.schemaDescription
    };
  },
  computed: {
    title() {
      return "Edit database";
    },
    endpoint() {
      return "/api/graphql";
    },
  },
  methods: {
    executeDeleteSchema() {
      this.loading = true;
      this.graphqlError = null;
      this.success = null;
      request(
        this.endpoint,
        `mutation updateSchema($name:String, $description:String){updateSchema(name:$name, description: $description){message}}`,
        {
          name: this.schemaName,
          description: this.newSchemaDescription,
        }
      )
        .then((data) => {
          this.success = data.updateSchema.message;
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
