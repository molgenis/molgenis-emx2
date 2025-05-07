<template>
  <div>
    <MatchTypeRadiobutton
      v-if="showMatchTypeSelector"
      class="p-2 pb-0"
      :matchTypeForFilter="facetIdentifier"
    />

    <div class="d-flex flex-column scrollable-content pt-2">
      <div v-for="groupName of Object.keys(groupedOptions)">
        <div>
          <b>{{ groupLabels[groupName] }}</b>
        </div>
        <div v-for="(option, index) of groupedOptions[groupName]">
          <CheckboxComponent
            :key="index + groupName"
            v-model="filterSelection"
            :option="option"
          />
        </div>
      </div>
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
import * as _ from "lodash";

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
    optionsFilter?: IOption[];
    selectAll?: boolean;
    showMatchTypeSelector?: boolean;
  }>(),
  { currentlyActive: false, selectAll: false, showMatchTypeSelector: false }
);

const resolvedOptions = ref<IOption[]>([]);
const reducedOptions = ref<IOption[]>(optionsFilter || []);
const groupedOptions = ref<Record<string, IOption[]>>({});
const groupLabels = ref<Record<string, string>>({});

onMounted(() => {
  options().then((response: IOption[]) => {
    resolvedOptions.value = response;
    groupedOptions.value = resolvedOptions.value.reduce(
      (accum: Record<string, IOption[]>, option) => {
        const name = option.extraAttributes["serviceCategory.name"];
        const label = option.extraAttributes["serviceCategory.label"];
        if (!accum[name]) {
          accum[name] = [];
          groupLabels.value[name] = label;
        }
        accum[name].push(option);
        return accum;
      },
      {}
    );
  });
  Object.keys(groupedOptions.value).forEach((key) => {
    groupedOptions.value[key] = groupedOptions.value[key].sort();
  });
});

const selectAllText = computed(() => {
  if (filterSelection.value?.length) {
    return settingsStore.uiText.value["deselect_all"];
  } else {
    return settingsStore.uiText.value["select_all"];
  }
});

const checkboxOptions = computed(() =>
  createCheckboxOptions(resolvedOptions.value, reducedOptions.value)
);

function createCheckboxOptions(options: IOption[], reducedOptions: IOption[]) {
  if (reducedOptions.length) {
    const selectedValues =
      filterSelection.value?.map((selection: IOption) => selection.value) || [];

    return options.filter(
      (option: IOption) =>
        reducedOptions.find((op: IOption) => op.value === option.value) ||
        selectedValues.includes(option.value)
    );
  } else {
    return options;
  }
}

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

interface IOption {
  label: string;
  value: string;
  extraAttributes: {
    "serviceCategory.name": string;
    "serviceCategory.label": string;
  };
  parent?: IOption;
  children?: IOption[];
}
</script>

<style scoped>
.btn-link:focus {
  box-shadow: none;
}
</style>
