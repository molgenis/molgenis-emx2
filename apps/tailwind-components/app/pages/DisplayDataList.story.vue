<script setup lang="ts">
import { computed, ref, watch } from "vue";
import type { IColumn, IRow } from "../../../metadata-utils/src/types";
import { LAYOUTS, type DisplayConfig, type Layout } from "../types/display";
import { resolveDisplay } from "../utils/displayUtils";
import fetchMetadata from "../composables/fetchMetadata";
import fetchTableMetadata from "../composables/fetchTableMetadata";
import type { IValueLabel } from "../../types/types";

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
  {
    acronym: "HELIX",
    name: "Human Early Life Exposome",
    description:
      "A birth cohort collaboration studying early-life environmental exposures.",
    website: "https://www.projecthelix.eu",
    logo: null,
    startYear: 2013,
    status: "Active",
    country: "Multiple",
  },
  {
    acronym: "ALSPAC",
    name: "Avon Longitudinal Study of Parents and Children",
    description:
      "A long-running birth cohort in the Bristol area following parents and children across four decades, with repeated clinical assessments, questionnaires, biological samples and, more recently, genetic and omics data linked to routine health records.",
    website: "https://www.bristol.ac.uk/alspac",
    logo: null,
    startYear: 1991,
    status: "Active",
    country: "United Kingdom",
  },
  {
    // Empty, not missing: demonstrates the promote rule (an empty title
    // slot shows the subtitle's text instead) when titleTemplate is set to
    // ${acronym} and subtitleTemplate to ${name}.
    acronym: "",
    name: "European Health Examination Survey",
    description:
      "A pilot survey harmonising health examinations across EU member states.",
    website: "https://www.ehes.info",
    logo: null,
    startYear: 2009,
    status: "Active",
    country: "Multiple",
  },
  {
    acronym: "MoBa",
    name: "Norwegian Mother, Father and Child Cohort Study",
    description: null,
    website: "https://www.fhi.no/en/studies/moba",
    logo: null,
    startYear: 1999,
    status: "Active",
    country: "Norway",
  },
  {
    acronym: "DNBC",
    name: "Danish National Birth Cohort",
    description:
      "Pregnancy and childhood follow-up of Danish mother-child pairs.",
    website: null,
    logo: null,
    startYear: 1996,
    status: "Closed",
    country: "Denmark",
  },
  {
    acronym: "CHOP",
    name: "Child Health and Obesity Project",
    description:
      "Diet and growth follow-up in early childhood across five European sites.",
    website: "https://www.projectchop.eu",
    logo: null,
    startYear: 2002,
    status: "Closed",
    country: "Multiple",
  },
  {
    acronym: "RS",
    name: "Rotterdam Study",
    description:
      "A prospective population-based cohort in Ommoord, Rotterdam, examining determinants of chronic disease in middle-aged and elderly residents through repeated in-person examinations, imaging and biobanking.",
    website: "https://www.erasmus-epidemiology.nl/rotterdamstudy",
    logo: {
      id: "logo-rs",
      size: 3540,
      filename: "rotterdam.png",
      extension: "png",
      url: "https://molgenis.github.io/images/logo.png",
    },
    startYear: 1990,
    status: "Active",
    country: "Netherlands",
  },
  {
    acronym: "NFBC",
    name: "Northern Finland Birth Cohort",
    description: null,
    website: "https://www.oulu.fi/nfbc",
    logo: null,
    startYear: 1966,
    status: null,
    country: "Finland",
  },
  {
    acronym: "TwinsUK",
    name: "TwinsUK Registry",
    description:
      "The largest adult twin registry in the UK, used for genetic epidemiology.",
    website: "https://twinsuk.ac.uk",
    logo: null,
    startYear: 1992,
    status: "Active",
    country: null,
  },
  {
    acronym: "SHIP",
    name: "Study of Health in Pomerania",
    description:
      "A population-based cohort in north-east Germany covering general health.",
    website: "https://www2.medizin.uni-greifswald.de/cm/fv/ship",
    logo: null,
    startYear: 1997,
    status: "Active",
    country: "Germany",
  },
  {
    acronym: "KORA",
    name: "Cooperative Health Research in the Region of Augsburg",
    description: null,
    website: null,
    logo: null,
    startYear: 1984,
    status: "Active",
    country: "Germany",
  },
  {
    acronym: "HUNT",
    name: "Trøndelag Health Study",
    description:
      "Repeated population health surveys of the Trøndelag county in Norway.",
    website: "https://www.ntnu.edu/hunt",
    logo: null,
    startYear: 1984,
    status: "Active",
    country: "Norway",
  },
  {
    acronym: "ELSA",
    name: "English Longitudinal Study of Ageing",
    description:
      "A panel study of ageing in England covering health, economic and social wellbeing, with biennial interviews and periodic nurse visits collecting biomarkers and cognitive measures.",
    website: "https://www.elsa-project.ac.uk",
    logo: null,
    startYear: 2002,
    status: "Active",
    country: "United Kingdom",
  },
  {
    acronym: "GAZEL",
    name: "GAZEL Cohort",
    description: "Occupational cohort of French utility company employees.",
    website: "https://www.gazel.inserm.fr",
    logo: null,
    startYear: 1989,
    status: "Closed",
    country: "France",
  },
  {
    acronym: "PELOTAS",
    name: "Pelotas Birth Cohorts",
    description: null,
    website: "https://www.epidemio-ufpel.org.br",
    logo: null,
    startYear: 1982,
    status: "Active",
    country: "Brazil",
  },
  {
    acronym: "CHDS",
    name: "Christchurch Health and Development Study",
    description:
      "A longitudinal study of a New Zealand birth cohort into midlife.",
    website: null,
    logo: null,
    startYear: 1977,
    status: "Closed",
    country: "New Zealand",
  },
  {
    acronym: "MICROS",
    name: "MICROS Study",
    description:
      "Isolated Alpine village populations studied for genetic epidemiology.",
    website: "https://www.eurac.edu/micros",
    logo: null,
    startYear: 2001,
    status: null,
    country: "Italy",
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

// Column overrides describe a fixture table. Switching table or entering
// live mode drops them, so a picked column id never outlives the table it
// named.
function resetColumnOverrides() {
  titleTemplate.value = "";
  subtitleTemplate.value = "";
  descriptionColumnId.value = "";
  detailColumnIds.value = [];
  logoColumnId.value = "";
}

watch(isLiveMode, (value) => {
  if (value) {
    resetColumnOverrides();
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
    resetColumnOverrides();
    loadColumns();
  }
});

const columns = computed(() =>
  isLiveMode.value ? liveColumns.value : fixtureColumns
);

const columnOptions = computed<IValueLabel[]>(() =>
  columns.value.map((column) => ({
    value: column.id,
    label: column.label || column.id,
  }))
);

const logoColumnOptions = computed(() =>
  columns.value.filter((column) => column.columnType === "FILE")
);

const LAYOUT_FILTER_OPTIONS: string[] = ["All", ...LAYOUTS];
const layoutFilter = ref<string>("All");
const visibleLayouts = computed<Layout[]>(() =>
  layoutFilter.value === "All" ? [...LAYOUTS] : [layoutFilter.value as Layout]
);

// The pre-filled default. Never reimplements the defaulting rule.
const defaultTitleTemplate = computed(
  () => resolveDisplay(columns.value).titleTemplate
);
const titleTemplate = ref("");
const titleTemplateModel = computed({
  get: () => titleTemplate.value || defaultTitleTemplate.value,
  set: (value: string) => {
    titleTemplate.value = value;
  },
});

const subtitleTemplate = ref("");
const descriptionColumnId = ref("");

const detailColumnIds = ref<string[]>([
  "website",
  "startYear",
  "status",
  "country",
]);
const logoColumnId = ref("logo");

const PAGE_SIZE_OPTIONS: string[] = ["2", "4", "5", "6", "8", "10", "25", "50"];
const pageSizeOption = ref("10");
const pageSize = computed(() => Number(pageSizeOption.value));

// DataList watches `filter` by identity, so it must come from a computed. An
// object literal in the template would be a new object on every render, and
// every render would refetch and reset to page 1.
const filterText = ref("");
const parsedFilter = computed<{
  value?: Record<string, unknown>;
  error?: string;
}>(() => {
  const text = filterText.value.trim();
  if (!text) {
    return {};
  }
  try {
    return { value: JSON.parse(text) as Record<string, unknown> };
  } catch {
    return { error: "not valid JSON, so no filter is passed" };
  }
});
const filter = computed(() => parsedFilter.value.value);
const filterError = computed(() => parsedFilter.value.error);

function displayFor(layout: Layout): DisplayConfig {
  return {
    layout,
    titleTemplate: titleTemplate.value || undefined,
    subtitleTemplate: subtitleTemplate.value || undefined,
    descriptionColumnId: descriptionColumnId.value || undefined,
    detailColumnIds: detailColumnIds.value.length
      ? detailColumnIds.value
      : undefined,
    logoColumnId: logoColumnId.value || undefined,
  };
}

function linkTo(row: IRow): string {
  return `#${encodeURIComponent(String(Object.values(row)[0] ?? ""))}`;
}

function panelTestId(
  layout: Layout,
  surface: "content" | "footer" | "transparent"
): string {
  return `panel-${layout.toLowerCase()}-${surface}`;
}
</script>

<template>
  <div>
    <div
      class="sticky top-0 z-10 bg-form p-4 mb-6 flex flex-wrap gap-4 items-start border-b border-theme"
    >
      <div class="flex flex-col gap-1">
        <label class="text-title-contrast" for="ddl-layout">layout</label>
        <InputSelect
          id="ddl-layout"
          class="w-36"
          :options="LAYOUT_FILTER_OPTIONS"
          v-model="layoutFilter"
        />
      </div>

      <div class="flex flex-col gap-1">
        <label class="text-title-contrast" for="ddl-title-template">
          titleTemplate
        </label>
        <InputString
          id="ddl-title-template"
          class="w-56"
          v-model="titleTemplateModel"
        />
      </div>

      <div class="flex flex-col gap-1">
        <label class="text-title-contrast" for="ddl-subtitle-template">
          subtitleTemplate
        </label>
        <InputString
          id="ddl-subtitle-template"
          class="w-56"
          v-model="subtitleTemplate"
        />
      </div>

      <div class="flex flex-col gap-1">
        <label class="text-title-contrast" for="ddl-summary-column">
          summary column
        </label>
        <select
          id="ddl-summary-column"
          class="w-40"
          v-model="descriptionColumnId"
        >
          <option value="">(default)</option>
          <option v-for="column in columns" :key="column.id" :value="column.id">
            {{ column.label || column.id }}
          </option>
        </select>
      </div>

      <fieldset class="flex flex-col gap-1 border-0 p-0 m-0">
        <legend class="text-title-contrast p-0">
          detailColumns (order = pick order)
        </legend>
        <div class="w-56 max-h-24 overflow-y-auto border border-theme p-1">
          <InputCheckboxGroup
            id="ddl-detail-columns"
            :options="columnOptions"
            v-model="detailColumnIds"
          />
        </div>
      </fieldset>

      <div class="flex flex-col gap-1">
        <label class="text-title-contrast" for="ddl-logo-column">
          logoColumn (FILE columns only)
        </label>
        <select
          id="ddl-logo-column"
          class="w-40"
          v-model="logoColumnId"
          :aria-describedby="
            logoColumnOptions.length === 0 ? 'ddl-logo-empty-hint' : undefined
          "
        >
          <option value="">(none)</option>
          <option
            v-for="column in logoColumnOptions"
            :key="column.id"
            :value="column.id"
          >
            {{ column.label || column.id }}
          </option>
        </select>
        <p
          v-if="logoColumnOptions.length === 0"
          id="ddl-logo-empty-hint"
          class="text-title-contrast"
        >
          This table has no FILE column.
        </p>
      </div>

      <div class="flex flex-col gap-1">
        <label class="text-title-contrast" for="ddl-page-size">
          pageSize
        </label>
        <InputSelect
          id="ddl-page-size"
          class="w-24"
          :options="PAGE_SIZE_OPTIONS"
          v-model="pageSizeOption"
        />
      </div>

      <div class="flex flex-col gap-1 justify-end">
        <div class="flex items-center gap-1">
          <InputCheckbox id="ddl-live-mode" v-model="isLiveMode" />
          <label class="text-title-contrast" for="ddl-live-mode">
            live mode (fetch from backend)
          </label>
        </div>
      </div>

      <div v-if="isLiveMode" class="flex flex-col gap-1">
        <label class="text-title-contrast" for="ddl-schema">schema</label>
        <InputSelect
          id="ddl-schema"
          class="w-40"
          :options="schemaIds"
          v-model="schemaId"
        />
      </div>

      <div v-if="isLiveMode" class="flex flex-col gap-1">
        <label class="text-title-contrast" for="ddl-table">table</label>
        <InputSelect
          id="ddl-table"
          class="w-40"
          :options="tableIds"
          v-model="tableId"
        />
      </div>

      <div v-if="isLiveMode" class="flex flex-col gap-1">
        <label class="text-title-contrast" for="ddl-filter">
          filter (GraphQL, as JSON)
        </label>
        <InputString
          id="ddl-filter"
          class="w-72"
          placeholder='{"name":{"like":"pooky"}}'
          v-model="filterText"
          :aria-describedby="
            filterError ? 'ddl-filter-error' : 'ddl-filter-hint'
          "
        />
        <p
          v-if="filterError"
          id="ddl-filter-error"
          class="text-title-contrast"
          role="alert"
        >
          {{ filterError }}
        </p>
        <p v-else id="ddl-filter-hint" class="text-title-contrast">
          Applies in live mode only. Paging and load more keep it.
        </p>
      </div>
    </div>

    <div class="flex-1 flex flex-col gap-8 px-4">
      <div
        v-for="layoutOption in visibleLayouts"
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
            :display-config="displayFor(layoutOption)"
            :rows="isLiveMode ? undefined : fixtureRows"
            :columns="isLiveMode ? undefined : fixtureColumns"
            :schema-id="isLiveMode ? schemaId : undefined"
            :table-id="isLiveMode ? tableId : undefined"
            :filter="isLiveMode ? filter : undefined"
            :page-size="pageSize"
            :link-to="linkTo"
          />
        </div>

        <div
          class="bg-footer surface-inverted p-4"
          :data-testid="panelTestId(layoutOption, 'footer')"
        >
          <p class="mb-2 text-title font-bold">
            A colour the theme owns (footer)
          </p>
          <DisplayDataList
            :display-config="displayFor(layoutOption)"
            :rows="isLiveMode ? undefined : fixtureRows"
            :columns="isLiveMode ? undefined : fixtureColumns"
            :schema-id="isLiveMode ? schemaId : undefined"
            :table-id="isLiveMode ? tableId : undefined"
            :filter="isLiveMode ? filter : undefined"
            :page-size="pageSize"
            :link-to="linkTo"
          />
        </div>

        <div
          :data-testid="panelTestId(layoutOption, 'transparent')"
          class="surface-inverted p-4"
        >
          <p class="mb-2 text-title font-bold">
            The page's own gradient (no background class, surface-inverted)
          </p>
          <DisplayDataList
            :display-config="displayFor(layoutOption)"
            :rows="isLiveMode ? undefined : fixtureRows"
            :columns="isLiveMode ? undefined : fixtureColumns"
            :schema-id="isLiveMode ? schemaId : undefined"
            :table-id="isLiveMode ? tableId : undefined"
            :filter="isLiveMode ? filter : undefined"
            :page-size="pageSize"
            :link-to="linkTo"
          />
        </div>
      </div>
    </div>
  </div>
</template>
