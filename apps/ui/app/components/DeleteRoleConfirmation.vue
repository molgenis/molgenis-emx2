<template>
  <Modal v-model:visible="visible" :title="`Delete role(s)`">
    <div class="overflow-y-auto">
      <FormError
        v-if="props.errorMessage"
        id="delete-roles-error"
        :message="props.errorMessage"
        :show-prev-next-buttons="false"
        class="mx-4 mt-4"
      />
      <div class="p-5">
        <p>Are you sure you want to delete the role(s)</p>
        <div
          v-for="customRole in props.roles"
          :key="customRole._rowIdString"
          class="mb-2"
        >
          <strong>{{ customRole.role }}</strong> ({{ customRole.schemaId }})
          <div v-if="customRole.users.length">
            {{ customRole.users.length }} user(s) will lose this role:
            {{ customRole.users.join(", ") }}
          </div>
        </div>
        This action cannot be undone.
      </div>
    </div>
    <template #footer>
      <div class="flex justify-between items-center">
        <menu class="flex items-center justify-end h-modal-footer">
          <div class="flex gap-4">
            <Button
              type="secondary"
              :disabled="props.deleting"
              @click="visible = false"
            >
              Cancel
            </Button>
            <Button
              id="confirm-delete-roles"
              type="primary"
              :disabled="!props.roles.length || props.deleting"
              @click="emit('deleteRoles', props.roles)"
            >
              Delete
              <BaseIcon
                v-if="props.deleting"
                class="inline animate-spin"
                name="ProgressActivity"
              />
            </Button>
          </div>
        </menu>
      </div>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import BaseIcon from "../../../tailwind-components/app/components/BaseIcon.vue";
import Button from "../../../tailwind-components/app/components/Button.vue";
import FormError from "../../../tailwind-components/app/components/form/Error.vue";
import Modal from "../../../tailwind-components/app/components/Modal.vue";
import type { CustomRole } from "../interfaces/interfaces.ts";

const props = defineProps<{
  roles: CustomRole[];
  errorMessage?: string;
  deleting?: boolean;
}>();

const visible = defineModel("visible", {
  required: true,
});

const emit = defineEmits<{
  deleteRoles: [roles: CustomRole[]];
}>();
</script>
