<template>
  <div class="flex flex-col gap-8 p-4">
    <section>
      <h2 class="text-title text-heading-xl mb-2">Live record</h2>
      <p class="text-title">
        Pick cms / Components to see a nested legend (SECTION columns).
      </p>

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
        <legend class="text-title font-bold">Record props</legend>
        <div>
          <label class="text-title font-bold" for="show-legend">
            Legend (showLegend):
          </label>
          <InputCheckbox
            id="show-legend"
            v-model="showLegend"
            name="show-legend"
          />
        </div>
        <div>
          <label class="text-title font-bold" for="show-cards">
            Cards (showCards):
          </label>
          <InputCheckbox
            id="show-cards"
            v-model="showCards"
            name="show-cards"
          />
        </div>
        <div>
          <label class="text-title font-bold" for="show-filter">
            Field filter (showFilter):
          </label>
          <InputCheckbox
            id="show-filter"
            v-model="showFilter"
            name="show-filter"
          />
        </div>
        <div>
          <label class="text-title font-bold" for="show-mg-columns">
            mg_ columns (showMgColumns):
          </label>
          <InputCheckbox
            id="show-mg-columns"
            v-model="showMgColumns"
            name="show-mg-columns"
          />
        </div>
      </fieldset>

      <DisplayRecord
        v-if="metadata"
        :key="`${schemaId} - ${metadata.id} - ${JSON.stringify(formValues)}`"
        :metadata="metadata"
        :rowData="formValues"
        :showLegend="showLegend"
        :showCards="showCards"
        :showFilter="showFilter"
        :showMgColumns="showMgColumns"
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
import DisplayRecord from "../../components/display/Record.vue";
import DemoDataControls from "../../DemoDataControls.vue";
import { useRoute, useRouter } from "vue-router";

const router = useRouter();
const route = useRoute();

const metadata = ref<ITableMetaData>();
const schemaId = ref<string>((route.query.schema as string) || "cms");
const formValues = ref<Record<columnId, columnValue>>({});
const tableId = ref<string>((route.query.table as string) || "Components");
const rowIndex = ref<number>(
  route.query.rowIndex ? Number(route.query.rowIndex) : 0
);

const showLegend = ref(true);
const showCards = ref(true);
const showFilter = ref(false);
const showMgColumns = ref(false);

watch([schemaId, tableId], ([newSchemaId, newTableId]) => {
  router.push({
    query: {
      schema: newSchemaId,
      table: newTableId,
    },
  });
});
</script>
