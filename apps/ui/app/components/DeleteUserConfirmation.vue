<template>
  <Modal v-model:visible="visible" :title="`Delete user: ${userName}`">
    <div class="overflow-y-auto">
      <div class="p-5">
        <div class="mb-4">
          <p>
            Are you sure you want to delete the user
            <strong>
              {{ userName }}
            </strong>
          </p>
          This action cannot be undone.
        </div>
      </div>
    </div>
    <template #footer>
      <div class="m-1">
        <div class="flex gap-1">
          <Button
            icon="trash"
            size="small"
            @click="
              emit('deleteUser', props.user);
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

<script setup lang="ts">
import type { IUser } from "../util/adminUtils";
import { computed } from "vue";
import Modal from "../../../tailwind-components/app/components/Modal.vue";
import Button from "../../../tailwind-components/app/components/Button.vue";

const props = defineProps<{
  user: IUser;
}>();

const visible = defineModel("visible", {
  required: true,
});

const userName = computed(() => props.user.email);
const emit = defineEmits(["deleteUser"]);
</script>
