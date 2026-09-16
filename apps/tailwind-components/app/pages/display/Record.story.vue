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
          <label class="text-title font-bold" for="record-layout">
            Cards (layout):
          </label>
          <InputCheckbox
            id="record-layout"
            v-model="useCardLayout"
            name="record-layout"
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
        <div>
          <label class="text-title font-bold" for="filter-term">
            Field filter (filterTerm):
          </label>
          <input
            id="filter-term"
            v-model="filterTerm"
            type="text"
            class="border-2 px-1"
            placeholder="e.g. name"
          />
        </div>
        <div>
          <label class="text-title font-bold" for="title-template">
            Title template (titleTemplate):
          </label>
          <input
            id="title-template"
            v-model="titleTemplate"
            type="text"
            class="border-2 px-1"
            placeholder="e.g. ${name}, a good dog"
          />
        </div>
      </fieldset>

      <DisplayRecord
        v-if="metadata"
        :key="`${schemaId} - ${metadata.id} - ${JSON.stringify(formValues)}`"
        :columns="storyColumns"
        :row="formValues"
        :showLegend="showLegend"
        :layout="useCardLayout ? 'CARDS' : 'PLAIN'"
        :titleTemplate="titleTemplate || undefined"
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import type {
  columnId,
  columnValue,
  IColumn,
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
const useCardLayout = ref(true);
const showMgColumns = ref(false);
const filterTerm = ref("");
const titleTemplate = ref("");

// Record no longer filters columns itself; the story reproduces the old
// showMgColumns/filterTerm controls here, the way any caller now must.
const storyColumns = computed<IColumn[]>(() =>
  (metadata.value?.columns ?? []).filter((column) => {
    if (column.columnType === "HEADING" || column.columnType === "SECTION") {
      return true;
    }
    if (column.id.startsWith("mg_") && !showMgColumns.value) {
      return false;
    }
    return column.label.toLowerCase().includes(filterTerm.value.toLowerCase());
  })
);

watch([schemaId, tableId], ([newSchemaId, newTableId]) => {
  router.push({
    query: {
      schema: newSchemaId,
      table: newTableId,
    },
  });
});
</script>
