<script setup lang="ts">
const route = useRoute();
import type { ICatalogue } from "~/interfaces/types";

const props = defineProps<{
  catalogues: ICatalogue[];
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
          :key="catalogue.network.id"
          @click="
            navigateTo(
              `/${route.params.schema}/ssr-catalogue/${catalogue.network.id}`
            )
          "
        >
          <TableCell class="hidden sm:table-cell">
            <div class="items-center flex justify-center w-32">
              <img :src="catalogue.network?.logo?.url" />
            </div>
          </TableCell>
          <TableCell>
            <span
              class="text-body-base font-extrabold text-blue-500 hover:underline hover:bg-blue-50"
              >{{ catalogue.network.id }}</span
            >
          </TableCell>
          <TableCell class="hidden sm:table-cell">
            {{ catalogue.network?.name }}
          </TableCell>
          <TableCell>
            <IconButton icon="arrow-right" class="text-blue-500" />
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
