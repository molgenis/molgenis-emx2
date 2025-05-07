<template>
  <Modal v-model:visible="visible" :title="`Edit user: ${userName}`">
    <div>
      <b>New password</b>
      <InputString
        id="New password"
        v-model="password"
        :valid="password.length >= 8"
        :hasError="password.length < 8"
        type="password"
      />
      <b>Repeat new password</b>
      <InputString
        id="New password"
        v-model="password2"
        :valid="password === password2 && password2 !== ''"
        :hasError="password !== password2"
        type="password"
      />
    </div>

    <div>
      <b>Disable user</b>
      <InputRadioGroup
        id="disabledUserRadio"
        :options="[
          { value: true, label: 'Enabled' },
          { value: false, label: 'Disabled' },
        ]"
        v-model="isEnabled"
      />
    </div>

    <div>
      <b>Roles</b>
      <InputSelect id="select-schema" v-model="schema" :options="SchemaIds" />
      <InputSelect id="select-role" v-model="role" :options="roles" />
      <Button size="tiny" icon="plus" @click="addRole" />

      <Table>
        <template #head>
          <TableHeadRow>
            <TableHead></TableHead>
            <TableHead>Schema</TableHead>
            <TableHead>Role</TableHead>
          </TableHeadRow>
        </template>
        <template #body>
          <TableRow v-for="role in userRoles">
            <TableCell>
              <Button size="tiny" icon="trash" @click="removeRole(role)" />
            </TableCell>
            <TableCell>{{ role.schemaId }}</TableCell>
            <TableCell>{{ role.role }}</TableCell>
          </TableRow>
        </template>
      </Table>
    </div>

    <!-- <div v-if="userTokens.length">
      <b>Tokens</b>
      <Table>
        <template #head>
          <TableHeadRow>
            <TableHead></TableHead>
            <TableHead>Token</TableHead>
          </TableHeadRow>
        </template>
        <template #body>
          <TableRow v-for="token in userTokens">
            <TableCell>
              <Button size="tiny" icon="trash" @click="removeToken(token)" />
            </TableCell>
            <TableCell>{{ token }}</TableCell>
          </TableRow>
        </template>
      </Table>
    </div> -->

    <template #footer>
      <div>
        <div v-if="password !== password2">Passwords do not match</div>
        <div v-if="password.length < 8">
          Password must be at least 8 characters
        </div>
      </div>
      <Button @click="saveUser()" :disabled="!isValidUser()">Save</Button>
      <Button @click="closeEditUserModal">Close</Button>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import type { IRole, ISchemaInfo, IUser } from "~/util/adminUtils";
import { isValidPassword, updateUser } from "~/util/adminUtils";
// import _ from "lodash";
import { computed, ref } from "vue";

const props = defineProps<{
  user: IUser;
  schemas: ISchemaInfo[];
  roles: string[];
}>();

const visible = defineModel("visible", {
  required: true,
});

const role = ref<string>("Viewer");
const schema = ref<string>(props.schemas.length ? props.schemas[0].id : "");

const userName = ref<string>(props.user.email);
const isEnabled = ref<boolean>(props.user.enabled);
const userRoles = ref<Record<string, IRole>>(getRoles(props.user.roles || []));
const revokedRoles = ref<Record<string, IRole>>({});
const userTokens = ref<string[]>(props.user.tokens || ([] as string[]));
const password = ref<string>("");
const password2 = ref<string>("");

const SchemaIds = computed(() => {
  return props.schemas.map((schema) => schema.id);
});

const emit = defineEmits(["userUpdated"]);

function closeEditUserModal() {
  visible.value = false;
}

function addRole() {
  if (schema.value && role.value) {
    delete revokedRoles.value[schema.value];
    userRoles.value[schema.value] = {
      schemaId: schema.value,
      role: role.value,
    };
  }
}

function removeRole(role: IRole) {
  revokedRoles.value[role.schemaId] = role;
  delete userRoles.value[role.schemaId];
}

// function removeToken(token: string) {
//   userTokens.value = _.reject(userTokens.value, (tok) => tok === token);
// }

function getRoles(roles: IRole[]): Record<string, IRole> {
  return roles.reduce((accum, role) => {
    accum[role.schemaId] = role;
    return accum;
  }, {} as Record<string, IRole>);
}

function isValidUser(): boolean {
  return password.value
    ? isValidPassword(password.value, password2.value)
    : true;
}

async function saveUser() {
  const editedUser: IUser = {
    email: userName.value,
    settings: [],
    enabled: isEnabled.value,
    tokens: userTokens.value,
    roles: Object.values(userRoles.value),
    revokedRoles: Object.values(revokedRoles.value),
  }; //TODO define update user object

  if (password.value) {
    editedUser.password = password.value;
  }

  await updateUser(editedUser);
  emit("userUpdated");
  visible.value = false;
}
</script>
