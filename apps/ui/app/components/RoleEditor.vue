<template>
  <div class="flex flex-wrap items-center gap-3 mb-4">
    <div class="flex items-center gap-2">
      <InputLabel>Role:</InputLabel>
      <InputSelect
        id="role-select"
        v-model="internalSelectedRole"
        :options="roleOptions"
        class="text-sm min-w-[200px]"
      />
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
        <InputLabel>Role Name</InputLabel>
        <InputString
          id="new-role-name"
          v-model="newRoleName"
          placeholder="Enter role name"
        />
      </div>
      <div>
        <InputLabel>Description</InputLabel>
        <InputString
          id="new-role-description"
          v-model="newRoleDescription"
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
import InputSelect from "../../../tailwind-components/app/components/input/Select.vue";
import InputString from "../../../tailwind-components/app/components/input/String.vue";
import InputLabel from "../../../tailwind-components/app/components/input/Label.vue";

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

const roleOptions = computed(() =>
  props.roles.map((role) => `${role.name}${role.system ? " (system)" : ""}`)
);

const internalSelectedRole = computed({
  get: () => {
    const info = selectedRoleInfo.value;
    return info ? `${info.name}${info.system ? " (system)" : ""}` : "";
  },
  set: (value: string | number | undefined | null) => {
    const strValue = String(value);
    const roleName = strValue.replace(/ \(system\)$/, "");
    emit("select", roleName);
  },
});

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
