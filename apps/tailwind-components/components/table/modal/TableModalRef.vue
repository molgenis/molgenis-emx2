<script setup lang="ts">
import DefinitionListTerm from "../../../../tailwind-components/components/DefinitionListTerm.vue";
import type {
  columnValue,
  IColumn,
  IRefColumn,
  IRow,
} from "../../../../metadata-utils/src/types";
import Modal from "../../../../tailwind-components/components/Modal.vue";
import DefinitionListDefinition from "../../../../tailwind-components/components/DefinitionListDefinition.vue";
import { computed, ref } from "vue";
import { rowToString } from "../../../utils/rowToString";
import fetchRowData from "../../../composables/fetchRowData";
import fetchRowPrimaryKey from "../../../composables/fetchRowPrimaryKey";
import ValueEMX2 from "../../value/EMX2.vue";
import fetchTableMetadata from "../../../composables/fetchTableMetadata";
import type { RefPayload } from "../../../types/types";
import { useRoute, useRouter } from "#app/composables/router";

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

const router = useRouter();
const route = useRoute();

// keep internal refs to allow walking data graph ( ref -> ref - > ref _> ... )
const currentMetadata = ref<IRefColumn>(props.metadata);
const currentRow = ref<IRow>(props.row);
const currentSchema = ref<string>(props.schema);
const currentSourceTableId = ref<string>(props.sourceTableId);

const labelStack = ref<string[]>([]);

const visible = ref(false);

const emit = defineEmits(["onClose"]);

function onModalClose() {
  // reset the query parameters to remove the modal state
  router.push({
    query: {
      ...route.query,
      detail: undefined,
      detailsType: undefined,
      refColumnId: undefined,
      refSourceTable: undefined,
      refRowId: undefined,
    },
  });

  emit("onClose");
}

const refColumnLabel = computed(() => {
  const labelTemplate = (
    currentMetadata.value.refLabel
      ? currentMetadata.value.refLabel
      : currentMetadata.value.refLabelDefault
  ) as string;
  return rowToString(currentRow.value, labelTemplate);
});

const loading = ref(true);
const sections = ref<
  {
    heading: string;
    fields: { key: string; value: columnValue; metadata: IColumn }[];
  }[]
>();

async function fetchData(
  row: IRow,
  tableId: string,
  schema: string,
  refColumnId: string,
  soureTableId: string
) {
  loading.value = true;
  const rowKey = await fetchRowPrimaryKey(row, tableId, schema);

  const refRow = await fetchRowData(schema, tableId, rowKey);

  const refRowMetadata = await fetchTableMetadata(schema, tableId);
  currentSourceTableId.value = soureTableId;

  loading.value = false;

  sections.value = refRowMetadata.columns
    .map((column) => {
      return {
        key: column.id,
        value: refRow[column.id],
        metadata: column,
      };
    })
    .filter((item) => {
      return !item.key.startsWith("mg_") || props.showDataOwner;
    })
    .filter((item) => {
      return (
        refRow.hasOwnProperty(item.key) ||
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

  router.push({
    query: {
      ...route.query,
      detail: "ref",
      detailsType: "modal",
      refColumnId: refColumnId,
      refSourceTable: currentSourceTableId.value,
      refRowId: JSON.stringify(rowKey),
    },
  });
}

await fetchData(
  props.row,
  props.metadata.refTableId,
  props.schema,
  props.metadata.id,
  props.sourceTableId
);

function handleValueClicked(event: RefPayload) {
  // store the label for go back path
  if (refColumnLabel.value) {
    labelStack.value.push(refColumnLabel.value);
  }

  // update the context to drill down
  const sourceTableId = currentMetadata.value.refTableId;
  currentMetadata.value = event.metadata;
  currentRow.value = event.data;
  currentSchema.value = event.metadata.refSchemaId ?? props.schema;

  // uodate the data
  fetchData(
    event.data,
    event.metadata.refTableId,
    event.metadata.refSchemaId ?? props.schema,
    event.metadata.id,
    sourceTableId
  );
}

function handleBackBtnClicked() {
  router.back();
  labelStack.value.pop();
}
</script>

<template>
  <Modal
    v-model:visible="visible"
    :title="refColumnLabel"
    :subtitle="currentMetadata.refTableId"
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
              :meta-data="field.metadata"
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
            v-if="labelStack.length === 0"
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
            >Back to {{ labelStack[labelStack.length - 1] }}</Button
          >
          <Button type="primary" size="medium" @click=""
            >Go to {{ refColumnLabel }}</Button
          >
        </menu>
      </div>
    </template>
  </Modal>
</template>
