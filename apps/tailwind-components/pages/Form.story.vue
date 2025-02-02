<script setup lang="ts">
import type { FormFields } from "#build/components";
import type {
  columnValue,
  IColumn,
  IFieldError,
  ISchemaMetaData,
  ITableMetaData,
} from "../../metadata-utils/src/types";

const exampleName = ref("simple");

const exampleMap = ref({
  simple: {
    schemaId: "pet store",
    tableId: "Pet",
  },
  "pet store order": {
    schemaId: "pet store",
    tableId: "Order",
  },
  complex: {
    schemaId: "catalogue-demo",
    tableId: "Resources",
  },
});

// just assuming that the table is there for the demo
const exampleConfig = computed(() => exampleMap.value[exampleName.value]);

const {
  data: schemaMeta,
  refresh: refetchMetadata,
  status,
} = await useAsyncData("form sample", () =>
  fetchMetadata(exampleConfig.value.schemaId)
);

const tableMeta = computed(
  () =>
    (schemaMeta.value as ISchemaMetaData)?.tables.find(
      (table) => table.id === exampleConfig.value.tableId
    ) as ITableMetaData
);

function refetch() {
  refetchMetadata();
}

const data = ref([] as Record<string, columnValue>[]);

const formFields = ref<InstanceType<typeof FormFields>>();

const formValues = ref<Record<string, columnValue>>({});

function onModelUpdate(value: Record<string, columnValue>) {
  formValues.value = value;
}

const errors = ref<Record<string, IFieldError[]>>({});

function onErrors(newErrors: Record<string, IFieldError[]>) {
  errors.value = newErrors;
}

function chapterFieldIds(chapterId: string) {
  const chapterFieldIds = [];
  let inChapter = false;
  for (const column of tableMeta.value.columns) {
    if (column.columnType === "HEADING" && column.id === chapterId) {
      inChapter = true;
    } else if (column.columnType === "HEADING" && column.id !== chapterId) {
      inChapter = false;
    } else if (inChapter) {
      chapterFieldIds.push(column.id);
    }
  }
  return chapterFieldIds;
}

function chapterErrorCount(chapterId: string) {
  return chapterFieldIds(chapterId).reduce((acc, fieldId) => {
    return acc + (errors.value[fieldId]?.length ?? 0);
  }, 0);
}

const currentSectionDomId = ref("");

const sections = computed(() => {
  return tableMeta.value?.columns
    .filter((column: IColumn) => column.columnType == "HEADING")
    .map((column: IColumn) => {
      return {
        label: column.label,
        domId: column.id,
        isActive: currentSectionDomId.value.startsWith(column.id),
        errorCount: chapterErrorCount(column.id),
      };
    });
});

function setUpChapterIsInViewObserver() {
  if (import.meta.client) {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          const id = entry.target.getAttribute("id");
          if (id && entry.intersectionRatio > 0) {
            currentSectionDomId.value = id;
          }
        });
      },
      {
        root: formFields.value?.$el,
        rootMargin: "0px",
        threshold: 0.5,
      }
    );

    document.querySelectorAll("[id$=chapter-title]").forEach((section) => {
      observer.observe(section);
    });
  }
}

onMounted(() => setUpChapterIsInViewObserver());

watch(
  () => tableMeta.value,
  async () => {
    await nextTick();
    setUpChapterIsInViewObserver();
  }
);
</script>

<template>
  <div class="flex flex-row">
    <div id="mock-form-contaner" class="basis-2/3 flex flex-row border">
      <div class="basis-1/3">
        <FormLegend
          v-if="sections"
          class="bg-sidebar-gradient mx-4"
          :sections="sections"
        />
      </div>

      <FormFields
        :key="exampleName"
        v-if="tableMeta && status === 'success'"
        class="basis-2/3 p-8 border-l overflow-y-auto h-screen"
        ref="formFields"
        :schemaId="exampleConfig.schemaId"
        :metadata="tableMeta"
        :data="data"
        @update:model-value="onModelUpdate"
        @error="onErrors($event)"
      />
    </div>

    <div class="basis-1/3 ml-2 h-screen overflow-y-scroll">
      <h2>Demo controls, settings and status</h2>

      <div class="p-4 border-2 mb-2">
        <label for="table-select">Demo data</label>
        <select
          id="table-select"
          @change="refetch()"
          v-model="exampleName"
          class="border-1 border-black"
        >
          <option value="simple">Simple form example</option>
          <option value="complex">Complex form example</option>
          <option value="pet store order">Pet store order</option>
        </select>

        <div>schema id = {{ exampleConfig.schemaId }}</div>
        <div>table id = {{ exampleConfig.tableId }}</div>

        <button
          class="border-gray-900 border-[1px] p-2 bg-gray-200"
          @click="formFields?.validate"
        >
          External Validate
        </button>

        <div class="mt-4 flex flex-row">
          <div v-if="Object.keys(formValues).length" class="basis-1/2">
            <h3 class="text-label">Values</h3>
            <dl class="flex flex-col">
              <template v-for="(value, key) in formValues">
                <dt class="font-bold">{{ key }}:</dt>
                <dd v-if="value !== null && value !== undefined" class="pl-3">
                  {{ value }}
                </dd>
              </template>
            </dl>
          </div>
          <div v-if="Object.keys(errors).length" class="basis-1/2">
            <h3 class="text-label">Errors</h3>

            <dl class="flex flex-col">
              <template v-for="(value, key) in errors">
                <dt class="font-bold">{{ key }}:</dt>
                <dd v-if="value.length" class="ml-1">{{ value }}</dd>
              </template>
            </dl>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
