<template>
  <Modal v-model:visible="visible" :title="`Edit user: ${userName}`">
    <div class="p-5">
      <b>New password</b>
      <InputString
        id="New password"
        v-model="password"
        :valid="password.length >= 8"
        :hasError="password.length < 8"
        type="password"
        class="mb-2"
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

    <div class="p-5">
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

    <div class="p-5">
      <b>Roles</b>

      <Table>
        <template #head>
          <TableHeadRow>
            <TableHead></TableHead>
            <TableHead>Schema</TableHead>
            <TableHead>Role</TableHead>
          </TableHeadRow>
        </template>
        <template #body>
          <TableRow v-if="_.isEmpty(userRoles)">
            <TableCell></TableCell>
            <TableCell>
              no roles found
            </TableCell>
            <TableCell></TableCell>
          </TableRow>

          <TableRow v-else v-for="role in userRoles">
            <TableCell>
              <Button iconOnly size="tiny" icon="trash" label="remove" @click="removeRole(role)" />
            </TableCell>
            <TableCell>{{ role.schemaId }}</TableCell>
            <TableCell>{{ role.role }}</TableCell>
          </TableRow>
        </template>
      </Table>
    </div>

    <div class="p-5 pt-0 flex">
      <InputSelect id="select-schema" v-model="schema" :options="SchemaIds" />
      <InputSelect id="select-role" v-model="role" :options="roles" />
      <Button size="small" icon="plus" @click="addRole" class="whitespace-nowrap">Add role</Button>
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
      <div class="m-1">
        <div v-if="password !== password2">Passwords do not match</div>
        <div v-if="password.length < 8 && password.length > 0">
          Password must be at least 8 characters
        </div>
      <div class="flex gap-1">
        <Button icon="Plus" size="small" @click="saveUser()" :disabled="!isValidUser()">Save</Button>
        <Button icon="Cross" size="small" @click="closeEditUserModal">Close</Button>
      </div>
    </div>
  </template>
  </Modal>
</template>

<script setup lang="ts">
import type { IRole, ISchemaInfo, IUser } from "~/util/adminUtils";
import { isValidPassword, updateUser } from "~/util/adminUtils";
import _ from "lodash";
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
