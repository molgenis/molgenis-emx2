<template>
  <Modal ref="modal" title="Edit User">
    <div>
      <h3>Disable user</h3>
      <InputRadioGroup
        id="disabledUserRadio"
        :radioOptions="[
          { value: true, label: `Enabled` },
          { value: false, label: `Disabled` },
        ]"
        :modelValue="isEnabled"
      />
    </div>

    <div>
      <h3>Roles</h3>
      <InputSelect id="select-schema" v-model="schema" :options="SchemaIds" />
      <InputSelect id="select-role" v-model="role" :options="roles" />
      <Button size="tiny" icon="plus" />

      <Table>
        <template #head>
          <TableHeadRow>
            <TableHead></TableHead>
            <TableHead>Schema</TableHead>
            <TableHead>Role</TableHead>
          </TableHeadRow>
        </template>
        <template #body>
          <TableRow v-for="role in user?.roles">
            <TableCell>
              <Button size="tiny" icon="trash" />
            </TableCell>
            <TableCell>{{ role.schemaId }}</TableCell>
            <TableCell>{{ role.role }}</TableCell>
          </TableRow>
        </template>
      </Table>
    </div>

    <div v-if="user?.tokens?.length">
      <h3>Tokens</h3>
      <Table>
        <template #head>
          <TableHeadRow>
            <TableHead></TableHead>
            <TableHead>Token</TableHead>
          </TableHeadRow>
        </template>
        <template #body>
          <TableRow v-for="token in user?.tokens">
            <TableCell>
              <Button size="tiny" icon="trash" />
            </TableCell>
            <TableCell>{{ token }}</TableCell>
          </TableRow>
        </template>
      </Table>
    </div>

    <template #footer>
      <Button @click="updateUser(user)">Save</Button>
      <Button @click="closeEditUserModal">Close</Button>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { type Modal } from "#build/components";
import type { ISchemaInfo, IUser } from "~/util/adminUtils";
import { updateUser } from "~/util/adminUtils";

const modal = ref<InstanceType<typeof Modal>>();

const { schemas, roles } = defineProps<{
  schemas: ISchemaInfo[];
  roles: string[];
}>();

const role = ref<string>("Viewer");
const schema = ref<string>("");
const isEnabled = ref<boolean>(true);
const user = ref<IUser>();
const SchemaIds = computed(() => {
  return schemas.map((schema) => schema.id);
});

function closeEditUserModal() {
  modal.value?.close();
}

function showModal(selectedUser: IUser) {
  console.log("open");
  user.value = JSON.parse(JSON.stringify(selectedUser));
  if (user.value) {
    isEnabled.value = user.value.enabled;
  }
  modal.value?.show();
}

const closeModal = () => {
  modal.value?.close();
};

defineExpose({
  show: showModal,
  close: closeModal,
});
</script>
