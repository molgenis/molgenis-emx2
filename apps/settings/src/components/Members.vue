<template>
  <div>
    <Spinner v-if="loading" />
    <div v-else>
      <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
    </div>
    <div v-if="session?.email === 'admin' || session?.roles?.length">
      <h5 class="card-title">Manage members</h5>
      <p>Use table below to add, edit or remove members</p>
      <form v-if="canEdit" class="form-inline">
        <InputString
          id="member-email"
          class="mb-2 mr-2 mr-sm-4 email-input"
          v-model="editMember['email']"
          placeholder="email or username"
          label="Email:"
        >
          <template v-slot:prepend>
            <Info class="mr-1">
              Enter valid user email address or use the specials group: 'user'
              or 'anonymous'
            </Info>
          </template>
        </InputString>
        <InputSelect
          id="member-role"
          class="mb-2 mr-sm-4"
          v-model="editMember['role']"
          :options="roles"
          placeholder="role"
          label="Role:"
        />
        <ButtonAction @click="updateMember" class="mb-2 mr-sm-2">
          Add/update
        </ButtonAction>
      </form>
      <TableSimple
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
      </TableSimple>
    </div>
    <div v-else>Not a member, cannot see settings</div>
  </div>
</template>

<script>
import {
  ButtonAction,
  ButtonAlt,
  TableSimple,
  Info,
  InputCheckbox,
  InputFile,
  InputSelect,
  InputString,
  LayoutCard,
  MessageError,
  MessageSuccess,
  Spinner,
} from "molgenis-components";
import { request } from "graphql-request";

export default {
  components: {
    ButtonAction,
    ButtonAlt,
    InputFile,
    TableSimple,
    MessageError,
    MessageSuccess,
    Spinner,
    LayoutCard,
    Info,
    InputCheckbox,
    InputString,
    InputSelect,
  },
  props: {
    session: Object,
  },
  data: function () {
    return {
      schema: null,
      members: [],
      selectedItems: [],
      editMember: {},
      roles: [],
      file: null,
      graphqlError: null,
      success: null,
      loading: false,
    };
  },
  computed: {
    canEdit() {
      return (
        this.session?.email === "admin" ||
        this.session?.roles.includes("Manager")
      );
    },
  },
  methods: {
    removeMember(row) {
      let name = [row.email];
      this.loading = true;
      this.graphqlError = null;
      this.success = null;
      request(
        "graphql",
        `mutation drop($member:[String]){drop(members:$member){message}}`,
        { member: name }
      )
        .then(() => {
          this.loadMembers();
        })
        .catch((error) => {
          this.graphqlError = error.response.errors[0].message;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    updateMember() {
      this.loading = true;
      this.graphqlError = null;
      this.success = null;
      request(
        "graphql",
        `mutation change($editMember:MolgenisMembersInput){change(members:[$editMember]){message}}`,
        { editMember: this.editMember }
      )
        .then(() => {
          this.selectedItems = [];
          this.loadMembers();
        })
        .catch((error) => {
          this.graphqlError = error.response.errors[0].message;
        })
        .finally((this.loading = false));
    },
    loadMembers() {
      this.loading = true;
      request("graphql", "{_schema{name,members{email,role}roles{name}}}")
        .then((data) => {
          this.schema = data._schema.name;
          this.members = data._schema.members;
          this.roles = data._schema.roles.map((role) => role.name);
        })
        .catch((error) => {
          this.graphqlError = error.response.errors[0].message;
        })
        .finally((this.loading = false));
    },
  },
  created() {
    this.loadMembers();
  },
};
</script>

<style>
.email-input input {
  min-width: 20rem !important;
}
.email-input .text-danger {
  margin-left: 0.5rem;
}
.email-input .input-group {
  flex-wrap: nowrap;
}
</style>
