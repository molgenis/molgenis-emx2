<script setup lang="ts">
import { computed, ref, watch } from "vue";
import ModalContentContainer from "../../ModalContentContainer.vue";
import Field from "../../Field.vue";
import Button from "../../Button.vue";
import type { UseFilters } from "../../../../types/filters";
import type { IColumn } from "../../../../../metadata-utils/src/types";
import { useDownloadColumns } from "../../../composables/useDownloadColumns";
const props = withDefaults(
  defineProps<{
    schemaId: string;
    tableId: string;
    filters: UseFilters | null;
    columns?: IColumn[];
    visibleColumns?: IColumn[];
    isRowSelectionActive?: boolean;
  }>(),
  {
    columns: () => [],
    visibleColumns: () => [],
    isRowSelectionActive: false,
  }
);

type apiTypes = "csv" | "zip" | "excel" | "jsonld" | "ttl";

const downloadHref = defineModel<string>("downloadHref");
const selectedType = ref<apiTypes>("csv");
const isFilterInclusionEnabled = computed(
  () =>
    (props.filters?.activeFilters.value.length ?? 0) > 0 &&
    isFilteredApiType.value
);
const applyFilterToDownload = ref(true);
const applyColumnSelectionToDownload = ref(true);
const applyRowSelectionToDownload = ref(true);

const isFilteredApiType = computed(() =>
  ["csv", "excel"].includes(selectedType.value)
);

const { isColumnSelectionActive, appendColumnsParam } = useDownloadColumns(
  computed(() => props.columns),
  computed(() => props.visibleColumns)
);
const isColumnFilterActive = computed(
  () => isColumnSelectionActive.value && isFilteredApiType.value
);

const href = computed(() => {
  const queryParams = new URLSearchParams();
  if (
    isFilteredApiType.value &&
    isFilterInclusionEnabled.value &&
    applyFilterToDownload.value
  ) {
    queryParams.append(
      "filter",
      JSON.stringify(props.filters?.gqlFilter.value ?? {})
    );
  }
  if (isColumnFilterActive.value && applyColumnSelectionToDownload.value) {
    appendColumnsParam(queryParams);
  }
  if (props.isRowSelectionActive && applyRowSelectionToDownload.value) {
    queryParams.append(
      "applyRowSelection",
      String(applyRowSelectionToDownload.value)
    );
  }
  return `/${props.schemaId}/api/${selectedType.value}/${props.tableId}${
    queryParams.size > 0 ? "?" : ""
  }${queryParams.toString()}`;
});

const isCopied = ref(false);

async function copyDownloadLink() {
  await navigator.clipboard.writeText(
    new URL(href.value, window.location.origin).href
  );
  isCopied.value = true;
  setTimeout(() => (isCopied.value = false), 3000);
}

watch(
  href,
  (newHref) => {
    downloadHref.value = newHref;
  },
  { immediate: true }
);
</script>
<template>
  <ModalContentContainer class="flex-1 flex flex-col gap-8">
    <div id="download-format">
      <Field
        id="download-radio-input-group"
        label="Download format"
        v-model="selectedType"
        type="RADIO"
        :options="[
          { value: 'csv', label: 'CSV' },
          { value: 'excel', label: 'Excel' },
          { value: 'zip', label: 'Zip' },
          { value: 'jsonld', label: 'JSON LD' },
          { value: 'ttl', label: 'TTL' },
        ]"
      />
    </div>

    <fieldset id="download-options" class="flex flex-col gap-4">
      <legend class="sr-only">Download options</legend>
      <Field
        id="apply-filter-to-download-boolean-input"
        label="Filters"
        description="If enabled, the active filters will be applied to the download."
        v-model="applyFilterToDownload"
        type="BOOL"
        :disabled="!isFilterInclusionEnabled"
        :showClearButton="false"
      />

      <Field
        id="apply-column-selection-to-download-boolean-input"
        label="Column selection"
        description="If enabled, only the columns currently visible in the table will be included in the download."
        v-model="applyColumnSelectionToDownload"
        type="BOOL"
        :disabled="!isColumnFilterActive"
        :showClearButton="false"
      />

      <Field
        id="apply-row-selection-to-download-boolean-input"
        label="Selected rows"
        description="If enabled, only the rows currently selected in the table will be included in the download."
        v-model="applyRowSelectionToDownload"
        type="BOOL"
        :disabled="!isRowSelectionActive"
        :showClearButton="false"
      />
    </fieldset>

    <div id="download-link" class="mt-auto">
      <span id="download-link-label" class="text-title-contrast font-bold">
        Download link
      </span>
      <div
        class="flex items-center gap-2 w-full h-input pl-3 pr-1 rounded-alt bg-input text-input"
      >
        <a
          :href="downloadHref"
          :title="downloadHref"
          aria-labelledby="download-link-label"
          target="_blank"
          class="flex-1 truncate min-w-0 text-link hover:underline"
        >
          {{ downloadHref }}
        </a>
        <Button
          id="download-link-copy-button"
          class="shrink-0"
          type="inline"
          size="small"
          :icon-only="true"
          :icon="isCopied ? 'check' : 'content-copy'"
          :label="isCopied ? 'Copied' : 'Copy download link'"
          @click="copyDownloadLink"
        />
      </div>
    </div>
  </ModalContentContainer>
</template>
