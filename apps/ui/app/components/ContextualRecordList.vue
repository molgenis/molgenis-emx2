<script setup lang="ts">
import BreadCrumbs from "../../../tailwind-components/app/components/BreadCrumbs.vue";
import Container from "../../../tailwind-components/app/components/Container.vue";
import PageHeader from "../../../tailwind-components/app/components/PageHeader.vue";
import DataList from "../../../tailwind-components/app/components/display/DataList.vue";
import type { Crumb } from "../../../tailwind-components/types/types";

const CONTEXTUAL_LIST_PAGE_SIZE = 20;

defineProps<{
  schemaId: string;
  tableId: string;
  filter?: object;
  hideColumns: string[];
  title: string;
  crumbs: Crumb[];
  parentRecordPath: string;
}>();
</script>

<template>
  <Container :wide="true">
    <PageHeader :title="title" align="left" :back-path="parentRecordPath">
      <template #prefix>
        <BreadCrumbs align="left" :crumbs="crumbs" />
      </template>
    </PageHeader>

    <DataList
      :schema-id="schemaId"
      :table-id="tableId"
      :filter="filter"
      :hide-columns="hideColumns"
      :page-size="CONTEXTUAL_LIST_PAGE_SIZE"
      layout="TABLE"
    />
  </Container>
</template>
