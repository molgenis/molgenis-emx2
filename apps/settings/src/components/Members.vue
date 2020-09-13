<template>
  <Molgenis
    :title="'Settings for ' + schema"
    :menuItems="menuItems"
    v-model="session"
  >
    <Spinner v-if="loading" />
    <div v-else>
      <MessageError v-if="error">{{ error }}</MessageError>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
    </div>
    <div v-if="session && session.roles.length > 0">
      <h2>Members</h2>
      <form v-if="canEdit" class="form-inline">
        <InputString
          class="mb-2 mr-sm-2"
          v-model="editMember['email']"
          placeholder="email address"
          label="Email:"
        />
        <InputSelect
          class="mb-2 mr-sm-2"
          v-model="editMember['role']"
          :options="roles"
          placeholder="role"
          label="Role:"
        />
        <ButtonAction @click="updateMember" class="mb-2 mr-sm-2">
          Add/update
        </ButtonAction>
      </form>
      <DataTable
        v-model="selectedItems"
        :defaultValue="selectedItems"
        :rows="members"
        :columns="['email', 'role']"
      >
        <template v-slot:rowheader="slotProps">
          <ButtonAction v-if="canEdit" @click="removeMember(slotProps.row)">
            Remove
          </ButtonAction>
        </template>
      </DataTable>
    </div>
    <div v-else>
      Not a member, cannot see settings
    </div>

    <ShowMore title="debug">
      <br />
      members: {{ JSON.stringify(members) }}
      <br />
      roles: {{ JSON.stringify(roles) }}
      <br />
      selectedItems: {{ JSON.stringify(selectedItems) }}
      <br />
      editMember: {{ JSON.stringify(editMember) }}
      <br />
      session: {{ JSON.stringify(session) }}
    </ShowMore>
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
  Spinner,
  ShowMore
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
    InputSelect,
    ShowMore
  },
  data: function() {
    return {
      session: null,
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
    canEdit() {
      return (
        this.session != null &&
        (this.session.email == "admin" ||
          this.session.roles.includes("Manager"))
      );
    },
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
    removeMember(row) {
      let name = [row.email];
      this.loading = true;
      this.error = null;
      this.success = null;
      request(
        "graphql",
        `mutation drop($member:[String]){drop(members:$member){message}}`,
        { member: name }
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
