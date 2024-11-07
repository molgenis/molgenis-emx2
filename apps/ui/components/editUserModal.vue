<template>
  <Modal ref="modal" :title="`Edit user: ${userName}`">
    <div>
      <b>New password</b>
      <InputString id="New password" v-model="password" />
    </div>

    <div>
      <b>Disable user</b>
      <InputRadioGroup
        id="disabledUserRadio"
        :radioOptions="[
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
            <TableCell>{{ role.schema }}</TableCell>
            <TableCell>{{ role.role }}</TableCell>
          </TableRow>
        </template>
      </Table>
    </div>

    <div v-if="userTokens.length">
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
    </div>

    <template #footer>
      <Button @click="saveUser()">Save</Button>
      <Button @click="closeEditUserModal">Close</Button>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { type Modal } from "#build/components";
import type { IMember, ISchemaInfo, IUser } from "~/util/adminUtils";
import { updateUser } from "~/util/adminUtils";
import _ from "lodash";

const modal = ref<InstanceType<typeof Modal>>();

const { schemas, roles } = defineProps<{
  schemas: ISchemaInfo[];
  roles: string[];
}>();

const role = ref<string>("Viewer");
const schema = ref<string>(schemas.length ? schemas[0].id : "");

const userName = ref<string>("");
const isEnabled = ref<boolean>(true);
const userRoles = ref<Record<string, IMember>>({});
const userTokens = ref<string[]>([]);
const password = ref<string>("");

const SchemaIds = computed(() => {
  return schemas.map((schema) => schema.id);
});

const emit = defineEmits(["userUpdated"]);

function closeEditUserModal() {
  modal.value?.close();
}

function addRole() {
  if (schema.value && role.value) {
    userRoles.value[schema.value] = {
      schema: schema.value,
      role: role.value,
    };
  }
}

function removeRole(role: IMember) {
  delete userRoles.value[role.schema];
}

function removeToken(token: string) {
  userTokens.value = _.reject(userTokens.value, (tok) => tok === token);
}

function showModal(selectedUser: IUser) {
  if (selectedUser) {
    userName.value = selectedUser.email;
    isEnabled.value = selectedUser.enabled;
    userRoles.value = getRoles(selectedUser.roles || []);
    userTokens.value = selectedUser.tokens || ([] as string[]);
  }
  modal.value?.show();
}

function getRoles(roles: IMember[]): Record<string, IMember> {
  return roles.reduce((accum, role) => {
    accum[role.schema] = role;
    return accum;
  }, {} as Record<string, IMember>);
}

async function saveUser() {
  const edittedUser: IUser = {
    email: userName.value,
    settings: [],
    enabled: isEnabled.value,
    tokens: userTokens.value,
    roles: Object.values(userRoles.value),
  }; //TODO define update user object

  if (password.value) {
    edittedUser.password = password.value;
  }

  await updateUser(edittedUser);
  emit("userUpdated");
  modal.value?.close();
}

function closeModal() {
  modal.value?.close();
}

defineExpose({
  show: showModal,
  close: closeModal,
});
</script>
