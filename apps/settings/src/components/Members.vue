<template>
  <div>
    <Spinner v-if="loading" />
    <div v-else>
      <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
    </div>
    <div
      v-if="
        session &&
        (session.email == 'admin' ||
          (session.roles && session.roles.length > 0))
      ">
      <h5 class="card-title">Manage members</h5>
      <p>Use table below to add, edit or remove members</p>
      <form v-if="canEdit" class="form-inline">
        <InputString
          id="member-email"
          class="mb-2 mr-sm-4"
          v-model="editMember['email']"
          placeholder="email address"
          label="Email:" />
        <InputSelect
          id="member-role"
          class="mb-2 mr-sm-4"
          v-model="editMember['role']"
          :options="roles"
          placeholder="role"
          label="Role:" />
        <ButtonAction @click="updateMember" class="mb-2 mr-sm-2">
          Add/update
        </ButtonAction>
      </form>
      <TableSimple
        v-model="selectedItems"
        :defaultValue="selectedItems"
        :rows="members"
        :columns="['email', 'role']">
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
        this.session != null &&
        (this.session.email == "admin" ||
          this.session.roles.includes("Manager"))
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
        .then(data => {
          this.loadMembers();
        })
        .catch(error => {
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
        .catch(error => {
          this.graphqlError = error.response.errors[0].message;
        })
        .finally((this.loading = false));
    },
    loadMembers() {
      this.loading = true;
      request("graphql", "{_schema{name,members{email,role}roles{name}}}")
        .then(data => {
          this.schema = data._schema.name;
          this.members = data._schema.members;
          this.roles = data._schema.roles.map(role => role.name);
        })
        .catch(error => {
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
