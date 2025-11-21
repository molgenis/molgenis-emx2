<script setup lang="ts">
import { useRoute } from "#app/composables/router";
import { useSession } from "../../../../../tailwind-components/app/composables/useSession";
import type {
  columnValue,
  IColumn,
} from "../../../../../metadata-utils/src/types";
import BreadCrumbs from "../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import PageHeader from "../../../../../tailwind-components/app/components/PageHeader.vue";
import fetchTableMetadata from "../../../../../tailwind-components/app/composables/fetchTableMetadata";
import fetchRowData from "../../../../../tailwind-components/app/composables/fetchRowData";
import { computed } from "vue";
import DefinitionList from "../../../../../tailwind-components/app/components/DefinitionList.vue";
import DefinitionListTerm from "../../../../../tailwind-components/app/components/DefinitionListTerm.vue";
import DefinitionListDefinition from "../../../../../tailwind-components/app/components/DefinitionListDefinition.vue";
import ValueEMX2 from "../../../../../tailwind-components/app/components/value/EMX2.vue";

const route = useRoute();
const schemaId = route.params.schema as string;
const tableId = route.params.table as string;
const entityId = route.params.entity as string;
const keys = route.query.keys as string | undefined;
const entityKeysObject = JSON.parse(keys || "{}");
const { isAdmin } = await useSession();

const tableMetadata = await fetchTableMetadata(schemaId, tableId);
const rowData = await fetchRowData(schemaId, tableId, entityKeysObject);

const sections = computed(() => {
  return tableMetadata.columns
    .map((column) => {
      return {
        key: column.id,
        value: rowData[column.id],
        metadata: column,
      };
    })
    .filter((item) => {
      return !item.key.startsWith("mg_") || isAdmin.value;
    })
    .filter((item) => {
      return (
        rowData.hasOwnProperty(item.key) ||
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
  <section class="mx-auto lg:px-[30px] px-0">
    <PageHeader :title="entityId" align="left">
      <template #prefix>
        <BreadCrumbs
          :align="'left'"
          :crumbs="[
            { label: schemaId, url: `/${schemaId}` },
            { label: tableId, url: `/${schemaId}/${tableId}` },
            { label: entityId, url: '' },
          ]"
        />
      </template>
    </PageHeader>
    <section
      v-for="section in sections"
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
          <DefinitionListTerm class="text-title-contrast"
            >{{ field.metadata.label }}
          </DefinitionListTerm>
          <DefinitionListDefinition class="text-title-contrast">
            <ValueEMX2 :data="field.value" :metadata="field.metadata" />
          </DefinitionListDefinition>
        </template>
      </DefinitionList>
    </section>
  </section>
</template>
