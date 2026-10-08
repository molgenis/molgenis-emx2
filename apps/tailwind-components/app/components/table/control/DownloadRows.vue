<script setup lang="ts">
import { computed, ref, watch } from "vue";
import ModalContentContainer from "../../ModalContentContainer.vue";
import Field from "../../Field.vue";
import type { UseFilters } from "../../../../types/filters";
const props = withDefaults(
  defineProps<{
    schemaId: string;
    tableId: string;
    filters: UseFilters | null;
    isColumnFilterActive?: boolean;
    isRowSelectionActive?: boolean;
  }>(),
  {
    isRowFilterActive: false,
    isColumnFilterActive: false,
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

watch(
  [
    selectedType,
    applyFilterToDownload,
    applyColumnSelectionToDownload,
    applyRowSelectionToDownload,
  ],
  () => {
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
    if (props.isColumnFilterActive && applyColumnSelectionToDownload.value) {
      queryParams.append(
        "applyColumnSelection",
        String(applyColumnSelectionToDownload.value)
      );
    }
    if (props.isRowSelectionActive && applyRowSelectionToDownload.value) {
      queryParams.append(
        "applyRowSelection",
        String(applyRowSelectionToDownload.value)
      );
    }
    downloadHref.value = `/${props.schemaId}/api/${selectedType.value}/${
      props.tableId
    }${queryParams.size > 0 ? "?" : ""}${queryParams.toString()}`;
  },
  { immediate: true }
);
</script>
<template>
  <ModalContentContainer>
    <div class="flex flex-col gap-4">
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

      <Field
        id="apply-filter-to-download-boolean-input"
        label="Apply filters to download"
        v-model="applyFilterToDownload"
        type="BOOL"
        :disabled="!isFilterInclusionEnabled"
        :showClearButton="false"
      />

      <Field
        id="apply-column-selection-to-download-boolean-input"
        label="Apply column selection to download"
        v-model="applyColumnSelectionToDownload"
        type="BOOL"
        :disabled="!isColumnFilterActive"
        :showClearButton="false"
      />

      <Field
        id="apply-row-selection-to-download-boolean-input"
        label="Apply row selection to download"
        v-model="applyRowSelectionToDownload"
        type="BOOL"
        :disabled="!isRowSelectionActive"
        :showClearButton="false"
      />

      <div>
        <p class="text-sm text-muted">Download link:</p>
        <a
          :href="downloadHref"
          target="_blank"
          class="text-sm text-link hover:underline break-all"
        >
          {{ downloadHref }}
        </a>
      </div>
    </div>
  </ModalContentContainer>
</template>
