<template>
  <Modal ref="modal" title="Create User">
    <label>Username</label>
    <InputString id="New username" v-model="username" />
    <label>Password</label>
    <InputPassword
      id="New user password"
      v-model="password"
      :valid="password.length >= 8"
      :hasError="password.length < 8"
    />
    <label>Repeat password</label>
    <InputPassword
      id="New user password"
      v-model="password2"
      :valid="password === password2 && password2 !== ''"
      :hasError="password !== password2"
    />
    <template #footer>
      <Button
        @click="addUser(username, password, password2)"
        :disabled="isValidUser()"
      >
        Add user
      </Button>
      <Button @click="closeCreateUserModal">Close</Button>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import type { Modal } from "#build/components";
import { isValidPassword } from "~/util/adminUtils";

const modal = ref<InstanceType<typeof Modal>>();

const { usernames } = defineProps<{ usernames: string[] }>();

const username = ref<string>("");
const password = ref<string>("");
const password2 = ref<string>("");

const emit = defineEmits(["addUser"]);

function addUser(userName: string, password: string, password2: string) {
  if (!isValidUser()) return;

  emit("addUser", userName, password);
  closeCreateUserModal();
}

function closeCreateUserModal() {
  modal.value?.close();
  username.value = "";
  password.value = "";
  password2.value = "";
}

function isValidUser(): boolean {
  console.log(!usernames.includes(username.value));
  return (
    !usernames.includes(username.value) &&
    !!username.value.length &&
    isValidPassword(password.value, password2.value)
  );
  // check for duplicate usernames
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
