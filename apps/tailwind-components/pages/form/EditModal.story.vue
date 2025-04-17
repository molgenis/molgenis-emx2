<template>
  <FormEditModal
    v-if="metadata"
    :metadata="metadata"
    :schemaId="schemaId"
    :formValues="formValues"
    :key="`${schemaId} - ${metadata.id} - ${JSON.stringify(formValues)}`"
  ></FormEditModal>

  <div>
    <DemoDataControles
      v-model:metadata="metadata"
      v-model:schemaId="schemaId"
      v-model:formValues="formValues"
    >
    </DemoDataControles>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import type {
  columnId,
  columnValue,
  ITableMetaData,
} from "../../../metadata-utils/src/types";
import DemoDataControles from "../../DemoDataControles.vue";

const metadata = ref<ITableMetaData>();
const schemaId = ref<string>("type test");
const formValues = ref<Record<columnId, columnValue>>({});
</script>
