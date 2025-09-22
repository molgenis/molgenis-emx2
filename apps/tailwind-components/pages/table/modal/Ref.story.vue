<template>
  <Button @click="showModal = true">Show</Button>

  <TableModalRef
    v-if="refTableColumn && refTableRow"
    v-model:visible="showModal"
    :metadata="refTableColumn"
    :row="refTableRow"
    schema="pet store"
    sourceTableId="Order"
    :showDataOwner="false"
  />
</template>

<script lang="ts" setup>
import fetchTableMetadata from "../../../composables/fetchTableMetadata";
import { ref } from "vue";
import type { IRefColumn, IRow } from "../../../../metadata-utils/src/types";

const showModal = ref(false);
const refTableRow = ref<IRow>();
const refTableColumn = ref<IRefColumn>();

const resp = await fetchTableMetadata("pet store", "Order");
refTableColumn.value = resp.columns.find(
  (col) => col.id === "pet"
) as IRefColumn;

refTableRow.value = {
  name: "pooky",
  category: { name: "cat" },
  photoUrls: [
    "https://emx2.dev.molgenis.org/pet%20store/tables/#/Pet%27%22%3E%3Cimg%20src=x%20onerror=alert(%22xss_reflected%22)%3E2",
  ],
  status: "available",
  weight: 9.4,
  mg_draft: false,
  mg_insertedBy: "admin",
  mg_insertedOn: "2025-01-23T14:40:08.779544",
  mg_updatedBy: "admin",
  mg_updatedOn: "2025-03-27T19:24:48.283381",
};
</script>
