<template>
  <div class="flex flex-wrap items-center gap-3 mb-4">
    <div class="flex items-center gap-2">
      <label class="text-sm font-medium">Role:</label>
      <select
        :value="selectedRole"
        class="px-2 py-1 text-sm border rounded bg-white"
        @change="handleRoleChange"
      >
        <option v-for="role in roles" :key="role.name" :value="role.name">
          {{ role.name }}{{ role.system ? " (system)" : "" }}
        </option>
      </select>
    </div>
    <div v-if="selectedRoleInfo?.description" class="text-sm text-gray-500">
      {{ selectedRoleInfo.description }}
    </div>
    <div class="flex gap-2 ml-auto">
      <Button
        icon="plus"
        type="secondary"
        size="small"
        @click="showCreateModal = true"
      >
        New Role
      </Button>
      <Button
        v-if="selectedRoleInfo && !selectedRoleInfo.system"
        icon="trash"
        type="secondary"
        size="small"
        @click="showDeleteModal = true"
      >
        Delete
      </Button>
    </div>
  </div>

  <Modal
    v-if="showCreateModal"
    v-model:visible="showCreateModal"
    title="Create New Role"
  >
    <div class="flex flex-col gap-3 p-4">
      <div>
        <label class="block text-sm font-medium mb-1">Role Name</label>
        <input
          v-model="newRoleName"
          class="w-full px-2 py-1 text-sm border rounded"
          placeholder="Enter role name"
        />
      </div>
      <div>
        <label class="block text-sm font-medium mb-1">Description</label>
        <input
          v-model="newRoleDescription"
          class="w-full px-2 py-1 text-sm border rounded"
          placeholder="Enter description (optional)"
        />
      </div>
    </div>
    <template #footer="{ hide }">
      <div class="flex gap-2 p-4">
        <Button
          type="primary"
          size="small"
          :disabled="!newRoleName.trim()"
          @click="handleCreate(hide)"
        >
          Create
        </Button>
        <Button type="secondary" size="small" @click="hide">Cancel</Button>
      </div>
    </template>
  </Modal>

  <Modal
    v-if="showDeleteModal"
    v-model:visible="showDeleteModal"
    title="Delete Role"
  >
    <div class="p-4">
      <p>
        Are you sure you want to delete role
        <strong>{{ selectedRole }}</strong
        >? This will remove all permissions for this role.
      </p>
    </div>
    <template #footer="{ hide }">
      <div class="flex gap-2 p-4">
        <Button type="primary" size="small" @click="handleDelete(hide)"
          >Delete</Button
        >
        <Button type="secondary" size="small" @click="hide">Cancel</Button>
      </div>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import type { IRoleInfo } from "~/util/roleUtils";
import Button from "../../../tailwind-components/app/components/Button.vue";
import Modal from "../../../tailwind-components/app/components/Modal.vue";

const props = defineProps<{
  roles: IRoleInfo[];
  selectedRole: string;
}>();

const emit = defineEmits<{
  select: [roleName: string];
  create: [name: string, description: string | null];
  delete: [roleName: string];
}>();

const showCreateModal = ref(false);
const showDeleteModal = ref(false);
const newRoleName = ref("");
const newRoleDescription = ref("");

const selectedRoleInfo = computed(() =>
  props.roles.find((role) => role.name === props.selectedRole)
);

function handleRoleChange(event: Event) {
  const target = event.target as HTMLSelectElement;
  emit("select", target.value);
}

function handleCreate(hide: () => void) {
  const name = newRoleName.value.trim();
  if (!name) return;
  emit("create", name, newRoleDescription.value.trim() || null);
  newRoleName.value = "";
  newRoleDescription.value = "";
  hide();
}

function handleDelete(hide: () => void) {
  emit("delete", props.selectedRole);
  hide();
}
</script>
