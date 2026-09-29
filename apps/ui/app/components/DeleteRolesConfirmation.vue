<script setup lang="ts">
import { computed } from "vue";
import Button from "../../../tailwind-components/app/components/Button.vue";
import Modal from "../../../tailwind-components/app/components/Modal.vue";

const props = defineProps<{
  selectedRoles: string[];
}>();

const emit = defineEmits(["deleteRoles"]);

const visible = defineModel("visible", {
  required: true,
});

const rolesString = computed(() => props.selectedRoles.join(", "));
</script>

<template>
  <Modal v-model:visible="visible" :title="`Delete roles`">
    <div class="overflow-y-auto">
      <div class="p-5">
        <div class="mb-4">
          <p>
            Are you sure you want to delete the following roles:
            <strong>
              {{ rolesString }}
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
              emit('deleteRoles');
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
