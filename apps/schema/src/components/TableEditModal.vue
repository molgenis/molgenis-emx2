<template>
  <!-- when authorisation error show login-->
  <Spinner v-if="loading" />
  <div v-else-if="showLogin">
    <MessageError v-if="error">{{ error }}</MessageError>
    <SigninForm @login="loginSuccess" @cancel="cancel" />
  </div>
  <!-- when update succesfull show result before close -->
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
  <!-- alter or add a table -->
  <LayoutModal v-else :title="title" :show="true" @close="$emit('close')">
    <template v-slot:body>
      <LayoutForm v-if="create">
        <InputString v-model="tableDraft.name" label="Table name" />
        <InputText v-model="tableDraft.description" label="Table Description" />
        <InputText
          v-model="tableDraft.jsonldType"
          label="jsonld type (conform JSON-LD @type specification)"
        />
      </LayoutForm>
      <div v-else>ALTER IS NOT YET IMPLEMENTED</div>
    </template>
    <template v-slot:footer>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <MessageError v-if="error">{{ error }}</MessageError>
      <ButtonAlt @click="$emit('close')">Close</ButtonAlt>
      <ButtonAction v-if="create" @click="executeCommand"
        >{{ action }}
      </ButtonAction>
    </template>
  </LayoutModal>
</template>

<script>
import { request } from "graphql-request";

import {
  ButtonAction,
  ButtonAlt,
  InputString,
  InputText,
  LayoutForm,
  LayoutModal,
  MessageError,
  MessageSuccess,
  SigninForm,
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
    LayoutForm,
    Spinner,
    SigninForm,
  },
  props: {
    schema: String,
    table: { type: Object, default: {} },
  },
  data: function () {
    return {
      loading: false,
      error: null,
      success: null,
      showLogin: false,
      create: false,
      tableDraft: {},
    };
  },
  computed: {
    title() {
      if (this.create) {
        return `Create table`;
      } else {
        return `Alter table '${this.table.name}'`;
      }
    },
    action() {
      if (this.create) return `Create table`;
      else return `Alter table ${this.table.name}`;
    },
  },
  methods: {
    executeCommand() {
      this.loading = true;
      this.error = null;
      this.success = null;
      request(
        "graphql",
        `mutation change($table:MolgenisTableInput){change(tables:[$table]){message}}`,
        {
          table: {
            name: this.table.name,
            description: this.table.description,
            jsonldType: this.table.jsonldType,
          },
        }
      )
        .then((data) => {
          if (this.table) {
            this.success = `Table ${this.table.name} altered`;
          } else {
            this.success = `Table ${this.table.name} created`;
          }
          this.$emit("close");
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
  },
  created() {
    this.tableDraft = this.table;
    if (this.tableDraft.name == null) {
      this.create = true;
      this.tableDraft.command = "CREATE";
    } else {
      this.tableDraft.command = "ALTER";
      this.tableDraft.oldName = this.tableDraft.name;
    }
  },
};
</script>
