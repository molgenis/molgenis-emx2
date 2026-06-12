<script setup lang="ts">
import LayoutsDetailPage from "../../../../components/layouts/DetailPage.vue";
import PageHeader from "../../../../../../tailwind-components/app/components/PageHeader.vue";
import SideNavigation from "../../../../components/SideNavigation.vue";
import ContentBlocks from "../../../../../../tailwind-components/app/components/content/ContentBlocks.vue";
import ContentBlock from "../../../../../../tailwind-components/app/components/content/ContentBlock.vue";
import { computed, useResourceDetailsCrumbs, useRuntimeConfig } from "#imports";
import fetchMetadata from "../../../../../../tailwind-components/app/composables/fetchMetadata.js";
import fetchTableData from "../../../../../../tailwind-components/app/composables/fetchTableData.js";
import { useRoute } from "vue-router";
import { toSections } from "../../../../utils/sectionsUtils.js";
import { isEmpty } from "../../../../utils/fieldUtils.js";
const config = useRuntimeConfig();
const route = useRoute();
const schema = config.public.schema;
const TABLE_NAME = "Resources";
const schemaMetaData = await fetchMetadata(schema);

const crumbs = useResourceDetailsCrumbs();

const metadata = schemaMetaData.tables.find((table) => table.id === TABLE_NAME);

const columns = metadata ? metadata.columns : [];

// fetch the resource data
const rowIdFilter = { id: { equals: [route.params.resource] } };
const data = await fetchTableData(schema, TABLE_NAME, {
  offset: 0,
  limit: 1,
  filter: rowIdFilter,
});
const resourceData = data.rows[0];

const sections = toSections(resourceData, metadata);

const tocItems = computed(() => {
  return sections.map((section: { id: any; label: any }) => ({
    id: section.id || "",
    label: section.label || "",
  }));
});

function getLogoUrl(value: unknown) {
  return typeof value === "object" && value !== null && "url" in value
    ? (value as { url?: string }).url
    : undefined;
}
</script>
<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader
        v-if="resourceData"
        id="generated-resource-page-header"
        :title="resourceData['acronym'] as string || resourceData['name'] as string || 'Unnamed Resource'"
        :description="resourceData['name'] as string || 'No description available.'"
      >
        <template #prefix>
          <BreadCrumbs :crumbs="crumbs" />
        </template>
      </PageHeader>
    </template>
    <template #side>
      <SideNavigation
        v-if="resourceData && sections.length > 0"
        :title="resourceData['acronym'] as string || resourceData['name'] as string || 'Unnamed Resource'"
        :image="getLogoUrl(resourceData['logo'])"
        :items="tocItems"
        header-target="#resource-page-header"
      />
    </template>
    <template #main>
      <ContentBlocks>
        <ContentBlock
          v-for="section in sections"
          :id="section.id"
          :title="section.label"
          :description="section.description || ''"
        >
          <ul v-if="section.fields.length > 0">
            <li
              v-for="field in section.fields.filter((f:any) => !isEmpty(f.value))"
              class="mb-2"
            >
              <strong>{{ field.label }}:</strong> {{ field.value }}
            </li>
          </ul>
        </ContentBlock>
      </ContentBlocks>
    </template>
  </LayoutsDetailPage>
</template>
