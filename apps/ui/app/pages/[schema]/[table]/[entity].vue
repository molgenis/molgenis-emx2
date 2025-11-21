<script setup lang="ts">
import { useRoute, useRouter } from "#app/composables/router";
import BreadCrumbs from "../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import PageHeader from "../../../../../tailwind-components/app/components/PageHeader.vue";

const route = useRoute();
const router = useRouter();
const schemaId = route.params.schema as string;
const tableId = route.params.table as string;
const entityId = route.params.entity as string;
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
            { label: entityId },
          ]"
        />
      </template>
    </PageHeader>
  </section>
</template>
