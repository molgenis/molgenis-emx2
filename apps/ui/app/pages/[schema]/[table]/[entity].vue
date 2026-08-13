<script setup lang="ts">
import { useAsyncData } from "#app";
import { useRoute, useRouter } from "#app/composables/router";
import { computed, ref, useId } from "vue";
import type {
  columnValue,
  IColumn,
  IRow,
} from "../../../../../metadata-utils/src/types";
import BreadCrumbs from "../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import Button from "../../../../../tailwind-components/app/components/Button.vue";
import ContentBlock from "../../../../../tailwind-components/app/components/content/ContentBlock.vue";
import DefinitionList from "../../../../../tailwind-components/app/components/DefinitionList.vue";
import DefinitionListDefinition from "../../../../../tailwind-components/app/components/DefinitionListDefinition.vue";
import DefinitionListTerm from "../../../../../tailwind-components/app/components/DefinitionListTerm.vue";
import DeleteModal from "../../../../../tailwind-components/app/components/form/DeleteModal.vue";
import EditModal from "../../../../../tailwind-components/app/components/form/EditModal.vue";
import InputSearch from "../../../../../tailwind-components/app/components/input/Search.vue";
import PageHeader from "../../../../../tailwind-components/app/components/PageHeader.vue";
import CellDetailModal from "../../../../../tailwind-components/app/components/table/cellDetail/CellDetailModal.vue";
import ValueEMX2 from "../../../../../tailwind-components/app/components/value/EMX2.vue";
import fetchRowData from "../../../../../tailwind-components/app/composables/fetchRowData";
import fetchTableMetadata from "../../../../../tailwind-components/app/composables/fetchTableMetadata";
import { useSession } from "../../../../../tailwind-components/app/composables/useSession";
import type { cellPayload } from "../../../../../tailwind-components/types/types";
import Container from "../../../../../tailwind-components/app/components/Container.vue";

const route = useRoute();
const router = useRouter();
const schemaId = route.params.schema as string;
const tableId = route.params.table as string;
const entityId = route.params.entity as string;
const keys = route.query.keys as string | undefined;
let entityKeysObject: IRow = {};

const showModal = ref(false);
const cellDetailPayload = ref<cellPayload>();

try {
  if (keys) {
    entityKeysObject = JSON.parse(keys) as IRow;
  }
} catch {
  // If the query parameter is malformed JSON, fall back to an empty object
  entityKeysObject = {};
}
const { isAdmin, session } = await useSession(schemaId);

const tableMetadata = await fetchTableMetadata(schemaId, tableId);
const { data: rowData, refresh } = await useAsyncData(
  keys || JSON.stringify(entityKeysObject),
  () => fetchRowData(schemaId, tableId, entityKeysObject)
);

const sections = computed(() => {
  return tableMetadata.columns
    .map((column) => {
      return {
        key: column.id,
        value: rowData.value?.[column.id],
        metadata: column,
      };
    })
    .filter((item) => {
      return !item.key.startsWith("mg_") || isAdmin.value;
    })
    .filter((item) => {
      return (
        (rowData.value && rowData.value.hasOwnProperty(item.key)) ||
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

const filterValue = ref("");

const filteredSections = computed(() => {
  if (!filterValue.value) {
    return sections.value;
  }
  const lowerCaseFilter = filterValue.value.toLowerCase();
  return sections.value
    .map((section) => {
      const filteredFields = section.fields.filter((field) =>
        field.metadata.label.toLowerCase().includes(lowerCaseFilter)
      );
      return { ...section, fields: filteredFields };
    })
    .filter((section) => section.fields.length > 0);
});

const showEditModal = ref(false);
const showDeleteModal = ref(false);

function afterRowDeleted() {
  router.push(`/${schemaId}/${tableId}`);
}
function afterEditClosed() {
  showEditModal.value = false;
  refresh();
}

const enableEditing = computed(() => {
  return session.value?.roles?.[schemaId]?.includes("Editor") || isAdmin.value;
});

const enableDeleting = computed(() => {
  return session.value?.roles?.[schemaId]?.includes("Editor") || isAdmin.value;
});

function handleCellClick(event: cellPayload) {
  cellDetailPayload.value = event;
  showModal.value = true;
}
</script>

<template>
  <Container>
    <PageHeader :title="entityId" align="left">
      <template #prefix>
        <BreadCrumbs
          :align="'left'"
          :crumbs="[
            { label: schemaId, url: `/${schemaId}` },
            { label: tableId, url: `/${schemaId}/${tableId}` },
          ]"
        />
      </template>
    </PageHeader>

    <div class="flex pb-[30px] gap-[10px] justify-between">
      <InputSearch
        class="w-3/5 xl:w-2/5 2xl:w-1/5"
        v-model="filterValue"
        :placeholder="`Filter fields...`"
        id="filter-input"
      />

      <div class="flex gap-[10px]">
        <Button
          type="outline"
          icon="edit"
          @click="showEditModal = true"
          v-if="enableEditing"
          >Edit
        </Button>
        <Button
          type="outline"
          icon="trash"
          @click="showDeleteModal = true"
          v-if="enableDeleting"
        >
          Delete
        </Button>
      </div>
    </div>

    <ContentBlock
      class="mt-1"
      :title="entityId"
      :description="tableMetadata?.label || tableId"
    >
      <section
        v-for="section in filteredSections"
        class="first:pt-[50px] last:pb-[100px]"
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
            <DefinitionListTerm class="text-title-contrast">
              {{ field.metadata.label }}
            </DefinitionListTerm>
            <DefinitionListDefinition class="text-title-contrast">
              <ValueEMX2
                :data="field.value"
                :metadata="field.metadata"
                @valueClick="handleCellClick($event)"
              />
            </DefinitionListDefinition>
          </template>
        </DefinitionList>
      </section>
    </ContentBlock>
  </Container>

  <CellDetailModal
    v-if="cellDetailPayload"
    :payload="cellDetailPayload"
    :schemaId="schemaId"
    v-model:showModal="showModal"
    @update:cellDetailPayload="cellDetailPayload = $event"
  />

  <DeleteModal
    v-if="tableMetadata && rowData && showDeleteModal"
    :showButton="false"
    :schemaId="schemaId"
    :metadata="tableMetadata"
    :formValues="rowData"
    v-model:visible="showDeleteModal"
    @update:deleted="afterRowDeleted"
    @update:cancelled="showDeleteModal = false"
  />

  <EditModal
    v-if="tableMetadata && rowData && showEditModal"
    :key="`edit-modal-${useId()}`"
    :showButton="false"
    :schemaId="schemaId"
    :metadata="tableMetadata"
    :formValues="rowData"
    :isInsert="false"
    v-model:visible="showEditModal"
    @update:cancelled="afterEditClosed"
    @update:added="afterEditClosed"
    @update:edited="afterEditClosed"
  />
</template>
