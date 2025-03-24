<template>
  <FormAddModal
    v-if="metadata"
    :metadata="metadata"
    :schemaId="schemaId"
    :key="metadata.id"
  ></FormAddModal>

  <div>
    <DemoDataControles
      :include-row-select="false"
      v-model:metadata="metadata"
      v-model:schemaId="schemaId"
    >
    </DemoDataControles>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import type { ITableMetaData } from "../../../metadata-utils/src/types";
import DemoDataControles from "~/DemoDataControles.vue";

const metadata = ref<ITableMetaData>();
const schemaId = ref<string>("type test");
</script>
