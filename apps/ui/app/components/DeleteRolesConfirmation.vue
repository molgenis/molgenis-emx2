<script setup lang="ts">
import { computed } from "vue";
import Button from "../../../tailwind-components/app/components/Button.vue";
import Modal from "../../../tailwind-components/app/components/Modal.vue";
import FormError from "../../../tailwind-components/app/components/form/Error.vue";
import TransitionSlideUp from "../../../tailwind-components/app/components/transition/SlideUp.vue";

const props = defineProps<{
  selectedRoles: string[];
  errorMessage?: string;
}>();

const emit = defineEmits(["deleteRoles"]);

const visible = defineModel("visible", {
  required: true,
});

const rolesString = computed(() => props.selectedRoles.join(", "));
</script>

<template>
  <Modal
    v-model:visible="visible"
    :title="`Delete roles`"
    size="medium"
    max-width="max-w-9/10"
  >
    <div class="flex h-96 min-h-0 flex-col">
      <div class="min-h-0 flex-1 overflow-y-auto p-5">
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
      <TransitionSlideUp>
        <FormError
          v-show="errorMessage"
          :message="errorMessage ?? ''"
          :showPrevNextButtons="false"
          class="mx-4 shrink-0 transition-all transition-discrete"
        />
      </TransitionSlideUp>
    </div>

    <template #footer>
      <menu class="flex items-center justify-end h-modal-footer">
        <div class="flex gap-4">
          <Button type="secondary" @click="visible = false"> Cancel </Button>
          <Button type="primary" @click="emit('deleteRoles')"> Delete </Button>
        </div>
      </menu>
    </template>
  </Modal>
</template>
