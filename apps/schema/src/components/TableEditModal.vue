<template>
  <!-- when authorisation graphqlError show login-->
  <Spinner v-if="loading" />
  <div v-else-if="showLogin">
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
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
      <LayoutForm v-if="(tableDraft.oldName = null)">
        <InputString v-model="tableDraft.name" label="Table name" />
        <InputText v-model="tableDraft.description" label="Table Description" />
        <InputString
          :list="true"
          v-model="tableDraft.semantics"
          label="semantics (comma separated list of IRI defining type, and/or keyword 'id')"
        />
      </LayoutForm>
      <div v-else>ALTER IS NOT YET IMPLEMENTED</div>
    </template>
    <template v-slot:footer>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
      <ButtonAlt @click="$emit('close')">Close</ButtonAlt>
      <ButtonAction v-if="tableDraft.oldName == false" @click="executeCommand"
        >{{ action }}
      </ButtonAction>
    </template>
  </LayoutModal>
</template>

<script>
import {request} from 'graphql-request';

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
} from '@mswertz/emx2-styleguide';

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
    table: {type: Object, default: () => {}}
  },
  data: function () {
    return {
      loading: false,
      graphqlError: null,
      success: null,
      showLogin: false,
      tableDraft: {}
    };
  },
  computed: {
    title() {
      if (this.tableDraft.command === 'CREATE') {
        return `Create table`;
      } else {
        return `Alter table '${this.table.name}'`;
      }
    },
    action() {
      if (this.tableDraft.command === 'CREATE') return `Create table`;
      else return `Alter table ${this.table.name}`;
    }
  },
  methods: {
    executeCommand() {
      this.loading = true;
      this.graphqlError = null;
      this.success = null;
      request(
        'graphql',
        `mutation change($table:MolgenisTableInput){change(tables:[$table]){message}}`,
        {
          table: {
            name: this.table.name,
            description: this.table.description,
            semantics: this.table.semantics
          }
        }
      )
        .then(() => {
          if (this.table) {
            this.success = `Table ${this.table.name} altered`;
          } else {
            this.success = `Table ${this.table.name} created`;
          }
          this.$emit('close');
        })
        .catch((error) => {
          if (error.response.status === 403) {
            this.graphqlError = 'Forbidden. Do you need to login?';
            this.showLogin = true;
          } else {
            this.graphqlError = error.response.errors[0].message;
            console.graphqlError(JSON.stringify(this.error));
          }
        });
      this.loading = false;
    }
  },
  watch: {
    table: {
      deep: true,
      handler() {
        this.created();
      }
    }
  },
  created() {
    this.tableDraft = this.table;
    if (this.tableDraft.name === null) {
      this.tableDraft.command = 'CREATE';
    } else {
      this.tableDraft.command = 'ALTER';
      this.tableDraft.oldName = this.tableDraft.name;
    }
  }
};
</script>
