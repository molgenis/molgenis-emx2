<template>
  <div class="flex flex-col gap-8 p-4">
    <section>
      <h2 class="text-title text-heading-xl mb-2">Live RecordAccordion</h2>

      <DemoDataControls
        v-model:metadata="metadata"
        v-model:schemaId="schemaId"
        v-model:formValues="formValues"
        v-model:tableId="tableId"
        :include-row-select="true"
        :row-index="rowIndex"
      >
      </DemoDataControls>

      <fieldset class="p-4 border-2 mb-2 flex flex-wrap gap-4">
        <legend class="text-title font-bold">RecordAccordion props</legend>
        <div>
          <label class="text-title font-bold" for="show-details">
            Details (showDetails):
          </label>
          <InputCheckbox
            id="show-details"
            v-model="showDetails"
            name="show-details"
          />
        </div>
        <div>
          <label class="text-title font-bold" for="open-by-default">
            Open by default (openByDefault):
          </label>
          <InputCheckbox
            id="open-by-default"
            v-model="openByDefault"
            name="open-by-default"
          />
        </div>
      </fieldset>

      <RecordAccordion
        v-if="metadata"
        :key="`${schemaId} - ${metadata.id} - ${JSON.stringify(formValues)}`"
        :metadata="metadata"
        :row-data="formValues"
        :show-details="showDetails"
        :open-by-default="openByDefault"
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import type {
  columnId,
  columnValue,
  ITableMetaData,
} from "../../../../metadata-utils/src/types";
import RecordAccordion from "../../components/display/RecordAccordion.vue";
import DemoDataControls from "../../DemoDataControls.vue";
import { useRoute, useRouter } from "vue-router";

const router = useRouter();
const route = useRoute();

const metadata = ref<ITableMetaData>();
const schemaId = ref<string>((route.query.schema as string) || "pet store");
const formValues = ref<Record<columnId, columnValue>>({});
const tableId = ref<string>((route.query.table as string) || "Pet");
const rowIndex = ref<number>(
  route.query.rowIndex ? Number(route.query.rowIndex) : 0
);

const showDetails = ref(true);
const openByDefault = ref(false);

watch([schemaId, tableId], ([newSchemaId, newTableId]) => {
  router.push({
    query: {
      schema: newSchemaId,
      table: newTableId,
    },
  });
});
</script>
