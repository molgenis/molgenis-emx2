<script setup lang="ts">
import DefinitionListTerm from "../../DefinitionListTerm.vue";
import type {
  columnValue,
  IColumn,
  IRefColumn,
  IRow,
  ITableMetaData,
} from "../../../../../metadata-utils/src/types";
import DefinitionListDefinition from "../../DefinitionListDefinition.vue";
import { computed, ref, unref } from "vue";
import { columnValueToString } from "../../../utils/columnValueToString";
import fetchRowData from "../../../composables/fetchRowData";
import fetchRowPrimaryKey from "../../../composables/fetchRowPrimaryKey";
import ValueEMX2 from "../../value/EMX2.vue";
import fetchTableMetadata from "../../../composables/fetchTableMetadata";
import type {
  ColumnPayload,
  ListPayload,
  RefPayload,
} from "../../../../types/types";
import DefinitionList from "../../DefinitionList.vue";

const props = withDefaults(
  defineProps<{
    metadata: IRefColumn;
    columnValue: columnValue;
    schema: string;
    sourceTableId: string;
    showDataOwner?: boolean;
  }>(),
  {
    showDataOwner: false,
  }
);

const loading = ref(true);
const refRow = ref<IRow>({});
const refRowMetadata = ref<ITableMetaData>();

const sourceTableMetadata = ref<ITableMetaData>();

const emit = defineEmits<{
  (e: "onRefClick", payload: RefPayload | ColumnPayload | ListPayload): void;
}>();

await fetchData(
  props.columnValue as IRow,
  props.metadata.refTableId,
  props.schema,
  props.sourceTableId
);

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
        const lastSection = acc[acc.length - 1];
        if (lastSection) {
          lastSection.fields.push(item);
        }
      }
      return acc;
    }, [] as { heading: string; fields: { key: string; value: columnValue; metadata: IColumn }[] }[])
    .filter((section) => {
      // Filter out empty sections
      return section.fields.length > 0;
    });
});
</script>

<template>
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
            @valueClick="$emit('onRefClick', $event)"
          />
        </DefinitionListDefinition>
      </template>
    </DefinitionList>
  </section>
</template>
