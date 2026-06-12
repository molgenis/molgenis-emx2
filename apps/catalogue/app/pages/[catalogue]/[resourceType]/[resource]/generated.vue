<script setup lang="ts">
import LayoutsDetailPage from "../../../../components/layouts/DetailPage.vue";
import PageHeader from "../../../../../../tailwind-components/app/components/PageHeader.vue";
import SideNavigation from "../../../../components/SideNavigation.vue";
import ContentBlocks from "../../../../../../tailwind-components/app/components/content/ContentBlocks.vue";
import ContentBlock from "../../../../../../tailwind-components/app/components/content/ContentBlock.vue";
import {
  computed,
  ref,
  useFetch,
  useResourceDetailsCrumbs,
  useRuntimeConfig,
} from "#imports";
import fetchMetadata from "../../../../../../tailwind-components/app/composables/fetchMetadata.js";
import fetchTableData from "../../../../../../tailwind-components/app/composables/fetchTableData.js";
import { useRoute } from "vue-router";
const config = useRuntimeConfig();
const route = useRoute();
const schema = config.public.schema;
const TABLE_NAME = "Resources";
const schemaMetaData = await fetchMetadata(schema);

const resource = ref<any>({
  name: "my resource",
  acronym: "MR",
  description: "This is a description of my resource",
});

const crumbs = useResourceDetailsCrumbs();

const metadata = schemaMetaData.tables.find((table) => table.id === TABLE_NAME);

const columns = metadata ? metadata.columns : [];
const headings = columns.filter((col) => col.columnType === "HEADING");

const tocItems = computed(() => {
  return headings.map((heading) => ({
    id: heading.id,
    label: heading.label,
  }));
});

// fetch the resource data 
const rowId = { id: route.params.resource };
const data = await fetchTableData(schema, TABLE_NAME, {filter: rowId, offset: 0, limit: 1});
</script>
<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader
        id="generated-resource-page-header"
        :title="resource?.acronym || resource?.name"
        :description="resource?.name ? resource.name : ''"
      >
        <template #prefix>
          <BreadCrumbs :crumbs="crumbs" />
        </template>
      </PageHeader>
    </template>
    <template #side>
      <SideNavigation
        :title="resource?.acronym || resource?.name"
        :image="resource?.logo?.url"
        :items="tocItems"
        header-target="#resource-page-header"
      />
    </template>
    <template #main>
      <ContentBlocks>
        <ContentBlock
          v-for="heading in headings"
          :id="heading.id"
          :title="heading.label"
          :description="heading.description"
          >my block</ContentBlock
        >
      </ContentBlocks>
    </template>
  </LayoutsDetailPage>
</template>
