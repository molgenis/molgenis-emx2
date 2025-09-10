<template>
  <FormAddModal
    v-if="metadata"
    :metadata="metadata"
    :schemaId="schemaId"
    :key="metadata.id"
  ></FormAddModal>

  <div>
    <DemoDataControls
      :include-row-select="false"
      v-model:metadata="metadata"
      v-model:schemaId="schemaId"
    >
    </DemoDataControls>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import type { ITableMetaData } from "../../../metadata-utils/src/types";
import DemoDataControls from "../../DemoDataControls.vue";

const metadata = ref<ITableMetaData>();
const schemaId = ref<string>("type test");
</script>
