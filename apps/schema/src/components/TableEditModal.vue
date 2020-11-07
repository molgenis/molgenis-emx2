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
      <LayoutForm :key="key">
        <InputString v-model="tableName" label="Table name" />
        <InputText v-model="tableDescription" label="Table Description" />
      </LayoutForm>
    </template>
    <template v-slot:footer>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <MessageError v-if="error">{{ error }}</MessageError>
      <ButtonAlt @click="$emit('close')">Close</ButtonAlt>
      <ButtonAction @click="executeCommand">{{ action }}</ButtonAction>
    </template>
  </LayoutModal>
</template>

<script>
import {request} from "graphql-request";

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
  Spinner
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
    SigninForm
  },
  props: {
    schema: String,
    create: {
      type: Boolean,
      default: true
    }
  },
  data: function() {
    return {
      key: 0,
      tableName: String,
      tableDescription: String,
      loading: false,
      error: null,
      success: null,
      showLogin: false
    };
  },
  computed: {
    title() {
      if (this.create) {
        return `Create table`;
      } else {
        return `Alter table '${this.tableName}'`;
      }
    },
    action() {
      if (this.create) return `Create table`;
      else return `Alter table`;
    }
  },
  methods: {
    executeCommand() {
      this.loading = true;
      this.error = null;
      this.success = null;
      let command = this.create ? "create" : "alter";
      request(
        "graphql",
        `mutation ${command}($name:String){${command}(tables:[{name:$name}]){message}}`,
        {
          name: this.tableName
        }
      )
        .then(data => {
          if (this.create) {
            this.success = `Table ${this.tableName} created`;
          } else {
            this.success = `Table ${this.table.name} altered`;
          }
          this.$emit("close");
        })
        .catch(error => {
          if (error.response.status === 403) {
            this.error = "Forbidden. Do you need to login?";
            this.showLogin = true;
          } else this.error = error;
        });
      this.loading = false;
    }
  }
};
</script>
