<template>
  <div>
    <MatchTypeRadiobutton
      v-if="showMatchTypeSelector"
      class="p-2 pb-0"
      :matchTypeForFilter="facetIdentifier"
    />

    <div class="d-flex flex-column scrollable-content pt-2">
      <CheckboxComponent
        v-for="(option, index) of checkboxOptions"
        :key="index"
        v-model="filterSelection"
        :option="option"
      />
    </div>
    <div>
      <button
        v-if="selectAll"
        type="button"
        class="btn btn-link p-2"
        @click.prevent="toggleSelect"
      >
        {{ selectAllText }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import MatchTypeRadiobutton from "./base/MatchTypeRadiobutton.vue";
import CheckboxComponent from "./base/CheckboxComponent.vue";
import { useSettingsStore } from "../../stores/settingsStore";
import { useFiltersStore } from "../../stores/filtersStore";
import { ref, watch, computed, onMounted } from "vue";

const settingsStore = useSettingsStore();
const filtersStore = useFiltersStore();

const {
  facetIdentifier,
  options,
  currentlyActive,
  optionsFilter,
  selectAll,
  showMatchTypeSelector,
} = withDefaults(
  defineProps<{
    facetIdentifier: string;
    options: Function;
    currentlyActive?: boolean;
    optionsFilter?: any[];
    selectAll?: boolean;
    showMatchTypeSelector?: boolean;
  }>(),
  { currentlyActive: false, selectAll: false, showMatchTypeSelector: false }
);

const resolvedOptions = ref<any[]>([]);
const reducedOptions = ref<any[]>([]);

onMounted(() => {
  reducedOptions.value = optionsFilter || [];
  options().then((response: any[]) => {
    resolvedOptions.value = response;
  });
});

const uiText = computed(() => {
  return settingsStore.uiText;
});

const selectAllText = computed(() => {
  if (filterSelection.value?.length) {
    return uiText.value["deselect_all"];
  } else {
    return uiText.value["select_all"];
  }
});

const checkboxOptions = computed(() => {
  if (reducedOptions.value.length) {
    const selectedValues =
      filterSelection.value?.map((selection: any) => selection.value) || [];

    return resolvedOptions.value.filter(
      (option) =>
        reducedOptions.value.includes(option.value) ||
        selectedValues.includes(option.value)
    );
  } else {
    return resolvedOptions.value;
  }
});

const filterSelection = computed({
  get() {
    return filtersStore.getFilterValue(facetIdentifier) || [];
  },
  set(value) {
    filtersStore.updateFilter(facetIdentifier, value);
  },
});

watch(
  () => optionsFilter,
  (newValue) => {
    if (!currentlyActive || !filterSelection.value.length) {
      reducedOptions.value = newValue || [];
    }
  }
);

function toggleSelect() {
  if (filterSelection.value?.length) {
    filterSelection.value = [];
  } else {
    filterSelection.value = checkboxOptions;
  }
}
</script>

<style scoped>
.btn-link:focus {
  box-shadow: none;
}
</style>
