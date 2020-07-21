<template>
  <Molgenis :title="'Settings for ' + schema" :menuItems="menuItems">
    <Spinner v-if="loading" />
    <div v-else>
      <MessageError v-if="error">{{ error }}</MessageError>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
    </div>
    Edit settings below.
    <h2>Members</h2>
    <form class="form-inline">
      <InputString
        v-model="editMember['email']"
        placeholder="email address"
        label="Email:"
      />
      <InputSelect
        v-model="editMember['role']"
        :options="roles"
        placeholder="role"
        label="Role:"
      />
      <ButtonAction @click="updateMember">
        Add/update
      </ButtonAction>
    </form>
    <DataTable
      v-model="selectedItems"
      selectColumn="email"
      :defaultValue="selectedItems"
      :rows="members"
      :columns="['email', 'role']"
    />

    <ButtonAction @click="removeSelectedMembers">
      Remove selected members
    </ButtonAction>

    <br />
    members: {{ JSON.stringify(members) }}
    <br />
    roles: {{ JSON.stringify(roles) }}
    <br />
    selectedItems: {{ JSON.stringify(selectedItems) }}
    <br />
    editMember: {{ JSON.stringify(editMember) }}
  </Molgenis>
</template>

<script>
import {
  ButtonAction,
  ButtonAlt,
  DataTable,
  InputCheckbox,
  InputFile,
  InputSelect,
  InputString,
  LayoutCard,
  MessageError,
  MessageSuccess,
  Molgenis,
  Spinner
} from "@mswertz/emx2-styleguide";
import { request } from "graphql-request";

export default {
  components: {
    ButtonAction,
    ButtonAlt,
    InputFile,
    DataTable,
    MessageError,
    MessageSuccess,
    Spinner,
    Molgenis,
    LayoutCard,
    InputCheckbox,
    InputString,
    InputSelect
  },
  data: function() {
    return {
      schema: null,
      members: [],
      selectedItems: [],
      editMember: {},
      roles: [],
      file: null,
      error: null,
      success: null,
      loading: false
    };
  },

  computed: {
    menuItems() {
      return [
        { label: "Tables", href: "../tables/" },
        {
          label: "Schema",
          href: "../schema/"
        },
        {
          label: "Upload",
          href: "../import/"
        },
        {
          label: "Download",
          href: "../download/"
        },
        {
          label: "GraphQL",
          href: "/api/playground.html?schema=/api/graphql/" + this.schema
        },
        {
          label: "Settings",
          href: "../settings/"
        }
      ];
    }
  },
  methods: {
    removeSelectedMembers() {
      let remove = this.members
        .filter(m => this.selectedItems.includes(m.email))
        .map(m => m.email);
      this.loading = true;
      this.error = null;
      this.success = null;
      request(
        "graphql",
        `mutation drop($members:[String]){drop(members:$members){message}}`,
        { members: remove }
      )
        .then(data => {
          this.loadSchema();
        })
        .catch(error => {
          this.error = error.response.errors[0].message;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    updateMember() {
      this.loading = true;
      this.error = null;
      this.success = null;
      request(
        "graphql",
        `mutation alter($editMember:MolgenisMembersInput){alter(members:[$editMember]){message}}`,
        { editMember: this.editMember }
      )
        .then(() => {
          this.selectedItems = [];
          this.loadSchema();
        })
        .catch(error => {
          this.error = error.response.errors[0].message;
        })
        .finally((this.loading = false));
    },
    loadSchema() {
      this.loading = true;
      request("graphql", "{_schema{name,members{email,role}roles{name}}}")
        .then(data => {
          this.schema = data._schema.name;
          this.members = data._schema.members;
          this.roles = data._schema.roles.map(role => role.name);
        })
        .catch(error => {
          this.error = error.response.errors[0].message;
        })
        .finally((this.loading = false));
    }
  },
  created() {
    this.loadSchema();
  }
};
</script>

<docs>
    Example
    ```
    <Download schema="pet store"/>

    ```
</docs>
