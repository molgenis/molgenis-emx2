<template>
  <fieldset :id="id" class="checkbox-group-search" @submit.prevent>
    <legend class="visually-hidden">{{ label }}</legend>
    <div class="btn-filter-well" v-if="selections.length">
      <template v-for="selection in selections">
        <button
          :id="`${id}-filter-${selection}-btn`"
          class="btn btn-outline-primary"
          @click="removeFilter(selection)"
        >
          <span>{{ selection }}</span>
          <XMarkIcon width="16" height="16" />
        </button>
      </template>
    </div>
    <div class="group-search">
      <InputSearch
        :id="`${id}-checkbox-group-search`"
        :label="searchInputLabel"
        @search="searchTerm = $event"
      />
    </div>
    <div class="checkbox-options">
      <template v-if="referenceData !== undefined">
        <div v-for="row in referenceData" class="checkbox-option">
          <input
            type="checkbox"
            :id="`${id}-${row[idColumn as keyof OntologyDataIF]}`"
            :value="row[labelColumn as keyof OntologyDataIF] || row[valueColumn as keyof OntologyDataIF]"
            :name="`${tableId}-checkbox-group`"
            v-model="selections"
            @change="onChange"
          />
          <label :for="`${id}-${row[idColumn as keyof OntologyDataIF]}`">
            {{
              row[labelColumn as keyof OntologyDataIF] ||
              row[valueColumn as keyof OntologyDataIF]
            }}
          </label>
        </div>
        <button
          :id="`${id}-checkbox-options-show-more`"
          @click="getCheckBoxOptions()"
          class="btn btn-primary"
          @submit.prevent
        >
          <span>Show more</span>
        </button>
      </template>
      <span v-else>No results found</span>
    </div>
  </fieldset>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from "vue";

// @ts-ignore
import { InputSearch } from "molgenis-viz";
import gql from "graphql-tag";
import { request } from "graphql-request";
import { XMarkIcon } from "@heroicons/vue/24/outline";
import type { OntologyDataIF } from "../interfaces";

interface CheckBoxGroupSearchIF {
  id: string;
  label: string;
  searchInputLabel?: string;
  tableId: string;
  idColumn: string;
  valueColumn: string;
  labelColumn: string;
  columns: string[];
  limit?: number;
}

const props = withDefaults(defineProps<CheckBoxGroupSearchIF>(), {
  searchInputLabel: "Search for terms",
  limit: 8,
});

const emit = defineEmits<{
  (e: "refDataLoaded", value: OntologyDataIF[]): void;
  (e: "change", value: string[]): void;
}>();

function onReferenceDataUpdate() {
  emit("refDataLoaded", referenceData.value);
}

function onChange() {
  emit("change", selections.value);
}

const selections = ref<string[]>([]);
const loading = ref<boolean>(false);
const error = ref<Error | boolean>(false);
const searchTerm = ref<string>("");
const referenceData = ref<OntologyDataIF[]>([]);
const showLimit = ref<number>(props.limit);

function removeFilter(value: string) {
  selections.value = selections.value.filter(
    (selection) => selection !== value
  );
  onChange();
}

async function fetchData() {
  const cols = props.columns.join(" ");
  const query = gql`query {
    ${props.tableId}(search:"${searchTerm.value}", limit: ${showLimit.value}) {
      ${cols}
      }
    }`;
  const response: Record<string, any> = await request("../api/graphql", query);
  const data = response[props.tableId];
  referenceData.value = data;
  onReferenceDataUpdate();
}

async function getCheckBoxOptions() {
  loading.value = false;
  await fetchData()
    .catch((err) => (error.value = err))
    .finally(() => (loading.value = false));
  showLimit.value = showLimit.value * 2;
}

onMounted(async () => getCheckBoxOptions());

watch([searchTerm], async () => getCheckBoxOptions());
</script>

<style lang="scss">
.checkbox-group-search {
  .btn-filter-well {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5em;
    .btn.btn-outline-primary {
      font-size: 0.9rem;
      padding: 0.4em;
      margin: 0;

      svg {
        $iconSize: 18px;
        width: $iconSize;
        height: $iconSize;
        margin-left: 0.25em;
      }
    }
  }

  legend {
    font-size: 1.25rem;
  }
  .checkbox-options {
    padding: 0.5em;
    max-height: 200px;
    overflow-y: scroll;
    border: 1px solid $gray-200;
    border-radius: 4px;
    box-shadow: $box-shadow-inset;

    .checkbox-option {
      display: flex;
      justify-content: flex-start;
      align-items: flex-start;
      gap: 0.5em;

      label {
        word-wrap: break-word;
        line-height: 1;
        font-size: 1rem;
      }
    }
  }
}
</style>
