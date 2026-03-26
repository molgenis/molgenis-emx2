<script setup lang="ts">
import { ref, computed, useId } from "vue";
import type { IListConfig } from "../../../types/types";
import { useTableData } from "../../composables/useTableData";
import InputSearch from "../input/Search.vue";
import LoadingContent from "../LoadingContent.vue";
import ListView from "./ListView.vue";

const props = defineProps<{
  schemaId: string;
  tableId: string;
  filter?: object;
  config?: IListConfig;
}>();

const searchInputId = useId();
const page = ref(1);
const searchTerms = ref("");

const { metadata, rows, status, totalPages, showPagination, errorMessage } =
  useTableData(props.schemaId, props.tableId, {
    pageSize: props.config?.pageSize || 10,
    page,
    filter: computed(() => props.filter),
    searchTerms,
  });

const columns = computed(() => metadata.value?.columns || []);

const errorText = computed(
  () =>
    errorMessage.value ||
    (status.value === "error" ? "Failed to load data" : undefined)
);
</script>

<template>
  <div>
    <InputSearch
      v-if="config?.showSearch && showPagination"
      :id="searchInputId"
      v-model="searchTerms"
      placeholder="Search..."
      size="small"
      class="mb-4"
    />
    <LoadingContent
      :id="`list-${schemaId}-${tableId}`"
      :status="status"
      loading-text="Loading..."
      :error-text="errorText"
      :show-slot-on-error="false"
    >
      <ListView
        :rows="rows"
        :columns="columns"
        :config="config"
        :total-pages="totalPages"
        :current-page="page"
        :show-pagination="showPagination"
        @update:page="page = $event"
      />
    </LoadingContent>
  </div>
</template>
