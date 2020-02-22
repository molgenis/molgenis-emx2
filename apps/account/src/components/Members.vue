<template>
  <div>
    <!-- todo: change to generic RowForm for this if we make settings table fields behave as normal tables?-->
    <div v-if="view === 'list'">
      <MessageError v-if="error">{{ error }}</MessageError>
      <DataTable v-else :columns="['user', 'role']" :rows="members">
        <template v-slot:colheader="slotProps">
          <IconAction icon="plus" @click="showAdd" />
        </template>
        <template v-slot:rowheader="slotProps">
          <IconBar>
            <IconAction icon="edit" @click="showEdit(slotProps.row)" />
          </IconBar>
        </template>
      </DataTable>
      endpoint: '{{ endpoint }}' members: {{ JSON.stringify(members) }}
    </div>
    <LayoutModal
      v-else-if="view === 'add'"
      title="add member"
      @close="view = 'list'"
    >
      <template v-slot:body>
        <InputString label="Email address" />
        <InputSelect label="Role" items="Manager,Editor,Viewer" />
      </template>
      <template v-slot:footer>
        <ButtonAlt @click="view = 'list'">Cancel</ButtonAlt>
        <ButtonAction>Add member</ButtonAction>
      </template>
    </LayoutModal>
    <LayoutModal v-else-if="view === 'edit'" title="Update member">
      <template v-slot:body>
        <InputString
          v-model="currentMember.user"
          label="Email address"
          readonly
          :default-value="currentMember.user"
        />
        <InputSelect
          v-model="currentMember.role"
          label="Role"
          :default-value="currentMember.role"
          :items="roles"
        />
      </template>
      <template v-slot:footer>
        <ButtonAlt @click="view = 'list'">Cancel</ButtonAlt>
        <ButtonAction>Update member</ButtonAction>
        <ButtonDanger>Delete member</ButtonDanger>
      </template>
    </LayoutModal>
  </div>
</template>

<script>
import { request } from 'graphql-request'

import {
  MessageError,
  IconAction,
  DataTable,
  IconBar,
  ButtonAction,
  ButtonAlt,
  ButtonDanger,
  LayoutModal,
  InputString,
  InputSelect
} from '@mswertz/molgenis-emx2-lib-elements'

export default {
  components: {
    MessageError,
    IconAction,
    ButtonAction,
    ButtonAlt,
    ButtonDanger,
    DataTable,
    IconBar,
    LayoutModal,
    InputSelect,
    InputString
  },
  props: {
    schema: String
  },
  data: function() {
    return {
      view: 'list',
      currentMember: {},
      roles: ['Owner', 'Manager', 'Editor', 'Viewer'],
      members: null,
      error: null
    }
  },
  computed: {
    endpoint() {
      return '/api/graphql/' + this.schema
    },
    account() {
      return this.$store.state.account.email
    }
  },
  watch: {
    account() {
      this.loadMembers()
    }
  },
  created() {
    this.loadMembers()
  },
  methods: {
    showAdd() {
      this.view = 'add'
    },
    showEdit(row) {
      this.currentMember = row
      this.view = 'edit'
    },
    loadMembers() {
      this.error = false
      this.members = []
      request(this.endpoint, `{_meta{members{user,role}}}`)
        .then(data => {
          this.members = data._meta.members
        })
        .catch(error => (this.error = error.response.error))
    }
  }
}
</script>

<docs>
```
<Members schema="pet store"/>
```
</docs>
