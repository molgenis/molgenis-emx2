<script setup lang="ts">
import DefinitionListTerm from "../../../../tailwind-components/components/DefinitionListTerm.vue";
import type { IRefColumn, IRow } from "../../../../metadata-utils/src/types";
import Modal from "../../../../tailwind-components/components/Modal.vue";
import DefinitionListDefinition from "../../../../tailwind-components/components/DefinitionListDefinition.vue";
import { computed, ref } from "vue";
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
    showDataOwner?: boolean;
  }>(),
  {
    showDataOwner: false,
  }
);

// keep internal refs to allow walking data graph ( ref -> ref - > ref _> ... )
const currentMetadata = ref<IRefColumn>(props.metadata);
const currentRow = ref<IRow>(props.row);
const currentSchema = ref<string>(props.schema);

const visible = ref(false);

const emit = defineEmits(["onClose"]);

const refColumnLabel = computed(() => {
  const labelTemplate = (
    currentMetadata.value.refLabel
      ? currentMetadata.value.refLabel
      : currentMetadata.value.refLabelDefault
  ) as string;
  return rowToString(currentRow.value, labelTemplate);
});

const loading = ref(true);
const columns = ref();

async function fetchData(row: IRow, tableId: string, schema: string) {
  loading.value = true;
  const rowKey = await fetchRowPrimaryKey(row, tableId, schema);

  const refRow = await fetchRowData(schema, tableId, rowKey);

  const refRowMetadata = await fetchTableMetadata(schema, tableId);

  loading.value = false;

  columns.value = Object.entries(refRow)
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
}

await fetchData(props.row, props.metadata.refTableId, props.schema);

function handleValueClicked(event: RefPayload) {
  // update the context to drill down
  currentMetadata.value = event.metadata;
  currentRow.value = event.data;
  currentSchema.value = event.metadata.refSchemaId ?? props.schema;
  // uodate the data
  fetchData(
    event.data,
    event.metadata.refTableId,
    event.metadata.refSchemaId ?? props.schema
  );
}
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
      <DefinitionList v-if="!loading" :compact="false">
        <template v-for="column in columns">
          <DefinitionListTerm class="text-title-contrast"
            >{{ column.metadata.label }}
          </DefinitionListTerm>
          <DefinitionListDefinition class="text-title-contrast">
            <ValueEMX2
              :data="column.value"
              :meta-data="column.metadata"
              @valueClick="handleValueClicked"
            />
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
