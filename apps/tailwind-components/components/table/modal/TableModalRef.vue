<script setup lang="ts">
import DefinitionListTerm from "../../../../tailwind-components/components/DefinitionListTerm.vue";
import type { IColumn, IRow } from "../../../../metadata-utils/src/types";
import Modal from "../../../../tailwind-components/components/Modal.vue";
import DefinitionListDefinition from "../../../../tailwind-components/components/DefinitionListDefinition.vue";
import { computed, ref } from "vue";
import { rowToString } from "../../../utils/rowToString";

const props = withDefaults(
  defineProps<{
    metadata: IColumn;
    row: IRow;
    schema?: string;
    showDataOwner?: boolean;
  }>(),
  {
    showDataOwner: false,
  }
);

const visible = ref(false);

const emit = defineEmits(["onClose"]);

const refColumnLabel = computed(() => {
  const labelTemplate = (
    props.metadata.refLabel
      ? props.metadata.refLabel
      : props.metadata.refLabelDefault
  ) as string;
  return rowToString(props.row, labelTemplate);
});

const columns = computed(() => {
  return Object.entries(props.row)
    .map(([key, value]) => ({
      key,
      value,
    }))
    .filter((item) => {
      return !item.key.startsWith("mg_") || props.showDataOwner;
    });
});
</script>

<template>
  <Modal
    v-model:visible="visible"
    :title="refColumnLabel"
    :subtitle="metadata.refTableId"
    max-width="max-w-9/10"
  >
    <section class="px-8 py-[50px]">
      <DefinitionList>
        <template v-for="column in columns">
          <DefinitionListTerm class="text-title-contrast"
            >{{ column.key }}
          </DefinitionListTerm>
          <DefinitionListDefinition>{{
            column.value
          }}</DefinitionListDefinition>
        </template>
      </DefinitionList>
    </section>
    <template #footer>
      <div class="flex width-full justify-end">
        <menu class="flex items-center justify-end h-[82px]">
          <Button type="primary" size="medium" @click=""
            >Go to {{ refColumnLabel }}</Button
          >
        </menu>
      </div>
    </template>
  </Modal>
</template>
