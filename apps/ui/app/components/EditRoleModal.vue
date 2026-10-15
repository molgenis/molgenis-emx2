<script setup lang="ts">
import { defineModel, defineProps, ref } from "vue";
import Field from "../../../tailwind-components/app/components/Field.vue";
import InputListbox from "../../../tailwind-components/app/components/input/Listbox.vue";
import Modal from "../../../tailwind-components/app/components/Modal.vue";

const props = defineProps<{
  isInsert: boolean;
  schemaOptions: { label: string; value: string }[];
}>();

const visible = defineModel("visible", {
  required: true,
});

const formValues = ref<Record<string, any>>({
  roleName: "",
  schema: "pet store",
});

const tables = ref([
  {
    name: "table1",
    description: "Description of table 1",
  },
  {
    name: "table2",
    description: "Description of table 2",
  },
]); // fetch tables for currently selected schema

function onCancel() {
  visible.value = false;
}
</script>

<template>
  <Modal v-model:visible="visible" size="large" max-width="max-w-9/10">
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
        <Field
          id="roleName"
          type="STRING"
          label="Role name"
          v-model="formValues.roleName"
        />

        <label for="schema">
          <span class="text-title-contrast font-bold"> Schema </span>
        </label>
        <InputListbox
          id="schema"
          v-model="formValues.schema"
          :options="schemaOptions"
        />

        <label for="permissions">
          <span class="text-title-contrast font-bold"> Permissions </span>
        </label>
        <table class="w-full border-collapse border border-divider">
          <thead>
            <tr>
              <th class="border border-divider p-2 text-left">Table</th>
              <th class="border border-divider p-2 text-left">Description</th>
              <th class="border border-divider p-2 text-left">Can select</th>
              <th class="border border-divider p-2 text-left">Can insert</th>
              <th class="border border-divider p-2 text-left">Can update</th>
              <th class="border border-divider p-2 text-left">Can delete</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="table in tables" :key="table.name">
              <td class="border border-divider p-2">{{ table.name }}</td>
              <td class="border border-divider p-2">
                {{ table.description }}
              </td>
              <td class="border border-divider p-2">
                <input type="checkbox" />
              </td>
              <td class="border border-divider p-2">
                <input type="checkbox" />
              </td>
              <td class="border border-divider p-2">
                <input type="checkbox" />
              </td>
              <td class="border border-divider p-2">
                <input type="checkbox" />
              </td>
            </tr>
          </tbody>
        </table>
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
