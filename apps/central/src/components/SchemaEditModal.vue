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
            id="schema-edit-description"
            v-model="newSchemaDescription"
            label="description"
            :defaultValue="schemaDescription"
        />
        <InputBoolean
            id="schema-edit-is-changelog-enabled"
            v-model="newSchemaIsChangelogEnabled"
            label="enable changelog"
            :inplace="true"
            :defaultValue="false"
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
  LayoutModal,
  MessageError,
  MessageSuccess,
  Spinner,
  InputBoolean,
  InputText,
} from "molgenis-components";

export default {
  components: {
    MessageSuccess,
    MessageError,
    ButtonAction,
    ButtonAlt,
    LayoutModal,
    Spinner,
    InputBoolean,
    InputText,
  },
  props: {
    schemaName: String,
    schemaDescription: String,
    schemaIsChangelogEnabled: Boolean
  },
  data: function () {
    return {
      key: 0,
      loading: false,
      graphqlError: null,
      success: null,
      newSchemaDescription: this.schemaDescription,
      newSchemaIsChangelogEnabled: this.schemaIsChangelogEnabled
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
        `mutation updateSchema($name:String, $description:String, $isChangelogEnabled: Boolean) {
          updateSchema(name:$name, description: $description, isChangelogEnabled: $isChangelogEnabled) {
            message
          }
        }`,
        {
          name: this.schemaName,
          description: this.newSchemaDescription,
          isChangelogEnabled: this.newSchemaIsChangelogEnabled
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
