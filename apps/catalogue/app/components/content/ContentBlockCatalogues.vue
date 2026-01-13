<script setup lang="ts">
import type { IResources } from "../../../interfaces/catalogue";
import { navigateTo } from "#app/composables/router";
import Table from "../../../../tailwind-components/app/components/Table.vue";
import TableHead from "../../../../tailwind-components/app/components/TableHead.vue";
import TableHeadRow from "../../../../tailwind-components/app/components/TableHeadRow.vue";
import TableRow from "../../../../tailwind-components/app/components/TableRow.vue";
import TableCell from "../../../../tailwind-components/app/components/TableCell.vue";
import IconButton from "../../../../tailwind-components/app/components/button/IconButton.vue";
import ContentBlock from "../../../../tailwind-components/app/components/content/ContentBlock.vue";
defineProps<{
  catalogues: IResources[];
  title: string;
  description: string;
}>();
</script>

<template>
  <ContentBlock class="mt-1" :title="title" :description="description">
    <Table>
      <template #head>
        <TableHeadRow>
          <TableHead class="hidden sm:table-cell"></TableHead>
          <TableHead></TableHead>
          <TableHead class="hidden sm:table-cell"></TableHead>
          <TableHead></TableHead>
        </TableHeadRow>
      </template>
      <template #body>
        <TableRow
          v-for="catalogue in catalogues"
          :key="catalogue.id"
          @click="navigateTo(`/${catalogue.id}`)"
        >
          <TableCell class="hidden sm:table-cell">
            <NuxtLink :to="`/${catalogue.id}`">
              <div class="items-center flex justify-center w-32 h-12">
                <img
                  v-if="catalogue?.logo?.url"
                  :src="catalogue?.logo?.url"
                  class="h-full object-contain"
                  :alt="catalogue.name"
                />
              </div>
            </NuxtLink>
          </TableCell>
          <TableCell>
            <NuxtLink :to="`/${catalogue.id}`">
              <span
                class="text-body-base font-extrabold text-link hover:underline hover:bg-link-hover"
                >{{ catalogue.id }}</span
              >
            </NuxtLink>
          </TableCell>
          <TableCell class="hidden sm:table-cell text-title-contrast">
            <NuxtLink :to="`/${catalogue.id}`">
              {{ catalogue?.name }}
            </NuxtLink>
          </TableCell>
          <TableCell>
            <NuxtLink :to="`/${catalogue.id}`">
              <IconButton icon="arrow-right" class="text-link" />
            </NuxtLink>
          </TableCell>
        </TableRow>

        <div v-if="catalogues.length == 0" class="flex justify-center pt-3">
          <span class="py-15 text-link">
            No catalogue found with current filters
          </span>
        </div>
      </template>
    </Table>
  </ContentBlock>
</template>
