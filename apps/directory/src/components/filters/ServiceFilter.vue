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
import { computed, onMounted, ref, toRefs } from "vue";
import { useFiltersStore } from "../../stores/filtersStore";
import { useSettingsStore } from "../../stores/settingsStore";
import CheckboxComponent from "./base/CheckboxComponent.vue";
import MatchTypeRadiobutton from "./base/MatchTypeRadiobutton.vue";
import { IFilterOption } from "../../interfaces/interfaces";

const filtersStore = useFiltersStore();
const settingsStore = useSettingsStore();

const props = withDefaults(
  defineProps<{
    facetIdentifier: string;
    options: Function;
    currentlyActive?: boolean;
    optionsFilter?: IOption[];
    showMatchTypeSelector?: boolean;
  }>(),
  { showMatchTypeSelector: false }
);

const { facetIdentifier, options, showMatchTypeSelector } = toRefs(props);

const resolvedOptions = ref<IOption[]>([]);
const groupedOptions = ref<Record<string, IOption[]>>({});
const groupLabels = ref<Record<string, string>>({});

onMounted(() => {
  options.value().then((response: IOption[]) => {
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

const filterSelection = computed({
  get() {
    return filtersStore.getFilterValue(facetIdentifier.value) || [];
  },
  set(filters) {
    const filterOptions: IFilterOption[] = filters.map((filter: IOption) => {
      return { text: filter.label, value: filter.value };
    });
    filtersStore.updateFilter(facetIdentifier.value, filterOptions);
  },
});

const selectAllText = computed(() => {
  if (filterSelection.value?.length) {
    return settingsStore.uiText["deselect_all"];
  } else {
    return settingsStore.uiText["select_all"];
  }
});

function toggleSelect() {
  if (filterSelection.value?.length) {
    filterSelection.value = [];
  } else {
    filterSelection.value = resolvedOptions;
  }
}

interface IOption {
  label: string;
  value: string;
  extraAttributes: {
    "serviceCategory.name": string;
    "serviceCategory.label": string;
  };
}
</script>

<style scoped>
.btn-link:focus {
  box-shadow: none;
}
</style>
