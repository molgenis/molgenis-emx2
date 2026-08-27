<script setup lang="ts">
import { computed, ref, watch } from "vue";
import type { IColumn, IRow } from "../../../metadata-utils/src/types";
import { LAYOUTS, type DisplayConfig, type Layout } from "../types/display";
import fetchMetadata from "../composables/fetchMetadata";
import fetchTableMetadata from "../composables/fetchTableMetadata";

const fixtureColumns: IColumn[] = [
  {
    id: "acronym",
    label: "Acronym",
    columnType: "STRING",
    key: 1,
    position: 1,
  },
  { id: "name", label: "Name", columnType: "STRING", key: 1, position: 2 },
  { id: "description", label: "Description", columnType: "TEXT" },
  { id: "website", label: "Website", columnType: "HYPERLINK" },
  { id: "logo", label: "Logo", columnType: "FILE" },
  { id: "startYear", label: "Start year", columnType: "INT" },
  { id: "status", label: "Status", columnType: "STRING" },
  { id: "country", label: "Country", columnType: "STRING" },
];

const fixtureRows: IRow[] = [
  {
    acronym: "LL",
    name: "Lifelines",
    description:
      "A three-generation cohort study following 167,000 Northern Netherlands residents.",
    website: "https://www.lifelines.nl",
    logo: {
      id: "logo-ll",
      size: 4213,
      filename: "lifelines.png",
      extension: "png",
      url: "https://molgenis.github.io/images/logo.png",
    },
    startYear: 2006,
    status: "Active",
    country: "Netherlands",
  },
  {
    acronym: "UKB",
    name: "UK Biobank",
    description:
      "A large-scale biomedical database of half a million UK participants.",
    website: "https://www.ukbiobank.ac.uk",
    logo: {
      id: "logo-ukb",
      size: 3877,
      filename: "ukb.png",
      extension: "png",
      url: "https://molgenis.github.io/images/logo.png",
    },
    startYear: 2006,
    status: "Active",
    country: "United Kingdom",
  },
  {
    acronym: "GenR",
    name: "Generation R",
    description:
      "A population-based prospective cohort study from fetal life until adulthood.",
    website: "https://generationr.nl",
    logo: null,
    startYear: 2002,
    status: "Active",
    country: "Netherlands",
  },
  {
    acronym: "EPIC",
    name: "European Prospective Investigation into Cancer and Nutrition",
    description:
      "A cohort study into diet, lifestyle and cancer across ten European countries.",
    website: "https://epic.iarc.fr",
    logo: null,
    startYear: 1992,
    status: "Closed",
    country: "Multiple",
  },
];

const isLiveMode = ref(false);
const schemaId = ref("pet store");
const tableId = ref("Pet");
const schemaIds = ref<string[]>([]);
const tableIds = ref<string[]>([]);
const liveColumns = ref<IColumn[]>([]);

async function loadSchemaIds() {
  const { data } = await $fetch<{ data: { _schemas: { id: string }[] } }>(
    "/graphql",
    {
      method: "POST",
      body: { query: "{ _schemas { id } }" },
    }
  );
  schemaIds.value = data._schemas.map((schema) => schema.id);
}

async function loadTableIds() {
  if (!schemaId.value) {
    return;
  }
  const schemaMetadata = await fetchMetadata(schemaId.value).catch(
    () => undefined
  );
  tableIds.value = schemaMetadata?.tables.map((table) => table.id) ?? [];
  if (!tableIds.value.includes(tableId.value)) {
    tableId.value = tableIds.value[0] ?? "";
  }
}

async function loadColumns() {
  if (!schemaId.value || !tableId.value) {
    liveColumns.value = [];
    return;
  }
  const metadata = await fetchTableMetadata(
    schemaId.value,
    tableId.value
  ).catch(() => undefined);
  liveColumns.value = metadata?.columns ?? [];
}

watch(isLiveMode, (value) => {
  if (value) {
    loadSchemaIds();
    loadTableIds().then(loadColumns);
  }
});
watch(schemaId, () => {
  if (isLiveMode.value) {
    loadTableIds().then(loadColumns);
  }
});
watch(tableId, () => {
  if (isLiveMode.value) {
    loadColumns();
  }
});

const columns = computed(() =>
  isLiveMode.value ? liveColumns.value : fixtureColumns
);

const titleTemplate = ref("${acronym} ${name}");
const descriptionTemplate = ref("${description}");
const detailColumnIds = ref<string[]>([
  "website",
  "startYear",
  "status",
  "country",
]);
const logoColumnId = ref("logo");

function onDetailColumnsChange(event: Event) {
  const select = event.target as HTMLSelectElement;
  const selectedIds = Array.from(select.selectedOptions).map(
    (option) => option.value
  );
  const kept = detailColumnIds.value.filter((id) => selectedIds.includes(id));
  const added = selectedIds.filter((id) => !kept.includes(id));
  detailColumnIds.value = [...kept, ...added];
}

