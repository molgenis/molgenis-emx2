<template>
  <Modal ref="modal" title="Create User">
    <label>User name</label>
    <InputString id="New user name" v-model="userName" />
    <label>Password</label>
    <InputString
      id="New user password"
      v-model="password"
      :valid="password.length >= 8"
      :hasError="password.length < 8"
    />
    <label>Repeat password</label>
    <InputString
      id="New user password"
      v-model="password2"
      :valid="password === password2 && password2 !== ''"
      :hasError="password !== password2"
    />

    <template #footer>
      <Button @click="addUser(userName, password, password2)">Add user</Button>
      <Button @click="closeCreateUserModal">Close</Button>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import type { Modal } from "#build/components";

const modal = ref<InstanceType<typeof Modal>>();

const userName = ref<string>("");
const password = ref<string>("");
const password2 = ref<string>("");

const emit = defineEmits(["addUser"]);

function addUser(userName: string, password: string, password2: string) {
  if (password !== password2) {
    return;
  }
  emit("addUser", userName, password);
  closeCreateUserModal();
}

function closeCreateUserModal() {
  modal.value?.close();
  userName.value = "";
  password.value = "";
  password2.value = "";
}

function showModal() {
  modal.value?.show();
}

function closeModal() {
  modal.value?.close();
}

defineExpose({
  show: showModal,
  close: closeModal,
});
</script>
