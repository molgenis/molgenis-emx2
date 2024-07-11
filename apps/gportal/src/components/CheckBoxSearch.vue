<template>
  <fieldset :id="id" class="checkbox-group-search">
    <legend>{{ label }}</legend>
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
            :id="`${id}-${row[idColumn]}`"
            :value="row[labelColumn] || row[valueColumn]"
            :name="`${tableId}-checkbox-group`"
            v-model="selections"
          />
          <label :for="`${id}-${row[idColumn]}`">{{
            row[labelColumn] || row[valueColumn]
          }}</label>
        </div>
        <button
          :id="`${id}-checkbox-options-show-more`"
          @click="getCheckBoxOptions()"
          class="btn btn-primary"
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
import { InputSearch, LoadingScreen } from "molgenis-viz";
import gql from "graphql-tag";
import { request } from "graphql-request";
import { XMarkIcon } from "@heroicons/vue/24/outline";

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
  searchInputLabel: "Search for options",
  limit: 8,
});

const loading = ref<boolean>(false);
const error = ref<Error | boolean>(false);
const searchTerm = ref<string>("");
const referenceData = ref([]);
const selections = ref<string[]>([]);
const showLimit = ref<number>(props.limit);

function removeFilter(value) {
  selections.value = selections.value.filter(
    (selection) => selection !== value
  );
}

async function fetchData() {
  const cols = props.columns.join(" ");
  const query = `query {
    ${props.tableId}(search:"${searchTerm.value}", limit: ${showLimit.value}) {
      ${cols}
      }
    }`;
  const response = await request("../api/graphql", query);
  const data = response[props.tableId];
  referenceData.value = data;
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

    label {
      margin-left: 0.5em;
    }
  }
}
</style>
