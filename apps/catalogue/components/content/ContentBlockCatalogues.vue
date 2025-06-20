<script setup lang="ts">
import type { IResources } from "~/interfaces/catalogue";
import { navigateTo } from "#app/composables/router";

const props = defineProps<{
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
                  alt="Catalogue Logo"
                />
              </div>
            </NuxtLink>
          </TableCell>
          <TableCell>
            <NuxtLink :to="`/${catalogue.id}`">
              <span
                class="text-body-base font-extrabold text-blue-500 hover:underline hover:bg-blue-50"
                >{{ catalogue.id }}</span
              >
            </NuxtLink>
          </TableCell>
          <TableCell class="hidden sm:table-cell">
            <NuxtLink :to="`/${catalogue.id}`">
              {{ catalogue?.name }}
            </NuxtLink>
          </TableCell>
          <TableCell>
            <NuxtLink :to="`/${catalogue.id}`">
              <IconButton icon="arrow-right" class="text-blue-500" />
            </NuxtLink>
          </TableCell>
        </TableRow>

        <div v-if="catalogues.length == 0" class="flex justify-center pt-3">
          <span class="py-15 text-blue-500">
            No catalogue found with current filters
          </span>
        </div>
      </template>
    </Table>
  </ContentBlock>
</template>
