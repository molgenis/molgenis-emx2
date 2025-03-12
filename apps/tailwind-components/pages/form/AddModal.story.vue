<template>
  <FormAddModal :metadata="metadata"></FormAddModal>
</template>

<script setup lang="ts">
import type { ITableMetaData } from "../../../metadata-utils/src/types";
import cohortTableMetadata from "../samples/data/cohort-table-metadata";

const metadata = ref<ITableMetaData>(cohortTableMetadata as ITableMetaData);
</script>
