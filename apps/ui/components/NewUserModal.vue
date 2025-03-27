<template>
  <Modal ref="modal" title="Create User">
    <label>Username</label>
    <InputString
      id="New username"
      v-model="username"
      :hasError="isDuplicateName"
    />
    <label>Password</label>
    <InputString
      id="New user password"
      v-model="password"
      :valid="password.length >= 8"
      :hasError="password.length < 8"
      type="password"
    />
    <label>Repeat password</label>
    <InputString
      id="New user password"
      v-model="password2"
      :valid="password === password2 && password2 !== ''"
      :hasError="password !== password2"
      type="password"
    />

    <template #footer>
      <div>
        <div v-if="isDuplicateName">Username already exists</div>
        <div v-if="password !== password2">Passwords do not match</div>
        <div v-if="password.length < 8">
          Password must be at least 8 characters
        </div>
      </div>
      <Button
        @click="addUser(username, password, password2)"
        :disabled="!isValidUser()"
      >
        Add user
      </Button>
      <Button @click="closeModal">Close</Button>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import type { Modal } from "#build/components";
import { computed, ref } from "vue";
import { isValidPassword } from "~/util/adminUtils";

const modal = ref<InstanceType<typeof Modal>>();

const { usernames } = defineProps<{ usernames: string[] }>();

const username = ref<string>("");
const password = ref<string>("");
const password2 = ref<string>("");

const emit = defineEmits(["addUser"]);

const isDuplicateName = computed(() => usernames.includes(username.value));

function addUser(userName: string, password: string, password2: string) {
  if (!isValidUser()) return;

  emit("addUser", userName, password);
  closeModal();
}

function isValidUser(): boolean {
  return (
    !isDuplicateName.value &&
    !!username.value.length &&
    isValidPassword(password.value, password2.value)
  );
}

function showModal() {
  modal.value?.show();
}

function closeModal() {
  username.value = "";
  password.value = "";
  password2.value = "";
  modal.value?.close();
}

defineExpose({
  show: showModal,
  close: closeModal,
});
</script>
