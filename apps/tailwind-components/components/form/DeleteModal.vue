<script setup lang="ts">
import { computed, ref } from "vue";
import type { ITableMetaData } from "../../../metadata-utils/src";
import type { columnId, columnValue } from "../../../metadata-utils/src/types";
import useForm from "../../composables/useForm";
import { errorToMessage } from "../../utils/errorToMessage";
import DisplayRecord from "../display/Record.vue";

const props = withDefaults(
  defineProps<{
    schemaId: string;
    metadata: ITableMetaData;
    formValues: Record<columnId, columnValue>;
  }>(),
  {}
);

const emit = defineEmits(["update:deleted", "update:cancelled"]);

const visible = defineModel("visible", {
  type: Boolean,
  default: false,
});

const deleteErrorMessage = ref<string>("");

function setVisible() {
  visible.value = true;
}

const rowType = computed(() => props.metadata.id);
const isDraft = ref(false);

function onCancel() {
  visible.value = false;
  emit("update:cancelled");
}

async function onDeleteConfirm() {
  const { deleteRecord } = useForm(
    props.metadata,
    props.formValues,
    ref<Record<columnId, string>>({}),
    (fieldId: string) => {
      return fieldId;
    }
  );
  const resp = await deleteRecord(props.schemaId, props.metadata.id).catch(
    (err) => {
      console.error("Error deleting data", err);
      deleteErrorMessage.value = errorToMessage(err, "Error deleting record");
      return null;
    }
  );

  if (!resp) {
    return;
  }

  isDraft.value = false;
  visible.value = false;
  emit("update:deleted", resp);
}
</script>

<template>
  <slot :setVisible="setVisible">
    <Button
      class="m-10"
      type="primary"
      size="small"
      icon="plus"
      @click="visible = true"
      >Delete {{ rowType }}</Button
    >
  </slot>
  <Modal v-model:visible="visible" max-width="max-w-9/10">
    <template #header>
      <header class="pt-[36px] px-8 overflow-y-auto border-b border-divider">
        <div class="mb-5 relative flex items-center">
          <h2
            class="uppercase text-heading-4xl font-display text-title-contrast"
          >
            Delete {{ rowType }}
          </h2>

          <span
            v-show="isDraft"
            class="ml-3 bg-gray-400 px-2 py-1 rounded text-white font-bold -mt-1"
            >Draft</span
          >
        </div>

        <button
          @click="visible = false"
          aria-label="Close modal"
          class="absolute top-7 right-8 p-1"
        >
          <BaseIcon class="text-gray-400" name="cross" />
        </button>
      </header>
    </template>

    <section class="grid grid-cols-4 gap-1"></section>
    <Transition name="slide-up">
      <FormError
        v-show="deleteErrorMessage"
        :message="deleteErrorMessage"
        :show-prev-next-buttons="false"
        class="sticky mx-4 bottom-0 transition-all transition-discrete"
      />
    </Transition>

    <div class="w-[90%] m-auto py-4">
      <DisplayRecord :table-metadata="metadata" :input-row-data="formValues" />
    </div>

    <template #footer>
      <div class="flex justify-between items-center">
        <menu class="flex items-center justify-end h-[116px]">
          <div class="flex gap-4">
            <Button type="secondary" @click="onCancel">Cancel</Button>
            <Button type="primary" @click="onDeleteConfirm">Delete</Button>
          </div>
        </menu>
      </div>
    </template>
  </Modal>
</template>
