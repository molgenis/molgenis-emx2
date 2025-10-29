<template>
  <FormEditModal
    v-if="metadata"
    :metadata="metadata"
    :schemaId="schemaId"
    :key="metadata.id"
  ></FormEditModal>

  <div>
    <DemoDataControls
      v-model:metadata="metadata"
      v-model:schemaId="schemaId"
      v-model:tableId="tableId"
    >
    </DemoDataControls>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import type { ITableMetaData } from "../../../../metadata-utils/src/types";
import DemoDataControls from "../../DemoDataControls.vue";

const metadata = ref<ITableMetaData>();
const schemaId = ref<string>("type test");
const tableId = ref<string>("Types");
</script>
