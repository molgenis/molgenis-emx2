<script setup lang="ts">
import DefinitionListTerm from "../../../../tailwind-components/components/DefinitionListTerm.vue";
import type { IRefColumn, IRow } from "../../../../metadata-utils/src/types";
import Modal from "../../../../tailwind-components/components/Modal.vue";
import DefinitionListDefinition from "../../../../tailwind-components/components/DefinitionListDefinition.vue";
import { computed, ref } from "vue";
import { rowToString } from "../../../utils/rowToString";
import fetchRowData from "~/composables/fetchRowData";
import fetchRowPrimaryKey from "../../../composables/fetchRowPrimaryKey";
import ColumnData from "../cellTypes/ColumnData.vue";
import fetchTableMetadata from "~/composables/fetchTableMetadata";

const props = withDefaults(
  defineProps<{
    metadata: IRefColumn;
    row: IRow;
    schema: string;
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

const rowKey = await fetchRowPrimaryKey(
  props.row,
  props.metadata.refTableId,
  props.schema
);

const refRow = await fetchRowData(
  props.schema,
  props.metadata.refTableId,
  rowKey
);

const refRowMetadata = await fetchTableMetadata(
  props.schema,
  props.metadata.refTableId
);

const columns = computed(() => {
  return Object.entries(refRow)
    .map(([key, value]) => ({ key, value }))
    .filter((item) => {
      return !item.key.startsWith("mg_") || props.showDataOwner;
    })
    .map((item) => {
      const columnMetadata = refRowMetadata.columns.find(
        (column) => column.id === item.key
      );
      if (!columnMetadata) {
        throw new Error(`Column metadata not found for ${item.key}`);
      }
      return {
        ...item,
        metadata: columnMetadata,
      };
    });
});
</script>

<template>
  <Modal
    v-model:visible="visible"
    :title="refColumnLabel"
    :subtitle="metadata.refTableId"
    max-width="max-w-9/10"
    :onClose="emit('onClose')"
  >
    <section class="px-8 py-[50px]">
      <DefinitionList>
        <template v-for="column in columns">
          <DefinitionListTerm class="text-title-contrast"
            >{{ column.key }}
          </DefinitionListTerm>
          <DefinitionListDefinition>
            <ColumnData :data="column.value" :meta-data="column.metadata" />
          </DefinitionListDefinition>
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
