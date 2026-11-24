<script setup lang="ts">
import Button from "../../../tailwind-components/app/components/Button.vue";
import Modal from "../../../tailwind-components/app/components/Modal.vue";
import type { User } from "../interfaces/interfaces.ts";

const props = defineProps<{
  users: User[];
  systemUsers: User[];
}>();

const visible = defineModel("visible", {
  required: true,
});

const emit = defineEmits(["deleteUser"]);
</script>

<template>
  <Modal v-model:visible="visible" :title="`Delete user(s)`">
    <div class="overflow-y-auto">
      <div class="p-5">
        <div v-if="props.users.length" class="mb-4">
          <p>Are you sure you want to delete the user(s)</p>
          <div v-for="user in props.users" :key="user.email">
            <strong>
              {{ user.email }}
            </strong>
          </div>
          This action cannot be undone.
        </div>
        <div v-if="props.systemUsers.length" id="delete-system-users-warning">
          <p>
            The following system users cannot be deleted and will be skipped:
          </p>
          <div v-for="user in props.systemUsers" :key="user.email">
            <strong>
              {{ user.email }}
            </strong>
          </div>
        </div>
      </div>
    </div>
    <template #footer>
      <div class="m-1">
        <div class="flex gap-1">
          <Button
            icon="trash"
            size="small"
            :disabled="!props.users.length"
            @click="
              emit('deleteUser', props.users);
              visible = false;
            "
          >
            Delete
          </Button>
          <Button icon="cross" size="small" @click="visible = false">
            Cancel
          </Button>
        </div>
      </div>
    </template>
  </Modal>
</template>
