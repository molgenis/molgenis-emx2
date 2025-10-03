<template>
  <FormEditModal
    v-if="metadata"
    :metadata="metadata"
    :schemaId="schemaId"
    :formValues="formValues"
    :key="`${schemaId} - ${metadata.id} - ${JSON.stringify(formValues)}`"
  ></FormEditModal>

  <div>
    <DemoDataControls
      v-model:metadata="metadata"
      v-model:schemaId="schemaId"
      v-model:formValues="formValues"
    >
    </DemoDataControls>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import type {
  columnId,
  columnValue,
  ITableMetaData,
} from "../../../metadata-utils/src/types";
import DemoDataControls from "../../DemoDataControls.vue";

const metadata = ref<ITableMetaData>();
const schemaId = ref<string>("type test");
const formValues = ref<Record<columnId, columnValue>>({});
</script>
