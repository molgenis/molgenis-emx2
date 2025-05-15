<script setup lang="ts">
import { fetchMetadata } from "#imports";
import { computed } from "vue";

const metadata = await fetchMetadata();

const tables = computed(() =>
  metadata.tables.filter((t) => t.id !== "Version")
);
</script>
<template>
  <LayoutsLandingPage>
    <ContentBlock :title="metadata.label" class="mt-5">
      <Table class="">
        <template #head>
          <TableHeadRow>
            <TableHead>
              <span class="hidden sm:table-cell">Name</span>
            </TableHead>
            <TableHead
              ><span class="hidden sm:table-cell">Description</span></TableHead
            >
          </TableHeadRow>
        </template>
        <template #body>
          <template v-for="table in tables">
            <TableRow>
              <TableCell>
                <NuxtLink :to="`/catalogue/all/${table.id}`">{{
                  table.label
                }}</NuxtLink></TableCell
              >
              <TableCell
                ><NuxtLink :to="`${table.id}`">{{
                  table.description ? table.description : ""
                }}</NuxtLink></TableCell
              >
            </TableRow>
          </template>
        </template>
      </Table>
    </ContentBlock>
  </LayoutsLandingPage>
</template>
