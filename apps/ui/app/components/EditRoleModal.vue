<script setup lang="ts">
import { ref } from "vue";
import Modal from "../../../tailwind-components/app/components/Modal.vue";

const visible = defineModel("visible", {
  required: true,
});

const isInsert = ref<boolean>(true);
const formValues = ref<Record<string, string>>({
  roleName: "",
});

function onCancel() {
  visible.value = false;
}
</script>

<template>
  <Modal v-model:visible="visible" size="medium" max-width="max-w-9/10">
    <template #header>
      <header
        class="pt-[36px] px-8 overflow-visible border-b border-divider flex-none"
      >
        <div class="mb-5 relative flex items-center pr-14">
          <h2
            class="uppercase text-heading-4xl font-display text-title-contrast"
          >
            {{ isInsert ? "Add" : "Edit" }} role
          </h2>
        </div>
        <button
          @click="onCancel"
          aria-label="Close modal"
          class="absolute top-7 right-8 p-1"
        >
          <BaseIcon class="text-gray-400" name="cross" />
        </button>
      </header>
    </template>

    <div class="min-h-0 flex-1">
      <div class="overflow-y-auto p-12.5">
        <InputString
          id="roleName"
          v-model="formValues.roleName"
          :valid="!!formValues.roleName?.length"
          :hasError="formValues.roleName?.length === 0"
          placeholder="Role name"
        />
      </div>
    </div>

    <template #footer>
      <menu class="flex items-center justify-end h-modal-footer">
        <div class="flex gap-4">
          <Button type="secondary" @click="onCancel">Cancel</Button>
          <Button type="primary" @click="onCancel">Save</Button>
        </div>
      </menu>
    </template>
  </Modal>
</template>
