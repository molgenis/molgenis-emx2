<template>
  <div>
    <Spinner v-if="loading" />
    <div v-else>
      <MessageError v-if="graphqlError">
        {{ graphqlError }}
      </MessageError>
      <MessageSuccess v-if="success">
        {{ success }}
      </MessageSuccess>
    </div>
    <div
      v-if="
        session &&
          (session.email == 'admin' ||
            (session.roles && session.roles.length > 0))
      "
    >
      <h5 class="card-title">
        Manage members
      </h5>
      <p>Use table below to add, edit or remove members</p>
      <form v-if="canEdit" class="form-inline">
        <InputString
          v-model="editMember['email']"
          class="mb-2 mr-sm-4"
          label="Email:"
          placeholder="email address"
        />
        <InputSelect
          v-model="editMember['role']"
          class="mb-2 mr-sm-4"
          label="Role:"
          :options="roles"
          placeholder="role"
        />
        <ButtonAction class="mb-2 mr-sm-2" @click="updateMember">
          Add/update
        </ButtonAction>
      </form>
      <TableSimple
        v-model="selectedItems"
        :columns="['email', 'role']"
        :default-value="selectedItems"
        :rows="members"
      >
        <template #rowheader="slotProps">
          <ButtonAction v-if="canEdit" @click="removeMember(slotProps.row)">
            Remove
          </ButtonAction>
        </template>
      </TableSimple>
    </div>
    <div v-else>
      Not a member, cannot see settings
    </div>

    <ShowMore title="debug">
      <br>
      members: {{ JSON.stringify(members) }}
      <br>
      roles: {{ JSON.stringify(roles) }}
      <br>
      selectedItems: {{ JSON.stringify(selectedItems) }}
      <br>
      editMember: {{ JSON.stringify(editMember) }}
      <br>
      session: {{ JSON.stringify(session) }}
    </ShowMore>
  </div>
</template>

<script>
import {request} from 'graphql-request'
import {
  ButtonAction,
  InputSelect,
  InputString,
  MessageError,
  MessageSuccess,
  ShowMore,
  Spinner,
  TableSimple,
} from '@/components/ui/index.js'

export default {
  components: {
    ButtonAction,
    InputSelect,
    InputString,
    MessageError,
    MessageSuccess,
    ShowMore,
    Spinner,
    TableSimple,
  },
  props: {
    session: Object,
  },
  data: function() {
    return {
      editMember: {},
      file: null,
      graphqlError: null,
      loading: false,
      members: [],
      roles: [],
      schema: null,
      selectedItems: [],
      success: null,
    }
  },

  computed: {
    canEdit() {
      return (
        this.session != null &&
        (this.session.email == 'admin' ||
          this.session.roles.includes('Manager'))
      )
    },
  },
  created() {
    this.loadMembers()
  },
  methods: {
    loadMembers() {
      this.loading = true
      request('graphql', '{_schema{name,members{email,role}roles{name}}}')
        .then((data) => {
          this.schema = data._schema.name
          this.members = data._schema.members
          this.roles = data._schema.roles.map((role) => role.name)
        })
        .catch((error) => {
          this.graphqlError = error.response.errors[0].message
        })
        .finally((this.loading = false))
    },
    removeMember(row) {
      let name = [row.email]
      this.loading = true
      this.graphqlError = null
      this.success = null
      request(
        'graphql',
        'mutation drop($member:[String]){drop(members:$member){message}}',
        {member: name},
      )
        .then(() => {
          this.loadMembers()
        })
        .catch((error) => {
          this.graphqlError = error.response.errors[0].message
        })
        .finally(() => {
          this.loading = false
        })
    },
    updateMember() {
      this.loading = true
      this.graphqlError = null
      this.success = null
      request(
        'graphql',
        'mutation change($editMember:MolgenisMembersInput){change(members:[$editMember]){message}}',
        {editMember: this.editMember},
      )
        .then(() => {
          this.selectedItems = []
          this.loadMembers()
        })
        .catch((error) => {
          this.graphqlError = error.response.errors[0].message
        })
        .finally((this.loading = false))
    },
  },
}
</script>