function displayFor(layout: Layout): DisplayConfig {
  return {
    layout,
    titleTemplate: titleTemplate.value || undefined,
    descriptionTemplate: descriptionTemplate.value || undefined,
    detailColumns: detailColumnIds.value,
    logoColumn: logoColumnId.value || undefined,
  };
}

function linkTo(row: IRow): string {
  return `#${encodeURIComponent(String(Object.values(row)[0] ?? ""))}`;
}

function panelTestId(layout: Layout, surface: "content" | "footer"): string {
  return `panel-${layout.toLowerCase()}-${surface}`;
}
</script>

<template>
  <div class="flex">
    <div class="flex-1 flex flex-col gap-8">
      <div
        v-for="layoutOption in LAYOUTS"
        :key="layoutOption"
        class="flex flex-col gap-2"
      >
        <p class="text-title-contrast font-bold">{{ layoutOption }}</p>
        <div
          class="bg-content p-4"
          :data-testid="panelTestId(layoutOption, 'content')"
        >
          <p class="mb-2 text-title-contrast font-bold">Content surface</p>
          <DisplayDataList
            :display="displayFor(layoutOption)"
            :rows="isLiveMode ? undefined : fixtureRows"
            :columns="isLiveMode ? undefined : fixtureColumns"
            :schema-id="isLiveMode ? schemaId : undefined"
            :table-id="isLiveMode ? tableId : undefined"
            :link-to="linkTo"
          />
        </div>

        <div
          class="bg-footer p-4"
          :data-testid="panelTestId(layoutOption, 'footer')"
        >
          <p class="mb-2 text-title font-bold">
            A colour the theme owns (footer)
          </p>
          <DisplayDataList
            :display="displayFor(layoutOption)"
            :rows="isLiveMode ? undefined : fixtureRows"
            :columns="isLiveMode ? undefined : fixtureColumns"
            :schema-id="isLiveMode ? schemaId : undefined"
            :table-id="isLiveMode ? tableId : undefined"
            :link-to="linkTo"
          />
        </div>
      </div>
    </div>

    <div class="ml-4 mt-2 w-72">
      <fieldset class="border border-theme mb-2 p-3">
        <legend class="px-2">Props</legend>

        <div class="mb-3">
          <label
            class="block text-title hover:cursor-pointer"
            for="ddl-title-template"
          >
            titleTemplate
          </label>
          <input
            id="ddl-title-template"
            class="w-full"
            type="text"
            v-model="titleTemplate"
          />
        </div>

        <div class="mb-3">
          <label
            class="block text-title hover:cursor-pointer"
            for="ddl-description-template"
          >
            descriptionTemplate
          </label>
          <input
            id="ddl-description-template"
            class="w-full"
            type="text"
            v-model="descriptionTemplate"
          />
        </div>

        <div class="mb-3">
          <label
            class="block text-title hover:cursor-pointer"
            for="ddl-detail-columns"
          >
            detailColumns (order = pick order)
          </label>
          <select
            id="ddl-detail-columns"
            class="w-full"
            multiple
            :value="detailColumnIds"
            @change="onDetailColumnsChange"
          >
            <option
              v-for="column in columns"
              :key="column.id"
              :value="column.id"
            >
              {{ column.label || column.id }}
            </option>
          </select>
          <p class="text-title">Picked: {{ detailColumnIds.join(", ") }}</p>
        </div>

        <div class="mb-3">
          <label
            class="block text-title hover:cursor-pointer"
            for="ddl-logo-column"
          >
            logoColumn
          </label>
          <select id="ddl-logo-column" class="w-full" v-model="logoColumnId">
            <option value="">(none)</option>
            <option
              v-for="column in columns"
              :key="column.id"
              :value="column.id"
            >
              {{ column.label || column.id }}
            </option>
          </select>
        </div>
      </fieldset>

      <hr />

      <fieldset class="border border-theme mt-2 p-3">
        <legend class="px-2">Data mode</legend>
        <div class="mb-2">
          <input id="ddl-live-mode" type="checkbox" v-model="isLiveMode" />
          <label
            class="ml-1 text-title hover:cursor-pointer"
            for="ddl-live-mode"
          >
            live mode (fetch from backend)
          </label>
        </div>

        <div v-if="isLiveMode" class="mb-2">
          <label class="block text-title hover:cursor-pointer" for="ddl-schema">
            schema
          </label>
          <select id="ddl-schema" class="w-full" v-model="schemaId">
            <option v-for="id in schemaIds" :key="id" :value="id">
              {{ id }}
            </option>
          </select>
        </div>

        <div v-if="isLiveMode" class="mb-2">
          <label class="block text-title hover:cursor-pointer" for="ddl-table">
            table
          </label>
          <select id="ddl-table" class="w-full" v-model="tableId">
            <option v-for="id in tableIds" :key="id" :value="id">
              {{ id }}
            </option>
          </select>
        </div>
      </fieldset>
    </div>
  </div>
</template>
