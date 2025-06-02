<template>
  <Modal
    v-model:visible="visible"
    :title="refColumnLabel"
    :subtitle="refSubTitle"
    max-width="max-w-9/10"
    @closed="onModalClose"
  >
    <section
      v-if="!loading"
      v-for="section in sections"
      class="px-8 first:pt-[50px] last:pb-[50px]"
      :class="section.heading ? 'pt-[50px]' : ''"
    >
      <h3
        v-if="section.heading"
        class="text-heading-3xl font-display text-title-contrast mb-4"
      >
        {{ section.heading }}
      </h3>
      <DefinitionList :compact="false">
        <template v-for="field in section.fields">
          <DefinitionListTerm class="text-title-contrast"
            >{{ field.metadata.label }}
          </DefinitionListTerm>
          <DefinitionListDefinition class="text-title-contrast">
            <ValueEMX2
              :data="field.value"
              :metadata="field.metadata"
              @valueClick="handleValueClicked"
            />
          </DefinitionListDefinition>
        </template>
      </DefinitionList>
    </section>
    <template #footer="{ hide }">
      <div class="flex width-full justify-end">
        <menu class="flex items-center justify-end h-[82px] gap-[10px]">
          <Button
            v-if="refStack.length === 0"
            type="secondary"
            size="medium"
            @click="hide"
            >Close</Button
          >
          <Button
            v-else
            type="secondary"
            size="medium"
            @click="handleBackBtnClicked"
            >Back to {{ backBtnLabel }}</Button
          >
          <Button type="primary" size="medium" @click=""
            >Go to {{ refColumnLabel }}</Button
          >
        </menu>
      </div>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import DefinitionListTerm from "../../../../tailwind-components/components/DefinitionListTerm.vue";
import type {
  columnValue,
  IColumn,
  IRefColumn,
  IRow,
  ITableMetaData,
} from "../../../../metadata-utils/src/types";
import Modal from "../../../../tailwind-components/components/Modal.vue";
import DefinitionListDefinition from "../../../../tailwind-components/components/DefinitionListDefinition.vue";
import { computed, ref, unref } from "vue";
import { rowToString } from "../../../utils/rowToString";
import fetchRowData from "../../../composables/fetchRowData";
import fetchRowPrimaryKey from "../../../composables/fetchRowPrimaryKey";
import ValueEMX2 from "../../value/EMX2.vue";
import fetchTableMetadata from "../../../composables/fetchTableMetadata";
import type { RefPayload } from "../../../types/types";

const props = withDefaults(
  defineProps<{
    metadata: IRefColumn;
    row: IRow;
    schema: string;
    sourceTableId: string;
    showDataOwner?: boolean;
  }>(),
  {
    showDataOwner: false,
  }
);

const loading = ref(true);
const refColumnId = ref(props.metadata.id);
const refRow = ref<IRow>({});
const refRowMetadata = ref<ITableMetaData>();

const sourceTableMetadata = ref<ITableMetaData>();

interface IRefStackItem {
  refRow: IRow;
  refMetadata: ITableMetaData;
  backBtnLabel?: string;
}
const refStack = ref<IRefStackItem[]>([]);

const visible = ref(false);

const emit = defineEmits(["onClose"]);

function onModalClose() {
  emit("onClose");
}

const sourceColumn = computed(() => {
  return sourceTableMetadata.value?.columns.find(
    (column) => column.id === refColumnId.value
  );
});

const refColumnLabel = computed(() => {
  const labelTemplate = (
    sourceColumn.value?.refLabel
      ? sourceColumn.value?.refLabel
      : sourceColumn.value?.refLabelDefault
  ) as string;
  return rowToString(refRow.value, labelTemplate);
});

const refSubTitle = computed(() => {
  const column = refRowMetadata.value?.columns.find(
    (column) => column.id === refColumnId.value
  );
  return column?.refTableId ?? props.metadata.refTableId;
});

const backBtnLabel = computed(() => {
  if (refStack.value.length === 0) {
    return "";
  }
  return refStack.value[refStack.value.length - 1].backBtnLabel;
});

async function fetchData(
  row: IRow,
  tableId: string,
  schema: string,
  sourceTableId: string
) {
  loading.value = true;
  const rowKey = await fetchRowPrimaryKey(row, tableId, schema);

  refRow.value = await fetchRowData(schema, tableId, rowKey);
  refRowMetadata.value = await fetchTableMetadata(schema, tableId);
  sourceTableMetadata.value = await fetchTableMetadata(schema, sourceTableId);

  loading.value = false;
}

const sections = computed(() => {
  if (!refRowMetadata.value) {
    return [];
  }

  return refRowMetadata.value.columns
    .map((column) => {
      return {
        key: column.id,
        value: refRow.value[column.id],
        metadata: column,
      };
    })
    .filter((item) => {
      return !item.key.startsWith("mg_") || props.showDataOwner;
    })
    .filter((item) => {
      return (
        refRow.value.hasOwnProperty(item.key) ||
        item.metadata.columnType === "HEADING"
      );
    })
    .reduce((acc, item) => {
      if (item.metadata.columnType === "HEADING") {
        // If the item is a heading, create a new section
        acc.push({ heading: item.metadata.label as string, fields: [] });
      } else {
        // If first item is not a section heading, create a default section
        if (acc.length === 0) {
          acc.push({ heading: "", fields: [] });
        }
        // Add the item to the last section
        acc[acc.length - 1].fields.push(item);
      }
      return acc;
    }, [] as { heading: string; fields: { key: string; value: columnValue; metadata: IColumn }[] }[])
    .filter((section) => {
      // Filter out empty sections
      return section.fields.length > 0;
    });
});

await fetchData(
  props.row,
  props.metadata.refTableId,
  props.schema,
  props.sourceTableId
);

function handleValueClicked(event: RefPayload) {
  if (refColumnLabel.value && refRowMetadata.value) {
    refStack.value.push({
      refRow: unref(refRow.value),
      refMetadata: unref(refRowMetadata.value),
      backBtnLabel: refColumnLabel.value,
    });
  }

  refColumnId.value = event.metadata.id;

  fetchData(
    event.data,
    event.metadata.refTableId,
    event.metadata.refSchemaId ?? props.schema,
    refRowMetadata.value?.id ?? props.sourceTableId
  );
}

function handleBackBtnClicked() {
  const previous = refStack.value.pop();
  if (previous) {
    refRow.value = previous.refRow;
    refRowMetadata.value = previous.refMetadata;
  }
}
</script>
